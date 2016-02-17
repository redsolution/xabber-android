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

import com.xabber.android.data.LogManager;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.OTRUnencryptedException;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.delay.Delay;
import com.xabber.xmpp.muc.MUC;

import net.java.otr4j.OtrException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.muc.packet.MUCUser;

/**
 * Represents normal chat.
 *
 * @author alexander.ivanov
 */
public class RegularChat extends AbstractChat {

    /**
     * Resource used for contact.
     */
    private String resource;


    RegularChat(String account, String user, boolean isPrivateMucChat) {
        super(account, user, isPrivateMucChat);
        resource = null;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public String getTo() {
        if (resource == null
                || (MUCManager.getInstance().hasRoom(account, Jid.getBareAddress(user)) && getType() != Message.Type.groupchat )) {
            return user;
        } else {
            return user + "/" + resource;
        }
    }

    @Override
    public Type getType() {
        return Type.chat;
    }

//    @Override
//    protected boolean canSendMessage() {
//        if (super.canSendMessage()) {
//            if (SettingsManager.securityOtrMode() != SecurityOtrMode.required)
//                return true;
//            SecurityLevel securityLevel = OTRManager.getInstance()
//                    .getSecurityLevel(account, user);
//            if (securityLevel != SecurityLevel.plain)
//                return true;
//            try {
//                OTRManager.getInstance().startSession(account, user);
//            } catch (NetworkException e) {
//            }
//        }
//        return false;
//    }

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
    protected boolean onPacket(String bareAddress, Stanza packet) {
        if (!super.onPacket(bareAddress, packet))
            return false;
        final String resource = Jid.getResource(packet.getFrom());
        if (packet instanceof Presence) {
            final Presence presence = (Presence) packet;

            if (this.resource != null && presence.getType() == Presence.Type.unavailable
                    && this.resource.equals(resource)) {
                this.resource = null;
            }

            if (presence.getType() == Presence.Type.unavailable) {
                OTRManager.getInstance().onContactUnAvailable(account, user);
            }
        } else if (packet instanceof Message) {
            final Message message = (Message) packet;
            if (message.getType() == Message.Type.error)
                return true;

            MUCUser mucUser = MUC.getMUCUserExtension(message);
            if (mucUser != null && mucUser.getInvite() != null)
                return true;

            String text = message.getBody();
            if (text == null)
                return true;

            String thread = message.getThread();
            updateThreadId(thread);
            boolean unencrypted = false;
            try {
                text = OTRManager.getInstance().transformReceiving(account, user, text);
            } catch (OtrException e) {
                if (e.getCause() instanceof OTRUnencryptedException) {
                    text = ((OTRUnencryptedException) e.getCause()).getText();
                    unencrypted = true;
                } else {
                    LogManager.exception(this, e);
                    // Invalid message received.
                    return true;
                }
            }
            // System message received.
            if (text == null || text.trim().equals(""))
                return true;
            if (!"".equals(resource))
                this.resource = resource;
            createAndSaveNewMessage(
                    resource,
                    text,
                    null,
                    Delay.getDelay(message),
                    true,
                    true,
                    unencrypted,
                    Delay.isOfflineMessage(Jid.getServer(account), packet),
                    packet.getStanzaId());
        }
        return true;
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        sendMessages();
    }

}
