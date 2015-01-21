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
package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.SubscriptionRequest;
import com.xabber.android.ui.helper.ManagedDialog;
import com.xabber.androiddev.R;

public class ContactSubscription extends ManagedDialog {

    private String account;
    private String user;
    private SubscriptionRequest subscriptionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        account = getAccount(intent);
        user = getUser(intent);
        subscriptionRequest = PresenceManager.getInstance()
                .getSubscriptionRequest(account, user);
        if (subscriptionRequest == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }
        setDialogMessage(subscriptionRequest.getConfirmation());
        setDialogTitle(R.string.subscription_request_message);
        findViewById(android.R.id.button3).setVisibility(View.GONE);
    }

    @Override
    public void onAccept() {
        super.onAccept();
        try {
            PresenceManager.getInstance().acceptSubscription(
                    subscriptionRequest.getAccount(),
                    subscriptionRequest.getUser());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
        startActivity(ContactAdd.createIntent(this, account, user));
        finish();
    }

    @Override
    public void onDecline() {
        super.onDecline();
        try {
            PresenceManager.getInstance().discardSubscription(
                    subscriptionRequest.getAccount(),
                    subscriptionRequest.getUser());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
        finish();
    }

    public static Intent createIntent(Context context, String account,
                                      String user) {
        return new EntityIntentBuilder(context, ContactSubscription.class)
                .setAccount(account).setUser(user).build();
    }

    private static String getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

}
