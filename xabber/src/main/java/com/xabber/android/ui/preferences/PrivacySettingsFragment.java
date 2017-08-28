package com.xabber.android.ui.preferences;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.xabber.android.R;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;

/**
 * Created by valer on 28.08.2017.
 */

public class PrivacySettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_privacy);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        PreferenceSummaryHelperActivity.updateSummary(preferenceScreen);
    }
}
