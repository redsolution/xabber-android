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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.MUCUser;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsShowStatusChange;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.delay.Delay;
import com.xabber.xmpp.muc.Affiliation;
import com.xabber.xmpp.muc.MUC;
import com.xabber.xmpp.muc.Role;

/**
 * Chat room.
 * 
 * Warning: We are going to remove SMACK components.
 * 
 * @author alexander.ivanov
 * 
 */
public class RoomChat extends AbstractChat {

	/**
	 * Joining was requested from the UI.
	 */
	private boolean requested;

	/**
	 * Nickname used in the room.
	 */
	private String nickname;

	private String password;
	private RoomState state;
	private String subject;

	/**
	 * SMACK MUC implementation.
	 */
	private MultiUserChat multiUserChat;

	/**
	 * Information about occupants for STRING-PREPed resource.
	 */
	private final Map<String, Occupant> occupants;

	/**
	 * Invited user for the sent packet ID.
	 */
	private final Map<String, String> invites;

	RoomChat(String account, String user, String nickname, String password) {
		super(account, user);
		this.nickname = nickname;
		this.password = password;
		requested = false;
		state = RoomState.unavailable;
		subject = "";
		multiUserChat = null;
		occupants = new HashMap<String, Occupant>();
		invites = new HashMap<String, String>();
	}

	@Override
	public String getTo() {
		return user;
	}

	@Override
	public Type getType() {
		return Type.groupchat;
	}

	String getRoom() {
		return user;
	}

	String getNickname() {
		return nickname;
	}

	void setNickname(String nickname) {
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
		if (state == RoomState.available)
			sendMessages();
	}

	Collection<Occupant> getOccupants() {
		return Collections.unmodifiableCollection(occupants.values());
	}

	String getSubject() {
		return subject;
	}

	MultiUserChat getMultiUserChat() {
		return multiUserChat;
	}

	void setMultiUserChat(MultiUserChat multiUserChat) {
		this.multiUserChat = multiUserChat;
	}

	void putInvite(String packetID, String user) {
		invites.put(packetID, user);
	}

	@Override
	protected MessageItem newMessage(String text) {
		return newMessage(nickname, text, null, null, false, false, false,
				false, true);
	}

	@Override
	protected boolean canSendMessage() {
		if (super.canSendMessage())
			return state == RoomState.available;
		return false;
	}

	@Override
	protected boolean notifyAboutMessage() {
		return SettingsManager.eventsMessage() == SettingsManager.EventsMessage.chatAndMuc;
	}

	@Override
	protected boolean onPacket(String bareAddress, Packet packet) {
		if (!super.onPacket(bareAddress, packet))
			return false;
		final String from = packet.getFrom();
		final String resource = StringUtils.parseResource(from);
		if (packet instanceof Message) {
			final Message message = (Message) packet;
			if (message.getType() == Message.Type.error) {
				String invite = invites.remove(message.getPacketID());
				if (invite != null) {
					newAction(nickname, invite, ChatAction.invite_error);
				}
				return true;
			}
			MUCUser mucUser = MUC.getMUCUserExtension(packet);
			if (mucUser != null && mucUser.getDecline() != null) {
				onInvitationDeclined(mucUser.getDecline().getFrom(), mucUser
						.getDecline().getReason());
				return true;
			}
			final String text = message.getBody();
			final String subject = message.getSubject();
			if (text == null && subject == null)
				return true;
			if (isSelf(resource)) // Own message
				return true;
			if (subject != null) {
				if (this.subject.equals(subject))
					return true;
				this.subject = subject;
				RosterManager.getInstance().onContactChanged(account,
						bareAddress);
				newAction(resource, subject, ChatAction.subject);
			} else {
				String packetID = message.getPacketID();
				Date delay = Delay.getDelay(message);
				for (MessageItem messageItem : messages) {
					// Search for duplicates
					if (packetID != null
							&& packetID.equals(messageItem.getPacketID()))
						return true;
					if (delay != null) {
						if (delay.equals(messageItem.getDelayTimestamp())
								&& resource.equals(messageItem.getResource())
								&& text.equals(messageItem.getText()))
							return true;
					}
				}
				updateThreadId(message.getThread());
				MessageItem messageItem = newMessage(resource, text, null,
						delay, true, true, false, false, true);
				messageItem.setPacketID(packetID);
			}
		} else if (packet instanceof Presence) {
			String stringPrep = Jid.getStringPrep(resource);
			Presence presence = (Presence) packet;
			if (presence.getType() == Presence.Type.available) {
				Occupant oldOccupant = occupants.get(stringPrep);
				Occupant newOccupant = createOccupant(resource, presence);
				occupants.put(stringPrep, newOccupant);
				if (oldOccupant == null) {
					onAvailable(resource);
					RosterManager.getInstance().onContactChanged(account, user);
				} else {
					boolean changed = false;
					if (oldOccupant.getAffiliation() != newOccupant
							.getAffiliation()) {
						changed = true;
						onAffiliationChanged(resource,
								newOccupant.getAffiliation());
					}
					if (oldOccupant.getRole() != newOccupant.getRole()) {
						changed = true;
						onRoleChanged(resource, newOccupant.getRole());
					}
					if (oldOccupant.getStatusMode() != newOccupant
							.getStatusMode()
							|| !oldOccupant.getStatusText().equals(
									newOccupant.getStatusText())) {
						changed = true;
						onStatusChanged(resource, newOccupant.getStatusMode(),
								newOccupant.getStatusText());
					}
					if (changed)
						RosterManager.getInstance().onContactChanged(account,
								user);
				}
			} else if (presence.getType() == Presence.Type.unavailable
					&& state == RoomState.available) {
				occupants.remove(stringPrep);
				MUCUser mucUser = MUC.getMUCUserExtension(presence);
				if (mucUser != null && mucUser.getStatus() != null) {
					String code = mucUser.getStatus().getCode();
					if ("307".equals(code)) {
						onKick(resource, mucUser.getItem().getActor());
					} else if ("301".equals(code)) {
						onBan(resource, mucUser.getItem().getActor());
					} else if ("303".equals(code)) {
						String newNick = mucUser.getItem().getNick();
						if (newNick == null)
							return true;
						onRename(resource, newNick);
						Occupant occupant = createOccupant(newNick, presence);
						occupants.put(Jid.getStringPrep(newNick), occupant);
					} else if ("321".equals(code)) {
						onRevoke(resource, mucUser.getItem().getActor());
					}
				} else {
					onLeave(resource);
				}
				RosterManager.getInstance().onContactChanged(account, user);
			}
		}
		return true;
	}

