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
package com.xabber.android.data.message.chat;

import android.content.Intent;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.ForwardIdRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.xmpp.groups.GroupMemberExtensionElement;
import com.xabber.xmpp.groups.GroupchatExtensionElement;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.OTRUnencryptedException;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.extension.references.ReferencesManager;
import com.xabber.android.data.extension.reliablemessagedelivery.TimeElement;
import com.xabber.android.data.extension.groups.GroupMemberManager;
import com.xabber.android.data.extension.groups.GroupsManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.ForwardManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUtils;
import com.xabber.android.data.xaccount.XMPPAuthManager;
import com.xabber.android.ui.OnNewIncomingMessageListener;
import com.xabber.android.utils.StringUtils;
import com.xabber.xmpp.sid.UniqueStanzaHelper;

import net.java.otr4j.OtrException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
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


    public RegularChat(AccountJid account, ContactJid user) {
        super(account, user);
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
            return JidCreate.fullFrom(contactJid.getJid().asEntityBareJidIfPossible(), OTRresource);
        } else {
            if (resource == null) {
                return contactJid.getJid();
            } else {
                return JidCreate.fullFrom(contactJid.getJid().asEntityBareJidIfPossible(), resource);
            }
        }
    }

    @Override
    public Type getType() {
        return Type.chat;
    }

    @Override
    public boolean canSendMessage() {
        if (super.canSendMessage()) {
            if (SettingsManager.securityOtrMode() != SettingsManager.SecurityOtrMode.required)
                return true;
            SecurityLevel securityLevel = OTRManager.getInstance()
                    .getSecurityLevel(account, contactJid);
            if (securityLevel != SecurityLevel.plain)
                return true;
            try {
                OTRManager.getInstance().startSession(account, contactJid);
            } catch (NetworkException e) {
            }
        }
        return false;
    }

    @Override
    protected String prepareText(String text) {
        text = super.prepareText(text);
        try {
            return OTRManager.getInstance().transformSending(account, contactJid, text);
        } catch (OtrException e) {
            LogManager.exception(this, e);
            return null;
        }
    }

    @Override
    public MessageRealmObject createNewMessageItem(String text) {
        String id = UUID.randomUUID().toString();
        return createMessageItem(null, text, null, null, null, false,
                false, false, false, id,
                id, null, null, null,
                account.getFullJid().toString(), false, null);
    }

    @Override
    public boolean onPacket(ContactJid bareAddress, Stanza packet, boolean isCarbons) {

        if (!super.onPacket(bareAddress, packet, isCarbons))
            return false;
        final Resourcepart resource = packet.getFrom().getResourceOrNull();
        if (packet instanceof Presence) {
            final Presence presence = (Presence) packet;

            if (this.resource != null && presence.getType() == Presence.Type.unavailable
                    && resource != null && this.resource.equals(resource)) {
                this.resource = null;
            }

            if (packet.hasExtension(GroupsManager.NAMESPACE)){
                return ChatManager.getInstance().convertRegularToGroup(bareAddress, packet, isCarbons, this);
            }

//            if (presence.getType() == Presence.Type.unavailable) {
//                OTRManager.getInstance().onContactUnAvailable(account, user);
//            }
        } else if (packet instanceof Message) {
            final Message message = (Message) packet;
            if (message.getType() == Message.Type.error)
                return true;

            String text = MessageUtils.getOptimalTextBody(message);
            if (text == null)
                return true;

            DelayInformation delayInformation = message.getExtension(DelayInformation.ELEMENT, DelayInformation.NAMESPACE);
            if (delayInformation != null && "Offline Storage".equals(delayInformation.getReason())) {
                return true;
            }

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
                    text = OTRManager.getInstance().transformReceiving(account, contactJid, text);
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

            RealmList<AttachmentRealmObject> attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(packet);

            String uid = UUID.randomUUID().toString();
            RealmList<ForwardIdRealmObject> forwardIdRealmObjects = parseForwardedMessage(true, packet, uid);
            String originalStanza = packet.toXML().toString();
            String originalFrom = packet.getFrom().toString();

            // forward comment (to support previous forwarded xep)
            String forwardComment = ForwardManager.parseForwardComment(packet);
            if (forwardComment != null) text = forwardComment;

            // System message received.
            if ((text == null || text.trim().equals("")) && (forwardIdRealmObjects == null || forwardIdRealmObjects.isEmpty()))
                return true;

            // modify body with references
            Pair<String, String> bodies = ReferencesManager.modifyBodyWithReferences(message, text);
            text = bodies.first;
            String markupText = bodies.second;
            Date timestamp = null;
            if (message.hasExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)){
                TimeElement timeElement = (TimeElement) message.getExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE);
                timestamp = StringUtils.parseReceivedReceiptTimestampString(timeElement.getStamp());
            }

            boolean isSystem = packet.hasExtension(GroupchatExtensionElement.ELEMENT,
                    GroupsManager.SYSTEM_MESSAGE_NAMESPACE);

            // create message with file-attachments
            if (attachmentRealmObjects.size() > 0)
                createAndSaveFileMessage(true, uid, resource, text, markupText, null,
                        timestamp, getDelayStamp(message), true, true, encrypted,
                        MessageManager.isOfflineMessage(account.getFullJid().getDomain(), packet),
                        getStanzaId(message), UniqueStanzaHelper.getOriginId(message), attachmentRealmObjects, originalStanza, null,
                        originalFrom, false, forwardIdRealmObjects,false, null);

                // create message without attachments
            else createAndSaveNewMessage(true, uid, resource, text, markupText, null,
                    timestamp, getDelayStamp(message), true, true, encrypted,
                    MessageManager.isOfflineMessage(account.getFullJid().getDomain(), packet),
                    getStanzaId(message), UniqueStanzaHelper.getOriginId(message), originalStanza, null,
                    originalFrom, false, forwardIdRealmObjects, false, null, isSystem);

            for (OnNewIncomingMessageListener listener :
                    Application.getInstance().getUIListeners(OnNewIncomingMessageListener.class)){
                listener.onNewIncomingMessage(account, contactJid);
            }
        }
        return true;
    }

    @Override
    protected String parseInnerMessage(boolean ui, Message message, Date timestamp, String parentMessageId) {
        if (message.getType() == Message.Type.error) return null;

        final Jid fromJid = message.getFrom();
        Resourcepart resource = null;
        if (fromJid != null) resource = fromJid.getResourceOrNull();
        String text = message.getBody();
        if (text == null) return null;

        boolean encrypted = OTRManager.getInstance().isEncrypted(text);

        RealmList<AttachmentRealmObject> attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(message);

        String uid = UUID.randomUUID().toString();
        RealmList<ForwardIdRealmObject> forwardIdRealmObjects = parseForwardedMessage(ui, message, uid);
        String originalStanza = message.toXML().toString();
        String originalFrom = "";
        if (fromJid != null) originalFrom = fromJid.toString();

        // groupchat
        String gropchatUserId = null;
        GroupMemberExtensionElement groupchatUser = ReferencesManager.getGroupchatUserFromReferences(message);
        if (groupchatUser != null) {
            gropchatUserId = groupchatUser.getId();
            GroupMemberManager.getInstance().saveGroupUser(groupchatUser, message.getFrom().asBareJid(), timestamp.getTime());
        }

        // forward comment (to support previous forwarded xep)
        String forwardComment = ForwardManager.parseForwardComment(message);
        if (forwardComment != null && !forwardComment.isEmpty()) text = forwardComment;

        // modify body with references
        Pair<String, String> bodies = ReferencesManager.modifyBodyWithReferences(message, text);
        text = bodies.first;
        String markupText = bodies.second;

        boolean isSystem = message.hasExtension(GroupchatExtensionElement.ELEMENT,
                GroupsManager.SYSTEM_MESSAGE_NAMESPACE);

        // create message with file-attachments
        if (attachmentRealmObjects.size() > 0)
            createAndSaveFileMessage(ui, uid, resource, text, markupText, null,
                    timestamp, getDelayStamp(message), true, false, encrypted,
                    false, getStanzaId(message), UniqueStanzaHelper.getOriginId(message), attachmentRealmObjects,
                    originalStanza, parentMessageId, originalFrom, true, forwardIdRealmObjects, true, gropchatUserId);

            // create message without attachments
        else createAndSaveNewMessage(ui, uid, resource, text, markupText, null,
                timestamp, getDelayStamp(message), true, false, encrypted,
                false, getStanzaId(message), UniqueStanzaHelper.getOriginId(message), originalStanza,
                parentMessageId, originalFrom, true, forwardIdRealmObjects, true, gropchatUserId, isSystem);

        return uid;
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        sendMessages();
    }
}
