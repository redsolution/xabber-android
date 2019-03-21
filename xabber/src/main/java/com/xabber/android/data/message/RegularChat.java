/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.message;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.ForwardId;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.chat_markers.ChatMarkerManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.OTRUnencryptedException;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.xaccount.XMPPAuthManager;

import net.java.otr4j.OtrException;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Domainpart;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmList;

/**
 * Represents normal chat.
 *
 * @author alexander.ivanov
 */
public class RegularChat extends AbstractChat {

    /**
     * Resource used for contact.
     */
    private Resourcepart resource;
    private Resourcepart OTRresource;
    private Intent intent;


    RegularChat(AccountJid account, UserJid user, boolean isPrivateMucChat) {
        super(account, user, isPrivateMucChat);
        resource = null;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public Resourcepart getOTRresource() {
        return OTRresource;
    }

    public void setOTRresource(Resourcepart OTRresource) {
        this.OTRresource = OTRresource;
    }

    public Resourcepart getResource() {
        return resource;
    }

    @NonNull
    @Override
    public Jid getTo() {
        if (OTRresource != null) {
            return JidCreate.fullFrom(user.getJid().asEntityBareJidIfPossible(), OTRresource);
        } else {
            if (resource == null
                    || (MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible()) && getType() != Message.Type.groupchat)) {
                return user.getJid();
            } else {
                return JidCreate.fullFrom(user.getJid().asEntityBareJidIfPossible(), resource);
            }
        }
    }

    @Override
    public Type getType() {
        return Type.chat;
    }

    @Override
    protected boolean canSendMessage() {
        if (super.canSendMessage()) {
            if (SettingsManager.securityOtrMode() != SettingsManager.SecurityOtrMode.required)
                return true;
            SecurityLevel securityLevel = OTRManager.getInstance()
                    .getSecurityLevel(account, user);
            if (securityLevel != SecurityLevel.plain)
                return true;
            try {
                OTRManager.getInstance().startSession(account, user);
            } catch (NetworkException e) {
            }
        }
        return false;
    }

    @Override
    protected String prepareText(String text) {
        text = super.prepareText(text);
        try {
            return OTRManager.getInstance().transformSending(account, user, text);
        } catch (OtrException e) {
            LogManager.exception(this, e);
            return null;
        }
    }

    @Override
    protected MessageItem createNewMessageItem(String text) {
        return createMessageItem(null, text, null, null, false,
                false, false, false, UUID.randomUUID().toString(),
                null, null, null,
                account.getFullJid().toString(), null, false);
    }

