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

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.entity.NestedMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;

/**
 * Manage message receive receipts as well as error replies.
 *
 * @author alexander.ivanov
 */
public class ReceiptManager implements OnPacketListener, OnDisconnectListener, ReceiptReceivedListener {

    /**
     * Sent messages for packet ids in accounts.
     */
    private final NestedMap<MessageItem> sent;

    private final static ReceiptManager instance;

    static {
        instance = new ReceiptManager();
        Application.getInstance().addManager(instance);

        // TODO: change to ifSubscribed when isSubscribedToMyPresence will work and problem with thread element will be solved
        DeliveryReceiptManager.setDefaultAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.disabled);

    }

    public static ReceiptManager getInstance() {
        return instance;
    }

    private ReceiptManager() {
        sent = new NestedMap<>();

        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {
                DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(ReceiptManager.this);
                DeliveryReceiptManager.getInstanceFor(connection).autoAddDeliveryReceiptRequests();
            }
        });

    }

    /**
     * Update outgoing message before sending.
     *
     * @param abstractChat
     * @param message
     * @param messageItem
     */
    public void updateOutgoingMessage(AbstractChat abstractChat,
                                      Message message, MessageItem messageItem) {
        sent.put(abstractChat.getAccount(), message.getStanzaId(), messageItem);
    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress, Stanza packet) {
        if (!(connection instanceof AccountItem))
            return;
        String account = ((AccountItem) connection).getAccount();
        final String user = packet.getFrom();
        if (user == null)
            return;
        if (!(packet instanceof Message))
            return;
        final Message message = (Message) packet;
        if (message.getType() == Message.Type.error) {
            final MessageItem messageItem = sent.remove(account,
                    message.getPacketID());
            if (messageItem != null && !messageItem.isError()) {
                messageItem.markAsError();
                Application.getInstance().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (messageItem.getId() != null)
                            MessageTable.getInstance().markAsError(
                                    messageItem.getId());
                    }
                });
                MessageManager.getInstance().onChatChanged(
                        messageItem.getChat().getAccount(),
                        messageItem.getChat().getUser(), false);
            }
        } else {
            // TODO setDefaultAutoReceiptMode should be used
            for (ExtensionElement packetExtension : message.getExtensions())
                if (packetExtension instanceof DeliveryReceiptRequest) {
                    String id = message.getPacketID();
                    if (id == null)
                        continue;
                    Message receipt = new Message(user);
                    receipt.addExtension(new DeliveryReceipt(id));
                    // the key problem is Thread - smack does not keep it in auto reply
                    receipt.setThread(message.getThread());
                    try {
                        ConnectionManager.getInstance().sendStanza(account,
                                receipt);
                    } catch (NetworkException e) {
                        LogManager.exception(this, e);
                    }
                }
        }
    }

    @Override
    public void onDisconnect(ConnectionItem connection) {
        if (!(connection instanceof AccountItem))
            return;
        String account = ((AccountItem) connection).getAccount();
        sent.clear(account);
    }

    @Override
    public void onReceiptReceived(String fromJid, String toJid, String receiptId, Stanza stanza) {
        DeliveryReceipt receipt = stanza.getExtension(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE);

        if (receipt == null) {
            return;
        }

        final MessageItem messageItem = sent.remove(toJid, receipt.getId());
        if (messageItem != null && !messageItem.isDelivered()) {
            messageItem.markAsDelivered();
            MessageManager.getInstance().onChatChanged(
                    messageItem.getChat().getAccount(),
                    messageItem.getChat().getUser(), false);
        }
    }
}
