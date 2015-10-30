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
package com.xabber.android.data.extension.muc;

import android.database.Cursor;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.EntityNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.muc.MUC;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Manage multi user chats.
 * <p/>
 * Warning: We are going to remove SMACK components.
 *
 * @author alexander.ivanov
 */
public class MUCManager implements OnLoadListener, OnPacketListener {

    private final static MUCManager instance;

    static {
        instance = new MUCManager();
        Application.getInstance().addManager(instance);
    }

    private final EntityNotificationProvider<RoomInvite> inviteProvider;
    private final EntityNotificationProvider<RoomAuthorizationError> authorizationErrorProvider;

    private MUCManager() {
        inviteProvider = new EntityNotificationProvider<>(R.drawable.ic_stat_add_circle);
        authorizationErrorProvider = new EntityNotificationProvider<>(R.drawable.ic_stat_error);
    }

    public static MUCManager getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        final Collection<RoomChat> roomChats = new ArrayList<>();
        final Collection<RoomChat> needJoins = new ArrayList<>();
        Cursor cursor = RoomTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    RoomChat roomChat = new RoomChat(
                            RoomTable.getAccount(cursor), RoomTable.getRoom(cursor),
                            RoomTable.getNickname(cursor), RoomTable.getPassword(cursor));
                    if (RoomTable.needJoin(cursor)) {
                        needJoins.add(roomChat);
                    }
                    roomChats.add(roomChat);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(roomChats, needJoins);
            }
        });
    }

    private void onLoaded(Collection<RoomChat> roomChats, Collection<RoomChat> needJoins) {
        for (RoomChat roomChat : roomChats) {
            AbstractChat abstractChat = MessageManager.getInstance().getChat(
                    roomChat.getAccount(), roomChat.getUser());
            if (abstractChat != null) {
                MessageManager.getInstance().removeChat(abstractChat);
            }
            MessageManager.getInstance().addChat(roomChat);
            if (needJoins.contains(roomChat)) {
                roomChat.setState(RoomState.waiting);
            }
        }
        NotificationManager.getInstance().registerNotificationProvider(inviteProvider);
        NotificationManager.getInstance().registerNotificationProvider(authorizationErrorProvider);
    }

    /**
     * @return <code>null</code> if does not exists.
     */
    public RoomChat getRoomChat(String account, String room) {
        AbstractChat chat = MessageManager.getInstance().getChat(account, room);
        if (chat != null && chat instanceof RoomChat) {
            return (RoomChat) chat;
        }
        return null;
    }

    /**
     * @return Whether there is such room.
     */
    public boolean hasRoom(String account, String room) {
        return getRoomChat(account, room) != null;
    }

    public boolean isMucPrivateChat(String account, String user) {
        return hasRoom(account, Jid.getBareAddress(user)) && !"".equals(Jid.getResource(user));
    }

    /**
     * @return nickname or empty string if room does not exists.
     */
    public String getNickname(String account, String room) {
        RoomChat roomChat = getRoomChat(account, room);
        if (roomChat == null) {
            return "";
        }
        return roomChat.getNickname();
    }

    /**
     * @param account
     * @param room
     * @return password or empty string if room does not exists.
     */
    public String getPassword(String account, String room) {
        RoomChat roomChat = getRoomChat(account, room);
        if (roomChat == null) {
            return "";
        }
        return roomChat.getPassword();
    }

    /**
     * @return list of occupants or empty list.
     */
    public Collection<Occupant> getOccupants(String account, String room) {
        RoomChat roomChat = getRoomChat(account, room);
        if (roomChat == null) {
            return Collections.emptyList();
        }
        return roomChat.getOccupants();
    }

    /**
     * @return <code>null</code> if there is no such invite.
     */
    public RoomInvite getInvite(String account, String room) {
        return inviteProvider.get(account, room);
    }

    public void removeInvite(RoomInvite abstractRequest) {
        inviteProvider.remove(abstractRequest);
    }

    public void removeRoom(final String account, final String room) {
        removeInvite(getInvite(account, room));
        RoomChat roomChat = getRoomChat(account, room);
        if (roomChat == null) {
            return;
        }
        leaveRoom(account, room);
        MessageManager.getInstance().removeChat(roomChat);
        RosterManager.getInstance().onContactChanged(account, room);
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                RoomTable.getInstance().remove(account, room);
            }
        });
    }

    /**
     * Creates or updates existed room.
     *
     */
    public void createRoom(String account, String room, String nickname,
                           String password, boolean join) {
        removeInvite(getInvite(account, room));
        AbstractChat chat = MessageManager.getInstance().getChat(account, room);
        RoomChat roomChat;
        if (chat == null || !(chat instanceof RoomChat)) {
            if (chat != null) {
                MessageManager.getInstance().removeChat(chat);
            }
            roomChat = new RoomChat(account, room, nickname, password);
            MessageManager.getInstance().addChat(roomChat);
        } else {
            roomChat = (RoomChat) chat;
            roomChat.setNickname(nickname);
            roomChat.setPassword(password);
        }
        requestToWriteRoom(account, room, nickname, password, join);
        if (join) {
            joinRoom(account, room, true);
        }
    }

    private void requestToWriteRoom(final String account, final String room,
                                    final String nickname, final String password, final boolean join) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                RoomTable.getInstance().write(account, room, nickname,
                        password, join);
            }
        });
    }

    /**
     * @return Whether room is disabled.
     */
    public boolean isDisabled(final String account, final String room) {
        RoomChat roomChat = getRoomChat(account, room);
        return roomChat == null || roomChat.getState() == RoomState.unavailable;
    }

    /**
     * @return Whether connected is establish or connection is in progress.
     */
    public boolean inUse(final String account, final String room) {
        RoomChat roomChat = getRoomChat(account, room);
        return roomChat != null && roomChat.getState().inUse();
    }

    /**
     * Requests to join to the room.
     *
     * @param requested Whether user request to join the room.
     */
    public void joinRoom(final String account, final String room, boolean requested) {
        final XMPPConnection xmppConnection;
        final RoomChat roomChat;
        final String nickname;
        final String password;
        final Thread thread;
        roomChat = getRoomChat(account, room);
        if (roomChat == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            return;
        }
        RoomState state = roomChat.getState();
        if (state == RoomState.available || state == RoomState.occupation) {
            Application.getInstance().onError(R.string.ALREADY_JOINED);
            return;
        }
        if (state == RoomState.creating || state == RoomState.joining) {
            Application.getInstance().onError(R.string.ALREADY_IN_PROGRESS);
            return;
        }
        nickname = roomChat.getNickname();
        password = roomChat.getPassword();
        requestToWriteRoom(account, room, nickname, password, true);
        ConnectionThread connectionThread = AccountManager.getInstance()
                .getAccount(account).getConnectionThread();
        if (connectionThread == null) {
            Application.getInstance().onError(R.string.NOT_CONNECTED);
            return;
        }
        xmppConnection = connectionThread.getXMPPConnection();
        if (xmppConnection == null) {
            Application.getInstance().onError(R.string.NOT_CONNECTED);
            return;
        }
        final MultiUserChat multiUserChat;
        try {
            multiUserChat = MultiUserChatManager.getInstanceFor(xmppConnection).getMultiUserChat(room);
        } catch (IllegalStateException e) {
            Application.getInstance().onError(R.string.NOT_CONNECTED);
            return;
        }
        roomChat.setState(RoomState.joining);
        roomChat.setMultiUserChat(multiUserChat);
        roomChat.setRequested(requested);
        thread = new Thread("Join to room " + room + " from " + account) {
            @Override
            public void run() {
                try {
                    if (roomChat.getMultiUserChat() != multiUserChat) {
                        return;
                    }
                    multiUserChat.join(nickname, password);
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (roomChat.getMultiUserChat() != multiUserChat) {
                                return;
                            }
                            if (roomChat.getState() == RoomState.joining) {
                                roomChat.setState(RoomState.occupation);
                            }
                            removeAuthorizationError(account, room);
                            RosterManager.getInstance().onContactChanged(account, room);
                        }
                    });
                    return;
                } catch (final XMPPException.XMPPErrorException e) {
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (roomChat.getMultiUserChat() != multiUserChat) {
                                return;
                            }
                            roomChat.setState(RoomState.error);
                            addAuthorizationError(account, room);

                            XMPPError xmppError = e.getXMPPError();

                                if (xmppError != null && xmppError.getCondition() == XMPPError.Condition.conflict) {
                                    Application.getInstance().onError(R.string.NICK_ALREADY_USED);
                                } else if (xmppError != null && xmppError.getCondition() == XMPPError.Condition.not_authorized) {
                                    Application.getInstance().onError(R.string.AUTHENTICATION_FAILED);
                                } else {
                                    Application.getInstance().onError(R.string.NOT_CONNECTED);
                                }
                            RosterManager.getInstance().onContactChanged(account, room);
                        }
                    });
                    return;
                } catch (IllegalStateException e) {
                } catch (Exception e) {
                    LogManager.exception(this, e);
                }
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (roomChat.getMultiUserChat() != multiUserChat) {
                            return;
                        }
                        roomChat.setState(RoomState.waiting);
                        Application.getInstance().onError(R.string.NOT_CONNECTED);
                        RosterManager.getInstance().onContactChanged(account, room);
                    }
                });
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    public void leaveRoom(String account, String room) {
        final MultiUserChat multiUserChat;
        RoomChat roomChat = getRoomChat(account, room);
        if (roomChat == null) {
            return;
        }
        multiUserChat = roomChat.getMultiUserChat();
        roomChat.setState(RoomState.unavailable);
        roomChat.setRequested(false);
        roomChat.newAction(roomChat.getNickname(), null, ChatAction.leave);
        requestToWriteRoom(account, room, roomChat.getNickname(), roomChat.getPassword(), false);
        if (multiUserChat != null) {
            Thread thread = new Thread("Leave to room " + room + " from " + account) {
                @Override
                public void run() {
                    try {
                        multiUserChat.leave();
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.setDaemon(true);
            thread.start();
        }
        RosterManager.getInstance().onContactChanged(account, room);
    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress, Stanza packet) {
        if (!(connection instanceof AccountItem)) {
            return;
        }
        String account = ((AccountItem) connection).getAccount();
        if (bareAddress == null || !(packet instanceof Message)) {
            return;
        }
        Message message = (Message) packet;
        if (message.getType() != Message.Type.normal && message.getType() != Message.Type.chat) {
            return;
        }
        MUCUser mucUser = MUC.getMUCUserExtension(packet);
        if (mucUser == null || mucUser.getInvite() == null) {
            return;
        }
        RoomChat roomChat = getRoomChat(account, bareAddress);
        if (roomChat == null || !roomChat.getState().inUse()) {
            String inviter = mucUser.getInvite().getFrom();
            if (inviter == null) {
                inviter = bareAddress;
            }
            inviteProvider.add(new RoomInvite(account, bareAddress, inviter,
                            mucUser.getInvite().getReason(), mucUser.getPassword()), true);
        }
    }

    /**
     * Sends invitation.
     *
     * @throws NetworkException
     */
    public void invite(String account, String room, String user) throws NetworkException {
        RoomChat roomChat = getRoomChat(account, room);
        if (roomChat == null || roomChat.getState() != RoomState.available) {
            Application.getInstance().onError(R.string.NOT_CONNECTED);
            return;
        }
        Message message = new Message(room);
        MUCUser mucUser = new MUCUser();
        MUCUser.Invite invite = new MUCUser.Invite();
        invite.setTo(user);
        invite.setReason("");
        mucUser.setInvite(invite);
        message.addExtension(mucUser);
        ConnectionManager.getInstance().sendStanza(account, message);
        roomChat.putInvite(message.getPacketID(), user);
        roomChat.newAction(roomChat.getNickname(), user, ChatAction.invite_sent);
    }

    public void removeAuthorizationError(String account, String room) {
        authorizationErrorProvider.remove(account, room);
    }

    public void addAuthorizationError(String account, String room) {
        authorizationErrorProvider.add(new RoomAuthorizationError(account, room), null);
    }


    public interface HostedRoomsListener {
        void onHostedRoomsReceived(Collection<HostedRoom> hostedRooms);
    }

    public static void requestHostedRooms(final String account, final String serviceName, final HostedRoomsListener listener) {
        final XMPPConnection xmppConnection = AccountManager.getInstance().getAccount(account).getConnectionThread().getXMPPConnection();

        final Thread thread = new Thread("Get hosted rooms on server " + serviceName + " for account " + account) {
            @Override
            public void run() {
                Collection<HostedRoom> hostedRooms = null;

                try {
                    hostedRooms = MultiUserChatManager.getInstanceFor(xmppConnection).getHostedRooms(serviceName);
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }

                final Collection<HostedRoom> finalHostedRooms = hostedRooms;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onHostedRoomsReceived(finalHostedRooms);
                    }
                });
            }
        };
        thread.start();

    }

    public interface RoomInfoListener {
        void onRoomInfoReceived(RoomInfo finalRoomInfo);
    }

    public static void requestRoomInfo(final String account, final String roomJid, final RoomInfoListener listener) {
        final XMPPConnection xmppConnection = AccountManager.getInstance().getAccount(account).getConnectionThread().getXMPPConnection();

        final Thread thread = new Thread("Get room " + roomJid + " info for account " + account) {
            @Override
            public void run() {
                RoomInfo roomInfo = null;

                try {
                    LogManager.i(MUCManager.class, "Requesting room info " + roomJid);
                    roomInfo = MultiUserChatManager.getInstanceFor(xmppConnection).getRoomInfo(roomJid);
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }

                final RoomInfo finalRoomInfo = roomInfo;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRoomInfoReceived(finalRoomInfo);
                    }
                });
            }
        };
        thread.start();
    }
}
