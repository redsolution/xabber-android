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

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.OTRUnencryptedException;

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


    RegularChat(AccountJid account, UserJid user, boolean isPrivateMucChat) {
        super(account, user, isPrivateMucChat);
        resource = null;
    }

    public Resourcepart getResource() {
        return resource;
    }

    @NonNull
    @Override
    public Jid getTo() {
        if (resource == null
                || (MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible()) && getType() != Message.Type.groupchat )) {
            return user.getJid();
        } else {
            return JidCreate.fullFrom(user.getJid().asEntityBareJidIfPossible(), resource);
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
        return createMessageItem(
                null,
                text,
                null,
                null,
                false,
                false,
                false,
                false,
                null);
    }

    @Override
    protected boolean onPacket(UserJid bareAddress, Stanza packet) {
        if (!super.onPacket(bareAddress, packet))
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

            String thread = message.getThread();
            updateThreadId(thread);
            boolean encrypted = OTRManager.getInstance().isEncrypted(text);
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
            // System message received.
            if (text == null || text.trim().equals(""))
                return true;
            if (resource != null && !resource.equals(Resourcepart.EMPTY)) {
                this.resource = resource;
            }
            createAndSaveNewMessage(
                    resource,
                    text,
                    null,
                    getDelayStamp(message),
                    true,
                    true,
                    encrypted,
                    isOfflineMessage(account.getFullJid().getDomain(), packet),
                    packet.getStanzaId());
            EventBus.getDefault().post(new NewIncomingMessageEvent(account, user));
        }
        return true;
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
