package com.xabber.android.ui.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;

import java.util.Collection;

public class ConnectionSettingsFragment extends android.preference.PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_connection);

        PreferenceSummaryHelperActivity.updateSummary(getPreferenceScreen());

        setDnsResolverSummary(SettingsManager.connectionDnsResolver());
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.connection_dns_resolver_type_key))) {
            String value = sharedPreferences.getString(key, getString(R.string.connection_dns_resolver_type_default));
            SettingsManager.DnsResolverType dnsResolverType = SettingsManager.getDnsResolverType(value);
            setDnsResolverSummary(dnsResolverType);

            // reconnect all enabled account to apply and check changes
            Collection<AccountJid> enabledAccounts = AccountManager.getInstance().getEnabledAccounts();
            for (AccountJid accountJid : enabledAccounts) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                if (accountItem != null) {
                    accountItem.recreateConnection();
                }
            }
        }
    }

    private void setDnsResolverSummary(SettingsManager.DnsResolverType dnsResolverType) {
        Preference preference = findPreference(getString(R.string.connection_dns_resolver_type_key));
        String summary = "";
        switch (dnsResolverType) {
            case dnsJavaResolver:
                summary = getString(R.string.connection_dns_resolver_type_dns_java_resolver);
                break;
            case miniDnsResolver:
                summary = getString(R.string.connection_dns_resolver_type_mini_dns_resolver);
                break;
        }
        preference.setSummary(summary);
    }
}
