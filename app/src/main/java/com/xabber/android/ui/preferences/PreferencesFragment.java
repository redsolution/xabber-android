package com.xabber.android.ui.preferences;


import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;

import com.xabber.android.data.LogManager;
import com.xabber.android.ui.helper.PreferenceSummaryHelper;
import com.xabber.androiddev.R;

public class PreferencesFragment extends android.preference.PreferenceFragment {

    private OnPreferencesFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_editor);

        Preference about = getPreferenceScreen().findPreference(getString(R.string.preference_about_key));
        about.setSummary(getString(R.string.application_name) + "\n" + getVersionName());

        getPreferenceScreen().findPreference(getString(R.string.cache_clear_key))
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
