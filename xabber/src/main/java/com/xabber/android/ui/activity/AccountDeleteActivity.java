package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.IntentHelpersKt;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.OnAccountChangedListener;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.AccountDeleteFragment;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class AccountDeleteActivity extends ManagedActivity implements
        Toolbar.OnMenuItemClickListener, OnAccountChangedListener {

    private static final String LOG_TAG = AccountDeleteActivity.class.getSimpleName();
    private AccountJid account;

    @NonNull
    public static Intent createIntent(Context context, AccountJid account) {
        return IntentHelpersKt.createAccountIntent(context, AccountDeleteActivity.class, account);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_toolbar_and_container);

        account = IntentHelpersKt.getAccountJid(getIntent());
        AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);
        if (accountItem == null) {
            LogManager.e(LOG_TAG, "Account item is null " + account);
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);

        toolbar.setNavigationOnClickListener(v -> finish());

        toolbar.inflateMenu(R.menu.toolbar_account_connection_settings);
        toolbar.getMenu().findItem(R.id.action_remove_account).setIcon(null);
        //toolbar.getMenu().get
        toolbar.setOnMenuItemClickListener(this);

        View view = toolbar.findViewById(R.id.action_remove_account);
        if (view != null && view instanceof TextView) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                ((TextView) view).setTextColor(getResources().getColor(R.color.grey_900));
            else ((TextView) view).setTextColor(Color.WHITE);
        }
        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, AccountDeleteFragment.newInstance(account))
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    protected void onPause() {
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);

        super.onPause();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_remove_account) {
            AccountDeleteFragment f = ((AccountDeleteFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container));
            if (f != null) f.deleteAccount();
        }
        return true;
    }

    @Override
    public void onAccountsChanged(@Nullable Collection<? extends AccountJid> accounts) {
        Application.getInstance().runOnUiThread(() -> {
            if (accounts.contains(account)) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);
                if (accountItem == null) {
                    finish();
                }
            }
        });
    }
}
