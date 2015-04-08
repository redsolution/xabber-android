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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.AccountType;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.dialog.OrbotInstallerDialogBuilder;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.ui.helper.OrbotHelper;
import com.xabber.androiddev.R;

public class AccountAdd extends ManagedActivity implements View.OnClickListener {

    private static final int ORBOT_DIALOG_ID = 9050;

    private CheckBox storePasswordView;
    private CheckBox useOrbotView;
    private CheckBox createAccount;
    private AccountType accountType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        setContentView(R.layout.account_add);

        accountType = AccountManager.getInstance().getAccountTypes().get(0);

        storePasswordView = (CheckBox) findViewById(R.id.store_password);
        useOrbotView = (CheckBox) findViewById(R.id.use_orbot);
        createAccount = (CheckBox) findViewById(R.id.register_account);

        findViewById(R.id.ok).setOnClickListener(this);
        createAccount.setOnClickListener(this);
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(findViewById(R.id.ok)
                .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_default));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.auth_panel).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.account_user_name)).setHint(accountType.getHint());
        ((TextView) findViewById(R.id.account_help)).setText(accountType.getHelp());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok:
                addAccount();

                break;

            case R.id.register_account:
                LinearLayout passwordConfirmView = (LinearLayout) findViewById(R.id.confirm_password_layout);
                if(createAccount.isChecked()) {
                    passwordConfirmView.setVisibility(View.VISIBLE);
                }
                else {
                    passwordConfirmView.setVisibility(View.GONE);
                }
            default:
                break;
        }
    }

    private void addAccount() {
        if (useOrbotView.isChecked() && !OrbotHelper.isOrbotInstalled()) {
            showDialog(ORBOT_DIALOG_ID);
            return;
        }

        EditText userView = (EditText) findViewById(R.id.account_user_name);
        EditText passwordView = (EditText) findViewById(R.id.account_password);
        EditText passwordConfirmView = (EditText) findViewById(R.id.confirm_password);
        if(createAccount.isChecked() &&
           !passwordView.getText().toString().contentEquals(passwordConfirmView.getText().toString())) {
            Toast.makeText(this, getString(R.string.CONFIRM_PASSWORD),
                    Toast.LENGTH_LONG).show();
            return;
        }
        String account;
        try {
            account = AccountManager.getInstance().addAccount(
                    userView.getText().toString(),
                    passwordView.getText().toString(), accountType,
                    false,
                    storePasswordView.isChecked(),
                    useOrbotView.isChecked(),
                    createAccount.isChecked());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
            return;
        }
        setResult(RESULT_OK, createAuthenticatorResult(account));
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ORBOT_DIALOG_ID) {
            return new OrbotInstallerDialogBuilder(this, ORBOT_DIALOG_ID).create();
        }
        return super.onCreateDialog(id);
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, AccountAdd.class);
    }

    private static Intent createAuthenticatorResult(String account) {
        return new AccountIntentBuilder(null, null).setAccount(account).build();
    }
}
