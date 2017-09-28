package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.XabberAccountCompleteRegsiterFrament;
import com.xabber.android.ui.fragment.XabberAccountConfirmationFragment;
import com.xabber.android.ui.fragment.XabberAccountInfoFragment;
import com.xabber.android.ui.fragment.XabberAccountLastFragment;
import com.xabber.android.ui.fragment.XabberAccountLoginFragment;
import com.xabber.android.utils.RetrofitErrorConverter;

import java.util.List;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccountInfoActivity extends BaseLoginActivity implements Toolbar.OnMenuItemClickListener {

    private final static String LOG_TAG = XabberAccountInfoActivity.class.getSimpleName();
    private final static String EMAIL_CONFIRMATION_URI = "https://www.xabber.com/account/emails/confirmation/";

    private final static String FRAGMENT_LOGIN = "fragment_login";
    private final static String FRAGMENT_INFO = "fragment_info";
    private final static String FRAGMENT_CONFIRM = "fragment_confirm";
    private final static String FRAGMENT_COMPLETE = "fragment_complete";
    private final static String FRAGMENT_LAST = "fragment_last";

    public final static String CALL_FROM = "call_from";
    public final static String CALL_FROM_LOGIN = "call_from_login";
    private final static String CALL_FROM_SETTINGS = "call_from_settings";

    private FragmentTransaction fTrans;

    private Toolbar toolbar;
    private BarPainter barPainter;
    private ProgressDialog progressDialog;

    private Fragment fragmentLogin;
    private Fragment fragmentInfo;
    private Fragment fragmentConfirmation;
    private Fragment fragmentCompleteRegsiter;
    private Fragment fragmentLastStep;

    private String callFrom = CALL_FROM_SETTINGS;
    private boolean needShowSyncDialog = false;

    @NonNull
    public static Intent createIntent(Context context) {
        return new Intent(context, XabberAccountInfoActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // handle call from
        Bundle extras = getIntent().getExtras();
        if (extras != null) callFrom = extras.getString(CALL_FROM);

        setContentView(R.layout.activity_xabber_account_info);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.inflateMenu(R.menu.toolbar_xabber_account_info);
        toolbar.setOnMenuItemClickListener(this);
        barPainter = new BarPainter(this, toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onPrepareOptionsMenu(toolbar.getMenu());

        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) {
            handleCallFrom(account);

            if (XabberAccount.STATUS_NOT_CONFIRMED.equals(account.getAccountStatus())) {
                showConfirmFragment();
            }
            if (XabberAccount.STATUS_CONFIRMED.equals(account.getAccountStatus())) {
                showCompleteFragment();
            }
            if (XabberAccount.STATUS_REGISTERED.equals(account.getAccountStatus())) {

                showInfoFragment();
                needShowSyncDialog = false;
            }
        } else {
            showLoginFragment();
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu == null) return true;
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) {
            menu.findItem(R.id.action_cancel_register).setVisible(!XabberAccount.STATUS_REGISTERED.equals(account.getAccountStatus()));
        } else menu.findItem(R.id.action_cancel_register).setVisible(false);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_cancel_register) {
            showCancelRegisterDialog();
            return true;
        }
        return false;
    }

    private void showCancelRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cancel_registration_progress)
                .setMessage(R.string.cancel_registration_dialog_message)
                .setPositiveButton(R.string.cancel_registration_dialog_resume, null)
                .setNegativeButton(R.string.cancel_registration_dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelRegistration();
                    }
                });
        Dialog dialog = builder.create();
        dialog.show();
    }

    private void cancelRegistration() {
        showProgress(getString(R.string.cancel_registration_progress));
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                XabberAccountManager.getInstance().deleteXabberAccountFromRealm();
                XabberAccountManager.getInstance().deleteSyncStatesFromRealm();
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        XabberAccountManager.getInstance().removeAccount();
                        showLoginFragment();
                        onPrepareOptionsMenu(toolbar.getMenu());
                        goToMainActivity();
                        hideProgress();
                    }
                });
            }
        });
    }

    private void handleCallFrom(XabberAccount account) {

        if (account != null && CALL_FROM_LOGIN.equals(callFrom))
            needShowSyncDialog = true;

    }

    @Override
    protected void showProgress(String title) {
        if (!isFinishing()) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(title);
            progressDialog.setMessage(getResources().getString(R.string.progress_message));
            progressDialog.show();
        }
    }

    @Override
    protected void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void updateAccountInfo(XabberAccount account) {
        if (account != null) {
            XabberAccountInfoFragment fragment = (XabberAccountInfoFragment) getFragmentManager().findFragmentByTag(FRAGMENT_INFO);
            if (fragment != null && fragment.isVisible())
                ((XabberAccountInfoFragment) fragmentInfo).updateData(account);
        }
    }

    private void updateLastSyncTime() {
        XabberAccountInfoFragment fragment = (XabberAccountInfoFragment) getFragmentManager().findFragmentByTag(FRAGMENT_INFO);
        if (fragment != null && fragment.isVisible())
            ((XabberAccountInfoFragment) fragmentInfo).updateLastSyncTime();
    }

    public void showLoginFragment() {
        if (fragmentLogin == null)
            fragmentLogin = new XabberAccountLoginFragment();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentLogin, FRAGMENT_LOGIN);
        fTrans.commit();

        toolbar.setTitle(R.string.title_register_xabber_account);
        barPainter.setBlue(this);
    }

    public void showInfoFragment() {
        if (fragmentInfo == null) {
            fragmentInfo = new XabberAccountInfoFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("SHOW_SYNC", needShowSyncDialog);
            fragmentInfo.setArguments(bundle);
        }

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentInfo, FRAGMENT_INFO);
        fTrans.commit();

        toolbar.setTitle(R.string.title_xabber_account);
        barPainter.setDefaultColor();
    }

    private void showConfirmFragment() {
        if (fragmentConfirmation == null)
            fragmentConfirmation = new XabberAccountConfirmationFragment();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentConfirmation, FRAGMENT_CONFIRM);
        fTrans.commit();

        toolbar.setTitle(R.string.title_register_xabber_account);
        barPainter.setBlue(this);
    }

    private void showCompleteFragment() {
        if (fragmentCompleteRegsiter == null)
            fragmentCompleteRegsiter = new XabberAccountCompleteRegsiterFrament();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentCompleteRegsiter, FRAGMENT_COMPLETE);
        fTrans.commit();

        toolbar.setTitle(R.string.title_register_xabber_account);
        barPainter.setBlue(this);
    }

    public void showLastFragment() {
        if (fragmentLastStep == null)
            fragmentLastStep = new XabberAccountLastFragment();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentLastStep, FRAGMENT_LAST);
        fTrans.commit();

        toolbar.setTitle(R.string.title_register_xabber_account);
        barPainter.setBlue(this);
    }

    public void onLoginClick() {
        Intent intent = TutorialActivity.createIntent(XabberAccountInfoActivity.this);
        startActivity(intent);
    }

    public void onLogoutClick(boolean deleteAccounts) {
        if (NetworkManager.isNetworkAvailable()) {
            logout(deleteAccounts);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    public void onSyncClick(boolean needGoToMainActivity) {
        if (NetworkManager.isNetworkAvailable()) {
            synchronize(needGoToMainActivity);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    public void onResendClick() {
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        String email;
        if (account != null && account.getEmails() != null && account.getEmails().size() > 0) {
            email = account.getEmails().get(0).getEmail();
        } else return;

        if (NetworkManager.isNetworkAvailable()) {
            resendConfirmEmail(email);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    public void onConfirmClick(String code) {
        if (NetworkManager.isNetworkAvailable()) {
            confirm(code);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    public void onCompleteClick(String username, String pass, String pass2, String firstName,
                                String lastName, boolean createToken) {
        if (NetworkManager.isNetworkAvailable()) {
            completeRegister(username, pass, pass2, firstName, lastName, createToken);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    public void onDeleteXabberOrgClick(String jid) {
        showProgress(getString(R.string.progress_title_complete));
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
        showInfoFragment();
        hideProgress();
        Toast.makeText(this, R.string.complete_success, Toast.LENGTH_SHORT).show();
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
        showInfoFragment();
        hideProgress();
    }

    private void handleIntent(Intent intent) {
        String appLinkAction = intent.getAction();
        Uri appLinkData = intent.getData();
        if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null) {
            String uri = appLinkData.toString();
            String key = uri.replace(EMAIL_CONFIRMATION_URI, "");
            key = key.replace("/", "");
            handlerEmailConfirmIntent(key);
        }
    }

    private void handlerEmailConfirmIntent(String key) {
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) {
            if (XabberAccount.STATUS_NOT_CONFIRMED.equals(account.getAccountStatus())) {
                confirmWithKey(key);
            } else {
                Toast.makeText(this, R.string.toast_email_already_confirm, Toast.LENGTH_SHORT).show();
            }
        } else
            Toast.makeText(this, R.string.toast_not_authorized, Toast.LENGTH_SHORT).show();
    }

    private void synchronize(boolean needGoToMainActivity) {
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null && account.getToken() != null) {
            showProgress(getResources().getString(R.string.progress_title_sync));
            getAccountWithUpdate(account.getToken(), needGoToMainActivity);
        } else {
            Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMainActivity() {
        Intent intent = ContactListActivity.createIntent(XabberAccountInfoActivity.this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finish();
        startActivity(intent);
    }

    private void updateSettings(final boolean needGoToMainActivity) {
        Subscription getSettingsSubscription = AuthManager.patchClientSettings(XabberAccountManager.getInstance().createSettingsList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
                        hideProgress();
                        updateLastSyncTime();
                        Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_success, Toast.LENGTH_SHORT).show();
                        if (needGoToMainActivity) goToMainActivity();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());
                        hideProgress();
                        Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(getSettingsSubscription);
    }

    private void getSettings() {
        Subscription getSettingsSubscription = AuthManager.getClientSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> settings) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
                        XabberAccountManager.getInstance().setXmppAccountsForCreate(settings);
                        hideProgress();
                        // update last synchronization time
                        SettingsManager.setLastSyncDate(XabberAccountManager.getCurrentTimeString());
                        Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_success, Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());
                        hideProgress();
                    }
                });
        compositeSubscription.add(getSettingsSubscription);
    }

    private void getAccountWithUpdate(String token, final boolean needGoToMainActivity) {
        Subscription loadAccountsSubscription = AuthManager.getAccount(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount s) {
                        Log.d(LOG_TAG, "Xabber account loading from net: successfully");
                        updateAccountInfo(s);

                        // if exist local accounts
                        if (AccountManager.getInstance().getAllAccountItems().size() > 0)
                            updateSettings(needGoToMainActivity);
                        else getSettings();

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "Xabber account loading from net: error: " + throwable.toString());
                        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
                        if (message != null && message.equals("Invalid token")) {
                            XabberAccountManager.getInstance().onInvalidToken();
                            showLoginFragment();
                        }

                        hideProgress();
                        Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(loadAccountsSubscription);
    }

    private void logout(final boolean deleteAccounts) {
        showProgress(getResources().getString(R.string.progress_title_quit));
        Subscription logoutSubscription = AuthManager.logout(deleteAccounts)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        handleSuccessLogout(s, deleteAccounts);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorLogout(throwable);
                    }
                });
        compositeSubscription.add(logoutSubscription);
    }

    private void handleSuccessLogout(ResponseBody s, boolean deleteAccounts) {
        if (deleteAccounts) XabberAccountManager.getInstance().deleteSyncedLocalAccounts();
        XabberAccountManager.getInstance().removeAccount();
        showLoginFragment();
        hideProgress();
        Toast.makeText(this, R.string.quit_success, Toast.LENGTH_SHORT).show();
        Intent intent = ContactListActivity.createIntent(XabberAccountInfoActivity.this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void handleErrorLogout(Throwable throwable) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.equals("Invalid token")) {
                XabberAccountManager.getInstance().onInvalidToken();
                showLoginFragment();
            } else {
                Log.d(LOG_TAG, "Error while logout: " + message);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG, "Error while logout: " + throwable.toString());
            Toast.makeText(this, "Error while logout: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
        hideProgress();
    }

    private void confirm(String code) {
        showProgress(getResources().getString(R.string.progress_title_confirm));
        Subscription confirmSubscription = AuthManager.confirmEmail(code)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount s) {
                        handleSuccessConfirm(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorConfirm(throwable);
                    }
                });
        compositeSubscription.add(confirmSubscription);
    }

    private void confirmWithKey(String key) {
        showProgress(getResources().getString(R.string.progress_title_confirm));
        Subscription confirmWithKeySubscription = AuthManager.confirmEmailWithKey(key)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount s) {
                        handleSuccessConfirm(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorConfirm(throwable);
                    }
                });
        compositeSubscription.add(confirmWithKeySubscription);
    }

    private void handleSuccessConfirm(XabberAccount response) {
        showCompleteFragment();
        hideProgress();
        Toast.makeText(this, R.string.confirm_success, Toast.LENGTH_SHORT).show();
    }

    private void handleErrorConfirm(Throwable throwable) {
        hideProgress();

        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            Log.d(LOG_TAG, "Error while confirming email: " + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(LOG_TAG, "Error while confirming email: " + throwable.toString());
            Toast.makeText(this, "Error while confirming email: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void completeRegister(String username, String pass, String pass2, String firstName, String lastName, boolean createToken) {
        showProgress(getResources().getString(R.string.progress_title_complete));
        Subscription completeSubscription = AuthManager.completeRegister(username, pass, pass2, firstName, lastName, "xabber.org", createToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount s) {
                        handleSuccessComplete(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorComplete(throwable);
                    }
                });
        compositeSubscription.add(completeSubscription);
    }

    private void handleSuccessComplete(XabberAccount response) {
        hideProgress();
        Toast.makeText(this, R.string.complete_success, Toast.LENGTH_SHORT).show();

        if (AccountManager.getInstance().getAllAccountItems().size() > 0) {
            needShowSyncDialog = true;
            showLastFragment();
        } else getSettings();
    }

    private void handleErrorComplete(Throwable throwable) {
        hideProgress();

        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            Log.d(LOG_TAG, "Error while completing register: " + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(LOG_TAG, "Error while completing register: " + throwable.toString());
            Toast.makeText(this, "Error while completing register: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void resendConfirmEmail(String email) {
        showProgress(getResources().getString(R.string.progress_title_resend));
        Subscription resendEmailSubscription = AuthManager.addEmail(email)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        handleSuccessResendEmail(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorResendEmail(throwable);
                    }
                });
        compositeSubscription.add(resendEmailSubscription);
    }

    private void handleSuccessResendEmail(ResponseBody response) {
        hideProgress();
        Toast.makeText(this, R.string.resend_success, Toast.LENGTH_SHORT).show();
    }

    private void handleErrorResendEmail(Throwable throwable) {
        hideProgress();

        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            Log.d(LOG_TAG, "Error while send verification email: " + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(LOG_TAG, "Error while send verification email: " + throwable.toString());
            Toast.makeText(this, "Error while send verification email: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
    }
}

