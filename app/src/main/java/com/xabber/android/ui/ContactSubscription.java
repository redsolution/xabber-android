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
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.SubscriptionRequest;
import com.xabber.android.ui.helper.AccountPainter;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.SingleActivity;

public class ContactSubscription extends SingleActivity implements View.OnClickListener {

    private String account;
    private String user;
    private SubscriptionRequest subscriptionRequest;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        account = getAccount(intent);
        user = getUser(intent);
        subscriptionRequest = PresenceManager.getInstance().getSubscriptionRequest(account, user);
        if (subscriptionRequest == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }

        setContentView(R.layout.contact_subscription);
        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle(getString(R.string.subscription_request_message));

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);
        AccountPainter accountPainter = new AccountPainter(this);

        View fakeToolbar = findViewById(R.id.fake_toolbar);

        fakeToolbar.setBackgroundColor(accountPainter.getAccountMainColor(account));
        toolbar.setBackgroundResource(android.R.color.transparent);

        AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);

        ((ImageView)fakeToolbar.findViewById(R.id.avatar)).setImageDrawable(abstractContact.getAvatar());

        ((TextView)fakeToolbar.findViewById(R.id.dialog_message)).setText(subscriptionRequest.getConfirmation());

        Button acceptButton = (Button) findViewById(R.id.accept_button);
        acceptButton.setTextColor(accountPainter.getAccountMainColor(account));
        acceptButton.setOnClickListener(this);

        findViewById(R.id.decline_button).setOnClickListener(this);


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.scrollable_container, ContactVcardViewerFragment.newInstance(account, user)).commit();
        }


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.accept_button:
                onAccept();
                break;

            case R.id.decline_button:
                onDecline();
                break;
        }
    }

    public void onAccept() {
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

    public void onDecline() {
        try {
            PresenceManager.getInstance().discardSubscription(
                    subscriptionRequest.getAccount(),
                    subscriptionRequest.getUser());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
        finish();
    }

}
