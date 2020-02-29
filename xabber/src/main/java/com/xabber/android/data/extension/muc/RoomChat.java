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

import android.os.Looper;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsShowStatusChange;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.Attachment;
import com.xabber.android.data.database.realmobjects.ForwardId;
import com.xabber.android.data.database.realmobjects.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.references.ReferencesManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.ForwardManager;
import com.xabber.android.data.message.NewIncomingMessageEvent;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.xmpp.sid.UniqStanzaHelper;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.muc.MUCAffiliation;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * Chat room.
 * <p/>
 * Warning: We are going to remove SMACK components.
 *
 * @author alexander.ivanov
 */
public class RoomChat extends AbstractChat {

    private final static String LOG_TAG = RoomChat.class.getSimpleName();
    /**
     * Information about occupants for STRING-PREPed resource.
     */
    private final Map<Resourcepart, Occupant> occupants;
    /**
     * Invited user for the sent packet ID.
     */
    private final Map<String, UserJid> invites;
    /**
     * Joining was requested from the UI.
     */
    private boolean requested;
    /**
     * Nickname used in the room.
     */
    private Resourcepart nickname;
    private String password;
    private RoomState state;
    private String subject;
    /**
     * SMACK MUC implementation.
     */
    private MultiUserChat multiUserChat;

    public static RoomChat create(AccountJid account, EntityBareJid user, Resourcepart nickname, String password) throws UserJid.UserJidCreateException {
        return new RoomChat(account, UserJid.from(user), nickname, password);
    }

    private RoomChat(AccountJid account, UserJid user, Resourcepart nickname, String password) {
        super(account, user, false);
        this.nickname = nickname;
        this.password = password;
        requested = false;
        state = RoomState.unavailable;
        subject = "";
        multiUserChat = null;
        occupants = new HashMap<>();
        invites = new HashMap<>();
    }

    @NonNull
    @Override
    public EntityBareJid getTo() {
        return getRoom();
    }

    @Override
    public Type getType() {
        return Type.groupchat;
    }

    EntityBareJid getRoom() {
        return user.getJid().asEntityBareJidIfPossible();
    }

    public Resourcepart getNickname() {
        return nickname;
    }

    void setNickname(Resourcepart nickname) {
        this.nickname = nickname;
    }

    String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    boolean isRequested() {
        return requested;
    }

    void setRequested(boolean requested) {
        this.requested = requested;
    }

    public RoomState getState() {
        return state;
    }

    void setState(RoomState state) {
        this.state = state;
        if (!state.inUse()) {
            multiUserChat = null;
            occupants.clear();
            invites.clear();
        }
        if (state == RoomState.available) {
            sendMessages();
        }
    }

    Collection<Occupant> getOccupants() {
        return Collections.unmodifiableCollection(occupants.values());
    }

    String getSubject() {
        return subject;
    }

    public MultiUserChat getMultiUserChat() {
        return multiUserChat;
    }

    void setMultiUserChat(MultiUserChat multiUserChat) {
        this.multiUserChat = multiUserChat;
    }

    void putInvite(String packetID, UserJid user) {
        invites.put(packetID, user);
    }

    @Override
    protected MessageItem createNewMessageItem(String text) {
        String id = UUID.randomUUID().toString();
        return createMessageItem(nickname, text, null, null, null, false,
                false, false, false, id, id, null,
        null, null, account.getFullJid().toString(), false, null, true);
    }

    @Override
    public boolean notifyAboutMessage() {
        int currentTime = (int) (System.currentTimeMillis() / 1000L);
        switch (notificationState.getMode()) {
            case enabled: return true;
            case disabled: return false;
            case snooze15m:
                return currentTime > notificationState.getTimestamp() + TimeUnit.MINUTES.toSeconds(15);
            case snooze1h:
                return currentTime > notificationState.getTimestamp() + TimeUnit.HOURS.toSeconds(1);
            case snooze2h:
                return currentTime > notificationState.getTimestamp() + TimeUnit.HOURS.toSeconds(2);
            case snooze1d:
                return currentTime > notificationState.getTimestamp() + TimeUnit.DAYS.toSeconds(1);
            default: return SettingsManager.eventsOnMuc();
        }
    }

