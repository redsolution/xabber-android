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
package com.xabber.android.data.extension.attention;

import android.media.AudioManager;
import android.net.Uri;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatAction;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.android.data.notification.EntityNotificationProvider;
import com.xabber.android.data.notification.NotificationChannelUtils;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.attention.packet.AttentionExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * XEP-0224: Attention.
 *
 * @author alexander.ivanov
 */
public class AttentionManager implements OnPacketListener, OnLoadListener {

    @SuppressWarnings("WeakerAccess")
    final static Object enabledLock;

    private static AttentionManager instance;

    static {
        enabledLock = new Object();
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {
                synchronized (enabledLock) {
                    if (SettingsManager.chatsAttention())
                        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(AttentionExtension.NAMESPACE);
                }
            }
        });
    }

    private final EntityNotificationProvider<AttentionRequest> attentionRequestProvider = new EntityNotificationProvider<AttentionRequest>(
            R.drawable.ic_stat_error, NotificationChannelUtils.DEFAULT_ATTENTION_CHANNEL_ID) {

        @Override
        public Uri getSound() {
            return SettingsManager.chatsAttentionSound();
        }

        @Override
        public int getStreamType() {
            return AudioManager.STREAM_RING;
        }

    };

    public static AttentionManager getInstance() {
        if (instance == null) {
            instance = new AttentionManager();
        }

        return instance;
    }

    private AttentionManager() {
    }

    public void onSettingsChanged() {
        synchronized (enabledLock) {
            for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                if (accountItem == null) {
                    continue;
                }
                ServiceDiscoveryManager manager = ServiceDiscoveryManager.getInstanceFor(accountItem.getConnection());
                if (manager == null) {
                    continue;
                }
                boolean contains = false;
                for (String feature : manager.getFeatures()) {
                    if (AttentionExtension.NAMESPACE.equals(feature)) {
                        contains = true;
                    }
                }
                if (SettingsManager.chatsAttention() == contains) {
                    continue;
                }
                if (SettingsManager.chatsAttention()) {
                    manager.addFeature(AttentionExtension.NAMESPACE);
                } else {
                    manager.removeFeature(AttentionExtension.NAMESPACE);
                }
            }
            AccountManager.getInstance().resendPresence();
        }
    }

    @Override
    public void onLoad() {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded();
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void onLoaded() {
        NotificationManager.getInstance().registerNotificationProvider(
                attentionRequestProvider);
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (!(stanza instanceof Message)) {
            return;
        }
        if (!SettingsManager.chatsAttention()) {
            return;
        }
        final AccountJid account = connection.getAccount();

        ContactJid from;
        try {
            from = ContactJid.from(stanza.getFrom());
        } catch (ContactJid.UserJidCreateException e) {
            e.printStackTrace();
            return;
        }

        for (ExtensionElement packetExtension : stanza.getExtensions()) {
            if (packetExtension instanceof AttentionExtension) {
                ChatManager.getInstance().openChat(account, from);
                ChatManager.getInstance()
                        .getChat(account, from)
                        .newAction(null,
                                Application.getInstance().getApplicationContext().getString(R.string.action_attention_requested),
                                ChatAction.attention_requested);
                attentionRequestProvider.add(new AttentionRequest(account, from.getBareUserJid()), true);
            }
        }
    }

    public void sendAttention(AccountJid account, ContactJid user) throws NetworkException {
        AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        if (!(chat instanceof RegularChat)) {
            throw new NetworkException(R.string.ENTRY_IS_NOT_FOUND);
        }
        Jid to = chat.getTo();
        if (to.getResourceOrNull() == null || to.getResourceOrNull().equals(Resourcepart.EMPTY)) {
            final Presence presence = RosterManager.getInstance().getPresence(account, user);
            if (presence == null) {
                to = null;
            } else {
                to = presence.getFrom();
            }
        }

        if (to == null) {
            throw new NetworkException(R.string.ENTRY_IS_NOT_AVAILABLE);
        }

        if (!CapabilitiesManager.getInstance().isFeatureSupported(to, AttentionExtension.NAMESPACE)) {
            throw new NetworkException(R.string.ATTENTION_IS_NOT_SUPPORTED);
        }

        Message message = new Message();
        message.setTo(to);
        message.setType(Message.Type.headline);
        message.addExtension(new AttentionExtension());
        StanzaSender.sendStanza(account, message);
        chat.newAction(null,
                Application.getInstance().getApplicationContext().getString(R.string.action_attention_called),
                ChatAction.attention_called);
    }

    public void removeAccountNotifications(AccountJid accountJid, ContactJid contactJid) {
        LogManager.i(this, "removeAccountNotifications " + contactJid);
        attentionRequestProvider.remove(accountJid, contactJid.getBareUserJid());
    }
}
