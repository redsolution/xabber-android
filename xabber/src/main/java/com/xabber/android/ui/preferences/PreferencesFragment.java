package com.xabber.android.ui.preferences;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.widget.BaseAdapter;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;
import com.xabber.android.ui.activity.TutorialActivity;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;
import com.xabber.android.ui.widget.XMPPListPreference;

public class PreferencesFragment extends android.preference.PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private OnPreferencesFragmentInteractionListener mListener;
    private XMPPListPreference xmppAccountsPref;

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

        xmppAccountsPref = (XMPPListPreference) getPreferenceScreen().findPreference("preference_accounts_key");
        xmppAccountsPref.setActivity((PreferenceEditor) getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        BaseAdapter adapter = (BaseAdapter) getPreferenceScreen().getRootAdapter();
        adapter.notifyDataSetChanged();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, xmppAccountsPref);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, xmppAccountsPref);
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
        void onThemeChanged();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.interface_theme_key))) {
            mListener.onThemeChanged();
        }
    }
}
