package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.OnAccountChangedListener;
import com.xabber.android.ui.color.BarPainter;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class AccountPushActivity extends ManagedActivity implements OnAccountChangedListener {

    private BarPainter barPainter;
    private RelativeLayout rlPushSwitch;
    private Switch switchPush;
    private TextView tvPushState;

    private AccountItem accountItem;

    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, AccountPushActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_push_notifications);

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(R.string.account_push);

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        switchPush = findViewById(R.id.switchPush);
        rlPushSwitch = findViewById(R.id.rlPushSwitch);
        tvPushState = findViewById(R.id.tvPushState);

        rlPushSwitch.setOnClickListener(v -> {
            AccountManager.getInstance().setPushEnabled(accountItem, !switchPush.isChecked());
            updateSwitchButton();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        checkAccount();
        updateSwitchButton();
        updateTitle();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onAccountsChanged(@Nullable Collection<? extends AccountJid> accounts) {
        Application.getInstance().runOnUiThread(this::updateSwitchButton);
    }

    private void updateSwitchButton() {
        boolean enabled = accountItem.getConnection().isConnected();
        rlPushSwitch.setEnabled(enabled);
        switchPush.setEnabled(enabled);

        switchPush.setChecked(accountItem.isPushEnabled());
        tvPushState.setText(accountItem.isPushWasEnabled()
                ? R.string.account_push_state_enabled : R.string.account_push_state_disabled);
    }

    private void checkAccount() {
        if (AccountManager.getInstance().getAccount(accountItem.getAccount()) == null) {
            // in case if account was removed
            finish();
            return;
        }
    }

    private void updateTitle() {
        barPainter.updateWithAccountName(accountItem.getAccount());
    }

}
