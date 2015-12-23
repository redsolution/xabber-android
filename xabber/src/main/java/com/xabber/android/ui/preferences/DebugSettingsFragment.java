package com.xabber.android.ui.preferences;

import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.LogManager;
import com.xabber.android.ui.activity.PreferenceSummaryHelper;

public class DebugSettingsFragment extends android.preference.PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_debug);

        if (!LogManager.isDebugable()) {
            getPreferenceScreen().removePreference(getPreferenceScreen().findPreference(getString(R.string.debug_log_key)));
        }

        PreferenceSummaryHelper.updateSummary(getPreferenceScreen());
    }
}
