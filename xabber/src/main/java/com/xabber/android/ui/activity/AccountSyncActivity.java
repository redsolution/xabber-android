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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
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
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.utils.RetrofitErrorConverter;

import org.greenrobot.eventbus.EventBus;

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

    private TextView tvJid;
    private TextView tvStatus;
    private ImageView ivStatus;
    private LinearLayout syncStatusView;
    private ProgressBar progressBar;

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
        tvJid = (TextView) findViewById(R.id.tvJid);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        ivStatus = (ImageView) findViewById(R.id.ivStatus);
        syncStatusView = (LinearLayout) findViewById(R.id.syncStatusView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        rlSyncSwitch.setOnClickListener(this);
        btnDeleteSettings.setOnClickListener(this);
        syncStatusView.setOnClickListener(this);
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
        updateTitle();
        getSyncStatus();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rlSyncSwitch:
                SettingsManager.setSyncAllAccounts(false);
                XabberAccountManager.getInstance().addAccountSyncState(jid, !switchSync.isChecked());
                if (!switchSync.isChecked()) {
                    updateAccountSettings();
                }
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
            case R.id.syncStatusView:
                updateAccountSettings();
                //getSyncStatus();
                break;
        }
    }

    private void updateSyncSwitchButton() {
        switchSync.setChecked(XabberAccountManager.getInstance().isAccountSynchronize(jid)
                || SettingsManager.isSyncAllAccounts());
    }

    private void deleteAccountSettings(final boolean deleteAccount) {
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
                        handleSuccessDelete(settings, deleteAccount);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorDelete(throwable);
                    }
                });
        compositeSubscription.add(deleteSubscription);
    }

    private void handleSuccessDelete(List<XMPPAccountSettings> settings, boolean deleteAccount) {
        if (!deleteAccount) {
            for (XMPPAccountSettings set : settings) {
                if (set.getJid().equals(jid))
                    AccountManager.getInstance().setTimestamp(accountItem.getAccount(), set.getTimestamp() + 1);
            }
        }

        hideProgress();
        Toast.makeText(this, R.string.settings_delete_success, Toast.LENGTH_SHORT).show();
        getSyncStatus();
        updateSyncSwitchButton();
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
        if (!isFinishing()) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(title);
            progressDialog.setMessage(getResources().getString(R.string.progress_message));
            progressDialog.show();
        }
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
    }

    private void showDeleteDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_delete_account_settings, null);
        final CheckBox chbDeleteAccount = (CheckBox) view.findViewById(R.id.chbDeleteAccount);
        chbDeleteAccount.setText(getString(R.string.delete_synced_account, jid));
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

    private void updateTitle() {
        barPainter.updateWithAccountName(accountItem.getAccount());
        tvJid.setText(accountItem.getAccount().getFullJid().asBareJid().toString());
    }

    private void getSyncStatus() {
        if (!XabberAccountManager.getInstance().isAccountSynchronize(jid) || !NetworkManager.isNetworkAvailable()) {
            setSyncStatus(XMPPAccountSettings.SyncStatus.local);
            return;
        }

        ivStatus.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        Subscription getSettingsSubscription = AuthManager.getClientSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> list) {
                        handleSuccessGetSettings(list);
                        ivStatus.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorGetSettings(throwable);
                        ivStatus.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
        compositeSubscription.add(getSettingsSubscription);
    }

    private void handleSuccessGetSettings(List<XMPPAccountSettings> list) {
        XMPPAccountSettings.SyncStatus status = XMPPAccountSettings.SyncStatus.local;

        for (XMPPAccountSettings item : list) {
            if (jid.equals(item.getJid())) {
                if (item.isDeleted()) {
                    status = XMPPAccountSettings.SyncStatus.deleted;
                    break;
                }

                if (item.getTimestamp() == accountItem.getTimestamp())
                    status = XMPPAccountSettings.SyncStatus.localEqualsRemote;
                else if (item.getTimestamp() > accountItem.getTimestamp())
                    status = XMPPAccountSettings.SyncStatus.remoteNewer;
                else {
                    status = XMPPAccountSettings.SyncStatus.localNewer;
                }
                break;
            }
        }
        setSyncStatus(status);
    }

    private void setSyncStatus(XMPPAccountSettings.SyncStatus status) {
        switch (status) {
            case local:
                tvStatus.setText(R.string.sync_status_no);
                ivStatus.setImageResource(R.drawable.ic_sync_disable);
                btnDeleteSettings.setVisibility(View.GONE);
                break;
            case remote:
                tvStatus.setText(R.string.sync_status_no);
                ivStatus.setImageResource(R.drawable.ic_sync_disable);
                btnDeleteSettings.setVisibility(View.GONE);
                break;
            case localNewer:
                tvStatus.setText(R.string.sync_status_local);
                ivStatus.setImageResource(R.drawable.ic_sync_upload);
                btnDeleteSettings.setVisibility(View.VISIBLE);
                break;
            case remoteNewer:
                tvStatus.setText(R.string.sync_status_remote);
                ivStatus.setImageResource(R.drawable.ic_sync_download);
                btnDeleteSettings.setVisibility(View.VISIBLE);
                break;
            case localEqualsRemote:
                tvStatus.setText(R.string.sync_status_ok);
                ivStatus.setImageResource(R.drawable.ic_sync_done);
                btnDeleteSettings.setVisibility(View.VISIBLE);
                break;
            case deleted:
                tvStatus.setText(R.string.sync_status_deleted);
                ivStatus.setImageResource(R.drawable.ic_delete_grey);
                btnDeleteSettings.setVisibility(View.GONE);
                break;
            default:
                tvStatus.setText(R.string.sync_status_no);
                ivStatus.setImageResource(R.drawable.ic_sync_disable);
                btnDeleteSettings.setVisibility(View.GONE);
                break;
        }
    }

    private void handleErrorGetSettings(Throwable throwable) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.equals("Invalid token")) {
                XabberAccountManager.getInstance().onInvalidToken();
                Toast.makeText(this, "Аккаунт был удален", Toast.LENGTH_LONG).show();
            } else {
                Log.d(LOG_TAG, "Error while synchronization: " + message);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG, "Error while synchronization: " + throwable.toString());
            Toast.makeText(this, "Error while synchronization: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void updateAccountSettings() {
        showProgress(getResources().getString(R.string.progress_title_sync));
        Subscription updateSettingsSubscription =
                AuthManager.patchClientSettings(XabberAccountManager.getInstance().createSettingsList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        handleSuccessUpdateSettings(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorUpdateSettings(throwable);
                    }
                });
        compositeSubscription.add(updateSettingsSubscription);
    }

    public void handleSuccessUpdateSettings(List<XMPPAccountSettings> list) {
        handleSuccessGetSettings(list);
        Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
        hideProgress();
        Toast.makeText(this, R.string.sync_success, Toast.LENGTH_SHORT).show();
        //getSyncStatus();
    }

    public void handleErrorUpdateSettings(Throwable throwable) {
        Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());

        // invalid token
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null && message.equals("Invalid token")) {
            // logout from deleted account
            XabberAccountManager.getInstance().onInvalidToken();
        }

        hideProgress();
        Toast.makeText(this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
        getSyncStatus();
    }

    @Override
    public void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EventBus.getDefault().removeStickyEvent(XabberAccountManager.XabberAccountDeletedEvent.class);
                        checkAccount();
                    }
                });
        Dialog dialog = builder.create();
        dialog.show();
    }
}
