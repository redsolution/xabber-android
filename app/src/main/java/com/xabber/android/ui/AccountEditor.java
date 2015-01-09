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

import java.util.HashMap;
import java.util.Map;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.AccountProtocol;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.dialog.OrbotInstallerDialogBuilder;
import com.xabber.android.ui.helper.BaseSettingsActivity;
import com.xabber.android.ui.helper.OrbotHelper;
import com.xabber.androiddev.R;

public class AccountEditor extends BaseSettingsActivity implements
		OnPreferenceClickListener {

	private static final int OAUTH_WML_REQUEST_CODE = 1;

	private static final String SAVED_TOKEN = "com.xabber.android.ui.AccountEditor.TOKEN";

	private static final String INVALIDATED_TOKEN = "com.xabber.android.ui.AccountEditor.INVALIDATED";

	private static final int ORBOT_DIALOG_ID = 9050;

	private String account;
	private AccountItem accountItem;

	private String token;

	private Preference oauthPreference;

	@Override
	protected void onInflate(Bundle savedInstanceState) {
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
		AccountProtocol protocol = accountItem.getConnectionSettings()
				.getProtocol();
		if (protocol == AccountProtocol.xmpp)
			addPreferencesFromResource(R.xml.account_editor_xmpp);
		else if (protocol == AccountProtocol.gtalk)
			addPreferencesFromResource(R.xml.account_editor_xmpp);
		else if (protocol == AccountProtocol.wlm)
			addPreferencesFromResource(R.xml.account_editor_oauth);
		else
			throw new IllegalStateException();
		if (!Application.getInstance().isContactsSupported())
			getPreferenceScreen().removePreference(
					findPreference(getString(R.string.account_syncable_key)));
		setTitle(getString(R.string.account_editor_title,
				getString(protocol.getShortResource()), AccountManager
						.getInstance().getVerboseName(account)));
		if (savedInstanceState == null)
			token = accountItem.getConnectionSettings().getPassword();
		else
			token = savedInstanceState.getString(SAVED_TOKEN);
		oauthPreference = findPreference(getString(R.string.account_oauth_key));
		if (oauthPreference != null)
			oauthPreference.setOnPreferenceClickListener(this);
		onOAuthChange();
		AccountManager.getInstance().removeAuthorizationError(account);
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
					if (value == null)
						Application.getInstance().onError(
								R.string.AUTHENTICATION_FAILED);
					else
						token = value;
				}
				onOAuthChange();
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (getString(R.string.account_port_key).equals(preference.getKey()))
			try {
				Integer.parseInt((String) newValue);
			} catch (NumberFormatException e) {
				Toast.makeText(this, getString(R.string.account_invalid_port),
						Toast.LENGTH_LONG).show();
				return false;
			}
		if (getString(R.string.account_tls_mode_key)
				.equals(preference.getKey())
				|| getString(R.string.account_archive_mode_key).equals(
						preference.getKey())
				|| getString(R.string.account_proxy_type_key).equals(
						preference.getKey()))
			preference.setSummary((String) newValue);
		else if (!getString(R.string.account_password_key).equals(
				preference.getKey())
				&& !getString(R.string.account_proxy_password_key).equals(
						preference.getKey())
				&& !getString(R.string.account_priority_key).equals(
						preference.getKey()))
			super.onPreferenceChange(preference, newValue);
		if (getString(R.string.account_proxy_type_key).equals(
				preference.getKey())) {
			boolean enabled = !getString(R.string.account_proxy_type_none)
					.equals(newValue)
					&& !getString(R.string.account_proxy_type_orbot).equals(
							newValue);
			for (int id : new Integer[] { R.string.account_proxy_host_key,
					R.string.account_proxy_port_key,
					R.string.account_proxy_user_key,
					R.string.account_proxy_password_key, }) {
				Preference proxyPreference = findPreference(getString(id));
				if (proxyPreference != null)
					proxyPreference.setEnabled(enabled);
			}
		}
		return true;
	}

	private void onOAuthChange() {
		if (oauthPreference == null)
			return;
		if (INVALIDATED_TOKEN.equals(token))
			oauthPreference.setSummary(R.string.account_oauth_invalidated);
		else
			oauthPreference.setSummary(R.string.account_oauth_summary);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (getString(R.string.account_oauth_key).equals(preference.getKey())) {
			startActivityForResult(OAuthActivity.createIntent(this, accountItem
					.getConnectionSettings().getProtocol()),
					OAUTH_WML_REQUEST_CODE);
			return true;
		}
		return false;
	}

	@Override
	protected Map<String, Object> getValues() {
		Map<String, Object> source = new HashMap<String, Object>();
		putValue(source, R.string.account_custom_key, accountItem
				.getConnectionSettings().isCustom());
		putValue(source, R.string.account_host_key, accountItem
				.getConnectionSettings().getHost());
		putValue(source, R.string.account_port_key, accountItem
				.getConnectionSettings().getPort());
		putValue(source, R.string.account_server_key, accountItem
				.getConnectionSettings().getServerName());
		putValue(source, R.string.account_username_key, accountItem
				.getConnectionSettings().getUserName());
		putValue(source, R.string.account_store_password_key,
				accountItem.isStorePassword());
		putValue(source, R.string.account_password_key, accountItem
				.getConnectionSettings().getPassword());
		putValue(source, R.string.account_resource_key, accountItem
				.getConnectionSettings().getResource());
		putValue(source, R.string.account_priority_key,
				accountItem.getPriority());
		putValue(source, R.string.account_enabled_key, accountItem.isEnabled());
		putValue(source, R.string.account_sasl_key, accountItem
				.getConnectionSettings().isSaslEnabled());
		putValue(
				source,
				R.string.account_tls_mode_key,
				Integer.valueOf(accountItem.getConnectionSettings()
						.getTlsMode().ordinal()));
		putValue(source, R.string.account_compression_key, accountItem
				.getConnectionSettings().useCompression());
		putValue(
				source,
				R.string.account_proxy_type_key,
				Integer.valueOf(accountItem.getConnectionSettings()
						.getProxyType().ordinal()));
		putValue(source, R.string.account_proxy_host_key, accountItem
				.getConnectionSettings().getProxyHost());
		putValue(source, R.string.account_proxy_port_key, accountItem
				.getConnectionSettings().getProxyPort());
		putValue(source, R.string.account_proxy_user_key, accountItem
				.getConnectionSettings().getProxyUser());
		putValue(source, R.string.account_proxy_password_key, accountItem
				.getConnectionSettings().getProxyPassword());
		putValue(source, R.string.account_syncable_key,
				accountItem.isSyncable());
		putValue(source, R.string.account_archive_mode_key,
				Integer.valueOf(accountItem.getArchiveMode().ordinal()));
		return source;
	}

	@Override
	protected Map<String, Object> getPreferences(Map<String, Object> source) {
		Map<String, Object> result = super.getPreferences(source);
		if (oauthPreference != null)
			putValue(result, R.string.account_password_key, token);
		return result;
	}

	@Override
	protected boolean setValues(Map<String, Object> source,
			Map<String, Object> result) {
		ProxyType proxyType = ProxyType.values()[getInt(result,
				R.string.account_proxy_type_key)];
		if (proxyType == ProxyType.orbot && !OrbotHelper.isOrbotInstalled()) {
			showDialog(ORBOT_DIALOG_ID);
			return false;
		}
		AccountManager
				.getInstance()
				.updateAccount(
						account,
						getBoolean(result, R.string.account_custom_key),
						getString(result, R.string.account_host_key),
						getInt(result, R.string.account_port_key),
						getString(result, R.string.account_server_key),
						getString(result, R.string.account_username_key),
						getBoolean(result, R.string.account_store_password_key),
						getString(result, R.string.account_password_key),
						getString(result, R.string.account_resource_key),
						getInt(result, R.string.account_priority_key),
						getBoolean(result, R.string.account_enabled_key),
						getBoolean(result, R.string.account_sasl_key),
						TLSMode.values()[getInt(result,
								R.string.account_tls_mode_key)],
						getBoolean(result, R.string.account_compression_key),
						proxyType,
						getString(result, R.string.account_proxy_host_key),
						getInt(result, R.string.account_proxy_port_key),
						getString(result, R.string.account_proxy_user_key),
						getString(result, R.string.account_proxy_password_key),
						getBoolean(result, R.string.account_syncable_key),
						ArchiveMode.values()[getInt(result,
								R.string.account_archive_mode_key)]);
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ORBOT_DIALOG_ID) {
			return new OrbotInstallerDialogBuilder(this, ORBOT_DIALOG_ID)
					.create();
		}
		return super.onCreateDialog(id);
	}

	private static String getAccount(Intent intent) {
		return AccountIntentBuilder.getAccount(intent);
	}

	public static Intent createIntent(Context context, String account) {
		return new AccountIntentBuilder(context, AccountEditor.class)
				.setAccount(account).build();
	}

}
