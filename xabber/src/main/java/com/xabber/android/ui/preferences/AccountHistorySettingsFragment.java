package com.xabber.android.ui.preferences;


import android.os.Bundle;
import android.preference.Preference;

import com.xabber.android.R;

import java.util.HashMap;
import java.util.Map;

public class AccountHistorySettingsFragment extends BaseSettingsFragment {
    @Override
    protected void onInflate(Bundle savedInstanceState) {
        addPreferencesFromResource(R.xml.account_history_settings);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChanges();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();


        if (getString(R.string.account_history_mam_key).equals(key)) {
            preference.setSummary((String) newValue);
        }

        return true;
    }


    @Override
    protected Map<String, Object> getValues() {
        Map<String, Object> source = new HashMap<>();

        putValue(source, R.string.account_clear_history_on_exit_key, false);
        putValue(source, R.string.account_history_mam_key, 0);

        return source;
    }

    @Override
    protected boolean setValues(Map<String, Object> source, Map<String, Object> result) {
        getBoolean(result, R.string.account_clear_history_on_exit_key);

        return true;
    }
}
