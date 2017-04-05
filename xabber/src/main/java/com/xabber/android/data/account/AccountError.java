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
package com.xabber.android.data.account;

import android.content.Context;
import android.content.Intent;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.entity.AccountRelated;
import com.xabber.android.data.notification.AccountNotificationItem;
import com.xabber.android.ui.activity.AccountActivity;

class AccountError extends AccountRelated implements
        AccountNotificationItem {

    private final AccountErrorEvent.Type type;

    AccountError(AccountErrorEvent accountErrorEvent) {
        super(accountErrorEvent.getAccount());
        type = accountErrorEvent.getType();
    }

    @Override
    public Intent getIntent() {
        Context context = Application.getInstance().getApplicationContext();

        return AccountActivity.createConnectionSettingsIntent(context, account);
    }

    @Override
    public String getTitle() {
        switch (type) {
            case AUTHORIZATION:
                return Application.getInstance().getString(R.string.AUTHENTICATION_FAILED);
            case CONNECTION:
                return Application.getInstance().getString(R.string.CONNECTION_FAILED);
        }
        return null;
    }

    @Override
    public String getText() {
        return AccountManager.getInstance().getVerboseName(account);
    }

}
