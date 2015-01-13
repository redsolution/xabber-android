package com.xabber.android.ui.preferences;


import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.ContactList;
import com.xabber.android.ui.helper.PreferenceSummaryHelper;
import com.xabber.androiddev.R;

public class PreferencesFragment extends android.preference.PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    private OnPreferencesFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_editor);

        Preference about = getPreferenceScreen().findPreference(getString(R.string.preference_about_key));
        about.setSummary(getString(R.string.application_name) + "\n" + getVersionName());

        getPreferenceScreen().findPreference(getString(R.string.cache_clear_key))
                .setOnPreferenceClickListener(mListener.getOnPreferenceClickListener());
        getPreferenceScreen().findPreference(getString(R.string.security_clear_certificate_key))
                .setOnPreferenceClickListener(mListener.getOnPreferenceClickListener());
        getPreferenceScreen().findPreference(getString(R.string.contacts_reset_offline_key))
                .setOnPreferenceClickListener(mListener.getOnPreferenceClickListener());
        getPreferenceScreen().findPreference(getString(R.string.debug_log_key))
                .setEnabled(LogManager.isDebugable());

        PreferenceSummaryHelper.updateSummary(getPreferenceScreen());
    }

     private String getVersionName() {
         try {
             PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
             return pInfo.versionName;
         } catch (PackageManager.NameNotFoundException e) {
             e.printStackTrace();
         }
         return "";
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
        if (key.equals(getString(R.string.contacts_show_accounts_key))) {
            changeGrouping();
        } else if (key.equals(getString(R.string.contacts_show_groups_key))) {
            changeGrouping();
        } else if (key.equals(getString(R.string.interface_theme_key))) {
            ActivityManager.getInstance().clearStack(true);
            startActivity(ContactList.createIntent(getActivity()));
        }
    }

    private void changeGrouping() {
        boolean grouped = SettingsManager.contactsShowAccounts()
                || SettingsManager.contactsShowGroups();
        ((CheckBoxPreference) getPreferenceScreen().findPreference(
                getString(R.string.contacts_stay_active_chats_key)))
                .setChecked(grouped);
        getPreferenceScreen().findPreference(getString(R.string.contacts_show_empty_groups_key))
                .setEnabled(grouped);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPreferencesFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnPreferencesFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnPreferencesFragmentInteractionListener {
        Preference.OnPreferenceClickListener getOnPreferenceClickListener();
    }
}
