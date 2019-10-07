package com.xabber.android.ui.activity;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.dialog.AddEmailDialogFragment;
import com.xabber.android.ui.dialog.ConfirmEmailDialogFragment;
import com.xabber.android.ui.fragment.XAccountLinksFragment;
import com.xabber.android.ui.helper.OnSocialBindListener;
import com.xabber.android.utils.RetrofitErrorConverter;

import java.util.List;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 31.07.17.
 */

public abstract class BaseLoginActivity extends ManagedActivity implements XAccountLinksFragment.Listener,
        AddEmailDialogFragment.Listener, ConfirmEmailDialogFragment.Listener, OnSocialBindListener {

    private final static String LOG_TAG = BaseLoginActivity.class.getSimpleName();

    protected CompositeSubscription compositeSubscription = new CompositeSubscription();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected abstract void onSocialAuthSuccess(final String provider, final String credentials);

    protected abstract void showProgress(String title);

    protected abstract void hideProgress();

    protected void handleError(Throwable throwable, String errorContext, String logTag) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            Log.d(logTag, errorContext + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(logTag, errorContext + throwable.toString());
            Toast.makeText(this, errorContext + throwable.toString(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** SYNCHRONIZATION */

    protected void synchronize(boolean needGoToMainActivity) {
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null && account.getToken() != null) {
            showProgress(getResources().getString(R.string.progress_title_sync));
            getAccountWithUpdate(account.getToken(), needGoToMainActivity);
        } else {
            Toast.makeText(BaseLoginActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
        }
    }

    protected void getAccountWithUpdate(String token, final boolean needGoToMainActivity) {
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
                        else getSettings(needGoToMainActivity);

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "Xabber account loading from net: error: " + throwable.toString());
                        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
                        if (message != null && message.equals("Invalid token")) {
                            XabberAccountManager.getInstance().onInvalidToken();
                            //showLoginFragment();
                        }

                        hideProgress();
                        Toast.makeText(BaseLoginActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(loadAccountsSubscription);
    }

    protected void updateSettings(final boolean needGoToMainActivity) {
        Subscription getSettingsSubscription = AuthManager.patchClientSettings(XabberAccountManager.getInstance().createSettingsList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
                        hideProgress();
                        updateLastSyncTime();
                        onSynchronized();
                        //Toast.makeText(BaseLoginActivity.this, R.string.sync_success, Toast.LENGTH_SHORT).show();
                        if (needGoToMainActivity) goToMainActivity();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());
                        hideProgress();
                        Toast.makeText(BaseLoginActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(getSettingsSubscription);
    }

    protected void getSettings(final boolean needGoToMainActivity) {
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
                        onSynchronized();
                        //Toast.makeText(BaseLoginActivity.this, R.string.sync_success, Toast.LENGTH_SHORT).show();
                        if (needGoToMainActivity) goToMainActivity();
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

    protected void updateAccountInfo(XabberAccount account) {}

    protected void updateLastSyncTime() {}

    protected void goToMainActivity() {
        Intent intent = ContactListActivity.createIntent(BaseLoginActivity.this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finish();
        startActivity(intent);
    }

    protected abstract void onSynchronized();

    /** ADD EMAIL */

    protected void resendConfirmEmail(String email) {
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
        synchronize(false);
    }

    private void handleErrorResendEmail(Throwable throwable) {
        hideProgress();
        handleError(throwable, "Error while send verification email: ", LOG_TAG);
    }

    /** CONFIRM EMAIL */

    protected void confirmEmail(String code) {
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

    private void handleSuccessConfirm(XabberAccount response) {
        hideProgress();
        Toast.makeText(this, R.string.confirm_success, Toast.LENGTH_SHORT).show();
        synchronize(false);
    }

    private void handleErrorConfirm(Throwable throwable) {
        hideProgress();
        handleError(throwable, "Error while confirming email: ", LOG_TAG);
    }

    /** DELETE EMAIL */

    protected void deleteEmail(int emailId) {
        showProgress(getResources().getString(R.string.progress_title_delete));
        Subscription deleteSubscription = AuthManager.deleteEmail(emailId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        handleSuccessDelete(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorDelete(throwable);
                    }
                });
        compositeSubscription.add(deleteSubscription);
    }

    private void handleSuccessDelete(ResponseBody response) {
        hideProgress();
        Toast.makeText(this, R.string.delete_success, Toast.LENGTH_SHORT).show();
        synchronize(false);
    }

    private void handleErrorDelete(Throwable throwable) {
        hideProgress();
        handleError(throwable, "Error while deleting email: ", LOG_TAG);
    }

    /** SOCIAL BIND / UNBIND */

    protected void bindSocial(String provider, String credentials) {
        showProgress(getResources().getString(R.string.progress_title_bind_social));
        Subscription loginSocialSubscription = AuthManager.bindSocial(provider, credentials)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        hideProgress();
                        Toast.makeText(BaseLoginActivity.this,
                                R.string.social_bind_success, Toast.LENGTH_SHORT).show();
                        synchronize(false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgress();
                        Toast.makeText(BaseLoginActivity.this,
                                R.string.social_bind_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(loginSocialSubscription);
    }

    protected void unbindSocial(String provider) {
        showProgress(getResources().getString(R.string.progress_title_unbind_social));
        Subscription unbindSocialSubscription = AuthManager.unbindSocial(provider)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody responseBody) {
                        hideProgress();
                        Toast.makeText(BaseLoginActivity.this,
                                R.string.social_unbind_success, Toast.LENGTH_SHORT).show();
                        synchronize(false);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgress();
                        Toast.makeText(BaseLoginActivity.this,
                                R.string.social_unbind_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(unbindSocialSubscription);
    }

    /** XABBER ACCOUNT LINKS LISTENER */

    @Override
    public void onBindClick(String provider) {
        Toast.makeText(this, R.string.nostore_restriction, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSocialUnbindClick(String provider) {
        unbindSocial(provider);
    }

    @Override
    public void onAddEmailClick(String email) {
        resendConfirmEmail(email);
    }

    @Override
    public void onDeleteEmailClick(int emailId) {
        deleteEmail(emailId);
    }

    @Override
    public void onResendCodeClick(String email) {
        resendConfirmEmail(email);
    }

    @Override
    public void onConfirmClick(String email, String code) {
        confirmEmail(code);
    }
}
