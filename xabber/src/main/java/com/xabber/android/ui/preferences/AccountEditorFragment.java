package com.xabber.android.ui.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.helper.OrbotHelper;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.HashMap;
import java.util.Map;

public class AccountEditorFragment extends BaseSettingsFragment {

    @Nullable
    private AccountEditorFragmentInteractionListener listener;

    @Override
    protected void onInflate(Bundle savedInstanceState) {
        if (listener == null) {
            return;
        }

        addPreferencesFromResource(R.xml.account_editor_xmpp);
        getPreferenceScreen().removePreference(findPreference(getString(R.string.account_sasl_key)));

        AccountManager.getInstance().removeAccountError(listener.getAccount());
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
                int newPort = Integer.parseInt((String) newValue);
                // TODO: Not IPv6 Compatible
                if (newPort < 0 || newPort > 0xFFFF) {
                    Toast.makeText(getActivity(), getString(R.string.account_invalid_port_range),
                    Toast.LENGTH_LONG).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), getString(R.string.account_invalid_port),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        if (getString(R.string.account_proxy_port_key).equals(key)) {
            try {
                int newPort = Integer.parseInt((String) newValue);
                // TODO: Not IPv6 Compatible
                if (newPort < 0 || newPort > 0xFFFF) {
                    Toast.makeText(getActivity(), getString(R.string.account_proxy_invalid_port_range),
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), getString(R.string.account_proxy_invalid_port),
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
            if (getString(R.string.orbot).equals(newValue) && !OrbotHelper.isOrbotInstalled()) {
                listener.showOrbotDialog();
                return false;
            }

            boolean enabled = !getString(R.string.account_proxy_type_none).equals(newValue)
                    && !getString(R.string.orbot).equals(newValue);
            for (int id : new Integer[]{R.string.account_proxy_host_key,
                    R.string.account_proxy_port_key, R.string.account_proxy_user_key,
                    R.string.account_proxy_password_key,}) {
                Preference proxyPreference = findPreference(getString(id));
                if (proxyPreference != null) {
                    proxyPreference.setEnabled(enabled);
                }
            }
        }

        return true;
    }

    @Override
    protected Map<String, Object> getValues() {
        Map<String, Object> source = new HashMap<>();
        AccountItem accountItem = listener.getAccountItem();

        putValue(source, R.string.account_priority_key, accountItem.getPriority());
        putValue(source, R.string.account_enabled_key, accountItem.isEnabled());
        putValue(source, R.string.account_store_password_key, accountItem.isStorePassword());
        putValue(source, R.string.account_syncable_key, accountItem.isSyncable());
        putValue(source, R.string.account_archive_mode_key, accountItem.getArchiveMode().ordinal());
        putValue(source, R.string.account_color_key, accountItem.getColorIndex());

        com.xabber.android.data.connection.ConnectionSettings connectionSettings = accountItem.getConnectionSettings();
        putValue(source, R.string.account_custom_key, connectionSettings.isCustomHostAndPort());
        putValue(source, R.string.account_host_key, connectionSettings.getHost());
        putValue(source, R.string.account_port_key, connectionSettings.getPort());
        putValue(source, R.string.account_server_key, connectionSettings.getServerName().toString());
        putValue(source, R.string.account_username_key, connectionSettings.getUserName().toString());
        putValue(source, R.string.account_password_key, connectionSettings.getPassword());
        putValue(source, R.string.account_resource_key, connectionSettings.getResource().toString());
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
        return super.getPreferences(source);
    }

    @Override
    protected boolean setValues(Map<String, Object> source, Map<String, Object> result) {
        if (listener == null) {
            return false;
        }

        ProxyType proxyType = ProxyType.values()[getInt(result, R.string.account_proxy_type_key)];
        if (proxyType == ProxyType.orbot && !OrbotHelper.isOrbotInstalled()) {
            listener.showOrbotDialog();
            return false;
        }

        DomainBareJid serverName;
        Localpart userName;
        Resourcepart resource;
        try {
            serverName = JidCreate.domainBareFrom(getString(result, R.string.account_server_key).trim());
            userName = Localpart.from(getString(result, R.string.account_username_key).trim());
            resource = Resourcepart.from(getString(result, R.string.account_resource_key).trim());
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            return false;
        }

        AccountManager.getInstance().updateAccount(
                listener.getAccount(),
                getBoolean(result, R.string.account_custom_key),
                getString(result, R.string.account_host_key),
                getInt(result, R.string.account_port_key),
                serverName,
                userName,
                getBoolean(result, R.string.account_store_password_key),
                getString(result, R.string.account_password_key),
                "",
                resource,
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
            listener = (AccountEditorFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AccountEditorFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface AccountEditorFragmentInteractionListener {
        AccountJid getAccount();
        AccountItem getAccountItem();
        void showOrbotDialog();
    }
}
