package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.push.PushState;
import com.xabber.android.ui.color.BarPainter;

import java.util.Collection;

public class AccountPushActivity extends ManagedActivity implements OnAccountChangedListener {

    private Toolbar toolbar;
    private BarPainter barPainter;
    private RelativeLayout rlPushSwitch;
    private Switch switchPush;

    private TextView tvJid;
    private TextView tvStatus;
    private ImageView ivStatus;
    private ProgressBar progressBar;

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

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(R.string.account_push);

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        switchPush = findViewById(R.id.switchPush);
        rlPushSwitch = findViewById(R.id.rlPushSwitch);
        tvJid = findViewById(R.id.tvJid);
        tvStatus = findViewById(R.id.tvStatus);
        ivStatus = findViewById(R.id.ivStatus);
        progressBar = findViewById(R.id.progressBar);

        rlPushSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccountManager.getInstance().setPushEnabled(accountItem, !switchPush.isChecked());
                updateSwitchButton();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccount();
        updateSwitchButton();
        updateTitle();
        updatePushStatus();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        updatePushStatus();
        updateSwitchButton();
    }

    private void updateSwitchButton() {
        switchPush.setChecked(accountItem.isPushEnabled());
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
        tvJid.setText(accountItem.getAccount().getFullJid().asBareJid().toString());
    }

    private void updatePushStatus() {
        PushState pushState = accountItem.getPushState();
        tvStatus.setText(pushState.getStringId());

        switch(pushState) {
            case disabled:
                ivStatus.setImageResource(R.drawable.ic_sync_disable);
                ivStatus.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                break;
            case notSupport:
                ivStatus.setImageResource(R.drawable.ic_sync_disable);
                ivStatus.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                break;
            case connecting:
                ivStatus.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                break;
            case enabled:
                ivStatus.setImageResource(R.drawable.ic_sync_done);
                ivStatus.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                break;
        }
    }
}
