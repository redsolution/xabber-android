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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v7.widget.Toolbar;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.OAuthActivity;
import com.xabber.android.ui.dialog.OrbotInstallerDialogBuilder;
import com.xabber.android.ui.helper.ActionBarPainter;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

public class AccountEditor extends ManagedActivity implements
        OnPreferenceClickListener, AccountEditorFragment.AccountEditorFragmentInteractionListener {

    private static final int OAUTH_WML_REQUEST_CODE = 1;

    private static final String SAVED_TOKEN = "com.xabber.android.ui.preferences.AccountEditor.TOKEN";

    public static final String INVALIDATED_TOKEN = "com.xabber.android.ui.preferences.AccountEditor.INVALIDATED";

    private static final int ORBOT_DIALOG_ID = 9050;

    private String account;
    private AccountItem accountItem;

    private String token;
    private ActionBarPainter actionBarPainter;

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
                    .add(R.id.preferences_activity_container, new AccountEditorFragment()).commit();
        } else {
            token = savedInstanceState.getString(SAVED_TOKEN);
        }

        setContentView(R.layout.activity_preferences);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_default));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(AccountManager.getInstance().getVerboseName(account));

        actionBarPainter = new ActionBarPainter(this);
        actionBarPainter.updateWithAccountName(account);
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
                        R.id.preferences_activity_container)).onOAuthChange();
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
    protected Dialog onCreateDialog(int id) {
        if (id == ORBOT_DIALOG_ID) {
            return new OrbotInstallerDialogBuilder(this, ORBOT_DIALOG_ID).create();
        }
        return super.onCreateDialog(id);
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    public static Intent createIntent(Context context, String account) {
        return new AccountIntentBuilder(context, AccountEditor.class).setAccount(account).build();
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
        showDialog(ORBOT_DIALOG_ID);
    }

    @Override
    public void onColorChange(String colorName) {
        actionBarPainter.updateWithColorName(colorName);
    }
}
