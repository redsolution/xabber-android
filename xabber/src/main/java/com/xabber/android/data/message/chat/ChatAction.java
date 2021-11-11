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
package com.xabber.android.data.message.chat;

import android.content.Context;

import com.xabber.android.R;
import com.xabber.android.data.account.StatusMode;

import org.jetbrains.annotations.NotNull;

/**
 * Action in chat.
 *
 * @author alexander.ivanov
 */
public enum ChatAction {

    /**
     * Contact becomes available.
     */
    available,

    /**
     * Contact becomes free for chat.
     */
    chat,

    /**
     * Contact go away.
     */
    away,

    /**
     * Contact go away for an extended period of time.
     */
    xa,

    /**
     * Contact ask to do not disturb.
     */
    dnd,

    /**
     * Contact becomes unavailable.
     */
    unavailable,

    /**
     * Contact changes status text.
     */
    status,

    /**
     * User joins room.
     */
    join,

    /**
     * User leaves room.
     */
    leave,

    /**
     * User was kicked.
     */
    kick,

    /**
     * User was banned.
     */
    ban,

    /**
     * User changes nickname.
     */
    nickname,

    /**
     * You have join room.
     */
    complete,

    /**
     * Invitation to the room was sent.
     */
    invite_sent,

    /**
     * Invitation was not received.
     */
    invite_error,

    /**
     * Subject of the room has been changed.
     */
    subject,

    /**
     * Call attention.
     */
    attention_called,

    /**
     * Request attention.
     */
    attention_requested,
    subscription_sent,
    subscription_received,
    subscription_sent_accepted,
    subscription_received_accepted,
    contact_blocked,
    contact_unblocked,
    contact_deleted;

    public static ChatAction getChatAction(StatusMode statusMode) {
        if (statusMode == StatusMode.unavailable)
            return ChatAction.unavailable;
        else if (statusMode == StatusMode.available)
            return ChatAction.available;
        else if (statusMode == StatusMode.away)
            return ChatAction.away;
        else if (statusMode == StatusMode.chat)
            return ChatAction.chat;
        else if (statusMode == StatusMode.dnd)
            return ChatAction.dnd;
        else if (statusMode == StatusMode.xa)
            return ChatAction.xa;
        else
            throw new IllegalStateException();
    }

    /**
     * @param name
     * @return Chat action by name. <code>null</code> if action is empty (usual
     * message) or action is unknown.
     */
    public static ChatAction getChatAction(String name) {
        for (ChatAction messageAction : ChatAction.values())
            if (messageAction.name().equals(name))
                return messageAction;
        return null;
    }

    /**
     * @param text
     * @return String to be added to the status mode change action.
     */
    private static String getOptionalText(String text) {
        if ("".equals(text.trim()))
            return "";
        else
            return " (" + text + ")";
    }

    /**
     * @param context
     * @param name    contact's name.
     * @param text    additional text depend on action.
     * @return Text representation for the action.
     */
    public String getText(@NotNull Context context, String name, String text) {
        if (this == ChatAction.available)
            return context.getString(R.string.action_status_available, name)
                    + getOptionalText(text);
        else if (this == ChatAction.away)
            return context.getString(R.string.action_status_away, name)
                    + getOptionalText(text);
        else if (this == ChatAction.chat)
            return context.getString(R.string.action_status_chat, name)
                    + getOptionalText(text);
        else if (this == ChatAction.dnd)
            return context.getString(R.string.action_status_dnd, name)
                    + getOptionalText(text);
        else if (this == ChatAction.unavailable)
            return context.getString(R.string.action_status_unavailable, name)
                    + getOptionalText(text);
        else if (this == ChatAction.xa)
            return context.getString(R.string.action_status_xa, name)
                    + getOptionalText(text);
        else if (this == ChatAction.subscription_sent)
            return context.getString(R.string.action_subscription_sent);
        else if (this == ChatAction.subscription_received)
            return context.getString(R.string.action_subscription_received);
        else if (this == ChatAction.subscription_sent_accepted)
            return context.getString(R.string.action_subscription_sent_add, name);
        else if (this == ChatAction.subscription_received_accepted)
            return context.getString(R.string.action_subscription_received_add, name);
        else if (this == ChatAction.status && "".equals(text))
            return context.getString(R.string.action_status_text_none, name);
        else if (this == ChatAction.status)
            return context.getString(R.string.action_status_text, name, text);
        else if (this == ChatAction.join)
            return context.getString(R.string.action_join, name);
        else if (this == ChatAction.kick && "".equals(text))
            return context.getString(R.string.action_kick, name);
        else if (this == ChatAction.kick)
            return context.getString(R.string.action_kick_by, name, text);
        else if (this == ChatAction.leave)
            return context.getString(R.string.action_leave, name);
        else if (this == ChatAction.ban && "".equals(text))
            return context.getString(R.string.action_ban, name);
        else if (this == ChatAction.ban)
            return context.getString(R.string.action_ban_by, name, text);
        else if (this == ChatAction.nickname)
            return context.getString(R.string.action_nickname, name, text);
        else if (this == ChatAction.complete)
            return context.getString(R.string.action_join_complete, name);
        else if (this == ChatAction.invite_sent)
            return context.getString(R.string.action_invite_sent, text);
        else if (this == ChatAction.invite_error)
            return context.getString(R.string.action_invite_error, text);
        else if (this == ChatAction.subject)
            return context.getString(R.string.action_subject, name, text);
        else if (this == ChatAction.attention_called)
            return context.getString(R.string.action_attention_called);
        else if (this == ChatAction.attention_requested)
            return context.getString(R.string.action_attention_requested);
        else if (this == ChatAction.contact_blocked)
            return context.getString(R.string.action_contact_blocked);
        else if (this == ChatAction.contact_unblocked)
            return context.getString(R.string.action_contact_unblocked);
        else if (this == ChatAction.contact_deleted)
            return context.getString(R.string.action_contact_deleted);
        else
            throw new IllegalStateException();
    }

    /**
     * @return Whether action is status change.
     */
    public boolean isStatusChage() {
        return this == ChatAction.available || this == ChatAction.away
                || this == ChatAction.chat || this == ChatAction.dnd
                || this == ChatAction.unavailable || this == ChatAction.xa
                || this == ChatAction.status;
    }

}