    @Override
    protected boolean onPacket(UserJid bareAddress, Stanza packet, boolean isCarbons) {
        if (!super.onPacket(bareAddress, packet, isCarbons))
            return false;
        final Resourcepart resource = packet.getFrom().getResourceOrNull();
        if (packet instanceof Presence) {
            final Presence presence = (Presence) packet;

            if (this.resource != null && presence.getType() == Presence.Type.unavailable
                    && resource != null && this.resource.equals(resource)) {
                this.resource = null;
            }

//            if (presence.getType() == Presence.Type.unavailable) {
//                OTRManager.getInstance().onContactUnAvailable(account, user);
//            }
        } else if (packet instanceof Message) {
            final Message message = (Message) packet;
            if (message.getType() == Message.Type.error)
                return true;

            MUCUser mucUser = MUCUser.from(message);
            if (mucUser != null && mucUser.getInvite() != null)
                return true;

            String text = message.getBody();
            if (text == null)
                return true;

            // Xabber service message received
            if (message.getType() == Type.headline) {
                if (XMPPAuthManager.getInstance().isXabberServiceMessage(message.getStanzaId()))
                    return true;
            }

            String thread = message.getThread();
            updateThreadId(thread);

            if (resource != null && !resource.equals(Resourcepart.EMPTY)) {
                this.resource = resource;
            }

            boolean encrypted = OTRManager.getInstance().isEncrypted(text);

            if (!isCarbons) {
                try {
                    text = OTRManager.getInstance().transformReceiving(account, user, text);
                } catch (OtrException e) {
                    if (e.getCause() instanceof OTRUnencryptedException) {
                        text = ((OTRUnencryptedException) e.getCause()).getText();
                        encrypted = false;
                    } else {
                        LogManager.exception(this, e);
                        // Invalid message received.
                        return true;
                    }
                }
            }

            RealmList<Attachment> attachments = HttpFileUploadManager.parseFileMessage(packet);

            String uid = UUID.randomUUID().toString();
            RealmList<ForwardId> forwardIds = parseForwardedMessage(true, packet, uid);
            String originalStanza = packet.toXML().toString();
            String originalFrom = packet.getFrom().toString();
            String forwardComment = ForwardManager.parseForwardComment(packet);
            if (forwardComment != null) text = forwardComment;

            // System message received.
            if ((text == null || text.trim().equals("")) && (forwardIds == null || forwardIds.isEmpty()))
                return true;

            // create message with file-attachments
            if (attachments.size() > 0)
                createAndSaveFileMessage(true, uid, resource, text, null, getDelayStamp(message),
                        true, true, encrypted,
                        isOfflineMessage(account.getFullJid().getDomain(), packet),
                        packet.getStanzaId(), attachments, originalStanza, null,
                        originalFrom, false, false);

                // create message without attachments
            else createAndSaveNewMessage(true, uid, resource, text, null, getDelayStamp(message),
                    true, true, encrypted,
                    isOfflineMessage(account.getFullJid().getDomain(), packet),
                    packet.getStanzaId(), originalStanza, null,
                    originalFrom, forwardIds,false, false);

            EventBus.getDefault().post(new NewIncomingMessageEvent(account, user));
        }
        return true;
    }

    @Override
    protected String parseInnerMessage(boolean ui, Message message, String parentMessageId) {
        if (message.getType() == Message.Type.error) return null;

        MUCUser mucUser = MUCUser.from(message);
        if (mucUser != null && mucUser.getInvite() != null) return null;

        final Jid fromJid = message.getFrom();
        Resourcepart resource = null;
        if (fromJid != null) resource = fromJid.getResourceOrNull();
        String text = message.getBody();
        if (text == null) return null;

        boolean encrypted = OTRManager.getInstance().isEncrypted(text);

        RealmList<Attachment> attachments = HttpFileUploadManager.parseFileMessage(message);

        String uid = UUID.randomUUID().toString();
        RealmList<ForwardId> forwardIds = parseForwardedMessage(ui, message, uid);
        String originalStanza = message.toXML().toString();
        String originalFrom = "";
        if (fromJid != null) originalFrom = fromJid.toString();
        boolean fromMuc = message.getType().equals(Type.groupchat);
        String forwardComment = ForwardManager.parseForwardComment(message);
        if (forwardComment != null && !forwardComment.isEmpty())
            text = forwardComment;

        // create message with file-attachments
        if (attachments.size() > 0)
            createAndSaveFileMessage(ui, uid, resource, text, null, null, true,
                    false, encrypted, false, message.getStanzaId(), attachments,
                    originalStanza, parentMessageId, originalFrom, fromMuc, true);

            // create message without attachments
        else createAndSaveNewMessage(ui, uid, resource, text, null, null, true,
                false, encrypted, false, message.getStanzaId(), originalStanza,
                parentMessageId, originalFrom, forwardIds, fromMuc, true);

        return uid;
    }

    /**
     * @return Whether message was delayed by server.
     */
    public static boolean isOfflineMessage(Domainpart server, Stanza stanza) {
        DelayInformation delayInformation = DelayInformation.from(stanza);

        return delayInformation != null
                && TextUtils.equals(delayInformation.getFrom(), server);
    }

    public static Date getDelayStamp(Message message) {
        DelayInformation delayInformation = DelayInformation.from(message);
        if (delayInformation != null) {
            return delayInformation.getStamp();
        } else {
            return null;
        }
    }


    @Override
    protected void onComplete() {
        super.onComplete();
        sendMessages();
    }
}
