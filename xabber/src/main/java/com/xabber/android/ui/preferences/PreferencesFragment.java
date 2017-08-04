package com.xabber.android.ui.preferences;


import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.BaseAdapter;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;
import com.xabber.android.ui.activity.TutorialActivity;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;

public class PreferencesFragment extends android.preference.PreferenceFragment {

    private OnPreferencesFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_editor);

        Preference about = getPreferenceScreen().findPreference(getString(R.string.preference_about_key));
        about.setSummary(getString(R.string.application_title_full) + "\n" + mListener.getVersionName());

        PreferenceSummaryHelperActivity.updateSummary(getPreferenceScreen());

        Preference xabberAccountPref = (Preference) getPreferenceScreen().findPreference("preference_xabber_account");
        xabberAccountPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                XabberAccount account = XabberAccountManager.getInstance().getAccount();
                if (account != null)
                    startActivity(XabberAccountInfoActivity.createIntent(getActivity()));
                else startActivity(TutorialActivity.createIntent(getActivity()));
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        BaseAdapter adapter = (BaseAdapter) getPreferenceScreen().getRootAdapter();
        adapter.notifyDataSetChanged();
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