    @Override
    protected boolean onPacket(UserJid bareAddress, Stanza stanza, boolean isCarbons) {
        if (!super.onPacket(bareAddress, stanza, isCarbons)) {
            return false;
        }

//        MUCUser mucUserExtension = MUCUser.from(stanza);
//        if (mucUserExtension != null && mucUserExtension.getInvite() != null) {
//            return false;
//        }

        final org.jxmpp.jid.Jid from = stanza.getFrom();
        final Resourcepart resource = from.getResourceOrNull();
        if (stanza instanceof Message) {
            final Message message = (Message) stanza;

            if (message.getType() == Message.Type.error) {
                UserJid invite = invites.remove(message.getStanzaId());
                if (invite != null) {
                    newAction(nickname, invite.toString(), ChatAction.invite_error, true);
                }
                return true;
            }
            MUCUser mucUser = MUCUser.from(stanza);
            if (mucUser != null && mucUser.getDecline() != null) {
                onInvitationDeclined(mucUser.getDecline().getFrom(), mucUser.getDecline().getReason());
                return true;
            }
            if (mucUser != null && mucUser.getStatus() != null
                    && mucUser.getStatus().contains(MUCUser.Status.create("100"))) {
                    // 'This room is not anonymous'
                    return true;
            }
            String text = message.getBody();
            final String subject = message.getSubject();
            if (text == null && subject == null) {
                return true;
            }
            if (subject != null) {
                if (this.subject.equals(subject)) {
                    return true;
                }
                this.subject = subject;
                RosterManager.onContactChanged(account, bareAddress);
                newAction(resource, subject, ChatAction.subject, true);
            } else {
                boolean notify = true;

                Date delay = getDelayStamp(message);

                if (delay != null) {
                    notify = false;
                }

                // forward comment (to support previous forwarded xep)
                String forwardComment = ForwardManager.parseForwardComment(stanza);
                if (forwardComment != null) text = forwardComment;

                // modify body with references
                Pair<String, String> bodies = ReferencesManager.modifyBodyWithReferences(message, text);
                text = bodies.first;
                String markupText = bodies.second;

                String originalFrom = stanza.getFrom().toString();

                String messageUId = getMessageIdIfInHistory(getStanzaId(message), text);
                if (messageUId != null) {
                    if (isSelf(resource)) {
                        markMessageAsDelivered(messageUId, originalFrom);
                    }
                    return true;
                }

                if (isSelf(resource)) {
                    notify = false;
                }

                updateThreadId(message.getThread());

                RealmList<Attachment> attachments = HttpFileUploadManager.parseFileMessage(stanza);

                String uid = UUID.randomUUID().toString();
                RealmList<ForwardId> forwardIds = parseForwardedMessage(true, stanza, uid);
                String originalStanza = stanza.toXML().toString();

                // create message with file-attachments
                if (attachments.size() > 0)
                    createAndSaveFileMessage(true, uid, resource, text, markupText, null,
                            null, delay, true, notify,
                            false, false, getStanzaId(message), UniqStanzaHelper.getOriginId(message), attachments,
                            originalStanza, null, originalFrom, true, forwardIds, true, false, null);

                    // create message without attachments
                else createAndSaveNewMessage(true, uid, resource, text, markupText, null,
                        null, delay, true, notify,
                        false, false, getStanzaId(message), UniqStanzaHelper.getOriginId(message),
                        originalStanza, null, originalFrom, true, forwardIds, true, false, null);

                EventBus.getDefault().post(new NewIncomingMessageEvent(account, user));
            }
        } else if (stanza instanceof Presence) {
            Presence presence = (Presence) stanza;
            if (presence.getType() == Presence.Type.available) {
                Occupant oldOccupant = occupants.get(resource);
                Occupant newOccupant = createOccupant(resource, presence);
                newOccupant.setJid(from);
                if (newOccupant != null && newOccupant.getNickname() != null) {
                    occupants.put(resource, newOccupant);
                }
                if (oldOccupant == null) {
                    onAvailable(resource);
                    RosterManager.onContactChanged(account, user);
                } else {
                    boolean changed = false;
                    if (oldOccupant.getAffiliation() != newOccupant.getAffiliation()) {
                        changed = true;
                        onAffiliationChanged(resource, newOccupant.getAffiliation());
                    }
                    if (oldOccupant.getRole() != newOccupant.getRole()) {
                        changed = true;
                        onRoleChanged(resource, newOccupant.getRole());
                    }
                    if (oldOccupant.getStatusMode() != newOccupant.getStatusMode()
                            || !oldOccupant.getStatusText().equals(newOccupant.getStatusText())) {
                        changed = true;
                        onStatusChanged(resource, newOccupant.getStatusMode(), newOccupant.getStatusText());
                    }
                    if (changed) {
                        RosterManager.onContactChanged(account, user);
                    }
                }
            } else if (presence.getType() == Presence.Type.unavailable && state == RoomState.available) {
                occupants.remove(resource);
                MUCUser mucUser = MUCUser.from(presence);
                if (mucUser != null && mucUser.getStatus() != null) {
                    if (mucUser.getStatus().contains(MUCUser.Status.KICKED_307)) {
                        onKick(resource, mucUser.getItem().getActor());
                    } else if (mucUser.getStatus().contains(MUCUser.Status.BANNED_301)){
                        onBan(resource, mucUser.getItem().getActor());
                    } else if (mucUser.getStatus().contains(MUCUser.Status.NEW_NICKNAME_303)) {
                        Resourcepart newNick = mucUser.getItem().getNick();
                        if (newNick == null) {
                            return true;
                        }
                        onRename(resource, newNick);
                        Occupant occupant = createOccupant(newNick, presence);
                        occupants.put(newNick, occupant);
                    } else if (mucUser.getStatus().contains(MUCUser.Status.REMOVED_AFFIL_CHANGE_321)) {
                        onRevoke(resource, mucUser.getItem().getActor());
                    }
                } else {
                    onLeave(resource);
                }
                RosterManager.onContactChanged(account, user);
            }
        }
        return true;
    }

