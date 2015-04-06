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
package com.xabber.android.ui.adapter;

import android.app.Activity;
import android.view.View;

import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.androiddev.R;

/**
 * Inflate view with contact's last message or status text as well as active
 * chat badge.
 *
 * @author alexander.ivanov
 */
public class ChatContactInflater extends ClientContactInflater {

    public ChatContactInflater(Activity activity) {
        super(activity);
    }

    @Override
    ViewHolder createViewHolder(int position, View view) {
        return new ViewHolder(view);
    }

    @Override
    String getStatusText(AbstractContact abstractContact) {
        if (MessageManager.getInstance()
                .hasActiveChat(abstractContact.getAccount(), abstractContact.getUser())) {
            return MessageManager.getInstance()
                    .getLastText(abstractContact.getAccount(), abstractContact.getUser());
        } else {
            return super.getStatusText(abstractContact);
        }
    }

    @Override
    public void getView(View view, AbstractContact abstractContact) {
        super.getView(view, abstractContact);
        if (MessageManager.getInstance().hasActiveChat(abstractContact.getAccount(), abstractContact.getUser())) {
            view.setBackgroundColor(activity.getResources().getColor(R.color.grey_50));
        } else {
            view.setBackgroundColor(activity.getResources().getColor(R.color.grey_300));
        }
    }

    static class ViewHolder extends ClientContactInflater.ViewHolder {

        final View panel;

        public ViewHolder(View view) {
            super(view);
            panel = view.findViewById(R.id.panel);
        }
    }

}
