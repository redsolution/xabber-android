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
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.Occupant;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.OccupantListActivity;

import org.jivesoftware.smackx.muc.MUCRole;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Adapter for {@link OccupantListActivity}.
 *
 * @author alexander.ivanov
 */
public class OccupantListAdapter extends BaseAdapter implements
        UpdatableAdapter {

    private final Activity activity;
    private final AccountJid account;
    private final EntityBareJid room;

    private final ArrayList<Occupant> occupants;

    public OccupantListAdapter(Activity activity, AccountJid account, EntityBareJid room) {
        this.activity = activity;
        this.account = account;
        this.room = room;
        occupants = new ArrayList<>();
    }

    @Override
    public void onChange() {
        occupants.clear();
        occupants.addAll(MUCManager.getInstance().getOccupants(account, room));
        Collections.sort(occupants);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return occupants.size();
    }

    @Override
    public Object getItem(int position) {
        return occupants.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = activity.getLayoutInflater().inflate(
                    R.layout.item_occupant, parent, false);
        } else {
            view = convertView;
        }
        final Occupant occupant = (Occupant) getItem(position);
        final ImageView avatarView = (ImageView) view.findViewById(R.id.avatar);
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                try {
                    intent = ContactActivity.createIntent(activity, account,
                            UserJid.from(JidCreate.domainFullFrom(room.asDomainBareJid(), occupant.getNickname())));
                    activity.startActivity(intent);
                } catch (UserJid.UserJidCreateException e) {
                    LogManager.exception(this, e);
                }

            }
        });


        final ImageView affilationView = (ImageView) view
                .findViewById(R.id.affilation);
        final TextView nameView = (TextView) view.findViewById(R.id.name);
        final TextView statusTextView = (TextView) view
                .findViewById(R.id.status);
        final ImageView statusModeView = (ImageView) view
                .findViewById(R.id.status_icon);
        if (MUCManager.getInstance().getNickname(account, room).equals(occupant.getNickname())) {
            avatarView.setImageDrawable(AvatarManager.getInstance() .getAccountAvatar(account));
        } else {
            try {
                avatarView.setImageDrawable(AvatarManager.getInstance()
                        .getUserAvatar(UserJid.from(occupant.getJid())));
            } catch (UserJid.UserJidCreateException e) {
                LogManager.exception(this, e);
                // set default avatar
                avatarView.setImageDrawable(
                        ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_avatar_1));
            }
        }
        affilationView.setImageLevel(occupant.getAffiliation().ordinal());
        nameView.setText(occupant.getNickname());
        int textStyle;
        if (occupant.getRole() == MUCRole.moderator)
            textStyle = R.style.OccupantList_Moderator;
        else if (occupant.getRole() == MUCRole.participant)
            textStyle = R.style.OccupantList_Participant;
        else
            textStyle = R.style.OccupantList_Visitor;
        nameView.setTextAppearance(activity, textStyle);
        statusTextView.setText(occupant.getStatusText());
        statusModeView.setImageLevel(occupant.getStatusMode().getStatusLevel());
        return view;
    }
}
