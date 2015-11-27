package com.xabber.android.data.message.chat;

import android.content.Intent;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.notification.EntityNotificationItem;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ContactList;

public class MucPrivateChatNotification extends BaseEntity implements EntityNotificationItem {

    public MucPrivateChatNotification(String account, String user) {
        super(account, user);
    }

    @Override
    public Intent getIntent() {
        return ContactList.createMucPrivateChatInviteIntent(Application.getInstance(), account, user);
    }

    @Override
    public String getTitle() {
        return RosterManager.getInstance().getBestContact(account, user).getName();
    }

    @Override
    public String getText() {
        return Application.getInstance().getString(R.string.conference_private_chat);
    }

}
