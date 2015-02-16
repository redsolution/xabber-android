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

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.utils.Emoticons;
import com.xabber.androiddev.R;

import org.jivesoftware.smackx.ChatState;

/**
 * Helper class to update <code>contact_title.xml</code>.
 *
 * @author alexander.ivanov
 */
public class ContactTitleInflater {

    /**
     * Fill title with information about {@link AbstractContact} and provides
     * back button callback.
     *
     * @param titleView
     * @param activity
     * @param abstractContact
     */
    public static void updateTitle(View titleView, final Activity activity,
                                   AbstractContact abstractContact) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);
        final ImageView avatarView = (ImageView) titleView.findViewById(R.id.avatar);
        final ImageView statusModeView = (ImageView) titleView.findViewById(R.id.status_mode);
        final TextView statusTextView = (TextView) titleView.findViewById(R.id.status_text);
        final View shadowView = titleView.findViewById(R.id.shadow);

        int[] accountActionBarColors = activity.getResources().getIntArray(R.array.account_action_bar);

        titleView.setBackgroundDrawable(new ColorDrawable(accountActionBarColors[
                AccountManager.getInstance().getColorLevel(abstractContact.getAccount())]));
        nameView.setText(abstractContact.getName());
        statusModeView.setImageLevel(abstractContact.getStatusMode().getStatusLevel());
        avatarView.setImageDrawable(abstractContact.getAvatar());

        setStatusText(activity, abstractContact, statusTextView);

        final Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.shadow);
        final BitmapDrawable shadowDrawable = new BitmapDrawable(bitmap);
        shadowDrawable.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
        shadowView.setBackgroundDrawable(shadowDrawable);

        if (abstractContact.isConnected()) {
            shadowView.setVisibility(View.GONE);
        } else {
            shadowView.setVisibility(View.VISIBLE);
        }
    }

    private static void setStatusText(Activity activity, AbstractContact abstractContact,
                                      TextView statusTextView) {
        ChatState chatState = ChatStateManager.getInstance().getChatState(
                abstractContact.getAccount(), abstractContact.getUser());

        final CharSequence statusText;
        if (chatState == ChatState.composing) {
            statusText = activity.getString(R.string.chat_state_composing);
        } else if (chatState == ChatState.paused) {
            statusText = activity.getString(R.string.chat_state_paused);
        } else {
            statusText = Emoticons.getSmiledText(activity, abstractContact.getStatusText());
        }
        statusTextView.setText(statusText);
    }

}
