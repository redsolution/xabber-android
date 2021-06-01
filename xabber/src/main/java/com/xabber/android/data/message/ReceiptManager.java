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

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jxmpp.jid.Jid;

import io.realm.Realm;

/**
 * Manage message receive receipts as well as error replies.
 *
 * @author alexander.ivanov
 */
public class ReceiptManager implements OnPacketListener, ReceiptReceivedListener {

    private static final String LOG_TAG = ReceiptManager.class.getSimpleName();

    private static ReceiptManager instance;

    static {
        // TODO: change to ifSubscribed when isSubscribedToMyPresence will work and problem with thread element will be solved
        DeliveryReceiptManager.setDefaultAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.disabled);

    }

    public static ReceiptManager getInstance() {
        if (instance == null) {
            instance = new ReceiptManager();
        }

        return instance;
    }

    private ReceiptManager() {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {
                DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(ReceiptManager.this);
                //DeliveryReceiptManager.getInstanceFor(connection).autoAddDeliveryReceiptRequests();
            }
        });

    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (!(connection instanceof AccountItem)) {
            return;
        }
        final AccountJid account = ((AccountItem) connection).getAccount();
        final Jid from = packet.getFrom();
        if (from == null) {
            return;
        }
        if (!(packet instanceof Message)) {
            return;
        }
        final Message message = (Message) packet;
        if (message.getType() == Message.Type.error) {
            Application.getInstance().runInBackgroundUserRequest(() -> markAsError(account, message));

        } else {
            // TODO setDefaultAutoReceiptMode should be used
            for (ExtensionElement packetExtension : message.getExtensions()) {
                if (packetExtension instanceof DeliveryReceiptRequest) {
                    String id = AbstractChat.getStanzaId(message);
                    if (id == null) {
                        continue;
                    }
                    Message receipt = new Message(from);
                    receipt.addExtension(new DeliveryReceipt(id));
                    // the key problem is Thread - smack does not keep it in auto reply
                    receipt.setThread(message.getThread());
                    receipt.setType(Message.Type.chat);
                    try {
                        StanzaSender.sendStanza(account, receipt);
                    } catch (NetworkException e) {
                        LogManager.exception(this, e);
                    }
                }
            }
        }
    }

    private void markAsError(final AccountJid account, final Message message) {
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            realm.executeTransaction(realm1 -> {
                MessageRealmObject first = realm1.where(MessageRealmObject.class)
                        .equalTo(MessageRealmObject.Fields.ACCOUNT, account.toString())
                        .equalTo(MessageRealmObject.Fields.ORIGIN_ID, message.getStanzaId())
                        .findFirst();
                if (first != null) {
                    first.setError(true);
                    XMPPError error = message.getError();
                    if (error != null) {
                        String errorStr = error.toString();
                        String descr = error.getDescriptiveText();
                        first.setErrorDescription(errorStr + "\n" + descr);
                    }
                }
            });
            EventBus.getDefault().post(new MessageUpdateEvent(account));
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
    }

    @Override
    public void onReceiptReceived(Jid fromJid, final Jid toJid, final String receiptId, Stanza stanza) {
        DeliveryReceipt receipt = DeliveryReceipt.from((Message) stanza);

        if (receipt == null) {
            return;
        }

        Application.getInstance().runInBackground(() -> markAsDelivered(toJid, receiptId));
    }

    private void markAsDelivered(final Jid toJid, final String receiptId) {
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            realm.executeTransaction(realm1 -> {
                MessageRealmObject first = realm1.where(MessageRealmObject.class)
                        .equalTo(MessageRealmObject.Fields.STANZA_ID, receiptId).findFirst();
                first.setDelivered(true);
            });

            EventBus.getDefault().post(new MessageUpdateEvent());
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally { if (realm != null && Looper.getMainLooper() != Looper.getMainLooper()) realm.close(); }
    }
}
