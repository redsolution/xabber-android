package com.xabber.android.ui.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.AccountProtocol;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.ui.helper.OrbotHelper;

import java.util.HashMap;
import java.util.Map;

public class AccountEditorFragment extends BaseSettingsFragment
        implements Preference.OnPreferenceClickListener {

    private AccountEditorFragmentInteractionListener mListener;

    private Preference oauthPreference;

    @Override
    protected void onInflate(Bundle savedInstanceState) {
        AccountProtocol protocol = mListener.getAccountItem().getConnectionSettings().getProtocol();
        if (protocol == AccountProtocol.xmpp) {
            addPreferencesFromResource(R.xml.account_editor_xmpp);
        } else if (protocol == AccountProtocol.gtalk) {
            addPreferencesFromResource(R.xml.account_editor_xmpp);
        } else if (protocol == AccountProtocol.wlm) {
            addPreferencesFromResource(R.xml.account_editor_oauth);
        } else {
            throw new IllegalStateException();
        }
        if (!Application.getInstance().isContactsSupported()) {
            getPreferenceScreen().removePreference(findPreference(getString(R.string.account_syncable_key)));
        }

        oauthPreference = findPreference(getString(R.string.account_oauth_key));
        if (oauthPreference != null) {
            oauthPreference.setOnPreferenceClickListener(this);
        }
        onOAuthChange();
        AccountManager.getInstance().removeAuthorizationError(mListener.getAccount());
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChanges();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (getString(R.string.account_port_key).equals(key)) {
            try {
                Integer.parseInt((String) newValue);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), getString(R.string.account_invalid_port),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        if (getString(R.string.account_tls_mode_key).equals(key)
                || getString(R.string.account_archive_mode_key).equals(key)
                || getString(R.string.account_proxy_type_key).equals(key)
                || getString(R.string.account_color_key).equals(key)) {
            preference.setSummary((String) newValue);
        } else if (!getString(R.string.account_password_key).equals(key)
                && !getString(R.string.account_proxy_password_key).equals(key)
                && !getString(R.string.account_priority_key).equals(key)) {
            super.onPreferenceChange(preference, newValue);
        }

        if (getString(R.string.account_proxy_type_key).equals(key)) {
            boolean enabled = !getString(R.string.account_proxy_type_none).equals(newValue)
                    && !getString(R.string.account_proxy_type_orbot).equals(newValue);
            for (int id : new Integer[]{R.string.account_proxy_host_key,
                    R.string.account_proxy_port_key, R.string.account_proxy_user_key,
                    R.string.account_proxy_password_key,}) {
                Preference proxyPreference = findPreference(getString(id));
                if (proxyPreference != null) {
                    proxyPreference.setEnabled(enabled);
                }
            }
        }

        if (getString(R.string.account_color_key).equals(key)) {
            mListener.onColorChange((String)newValue);
        }

        return true;
    }

    public void onOAuthChange() {
        if (oauthPreference == null) {
            return;
        }
        if (AccountEditor.INVALIDATED_TOKEN.equals(mListener.getToken())) {
            oauthPreference.setSummary(R.string.account_oauth_invalidated);
        } else {
            oauthPreference.setSummary(R.string.account_oauth_summary);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (getString(R.string.account_oauth_key).equals(preference.getKey())) {
            mListener.onOAuthClick();
            return true;
        }
        return false;
    }

    @Override
    protected Map<String, Object> getValues() {
        Map<String, Object> source = new HashMap<>();
        AccountItem accountItem = mListener.getAccountItem();

        putValue(source, R.string.account_priority_key, accountItem.getPriority());
        putValue(source, R.string.account_enabled_key, accountItem.isEnabled());
        putValue(source, R.string.account_store_password_key, accountItem.isStorePassword());
        putValue(source, R.string.account_syncable_key, accountItem.isSyncable());
        putValue(source, R.string.account_archive_mode_key, accountItem.getArchiveMode().ordinal());
        putValue(source, R.string.account_color_key, accountItem.getColorIndex());

        com.xabber.android.data.connection.ConnectionSettings connectionSettings = accountItem.getConnectionSettings();
        putValue(source, R.string.account_custom_key, connectionSettings.isCustom());
        putValue(source, R.string.account_host_key, connectionSettings.getHost());
        putValue(source, R.string.account_port_key, connectionSettings.getPort());
        putValue(source, R.string.account_server_key, connectionSettings.getServerName());
        putValue(source, R.string.account_username_key, connectionSettings.getUserName());
        putValue(source, R.string.account_password_key, connectionSettings.getPassword());
        putValue(source, R.string.account_resource_key, connectionSettings.getResource());
        putValue(source, R.string.account_sasl_key, connectionSettings.isSaslEnabled());
        putValue(source, R.string.account_tls_mode_key, connectionSettings.getTlsMode().ordinal());
        putValue(source, R.string.account_compression_key, connectionSettings.useCompression());
        putValue(source, R.string.account_proxy_type_key, connectionSettings.getProxyType().ordinal());
        putValue(source, R.string.account_proxy_host_key, connectionSettings.getProxyHost());
        putValue(source, R.string.account_proxy_port_key, connectionSettings.getProxyPort());
        putValue(source, R.string.account_proxy_user_key, connectionSettings.getProxyUser());
        putValue(source, R.string.account_proxy_password_key, connectionSettings.getProxyPassword());

        return source;
    }

    @Override
    protected Map<String, Object> getPreferences(Map<String, Object> source) {
        Map<String, Object> result = super.getPreferences(source);
        if (oauthPreference != null) {
            putValue(result, R.string.account_password_key, mListener.getAccount());
        }
        return result;
    }

    @Override
    protected boolean setValues(Map<String, Object> source, Map<String, Object> result) {
        ProxyType proxyType = ProxyType.values()[getInt(result, R.string.account_proxy_type_key)];
        if (proxyType == ProxyType.orbot && !OrbotHelper.isOrbotInstalled()) {
            mListener.showOrbotDialog();
            return false;
        }
        AccountManager.getInstance().updateAccount(
                mListener.getAccount(),
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
                TLSMode.values()[getInt(result, R.string.account_tls_mode_key)],
                getBoolean(result, R.string.account_compression_key),
                proxyType,
                getString(result, R.string.account_proxy_host_key),
                getInt(result, R.string.account_proxy_port_key),
                getString(result, R.string.account_proxy_user_key),
                getString(result, R.string.account_proxy_password_key),
                getBoolean(result, R.string.account_syncable_key),
                ArchiveMode.values()[getInt(result, R.string.account_archive_mode_key)],
                getInt(result, R.string.account_color_key)
        );

        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (AccountEditorFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AccountEditorFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface AccountEditorFragmentInteractionListener {
        String getAccount();
        AccountItem getAccountItem();
        String getToken();
        void onOAuthClick();
        void showOrbotDialog();
        void onColorChange(String colorName);
    }
}
