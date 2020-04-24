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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.NestedNestedMaps;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.receiver.ComposingPausedReceiver;
import com.xabber.xmpp.uuu.ChatState;
import com.xabber.xmpp.uuu.ChatStateExtension;
import com.xabber.xmpp.uuu.ChatStateSubtype;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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

    private static final long REMOVE_COMPOSING_STATE_DELAY = 15 * 1000;
    private static final long REMOVE_PAUSED_STATE_DELAY = 0;
    private static final long SEND_REPEATED_COMPOSING_STATE_DELAY = 5 * 1000;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
                    @Override
                    public void connectionCreated(final XMPPConnection connection) {
                        ServiceDiscoveryManager.getInstanceFor(connection)
                                .addFeature("http://jabber.org/protocol/chatstates");
                        ServiceDiscoveryManager.getInstanceFor(connection)
                                .addFeature("https://xabber.com/protocol/extended-chatstates");
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
    private final HashMap<String, ChatStateSubtype> chatStateSubtypes;

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
    /**
     * Handler for sending composing states.
     */
    private final Handler stateSenderHandler;
    private ArrayList<Runnable> stateSenders;

    private ChatStateManager() {
        chatStates = new NestedNestedMaps<>();
        chatStateSubtypes = new HashMap<>();
        stateCleaners = new NestedNestedMaps<>();
        supports = new NestedNestedMaps<>();
        sent = new NestedMap<>();
        pauseIntents = new NestedMap<>();
        alarmManager = (AlarmManager) Application.getInstance()
                .getSystemService(Context.ALARM_SERVICE);
        handler = new Handler();
        stateSenderHandler = new Handler();
        stateSenders = new ArrayList<>();
    }

    /**
     * Returns best information chat state for specified bare address.
     *
     * @return <code>null</code> if there is no available information.
     */
    private ChatState getChatState(AccountJid account, ContactJid bareAddress) {
        Map<Resourcepart, ChatState> map = chatStates.get(account.toString(), bareAddress.toString());
        if (map == null) {
            return null;
        }
        ChatState chatState = null;
        for (ChatState check : map.values()) {
            if (chatState == null || check.compareTo(chatState) > 0) {
                chatState = check;
            }
        }
        return chatState;
    }

    private ChatStateSubtype getChatSubstate(AccountJid account, ContactJid bareAddress) {
        String key = account.toString() + bareAddress.toString();
        return chatStateSubtypes.get(key);
    }

    public String getFullChatStateString(AccountJid account, ContactJid bareAddress) {
        ChatState chatState = getChatState(account, bareAddress);
        ChatStateSubtype chatStateSubtype = getChatSubstate(account, bareAddress);
        String chatStateString = null;
        if (chatState == ChatState.composing) {
            if (chatStateSubtype == null) {
                chatStateString = Application.getInstance().getString(R.string.chat_state_composing);
            } else {
                switch (chatStateSubtype) {
                    case voice:
                        chatStateString = Application.getInstance().getString(R.string.chat_state_composing_voice);
                        break;
                    case video:
                        chatStateString = Application.getInstance().getString(R.string.chat_state_composing_video);
                        break;
                    case upload:
                        chatStateString = Application.getInstance().getString(R.string.chat_state_composing_upload);
                        break;
                    default:
                        chatStateString = Application.getInstance().getString(R.string.chat_state_composing);
                        break;
                }
            }
        }
        return chatStateString;
    }

    /**
     * Whether sending chat notification for specified chat is supported.
     */
    private boolean isSupported(AbstractChat chat, boolean outgoingMessage) {
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
        cancelComposingSender();
    }

    /**
     * Update chat state information and send message if necessary.
     */
    private void updateChatState(AccountJid account, ContactJid user,
                                 ChatState chatState) {
        updateChatState(account, user, chatState, null);
    }

    private void updateChatState(final AccountJid account, final ContactJid user,
                                 final ChatState chatState, final ChatStateSubtype type) {
        if (!SettingsManager.chatsStateNotification()
                || sent.get(account.toString(), user.toString()) == chatState) {
            return;
        }
        final AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        if (chat == null || !isSupported(chat, false)) {
            return;
        }

        sent.put(chat.getAccount().toString(), chat.getUser().toString(), chatState);

        cancelComposingSender();

        Message message = new Message();
        message.setType(chat.getType());
        message.setTo(chat.getTo());
        message.addExtension(new ChatStateExtension(chatState, type));
        //try {
        StanzaSender.sendStanzaAsync(account, message);
        if (chatState == ChatState.composing) {
            setComposingSender(chat, chatState, type);
        } else {
            cancelComposingSender();
        }
        //} catch (NetworkException e) {
        //    sent.remove(chat.getAccount().toString(), chat.getUser().toString());
        //}
    }

    private void setComposingSender(final AbstractChat chat, final ChatState chatState, final ChatStateSubtype type) {
        Runnable stateSender = new Runnable() {
            @Override
            public void run() {
                Message message = new Message();
                message.setType(chat.getType());
                message.setTo(chat.getTo());
                message.addExtension(new ChatStateExtension(chatState, type));
                //try {
                StanzaSender.sendStanzaAsync(chat.getAccount(), message);
                //} catch (NetworkException e) {
                //    // Just ignore it.
                //}
                stateSenderHandler.postDelayed(this, SEND_REPEATED_COMPOSING_STATE_DELAY);
            }
        };
        stateSenders.add(stateSender);
        stateSenderHandler.postDelayed(stateSender, SEND_REPEATED_COMPOSING_STATE_DELAY);
    }

    /**
     * Cancel pause intent from the schedule.
     */
    private void cancelPauseIntent(AccountJid account, ContactJid user) {
        PendingIntent pendingIntent = pauseIntents.remove(account.toString(), user.toString());
        if (pendingIntent != null)
            alarmManager.cancel(pendingIntent);
    }

    public void cancelComposingSender() {
        if (!stateSenders.isEmpty()) {
            for (Runnable sender : stateSenders) {
                stateSenderHandler.removeCallbacks(sender);
            }
        }
        stateSenders.clear();
    }

    public void onChatOpening(AccountJid account, ContactJid user) {
        if (!SettingsManager.chatsStateNotification()
                || sent.get(account.toString(), user.toString()) == ChatState.active) {
            return;
        }
        final AbstractChat chat = ChatManager.getInstance().getChat(account, user);

        Message message = new Message();
        message.setType(chat.getType());
        message.setTo(chat.getTo());
        message.addExtension(new ChatStateExtension(ChatState.active));
        //try {
        StanzaSender.sendStanzaAsync(account, message);
        sent.put(chat.getAccount().toString(), chat.getUser().toString(), ChatState.active);
        //} catch (NetworkException e) {
        //    // Just ignore it.
        //}
    }

    /**
     * Must be call each time user change text message.
     */
    public void onComposing(AccountJid account, ContactJid user, CharSequence text) {
        onComposing(account, user, text, null);
    }

    public void onComposing(AccountJid account, ContactJid user, CharSequence text, ChatStateSubtype type) {
        cancelPauseIntent(account, user);
        if (text != null && text.length() == 0 && type == null) {
            updateChatState(account, user, ChatState.active);
            return;
        } else {
            updateChatState(account, user, ChatState.composing, type);
        }
        if (type == null) {
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
    }

    public void onPaused(AccountJid account, ContactJid user) {
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
        chatStateSubtypes.clear();
        for (Map<Resourcepart, Runnable> map : stateCleaners.getNested(account.toString()).values()) {
            for (Runnable runnable : map.values()) {
                handler.removeCallbacks(runnable);
            }
        }
        stateCleaners.clear(account.toString());
        cancelComposingSender();
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
        final ContactJid bareContactJid;
        try {
            bareContactJid = ContactJid.from(stanza.getFrom()).getBareUserJid();
        } catch (ContactJid.UserJidCreateException e) {
            return;
        }

        if (stanza instanceof Presence) {
            Presence presence = (Presence) stanza;
            if (presence.getType() != Type.unavailable) {
                return;
            }
            chatStates.remove(account.toString(), bareContactJid.toString(), resource);
            chatStateSubtypes.remove(account.toString() + bareContactJid.toString());
            removeCallback(account, bareContactJid.getBareJid(), resource);
            supports.remove(account.toString(), bareContactJid.toString(), resource);
        } else if (stanza instanceof Message) {
            boolean support = false;
            for (ExtensionElement extension : stanza.getExtensions())
                if (extension instanceof ChatStateExtension) {
                    removeCallback(account, bareContactJid.getBareJid(), resource);
                    ChatState chatState = ((ChatStateExtension) extension).getChatState();
                    ChatStateSubtype subtype = ((ChatStateExtension) extension).getType();
                    chatStates.put(account.toString(), bareContactJid.toString(), resource, chatState);

                    if (chatState == ChatState.composing) chatStateSubtypes.put(account.toString() + bareContactJid.toString(), subtype);
                    if (chatState != ChatState.active) {
                        final ChatState finalState = chatState;
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                if (this != stateCleaners.get(account.toString(), bareContactJid.toString(), resource)) {
                                    return;
                                }
                                chatStates.remove(account.toString(), bareContactJid.toString(), resource);
                                if (finalState == ChatState.paused)
                                    chatStateSubtypes.remove(account.toString() + bareContactJid.toString());
                                removeCallback(account, bareContactJid.getBareJid(), resource);
                                RosterManager.onChatStateChanged(account, bareContactJid);
                            }
                        };
                        if (chatState == ChatState.composing)
                            handler.postDelayed(runnable, REMOVE_COMPOSING_STATE_DELAY);
                        else
                            handler.postDelayed(runnable, REMOVE_PAUSED_STATE_DELAY);
                        stateCleaners.put(account.toString(), bareContactJid.toString(), resource, runnable);
                    }
                    RosterManager.onChatStateChanged(account, bareContactJid);
                    support = true;
                    break;
                }
            Message message = (Message) stanza;
            if (message.getType() != Message.Type.chat
                    && message.getType() != Message.Type.groupchat) {
                return;
            }
            if (support) {
                supports.put(account.toString(), bareContactJid.toString(), resource, true);
                if (!SettingsManager.chatsStateNotification()
                        || sent.get(account.toString(), bareContactJid.toString()) != null) {
                    return;
                }
                onChatOpening(account, bareContactJid);
            } else if (supports.get(account.toString(), bareContactJid.toString(), resource) == null) {
                // Disable only if there no information about support.
                supports.put(account.toString(), bareContactJid.toString(), resource, false);
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

    public void processCarbonsMessage(AccountJid account, final Message message, CarbonExtension.Direction direction) {
        if (direction == CarbonExtension.Direction.sent) {
            for (ExtensionElement extension : message.getExtensions())
                if (extension instanceof ChatStateExtension) {
                    ChatState chatState = ((ChatStateExtension) extension).getChatState();
                    if (chatState == ChatState.active || chatState == ChatState.composing) {
                        AccountManager.getInstance().startGracePeriod(account);
                    }
                    break;
                }
        }
    }

}
