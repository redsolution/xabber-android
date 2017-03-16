package com.xabber.android.ui.preferences;

import android.os.Bundle;
import android.preference.PreferenceScreen;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;

public class DebugSettingsFragment extends android.preference.PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_debug);

        PreferenceScreen preferenceScreen = getPreferenceScreen();

        preferenceScreen.removePreference(preferenceScreen.findPreference(getString(R.string.debug_log_key)));
        preferenceScreen.removePreference(preferenceScreen.findPreference(getString(R.string.cache_clear_key)));
        preferenceScreen.removePreference(preferenceScreen.findPreference(getString(R.string.debug_connection_errors_key)));

        if (!BuildConfig.FLAVOR.equals("beta")) {
            preferenceScreen.removePreference(preferenceScreen.findPreference(getString(R.string.debug_crash_reports_key)));
        }

        PreferenceSummaryHelperActivity.updateSummary(preferenceScreen);
    }
}
