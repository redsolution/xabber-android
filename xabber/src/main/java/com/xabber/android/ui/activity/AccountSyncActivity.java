package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.utils.RetrofitErrorConverter;

import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 11.09.17.
 */

public class AccountSyncActivity extends ManagedActivity implements View.OnClickListener {

    private final static String LOG_TAG = AccountSyncActivity.class.getSimpleName();

    private Toolbar toolbar;
    private BarPainter barPainter;
    private RelativeLayout rlSyncSwitch;
    private Button btnDeleteSettings;
    private Switch switchSync;
    private ProgressDialog progressDialog;

    private AccountItem accountItem;
    private String jid;
    protected CompositeSubscription compositeSubscription = new CompositeSubscription();
    private boolean dialogShowed;

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
        btnDeleteSettings.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccount();
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
                if (NetworkManager.isNetworkAvailable()) {
                    if (!dialogShowed) {
                        dialogShowed = true;
                        showDeleteDialog();
                    }
                } else
                    Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void updateSyncSwitchButton() {
        switchSync.setChecked(XabberAccountManager.getInstance().isAccountSynchronize(jid)
                || SettingsManager.isSyncAllAccounts());
    }

    private void deleteAccountSettings(boolean deleteAccount) {
        showProgress(getResources().getString(R.string.progress_title_delete_settings));

        if (XabberAccountManager.getInstance().getAccountSyncState(jid) != null && !deleteAccount) {
            SettingsManager.setSyncAllAccounts(false);
            XabberAccountManager.getInstance().setAccountSyncState(jid, false);
        }

        Subscription deleteSubscription = AuthManager.deleteClientSettings(jid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> settings) {
                        handleSuccessDelete(settings);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorDelete(throwable);
                    }
                });
        compositeSubscription.add(deleteSubscription);
    }

    private void handleSuccessDelete(List<XMPPAccountSettings> settings) {
        hideProgress();
        Toast.makeText(this, R.string.settings_delete_success, Toast.LENGTH_SHORT).show();
    }

    private void handleErrorDelete(Throwable throwable) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.equals("Invalid token")) {
                XabberAccountManager.getInstance().onInvalidToken();
            } else {
                Log.d(LOG_TAG, "Error while deleting settings: " + message);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG, "Error while deleting settings: " + throwable.toString());
            Toast.makeText(this, "Error while deleting settings: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
        hideProgress();
    }

    protected void showProgress(String title) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title);
        progressDialog.setMessage(getResources().getString(R.string.progress_message));
        progressDialog.show();
    }

    protected void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
            checkAccount();
        }
    }

    private void checkAccount() {
        if (AccountManager.getInstance().getAccount(accountItem.getAccount()) == null) {
            // in case if account was removed
            finish();
            return;
        }

        if (XabberAccountManager.getInstance().getAccountSyncState(jid) != null)
            btnDeleteSettings.setVisibility(View.VISIBLE);
        else btnDeleteSettings.setVisibility(View.GONE);
    }

    private void showDeleteDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_delete_account_settings, null);
        final CheckBox chbDeleteAccount = (CheckBox) view.findViewById(R.id.chbDeleteAccount);
        chbDeleteAccount.setChecked(switchSync.isChecked());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.progress_title_delete_settings)
                .setMessage(R.string.delete_settings_summary)
                .setView(view)
                .setPositiveButton(R.string.delete_settings_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAccountSettings(chbDeleteAccount.isChecked());
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        Dialog dialog = builder.create();
        dialog.show();
        dialogShowed = false;
    }
}
