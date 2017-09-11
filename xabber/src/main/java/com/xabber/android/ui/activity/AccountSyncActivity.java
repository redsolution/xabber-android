package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.BarPainter;

/**
 * Created by valery.miller on 11.09.17.
 */

public class AccountSyncActivity extends ManagedActivity implements View.OnClickListener {

    private Toolbar toolbar;
    private BarPainter barPainter;
    private RelativeLayout rlSyncSwitch;
    private Button btnDeleteSettings;
    private Switch switchSync;

    private AccountItem accountItem;
    private String jid;

    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, AccountSyncActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_synchronization);

        final Intent intent = getIntent();

        AccountJid account = getAccount(intent);
        if (account == null) {
            finish();
            return;
        }

        accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        jid = accountItem.getAccount().getFullJid().asBareJid().toString();

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(R.string.account_sync);

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        btnDeleteSettings = (Button) findViewById(R.id.btnDeleteSettings);
        switchSync = (Switch) findViewById(R.id.switchSync);
        rlSyncSwitch = (RelativeLayout) findViewById(R.id.rlSyncSwitch);
        rlSyncSwitch.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSyncSwitchButton();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rlSyncSwitch:
                SettingsManager.setSyncAllAccounts(false);
                XabberAccountManager.getInstance().addAccountSyncState(jid, !switchSync.isChecked());
                updateSyncSwitchButton();
                break;
            case R.id.btnDeleteSettings:
                break;
        }
    }

    private void updateSyncSwitchButton() {
        switchSync.setChecked(XabberAccountManager.getInstance().isAccountSynchronize(jid)
                || SettingsManager.isSyncAllAccounts());
    }
}
