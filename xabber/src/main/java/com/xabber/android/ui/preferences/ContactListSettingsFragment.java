package com.xabber.android.ui.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;

public class ContactListSettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_contact_list);

        PreferenceSummaryHelperActivity.updateSummary(getPreferenceScreen());
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
        if (key.equals(getString(R.string.contacts_show_accounts_key))
                || key.equals(getString(R.string.contacts_show_circles_key))) {
            changeGrouping();
        }
    }

    private void changeGrouping() {
        boolean grouped = SettingsManager.contactsShowAccounts()
                || SettingsManager.contactsShowGroups();
        getPreferenceScreen().findPreference(getString(R.string.contacts_show_empty_circles_key))
                .setEnabled(grouped);
    }

}