    @Override
    protected String parseInnerMessage(boolean ui, Message message, Date timestamp, String parentMessageId) {
        if (message.getType() == Message.Type.error) return null;

        final org.jxmpp.jid.Jid from = message.getFrom();
        final Resourcepart resource = from.getResourceOrNull();
        String text = message.getBody();
        final String subject = message.getSubject();

        if (text == null) return null;
        if (subject != null) return null;

        RealmList<Attachment> attachments = HttpFileUploadManager.parseFileMessage(message);

        String uid = UUID.randomUUID().toString();
        RealmList<ForwardId> forwardIds = parseForwardedMessage(ui, message, uid);
        String originalStanza = message.toXML().toString();
        String originalFrom = message.getFrom().toString();
        boolean fromMUC = message.getType().equals(Type.groupchat);

        // forward comment (to support previous forwarded xep)
        String forwardComment = ForwardManager.parseForwardComment(message);
        if (forwardComment != null) text = forwardComment;

        // modify body with references
        Pair<String, String> bodies = ReferencesManager.modifyBodyWithReferences(message, text);
        text = bodies.first;
        String markupText = bodies.second;

        // create message with file-attachments
        if (attachments.size() > 0)
            createAndSaveFileMessage(ui, uid, resource, text, markupText, null, timestamp, getDelayStamp(message),
                    true, false, false, false, getStanzaId(message), UniqStanzaHelper.getOriginId(message),
                    attachments, originalStanza, parentMessageId, originalFrom, true, forwardIds, fromMUC, true, null);

            // create message without attachments
        else createAndSaveNewMessage(ui, uid, resource, text, markupText, null, timestamp, getDelayStamp(message),
                true, false, false, false, getStanzaId(message), UniqStanzaHelper.getOriginId(message),
                originalStanza, parentMessageId, originalFrom, true, forwardIds, fromMUC, true, null);

        return uid;
    }

