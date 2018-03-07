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
package com.xabber.android.data.extension.cs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.NestedNestedMaps;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.receiver.ComposingPausedReceiver;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.Calendar;
import java.util.Map;

/**
 * Provide information about chat state.
 *
 * @author alexander.ivanov
 */
public class ChatStateManager implements OnDisconnectListener,
        OnPacketListener, OnCloseListener {

    private static ChatStateManager instance;

    private static final int PAUSE_TIMEOUT = 30 * 1000;

    private static final long REMOVE_STATE_DELAY = 10 * 1000;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
                    @Override
                    public void connectionCreated(final XMPPConnection connection) {
                        ServiceDiscoveryManager.getInstanceFor(connection)
                                .addFeature("http://jabber.org/protocol/chatstates");
                    }
                });
    }

    public static ChatStateManager getInstance() {
        if (instance == null) {
            instance = new ChatStateManager();
        }

        return instance;
    }

    /**
     * Chat states for lower cased resource for bareAddress in account.
     */
    private final NestedNestedMaps<Resourcepart, ChatState> chatStates;

    /**
     * Cleaners for chat states for lower cased resource for bareAddress in
     * account.
     */
    private final NestedNestedMaps<Resourcepart, Runnable> stateCleaners;

    /**
     * Information about chat state notification support for lower cased
     * resource for bareAddress in account.
     */
    private final NestedNestedMaps<Resourcepart, Boolean> supports;

    /**
     * Sent chat state notifications for bareAddress in account.
     */
    private final NestedMap<ChatState> sent;

    /**
     * Scheduled pause intents for bareAddress in account.
     */
    private final NestedMap<PendingIntent> pauseIntents;

    /**
     * Alarm manager.
     */
    private final AlarmManager alarmManager;

    /**
     * Handler for clear states on timeout.
     */
    private final Handler handler;

    private ChatStateManager() {
        chatStates = new NestedNestedMaps<>();
        stateCleaners = new NestedNestedMaps<>();
        supports = new NestedNestedMaps<>();
        sent = new NestedMap<>();
        pauseIntents = new NestedMap<>();
        alarmManager = (AlarmManager) Application.getInstance()
                .getSystemService(Context.ALARM_SERVICE);
        handler = new Handler();
    }

    /**
     * Returns best information chat state for specified bare address.
     *
     * @return <code>null</code> if there is no available information.
     */
    public ChatState getChatState(AccountJid account, UserJid bareAddress) {
        Map<Resourcepart, ChatState> map = chatStates.get(account.toString(), bareAddress.toString());
        if (map == null) {
            return null;
        }
        ChatState chatState = null;
        for (ChatState check : map.values()) {
            if (chatState == null || check.compareTo(chatState) < 0) {
                chatState = check;
            }
        }
        return chatState;
    }

    /**
     * Whether sending chat notification for specified chat is supported.
     */
    private boolean isSupported(AbstractChat chat, boolean outgoingMessage) {
        if (chat instanceof RoomChat)
            return false;
        Jid to = chat.getTo();
        BareJid bareAddress = to.asBareJid();
        Resourcepart resource = to.getResourceOrNull();
        Map<Resourcepart, Boolean> map = supports.get(chat.getAccount().toString(), bareAddress.toString());
        if (map != null) {
            if (resource!= null && !resource.equals(Resourcepart.EMPTY)) {
                Boolean value = map.get(resource);
                if (value != null)
                    return value;
            } else {
                if (outgoingMessage)
                    return true;
                for (Boolean value : map.values())
                    if (value != null && value)
                        return true;
            }
        }
        return outgoingMessage;
    }

    /**
     * Update outgoing message before sending.
     */
    public void updateOutgoingMessage(AbstractChat chat, Message message) {
        if (!isSupported(chat, true)) {
            return;
        }
        message.addExtension(new ChatStateExtension(ChatState.active));
        sent.put(chat.getAccount().toString(), chat.getUser().toString(), ChatState.active);
        cancelPauseIntent(chat.getAccount(), chat.getUser());
    }

    /**
     * Update chat state information and send message if necessary.
     */
    private void updateChatState(AccountJid account, UserJid user,
                                 ChatState chatState) {
        if (!SettingsManager.chatsStateNotification()
                || sent.get(account.toString(), user.toString()) == chatState) {
            return;
        }
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        if (chat == null || !isSupported(chat, false)) {
            return;
        }
        sent.put(chat.getAccount().toString(), chat.getUser().toString(), chatState);
        Message message = new Message();
        message.setType(chat.getType());
        message.setTo(chat.getTo());
        message.addExtension(new ChatStateExtension(chatState));
        try {
            StanzaSender.sendStanza(account, message);
        } catch (NetworkException e) {
            // Just ignore it.
        }
    }

    /**
     * Cancel pause intent from the schedule.
     */
    private void cancelPauseIntent(AccountJid account, UserJid user) {
        PendingIntent pendingIntent = pauseIntents.remove(account.toString(), user.toString());
        if (pendingIntent != null)
            alarmManager.cancel(pendingIntent);
    }

    /**
     * Must be call each time user change text message.
     */
    public void onComposing(AccountJid account, UserJid user, CharSequence text) {
        cancelPauseIntent(account, user);
        if (text.length() == 0) {
            updateChatState(account, user, ChatState.active);
            return;
        } else {
            updateChatState(account, user, ChatState.composing);
        }
        Intent intent = ComposingPausedReceiver.createIntent(
                Application.getInstance(), account, user);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                Application.getInstance(), 0, intent, 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MILLISECOND, PAUSE_TIMEOUT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                pendingIntent);
        pauseIntents.put(account.toString(), user.toString(), pendingIntent);
    }

    public void onPaused(AccountJid account, UserJid user) {
        if (account == null || user == null)
            return;
        if (sent.get(account.toString(), user.toString()) != ChatState.composing) {
            return;
        }

        updateChatState(account, user, ChatState.paused);
        pauseIntents.remove(account.toString(), user.toString());
    }

    @Override
    public void onDisconnect(ConnectionItem connection) {
        if (!(connection instanceof AccountItem))
            return;
        AccountJid account = ((AccountItem) connection).getAccount();
        chatStates.clear(account.toString());
        for (Map<Resourcepart, Runnable> map : stateCleaners.getNested(account.toString()).values()) {
            for (Runnable runnable : map.values()) {
                handler.removeCallbacks(runnable);
            }
        }
        stateCleaners.clear(account.toString());
        supports.clear(account.toString());
        sent.clear(account.toString());
        for (PendingIntent pendingIntent : pauseIntents.getNested(account.toString()).values()) {
            alarmManager.cancel(pendingIntent);
        }
        pauseIntents.clear(account.toString());
    }

    private void removeCallback(AccountJid account, BareJid bareAddress, Resourcepart resource) {
        Runnable runnable = stateCleaners.remove(account.toString(), bareAddress.toString(), resource);
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (stanza.getFrom() == null) {
            return;
        }

        final Resourcepart resource = stanza.getFrom().getResourceOrNull();
        if (resource == null) {
            return;
        }

        final AccountJid account = ((AccountItem) connection).getAccount();
        final UserJid bareUserJid;
        try {
            bareUserJid = UserJid.from(stanza.getFrom()).getBareUserJid();
        } catch (UserJid.UserJidCreateException e) {
            return;
        }

        if (stanza instanceof Presence) {
            Presence presence = (Presence) stanza;
            if (presence.getType() != Type.unavailable) {
                return;
            }
            chatStates.remove(account.toString(), bareUserJid.toString(), resource);
            removeCallback(account, bareUserJid.getBareJid(), resource);
            supports.remove(account.toString(), bareUserJid.toString(), resource);
        } else if (stanza instanceof Message) {
            boolean support = false;
            for (ExtensionElement extension : stanza.getExtensions())
                if (extension instanceof ChatStateExtension) {
                    removeCallback(account, bareUserJid.getBareJid(), resource);
                    ChatState chatState = ((ChatStateExtension) extension).getChatState();
                    chatStates.put(account.toString(), bareUserJid.toString(), resource, chatState);
                    if (chatState != ChatState.active) {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                if (this != stateCleaners.get(account.toString(), bareUserJid.toString(), resource)) {
                                    return;
                                }
                                chatStates.remove(account.toString(), bareUserJid.toString(), resource);
                                removeCallback(account, bareUserJid.getBareJid(), resource);
                                RosterManager.onChatStateChanged(account, bareUserJid);
                            }
                        };
                        handler.postDelayed(runnable, REMOVE_STATE_DELAY);
                        stateCleaners.put(account.toString(), bareUserJid.toString(), resource, runnable);
                    }
                    RosterManager.onChatStateChanged(account, bareUserJid);
                    support = true;
                    break;
                }
            Message message = (Message) stanza;
            if (message.getType() != Message.Type.chat
                    && message.getType() != Message.Type.groupchat) {
                return;
            }
            if (support) {
                supports.put(account.toString(), bareUserJid.toString(), resource, true);
            } else if (supports.get(account.toString(), bareUserJid.toString(), resource) == null) {
                // Disable only if there no information about support.
                supports.put(account.toString(), bareUserJid.toString(), resource, false);
            }
        }
    }

    @Override
    public void onClose() {
        for (PendingIntent pendingIntent : pauseIntents.values()) {
            alarmManager.cancel(pendingIntent);
        }
        pauseIntents.clear();
    }

}
