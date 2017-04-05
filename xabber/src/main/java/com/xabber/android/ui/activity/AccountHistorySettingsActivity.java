package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.preferences.AccountHistorySettingsFragment;

public class AccountHistorySettingsActivity extends ManagedActivity {

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @NonNull
    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, AccountHistorySettingsActivity.class).setAccount(account).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_history_settings);


        AccountJid account = getAccount(getIntent());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setTitle(R.string.account_chat_history);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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