        private void markMessageAsDelivered(final String messageUId, final String originalFrom) {
        Application.getInstance().runInBackground(() ->  {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 ->  {
                    MessageItem message = realm1.where(MessageItem.class)
                            .equalTo(MessageItem.Fields.UNIQUE_ID, messageUId).findFirst();
                    message.setDelivered(true);
                    message.setOriginalFrom(originalFrom);
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    @Nullable
    private String getMessageIdIfInHistory(String stanzaId, String body) {
        if (stanzaId == null) return null;
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        MessageItem message = realm
                .where(MessageItem.class)
                .equalTo(MessageItem.Fields.TEXT, body)
                .equalTo(MessageItem.Fields.STANZA_ID, stanzaId)
                .findFirst();
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        if (message != null) return message.getUniqueId();
        else return null;
    }

    /**
     * @return Whether status change action should be added to the chat history.
     */
    private boolean showStatusChange() {
        return SettingsManager.chatsShowStatusChange() != ChatsShowStatusChange.never;
    }

    /**
     * @return Whether resource is own nickname.
     * @param resource
     */
    private boolean isSelf(Resourcepart resource) {
        return nickname != null && resource != null && nickname.equals(resource);
    }

    /**
     * Informs that the invitee has declined the invitation.
     */
    private void onInvitationDeclined(EntityBareJid from, String reason) {
        // TODO
    }

    /**
     * A occupant becomes available.
     * @param resource
     */
    private void onAvailable(Resourcepart resource) {
        if (isSelf(resource)) {
            setState(RoomState.available);
            if (isRequested()) {
                if (showStatusChange()) {
                    createAndSaveNewMessage(true, UUID.randomUUID().toString(), resource, Application.getInstance().getString(
                                    R.string.action_join_complete_to, user), null,
                            ChatAction.complete, null, null, true, true,
                            false, false, null, null,
                            null, null, null, false,null, true, false, null);
                }
                active = true;
                setRequested(false);
            } else {
                if (showStatusChange()) {
                    newAction(resource, null, ChatAction.complete, true);
                }
            }
        } else {
            if (state == RoomState.available) {
                if (showStatusChange()) {
                    newAction(resource, null, ChatAction.join, true);
                }
            }
        }
    }

    /**
     * Warning: this method should be placed with packet provider.
     *
     * @return New occupant based on presence information.
     */
    private Occupant createOccupant(Resourcepart resource, Presence presence) {
        Occupant occupant = new Occupant(resource);
        org.jxmpp.jid.Jid jid = null;
        MUCAffiliation affiliation = MUCAffiliation.none;
        MUCRole role = MUCRole.none;


        StatusMode statusMode = StatusMode.unavailable;
        String statusText = null;
        MUCUser mucUser = MUCUser.from(presence);
        if (mucUser != null) {
            MUCItem item = mucUser.getItem();
            if (item != null) {
                jid = item.getJid();
                try {
                    affiliation = item.getAffiliation();
                } catch (NoSuchElementException e) {
                }
                try {
                    role = item.getRole();
                } catch (NoSuchElementException e) {
                }
                statusMode = StatusMode.createStatusMode(presence);
                statusText = presence.getStatus();
            }
        }
        if (statusText == null) {
            statusText = "";
        }
        occupant.setJid(jid);
        occupant.setAffiliation(affiliation);
        occupant.setRole(role);
        occupant.setStatusMode(statusMode);
        occupant.setStatusText(statusText);
        return occupant;
    }

    private void onAffiliationChanged(Resourcepart resource, MUCAffiliation affiliation) {
    }

    private void onRoleChanged(Resourcepart resource, MUCRole role) {
    }

    private void onStatusChanged(Resourcepart resource, StatusMode statusMode, String statusText) {
    }

    /**
     * A occupant leaves room.
     * @param resource
     */
    private void onLeave(Resourcepart resource) {
        if (showStatusChange()) {
            newAction(resource, null, ChatAction.leave, true);
        }
        if (isSelf(resource)) {
            setState(RoomState.waiting);
            RosterManager.onContactChanged(account, user);
        }
    }

    /**
     * A occupant was kicked.
     *
     * @param resource
     * @param actor
     */
    private void onKick(Resourcepart resource, org.jxmpp.jid.Jid actor) {
        if (showStatusChange()) {
            if (actor != null) newAction(resource, actor.toString(), ChatAction.kick, true);
            else newAction(resource, "", ChatAction.kick, true);
        }
        if (isSelf(resource)) {
            MUCManager.getInstance().leaveRoom(account, getRoom());
        }
    }

    /**
     * A occupant was banned.
     *
     * @param resource
     * @param actor
     */
    private void onBan(Resourcepart resource, org.jxmpp.jid.Jid actor) {
        if (showStatusChange()) {
            newAction(resource, actor.toString(), ChatAction.ban, true);
        }
        if (isSelf(resource)) {
            MUCManager.getInstance().leaveRoom(account, getRoom());
        }
    }

    /**
     * A occupant has changed his nickname in the room.
     *
     * @param resource
     * @param newNick
     */
    private void onRename(Resourcepart resource, Resourcepart newNick) {
        if (showStatusChange()) {
            newAction(resource, newNick.toString(), ChatAction.nickname, true);
        }
    }

    /**
     * A user's membership was revoked from the room
     *
     * @param resource
     * @param actor
     */
    private void onRevoke(Resourcepart resource, org.jxmpp.jid.Jid actor) {
        if (showStatusChange()) {
            newAction(resource, actor.toString(), ChatAction.kick, true);
        }
        if (isSelf(resource)) {
            MUCManager.getInstance().leaveRoom(account, getRoom());
        }
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        if (getState() == RoomState.waiting) {
            MUCManager.getInstance().joinRoom(account, getRoom(), false);
        }
    }

    @Override
    protected void onDisconnect() {
        super.onDisconnect();
        if (state != RoomState.unavailable) {
            setState(RoomState.waiting);
        }
    }

}
