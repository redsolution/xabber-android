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
package com.xabber.android.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v7.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.OAuthActivity;
import com.xabber.android.ui.dialog.OrbotInstallerDialogBuilder;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.ManagedActivity;

public class AccountEditor extends ManagedActivity implements
        OnPreferenceClickListener, AccountEditorFragment.AccountEditorFragmentInteractionListener {

    public static final String INVALIDATED_TOKEN = "com.xabber.android.ui.preferences.AccountEditor.INVALIDATED";
    private static final int OAUTH_WML_REQUEST_CODE = 1;
    private static final String SAVED_TOKEN = "com.xabber.android.ui.preferences.AccountEditor.TOKEN";
    private String account;
    private AccountItem accountItem;

    private String token;
    private BarPainter barPainter;

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    public static Intent createIntent(Context context, String account) {
        return new AccountIntentBuilder(context, AccountEditor.class).setAccount(account).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        account = AccountEditor.getAccount(getIntent());
        if (account == null) {
            finish();
            return;
        }
        accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        if (savedInstanceState == null) {
            token = accountItem.getConnectionSettings().getPassword();

            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new AccountEditorFragment()).commit();
        } else {
            token = savedInstanceState.getString(SAVED_TOKEN);
        }

        setContentView(R.layout.activity_with_toolbar_and_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle(AccountManager.getInstance().getVerboseName(account));

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_TOKEN, token);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OAUTH_WML_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (OAuthActivity.isInvalidated(data)) {
                    token = INVALIDATED_TOKEN;
                } else {
                    String value = OAuthActivity.getToken(data);
                    if (value == null) {
                        Application.getInstance().onError(R.string.AUTHENTICATION_FAILED);
                    } else {
                        token = value;
                    }
                }

                ((AccountEditorFragment) getFragmentManager().findFragmentById(
                        R.id.fragment_container)).onOAuthChange();
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (getString(R.string.account_oauth_key).equals(preference.getKey())) {
            startActivityForResult(OAuthActivity.createIntent(this,
                    accountItem.getConnectionSettings().getProtocol()), OAUTH_WML_REQUEST_CODE);
            return true;
        }
        return false;
    }

    @Override
    public String getAccount() {
        return account;
    }

    @Override
    public AccountItem getAccountItem() {
        return accountItem;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void onOAuthClick() {
        startActivityForResult(OAuthActivity.createIntent(this,
                        accountItem.getConnectionSettings().getProtocol()), OAUTH_WML_REQUEST_CODE);
    }

    @Override
    public void showOrbotDialog() {
        OrbotInstallerDialogBuilder.show(this);
    }

    @Override
    public void onColorChange(String colorName) {
        barPainter.updateWithColorName(colorName);
    }
}
