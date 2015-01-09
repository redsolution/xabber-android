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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.ui.dialog.ConfirmDialogBuilder;
import com.xabber.android.ui.dialog.ConfirmDialogListener;
import com.xabber.android.ui.dialog.DialogBuilder;
import com.xabber.android.ui.helper.ManagedPreferenceActivity;
import com.xabber.android.ui.helper.PreferenceSummaryHelper;
import com.xabber.androiddev.R;

public class PreferenceEditor extends ManagedPreferenceActivity implements
		OnPreferenceClickListener, OnSharedPreferenceChangeListener,
		ConfirmDialogListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;

		addPreferencesFromResource(R.xml.preference_editor);

		getPreferenceScreen().findPreference(
				getString(R.string.preference_accounts_key)).setIntent(
				AccountList.createIntent(this));
		getPreferenceScreen()
				.findPreference(getString(R.string.events_phrases)).setIntent(
						PhraseList.createIntent(this));

		getPreferenceScreen().findPreference(
				getString(R.string.cache_clear_key))
				.setOnPreferenceClickListener(this);
		getPreferenceScreen().findPreference(
				getString(R.string.security_clear_certificate_key))
				.setOnPreferenceClickListener(this);
		getPreferenceScreen().findPreference(
				getString(R.string.contacts_reset_offline_key))
				.setOnPreferenceClickListener(this);
		getPreferenceScreen().findPreference(getString(R.string.debug_log_key))
				.setEnabled(LogManager.isDebugable());

		// Force request sound. This will set default value if not specified.
		SettingsManager.eventsSound();
		SettingsManager.chatsAttentionSound();

		PreferenceScreen about = (PreferenceScreen) getPreferenceScreen()
				.findPreference(getString(R.string.preference_about_key));
		about.setSummary(getString(R.string.application_name) + "\n"
				+ getString(R.string.application_version));
		about.setIntent(AboutViewer.createIntent(this));
		PreferenceSummaryHelper.updateSummary(getPreferenceScreen());
	}

	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	private void changeGrouping() {
		boolean grouped = SettingsManager.contactsShowAccounts()
				|| SettingsManager.contactsShowGroups();
		((CheckBoxPreference) getPreferenceScreen().findPreference(
				getString(R.string.contacts_stay_active_chats_key)))
				.setChecked(grouped);
		((CheckBoxPreference) getPreferenceScreen().findPreference(
				getString(R.string.contacts_show_empty_groups_key)))
				.setEnabled(grouped);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(getString(R.string.contacts_show_accounts_key))) {
			changeGrouping();
		} else if (key.equals(getString(R.string.contacts_show_groups_key))) {
			changeGrouping();
		} else if (key.equals(getString(R.string.interface_theme_key))) {
			ActivityManager.getInstance().clearStack(true);
			startActivity(ContactList.createIntent(this));
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(getString(R.string.cache_clear_key))) {
			showDialog(R.string.cache_clear_warning);
		} else if (preference.getKey().equals(
				getString(R.string.security_clear_certificate_key))) {
			showDialog(R.string.security_clear_certificate_warning);
		} else if (preference.getKey().equals(
				getString(R.string.contacts_reset_offline_key))) {
			showDialog(R.string.contacts_reset_offline_warning);
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
		case R.string.security_clear_certificate_warning:
			return new ConfirmDialogBuilder(this,
					R.string.security_clear_certificate_warning, this)
					.setMessage(R.string.security_clear_certificate_warning)
					.create();
		case R.string.contacts_reset_offline_warning:
			return new ConfirmDialogBuilder(this,
					R.string.contacts_reset_offline_warning, this).setMessage(
					R.string.contacts_reset_offline_warning).create();
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
		case R.string.security_clear_certificate_warning:
			CertificateManager.getInstance().removeCertificates();
			ConnectionManager.getInstance().updateConnections(true);
			break;
		case R.string.contacts_reset_offline_warning:
			GroupManager.getInstance().resetShowOfflineModes();
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

}
