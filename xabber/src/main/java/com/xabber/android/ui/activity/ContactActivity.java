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
package com.xabber.android.ui.activity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.fragment.ConferenceInfoFragment;
import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
import com.xabber.android.ui.helper.ContactTitleInflater;

import java.util.Collection;

public class ContactActivity extends ManagedActivity implements
        OnContactChangedListener, OnAccountChangedListener, ContactVcardViewerFragment.Listener {

    private static final String LOG_TAG = ContactActivity.class.getSimpleName();
    private AccountJid account;
    private UserJid user;
    private Toolbar toolbar;
    private View contactTitleView;
    private AbstractContact bestContact;
    private CollapsingToolbarLayout collapsingToolbar;

    public static Intent createIntent(Context context, AccountJid account, UserJid user) {
        return new EntityIntentBuilder(context, ContactActivity.class)
                .setAccount(account).setUser(user).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static UserJid getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    protected Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        account = getAccount(getIntent());
        user = getUser(getIntent());

        AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);
        if (accountItem == null) {
            LogManager.e(LOG_TAG, "Account item is null " + account);
            finish();
            return;
        }

        if (user != null && user.getBareJid().equals(account.getFullJid().asBareJid())) {
            try {
                user = UserJid.from(accountItem.getRealJid().asBareJid());
            } catch (UserJid.UserJidCreateException e) {
                LogManager.exception(this, e);
            }
        }

        if (account == null || user == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }

        setContentView(R.layout.activity_contact);

        if (savedInstanceState == null) {

            Fragment fragment;
            if (MUCManager.getInstance().hasRoom(account, user)) {
                fragment = ConferenceInfoFragment.newInstance(account, user.getJid().asEntityBareJidIfPossible());
            } else {
                fragment = ContactVcardViewerFragment.newInstance(account, user);
            }

            getSupportFragmentManager().beginTransaction().add(R.id.scrollable_container, fragment).commit();


        }

        bestContact = RosterManager.getInstance().getBestContact(account, user);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        StatusBarPainter statusBarPainter = new StatusBarPainter(this);
        statusBarPainter.updateWithAccountName(account);

        final int accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

/*        MessageManager messageManager = MessageManager.getInstance();
        AbstractChat chat = messageManager.getOrCreateChat(bestContact.getAccount(), bestContact.getUser());*/


        contactTitleView = findViewById(R.id.contact_title_expanded);
 /*       if(chat.isGroupchat()){
            findViewById(R.id.ivStatus).setVisibility(View.GONE);
            findViewById(R.id.ivStatusGroupchat).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.ivStatus).setVisibility(View.VISIBLE);
            findViewById(R.id.ivStatusGroupchat).setVisibility(View.GONE);
        }*/
        contactTitleView.setBackgroundColor(accountMainColor);
        TextView contactNameView = (TextView) findViewById(R.id.name);
        contactNameView.setVisibility(View.INVISIBLE);

        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(bestContact.getName());

        collapsingToolbar.setBackgroundColor(accountMainColor);
        collapsingToolbar.setContentScrimColor(accountMainColor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact);
        updateName();
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(account, user)) {
                updateName();
                break;
            }
        }
    }

    private void updateName() {
        if (MUCManager.getInstance().isMucPrivateChat(account, user)) {
            String vCardName = VCardManager.getInstance().getName(user.getJid());
            if (!"".equals(vCardName)) {
                collapsingToolbar.setTitle(vCardName);
            } else {
                collapsingToolbar.setTitle(user.getJid().getResourceOrNull().toString());
            }

        } else {
            collapsingToolbar.setTitle(RosterManager.getInstance().getBestContact(account, user).getName());
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            updateName();
        }
    }

    protected AccountJid getAccount() {
        return account;
    }

    protected UserJid getUser() {
        return user;
    }

    @Override
    public void onVCardReceived() {
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact);
    }

    @Override
    public void registerVCardFragment(ContactVcardViewerFragment fragment) {}
}
