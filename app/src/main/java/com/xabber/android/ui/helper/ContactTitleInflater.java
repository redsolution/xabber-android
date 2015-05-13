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
package com.xabber.android.ui.helper;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.roster.AbstractContact;

import org.jivesoftware.smackx.ChatState;

public class ContactTitleInflater {

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);
        final ImageView avatarView = (ImageView) titleView.findViewById(R.id.avatar);

        nameView.setText(abstractContact.getName());
        avatarView.setImageDrawable(abstractContact.getAvatar());
        setStatus(context, titleView, abstractContact);
    }

    private static void setStatus(Context context, View titleView, AbstractContact abstractContact) {
        final ImageView statusModeView = (ImageView) titleView.findViewById(R.id.status_icon);

        int statusLevel = abstractContact.getStatusMode().getStatusLevel();
        if (isContactOffline(statusLevel)) {
            statusModeView.setVisibility(View.GONE);
        } else {
            statusModeView.setVisibility(View.VISIBLE);
            statusModeView.setImageLevel(statusLevel);
        }

        final TextView statusTextView = (TextView) titleView.findViewById(R.id.status_text);


        ChatState chatState = ChatStateManager.getInstance().getChatState(
                abstractContact.getAccount(), abstractContact.getUser());

        CharSequence statusText;
        if (chatState == ChatState.composing) {
            statusText = context.getString(R.string.chat_state_composing);
        } else if (chatState == ChatState.paused) {
            statusText = context.getString(R.string.chat_state_paused);
        } else {
            statusText = abstractContact.getStatusText().trim();
            if (statusText.toString().isEmpty()) {
                statusText = context.getString(abstractContact.getStatusMode().getStringID());
            }
        }
        statusTextView.setText(statusText);
    }

    private static boolean isContactOffline(int statusLevel) {
        return statusLevel == 6;
    }

}
