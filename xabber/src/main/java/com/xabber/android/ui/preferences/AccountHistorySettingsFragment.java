package com.xabber.android.ui.preferences;


import android.os.Bundle;
import android.preference.Preference;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

import org.jivesoftware.smackx.mam.element.MamPrefsIQ;

import java.util.HashMap;
import java.util.Map;

public class AccountHistorySettingsFragment extends BaseSettingsFragment {
    private static final String ARGUMENT_ACCOUNT = AccountHistorySettingsFragment.class.getName() + "ARGUMENT_ACCOUNT";
    private AccountJid account;

    public static AccountHistorySettingsFragment newInstance(AccountJid account) {
        AccountHistorySettingsFragment fragment = new AccountHistorySettingsFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            account = getArguments().getParcelable(ARGUMENT_ACCOUNT);
        }
    }

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

        if (getString(R.string.account_mam_default_behavior_key).equals(key)) {
            preference.setSummary((String) newValue);
        }

        return true;
    }

    @Override
    protected Map<String, Object> getValues() {
        Map<String, Object> source = new HashMap<>();

        AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);

        putValue(source, R.string.account_clear_history_on_exit_key, accountItem.isClearHistoryOnExit());

        // order of enum fields is very important!
        putValue(source, R.string.account_mam_default_behavior_key, accountItem.getMamDefaultBehaviour().ordinal());

        return source;
    }

    @Override
    protected boolean setValues(Map<String, Object> source, Map<String, Object> result) {
        AccountManager.getInstance().setClearHistoryOnExit(account, getBoolean(result, R.string.account_clear_history_on_exit_key));

        // order of enum fields and value array is very important
        int index = getInt(result, R.string.account_mam_default_behavior_key);
        AccountManager.getInstance().setMamDefaultBehaviour(account, MamPrefsIQ.DefaultBehavior.values()[index]);
        return true;
    }
}
