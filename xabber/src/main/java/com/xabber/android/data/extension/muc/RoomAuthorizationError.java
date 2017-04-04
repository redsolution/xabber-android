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

import android.content.Intent;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.notification.EntityNotificationItem;
import com.xabber.android.ui.activity.ConferenceAddActivity;

public class RoomAuthorizationError extends BaseEntity implements EntityNotificationItem {

    public RoomAuthorizationError(AccountJid account, UserJid user) {
        super(account, user);
    }

    @Override
    public Intent getIntent() {
        return ConferenceAddActivity.createIntent(Application.getInstance(), account, user.getBareUserJid());
    }

    @Override
    public String getTitle() {
        return user.toString();
    }

    @Override
    public String getText() {
        return Application.getInstance().getString(R.string.AUTHENTICATION_FAILED);
    }

}
