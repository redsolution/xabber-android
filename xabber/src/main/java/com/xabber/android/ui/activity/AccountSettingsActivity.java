package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.dialog.AccountDeleteDialog;
import com.xabber.android.ui.dialog.OrbotInstallerDialog;
import com.xabber.android.ui.preferences.AccountEditorFragment;

import java.util.Collection;


public class AccountSettingsActivity extends ManagedActivity
        implements AccountEditorFragment.AccountEditorFragmentInteractionListener, Toolbar.OnMenuItemClickListener,
        OnAccountChangedListener {

    private static final String LOG_TAG = AccountSettingsActivity.class.getSimpleName();
    private AccountJid account;
    private AccountItem accountItem;
    private Toolbar toolbar;
    private BarPainter barPainter;

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @NonNull
    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, AccountSettingsActivity.class).setAccount(account).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);


        account = getAccount(getIntent());
        accountItem = AccountManager.getInstance().getAccount(this.account);
        if (accountItem == null) {
            LogManager.e(LOG_TAG, "Account item is null " + account);
            finish();
            return;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setTitle(R.string.account_connection_settings);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        toolbar.inflateMenu(R.menu.toolbar_account_connection_settings);
        toolbar.setOnMenuItemClickListener(this);

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.account_settings_fragment, new AccountEditorFragment())
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
    public AccountJid getAccount() {
        return account;
    }

    @Override
    public AccountItem getAccountItem() {
        return accountItem;
    }

    @Override
    public void showOrbotDialog() {
        OrbotInstallerDialog.newInstance().show(getFragmentManager(), OrbotInstallerDialog.newInstance().getTag());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_remove_account) {
            AccountDeleteDialog.newInstance(account).show(getSupportFragmentManager(), AccountDeleteDialog.class.getSimpleName());
            return true;
        }

        return false;
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);
            if (accountItem == null) {
                // in case if account was removed
                finish();
            }
        }
    }
}
