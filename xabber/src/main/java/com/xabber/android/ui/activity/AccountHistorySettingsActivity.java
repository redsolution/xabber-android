package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.IntentHelpersKt;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.preferences.AccountHistorySettingsFragment;

public class AccountHistorySettingsActivity extends ManagedActivity {

    @NonNull
    public static Intent createIntent(Context context, AccountJid account) {
        return IntentHelpersKt.createAccountIntent(
                context, AccountHistorySettingsActivity.class, account
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_history_settings);

        AccountJid account = IntentHelpersKt.getAccountJid(getIntent());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        }
        toolbar.setTitle(R.string.account_chat_history);
        toolbar.setNavigationOnClickListener(v -> finish());

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.account_history_settings_fragment,
                            AccountHistorySettingsFragment.newInstance(account))
                    .commit();
        }

    }

}
