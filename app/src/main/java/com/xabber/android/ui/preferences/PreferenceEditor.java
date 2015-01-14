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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.ui.dialog.ConfirmDialogBuilder;
import com.xabber.android.ui.dialog.ConfirmDialogListener;
import com.xabber.android.ui.dialog.DialogBuilder;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

public class PreferenceEditor extends ManagedActivity
        implements PreferencesFragment.OnPreferencesFragmentInteractionListener,
        Preference.OnPreferenceClickListener, ConfirmDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        setContentView(R.layout.activity_preferences);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.preferences_activity_container, new PreferencesFragment()).commit();
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Force request sound. This will set default value if not specified.
        SettingsManager.eventsSound();
        SettingsManager.chatsAttentionSound();
}

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(getString(R.string.cache_clear_key))) {
            showDialog(R.string.cache_clear_warning);
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        super.onCreateDialog(id);
        switch (id) {
        case R.string.cache_clear_warning:
            return new ConfirmDialogBuilder(this, R.string.cache_clear_warning,
                    this).setMessage(R.string.cache_clear_warning).create();
        case R.string.application_state_closing:
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog
                    .setMessage(getString(R.string.application_state_closing));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            return progressDialog;
        default:
            return null;
        }
    }

    @Override
    public void onAccept(DialogBuilder dialogBuilder) {
        switch (dialogBuilder.getDialogId()) {
        case R.string.cache_clear_warning:
            AccountManager.getInstance()
                    .setStatus(StatusMode.unavailable, null);
            ((Application) getApplication()).requestToClear();
            Application.getInstance().requestToClose();
            showDialog(R.string.application_state_closing);
            break;
        }
    }

    @Override
    public void onDecline(DialogBuilder dialogBuilder) {
    }

    @Override
    public void onCancel(DialogBuilder dialogBuilder) {
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, PreferenceEditor.class);
    }

    @Override
    public Preference.OnPreferenceClickListener getOnPreferenceClickListener() {
        return this;
    }

}
