package com.xabber.android.ui.preferences;


import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;

import com.xabber.android.R;
import com.xabber.android.ui.helper.PreferenceSummaryHelper;

public class PreferencesFragment extends android.preference.PreferenceFragment {

    private OnPreferencesFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_editor);

        Preference about = getPreferenceScreen().findPreference(getString(R.string.preference_about_key));
        about.setSummary(getString(R.string.application_title_full) + "\n" + mListener.getVersionName());

        PreferenceSummaryHelper.updateSummary(getPreferenceScreen());
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
        String getVersionName();
    }
}
