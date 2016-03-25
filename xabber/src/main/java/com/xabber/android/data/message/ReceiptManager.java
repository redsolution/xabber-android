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
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.realm.MessageItem;

import org.greenrobot.eventbus.EventBus;
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

import io.realm.Realm;

/**
 * Manage message receive receipts as well as error replies.
 *
 * @author alexander.ivanov
 */
public class ReceiptManager implements OnPacketListener, ReceiptReceivedListener {

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
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {
                DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(ReceiptManager.this);
                DeliveryReceiptManager.getInstanceFor(connection).autoAddDeliveryReceiptRequests();
            }
        });

    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress, Stanza packet) {
        if (!(connection instanceof AccountItem)) {
            return;
        }
        final String account = ((AccountItem) connection).getAccount();
        final String user = packet.getFrom();
        if (user == null) {
            return;
        }
        if (!(packet instanceof Message)) {
            return;
        }
        final Message message = (Message) packet;
        if (message.getType() == Message.Type.error) {
            markAsError(account, message);
        } else {
            // TODO setDefaultAutoReceiptMode should be used
            for (ExtensionElement packetExtension : message.getExtensions()) {
                if (packetExtension instanceof DeliveryReceiptRequest) {
                    String id = message.getPacketID();
                    if (id == null) {
                        continue;
                    }
                    Message receipt = new Message(user);
                    receipt.addExtension(new DeliveryReceipt(id));
                    // the key problem is Thread - smack does not keep it in auto reply
                    receipt.setThread(message.getThread());
                    try {
                        ConnectionManager.getInstance().sendStanza(account, receipt);
                    } catch (NetworkException e) {
                        LogManager.exception(this, e);
                    }
                }
            }
        }
    }

    private void markAsError(final String account, final Message message) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        MessageItem first = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account)
                .equalTo(MessageItem.Fields.STANZA_ID, message.getStanzaId()).findFirst();
        if (first != null) {
            first.setError(true);
        }
        realm.commitTransaction();
        realm.close();
        EventBus.getDefault().post(new MessageUpdateEvent(account));
    }

    @Override
    public void onReceiptReceived(String fromJid, final String toJid, final String receiptId, Stanza stanza) {
        DeliveryReceipt receipt = DeliveryReceipt.from((Message) stanza);

        if (receipt == null) {
            return;
        }

        markAsDelivered(toJid, receiptId);
    }

    private void markAsDelivered(final String toJid, final String receiptId) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        MessageItem first = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.STANZA_ID, receiptId).findFirst();
        if (first != null) {
            first.setDelivered(true);
        }
        realm.commitTransaction();
        realm.close();
        EventBus.getDefault().post(new MessageUpdateEvent());
    }
}
