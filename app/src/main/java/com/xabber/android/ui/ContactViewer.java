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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleExpandableToolbarInflater;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.xmpp.address.Jid;

import java.util.Collection;
import java.util.List;

public class ContactViewer extends ManagedActivity implements
        OnContactChangedListener, OnAccountChangedListener {

    private String account;
    private String bareAddress;

    private ContactTitleExpandableToolbarInflater contactTitleExpandableToolbarInflater;
    private TextView contactNameView;

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactViewer.class)
                .setAccount(account).setUser(user).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            // View information about contact from system contact list
            Uri data = getIntent().getData();
            if (data != null && "content".equals(data.getScheme())) {
                List<String> segments = data.getPathSegments();
                if (segments.size() == 2 && "data".equals(segments.get(0))) {
                    Long id;
                    try {
                        id = Long.valueOf(segments.get(1));
                    } catch (NumberFormatException e) {
                        id = null;
                    }
                    if (id != null)
                        // FIXME: Will be empty while application is loading
                        for (RosterContact rosterContact : RosterManager.getInstance().getContacts())
                            if (id.equals(rosterContact.getViewId())) {
                                account = rosterContact.getAccount();
                                bareAddress = rosterContact.getUser();
                                break;
                            }
                }
            }
        } else {
            account = getAccount(getIntent());
            bareAddress = Jid.getBareAddress(getUser(getIntent()));
        }

        if (bareAddress != null && bareAddress.equalsIgnoreCase(GroupManager.IS_ACCOUNT)) {
            bareAddress = Jid.getBareAddress(AccountManager.getInstance().getAccount(account).getRealJid());
        }

        if (account == null || bareAddress == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.scrollable_container, ContactVcardViewerFragment.newInstance(account, bareAddress)).commit();
        }


        contactTitleExpandableToolbarInflater = new ContactTitleExpandableToolbarInflater(this);
        AbstractContact bestContact = RosterManager.getInstance().getBestContact(account, bareAddress);
        contactTitleExpandableToolbarInflater.onCreate(bestContact);

        View contactTitleView = findViewById(R.id.expandable_contact_title);
        contactTitleView.findViewById(R.id.status_icon).setVisibility(View.GONE);
        contactTitleView.findViewById(R.id.status_text).setVisibility(View.GONE);
        contactNameView = (TextView) contactTitleView.findViewById(R.id.name);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);

        contactTitleExpandableToolbarInflater.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(account, bareAddress)) {
                contactNameView.setText(RosterManager.getInstance().getBestContact(account, bareAddress).getName());
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(account)) {
            contactNameView.setText(RosterManager.getInstance().getBestContact(account, bareAddress).getName());
        }
    }

    protected String getAccount() {
        return account;
    }

    protected String getBareAddress() {
        return bareAddress;
    }
}
