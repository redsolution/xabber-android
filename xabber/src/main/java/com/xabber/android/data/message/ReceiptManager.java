/*
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
import com.xabber.android.data.extension.groups.GroupsManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.OnMessageUpdatedListener;
import com.xabber.xmpp.groups.GroupExtensionElement;
import com.xabber.xmpp.sid.UniqueIdsHelper;

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
        DeliveryReceiptManager.setDefaultAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.disabled);
    }

    public static ReceiptManager getInstance() {
        if (instance == null) instance = new ReceiptManager();

        return instance;
    }

    private ReceiptManager() {
        XMPPConnectionRegistry.addConnectionCreationListener(connection ->
                DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(ReceiptManager.this));
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (!(connection instanceof AccountItem)) return;

        final AccountJid account = ((AccountItem) connection).getAccount();
        final Jid from = packet.getFrom();

        if (!(packet instanceof Message) || from == null) return;

        final Message message = (Message) packet;

        boolean isGroup = message.hasExtension(GroupExtensionElement.ELEMENT, GroupsManager.SYSTEM_MESSAGE_NAMESPACE)
                || message.hasExtension(GroupExtensionElement.ELEMENT, GroupExtensionElement.NAMESPACE);

        if (message.getType() == Message.Type.error) {
            if (message.getError().getCondition().equals(XMPPError.Condition.service_unavailable)){
                LogManager.e(LOG_TAG, "Got service unavailable error to message! But message status <b>WAS NOT<b> "
                        + "set to error");
            } else Application.getInstance().runInBackground(() -> markAsError(account, message));
        } else {
            for (ExtensionElement packetExtension : message.getExtensions()) {
                if (packetExtension instanceof DeliveryReceiptRequest) {
                    String id = UniqueIdsHelper.getStanzaIdBy(
                            message, isGroup ? from.asBareJid().toString() : account.getBareJid().toString());

                    if (id == null) continue;

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
                    first.setMessageStatus(MessageStatus.ERROR);
                    XMPPError error = message.getError();
                    if (error != null) {
                        String errorStr = error.toString();
                        String descr = error.getDescriptiveText();
                        first.setErrorDescription(errorStr + "\n" + descr);
                    }
                }
            });
            for (OnMessageUpdatedListener listener :
                    Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                listener.onMessageUpdated();
            }
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close(); }
    }

    @Override
    public void onReceiptReceived(Jid fromJid, final Jid toJid, final String receiptId, Stanza stanza) {
        DeliveryReceipt receipt = DeliveryReceipt.from((Message) stanza);

        if (receipt == null) return;

        Application.getInstance().runInBackground(() -> markAsReceived(toJid, receiptId));
    }

    private void markAsReceived(final Jid toJid, final String receiptId) {
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            realm.executeTransaction(realm1 -> {
                MessageRealmObject first = realm1.where(MessageRealmObject.class)
                        .equalTo(MessageRealmObject.Fields.STANZA_ID, receiptId).findFirst();
                first.setMessageStatus(MessageStatus.RECEIVED);
            });

            for (OnMessageUpdatedListener listener
                    : Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                listener.onMessageUpdated();
            }
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally {
            if (realm != null && Looper.getMainLooper() != Looper.getMainLooper()) realm.close();
        }
    }

}
