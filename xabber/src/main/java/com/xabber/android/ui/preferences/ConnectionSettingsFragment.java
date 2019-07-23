package com.xabber.android.ui.preferences;

import android.os.Bundle;
import android.preference.Preference;

import com.xabber.android.R;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;
import com.xabber.android.ui.helper.BatteryHelper;

public class ConnectionSettingsFragment extends android.preference.PreferenceFragment {

    private Preference batteryOptimizationPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_connection);

        PreferenceSummaryHelperActivity.updateSummary(getPreferenceScreen());

        batteryOptimizationPreference = findPreference(getString(R.string.battery_optimization_disable_key));
        batteryOptimizationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                BatteryHelper.sendIgnoreButteryOptimizationIntent(getActivity());
                updateBatteryOptimizationPreference();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBatteryOptimizationPreference();
    }

    private void updateBatteryOptimizationPreference() {
        if (!BatteryHelper.isOptimizingBattery())
            batteryOptimizationPreference.setSummary(R.string.battery_optimization_disabled);
        else batteryOptimizationPreference.setSummary(R.string.battery_optimization_enabled);
    }
}