	/**
	 * @return Whether status change action should be added to the chat history.
	 */
	private boolean showStatusChange() {
		return SettingsManager.chatsShowStatusChange() != ChatsShowStatusChange.never;
	}

	/**
	 * @param resource
	 * @return Whether resource is own nickname.
	 */
	private boolean isSelf(String resource) {
		return Jid.getStringPrep(nickname).equals(Jid.getStringPrep(resource));
	}

	/**
	 * Informs that the invitee has declined the invitation.
	 * 
	 * @param from
	 * @param reason
	 */
	private void onInvitationDeclined(String from, String reason) {
		// TODO
	}

	/**
	 * A occupant becomes available.
	 * 
	 * @param resource
	 */
	private void onAvailable(String resource) {
		if (isSelf(resource)) {
			setState(RoomState.available);
			if (isRequested()) {
				if (showStatusChange())
					newMessage(
							resource,
							Application.getInstance().getString(
									R.string.action_join_complete_to, user),
							ChatAction.complete, null, true, true, false,
							false, true);
				active = true;
				setRequested(false);
			} else {
				if (showStatusChange())
					newAction(resource, null, ChatAction.complete);
			}
		} else {
			if (state == RoomState.available)
				if (showStatusChange())
					newAction(resource, null, ChatAction.join);
		}
	}

	/**
	 * Warning: this method should be placed with packet provider.
	 * 
	 * @param resource
	 * @param presence
	 * @return New occupant based on presence information.
	 */
	private Occupant createOccupant(String resource, Presence presence) {
		Occupant occupant = new Occupant(resource);
		String jid = null;
		Affiliation affiliation = Affiliation.none;
		Role role = Role.none;
		StatusMode statusMode = StatusMode.unavailable;
		String statusText = null;
		MUCUser mucUser = MUC.getMUCUserExtension(presence);
		if (mucUser != null) {
			MUCUser.Item item = mucUser.getItem();
			if (item != null) {
				jid = item.getJid();
				try {
					affiliation = Affiliation.fromString(item.getAffiliation());
				} catch (NoSuchElementException e) {
				}
				try {
					role = Role.fromString(item.getRole());
				} catch (NoSuchElementException e) {
				}
				statusMode = StatusMode.createStatusMode(presence);
				statusText = presence.getStatus();
			}
		}
		if (statusText == null)
			statusText = "";
		occupant.setJid(jid);
		occupant.setAffiliation(affiliation);
		occupant.setRole(role);
		occupant.setStatusMode(statusMode);
		occupant.setStatusText(statusText);
		return occupant;
	}

	private void onAffiliationChanged(String resource, Affiliation affiliation) {
	}

	private void onRoleChanged(String resource, Role role) {
	}

	private void onStatusChanged(String resource, StatusMode statusMode,
			String statusText) {
	}

	/**
	 * A occupant leaves room.
	 * 
	 * @param resource
	 */
	private void onLeave(String resource) {
		if (showStatusChange())
			newAction(resource, null, ChatAction.leave);
		if (isSelf(resource)) {
			setState(RoomState.waiting);
			RosterManager.getInstance().onContactChanged(account, user);
		}
	}

	/**
	 * A occupant was kicked.
	 * 
	 * @param resource
	 * @param actor
	 */
	private void onKick(String resource, String actor) {
		if (showStatusChange())
			newAction(resource, actor, ChatAction.kick);
		if (isSelf(resource))
			MUCManager.getInstance().leaveRoom(account, user);
	}

	/**
	 * A occupant was banned.
	 * 
	 * @param resource
	 * @param actor
	 */
	private void onBan(String resource, String actor) {
		if (showStatusChange())
			newAction(resource, actor, ChatAction.ban);
		if (isSelf(resource))
			MUCManager.getInstance().leaveRoom(account, user);
	}

	/**
	 * A occupant has changed his nickname in the room.
	 * 
	 * @param resource
	 * @param newNick
	 */
	private void onRename(String resource, String newNick) {
		if (showStatusChange())
			newAction(resource, newNick, ChatAction.nickname);
	}

	/**
	 * A user's membership was revoked from the room
	 * 
	 * @param resource
	 * @param actor
	 */
	private void onRevoke(String resource, String actor) {
		if (showStatusChange())
			newAction(resource, actor, ChatAction.kick);
		if (isSelf(resource))
			MUCManager.getInstance().leaveRoom(account, user);
	}

	@Override
	protected void onComplete() {
		super.onComplete();
		if (getState() == RoomState.waiting)
			MUCManager.getInstance().joinRoom(account, user, false);
	}

	@Override
	protected void onDisconnect() {
		super.onDisconnect();
		if (state != RoomState.unavailable)
			setState(RoomState.waiting);
	}

}