package com.xabber.android.ui.preferences;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.Nullable;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.mam.LoadHistorySettings;
import com.xabber.android.data.extension.mam.MamManager;

import org.jivesoftware.smackx.mam.element.MamPrefsIQ;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AccountHistorySettingsFragment extends BaseSettingsFragment implements OnAccountChangedListener {
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

        Preference mamPreference = findPreference(getString(R.string.account_mam_default_behavior_key));

        setUpMamPreference(mamPreference, null);
    }

    private void setUpMamPreference(Preference mamPreference, @Nullable String newSummary) {
        Boolean supported = MamManager.getInstance().isSupported(account);
        if (supported == null) {
            mamPreference.setEnabled(false);
            mamPreference.setSummary(getString(R.string.account_chat_history_unknown));
        } else if (!supported) {
            mamPreference.setEnabled(true);
            mamPreference.setSummary(getString(R.string.account_chat_history_not_supported));
        } else {
            mamPreference.setEnabled(true);
            if (newSummary != null) {
                mamPreference.setSummary(newSummary);
            } else {
                ListPreference mamListPreference = (ListPreference) mamPreference;
                mamPreference.setSummary(mamListPreference.getValue());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChanges();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (getString(R.string.account_mam_default_behavior_key).equals(key)) {
            setUpMamPreference(preference, (String) newValue);
        }

        if (getString(R.string.account_mam_sync_key).equals(key)) {
            preference.setSummary((String)newValue);
        }

        return true;
    }

    @Override
    protected Map<String, Object> getValues() {
        Map<String, Object> source = new HashMap<>();

        AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);

        if (accountItem != null) {
            putValue(source, R.string.account_clear_history_on_exit_key, accountItem.isClearHistoryOnExit());

            // order of enum fields is very important!
            putValue(source, R.string.account_mam_default_behavior_key, accountItem.getMamDefaultBehaviour().ordinal());

            putValue(source, R.string.account_mam_sync_key, accountItem.getLoadHistorySettings().ordinal());
        }

        return source;
    }

    @Override
    protected boolean setValues(Map<String, Object> source, Map<String, Object> result) {
        AccountManager.getInstance().setClearHistoryOnExit(account, getBoolean(result, R.string.account_clear_history_on_exit_key));

        // order of enum fields and value array is very important
        int mamBehaviorIndex = getInt(result, R.string.account_mam_default_behavior_key);
        AccountManager.getInstance()
                .setMamDefaultBehaviour(account, MamPrefsIQ.DefaultBehavior.values()[mamBehaviorIndex]);

        int loadHistoryIndex = getInt(result, R.string.account_mam_sync_key);
        AccountManager.getInstance()
                .setLoadHistorySettings(account, LoadHistorySettings.values()[loadHistoryIndex]);
        return true;
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        Preference mamPreference = findPreference(getString(R.string.account_mam_default_behavior_key));
        setUpMamPreference(mamPreference, null);

    }
}
