/*
  Copyright (c) 2013, Redsolution LTD. All rights reserved.

  This file is part of Xabber project; you can redistribute it and/or
  modify it under the terms of the GNU General Public License, Version 3.

  Xabber is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License,
  along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.notification.NotificationManager;

/**
 * Basic service to work in background.
 *
 * @author alexander.ivanov
 */
public class XabberService extends Service {

    private static XabberService instance;

    public static XabberService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        LogManager.i(this, "onCreate");
        changeForeground();
    }

    public void changeForeground() {
        LogManager.i(this, "changeForeground");
        if (needForeground()
                && Application.getInstance().isInitialized()
                && !AccountManager.INSTANCE.getEnabledAccounts().isEmpty()) {
            startForeground(NotificationManager.PERSISTENT_NOTIFICATION_ID,
                    NotificationManager.getInstance().getPersistentNotification());
        } else {
            stopForeground(true);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        LogManager.i(this, "onStartCommand");
        Application.getInstance().onServiceStarted();
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogManager.i(this, "onDestroy");
        stopForeground(true);
        Application.getInstance().onServiceDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, XabberService.class);
    }

    public boolean needForeground() {
        for (AccountJid accountJid : AccountManager.INSTANCE.getEnabledAccounts()) {
            AccountItem accountItem = AccountManager.INSTANCE.getAccount(accountJid);
            if (accountItem != null) {
                return true;
            }
        }
        return false;
    }

    public static void startXabberServiceCompat(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(XabberService.createIntent(context));
        } else {
            context.startService(XabberService.createIntent(context));
        }
    }

}
