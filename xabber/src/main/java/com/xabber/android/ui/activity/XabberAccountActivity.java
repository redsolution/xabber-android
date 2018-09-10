package com.xabber.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.XabberAccountInfoFragment;
import com.xabber.android.utils.RetrofitErrorConverter;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class XabberAccountActivity extends BaseLoginActivity
        implements XabberAccountInfoFragment.Listener {

    private final static String LOG_TAG = XabberAccountActivity.class.getSimpleName();
    private final static String FRAGMENT_INFO = "fragment_info";
    private final static String SHOW_SYNC = "show_sync";

    private FragmentTransaction fTrans;
    private Fragment fragmentInfo;

    private Toolbar toolbar;
    private BarPainter barPainter;
    private ProgressDialog progressDialog;
    private boolean needShowSyncDialog = false;

    @NonNull
    public static Intent createIntent(Context context, boolean showSync) {
        Intent intent = new Intent(context, XabberAccountActivity.class);
        intent.putExtra(SHOW_SYNC, showSync);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_xabber_account_info);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.inflateMenu(R.menu.toolbar_xabber_account_info);
        toolbar.setTitleTextColor(getResources().getColor(R.color.black_text));
        barPainter = new BarPainter(this, toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onPrepareOptionsMenu(toolbar.getMenu());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            needShowSyncDialog = extras.getBoolean(SHOW_SYNC);
            extras.clear();
        }

        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) showInfoFragment();
        else finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
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

    /** Social Auth */

    @Override
    public void onSocialBindClick(String provider) {
        switch (provider) {
            case AuthManager.PROVIDER_GOOGLE:
                loginGoogle();
                break;
            case AuthManager.PROVIDER_FACEBOOK:
                loginFacebook();
                break;
            case AuthManager.PROVIDER_TWITTER:
                loginTwitter();
                break;
        }
    }

    @Override
    public void onSocialUnbindClick(String provider) {
        showProgress(getResources().getString(R.string.progress_title_unbind_social));
        Subscription unbindSocialSubscription = AuthManager.unbindSocial(provider)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody responseBody) {
                        Toast.makeText(XabberAccountActivity.this,
                                R.string.social_unbind_success, Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Toast.makeText(XabberAccountActivity.this,
                                R.string.social_unbind_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(unbindSocialSubscription);
    }

    /** Xabber Account Info */

    @Override
    public void onLogoutClick(boolean deleteAccounts) {
        if (NetworkManager.isNetworkAvailable()) {
            logout(deleteAccounts);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSyncClick(boolean needGoToMainActivity) {
        if (NetworkManager.isNetworkAvailable()) {
            synchronize(needGoToMainActivity);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    /** Email */

    @Override
    public void onAddEmailClick(String email) {
        resendConfirmEmail(email);
    }

    @Override
    public void onConfirmEmailClick(String email, String code) {
        confirmEmail(code);
    }

    private void showInfoFragment() {
        if (fragmentInfo == null) {
            fragmentInfo = XabberAccountInfoFragment.newInstance(this);
            Bundle bundle = new Bundle();
            bundle.putBoolean("SHOW_SYNC", needShowSyncDialog);
            fragmentInfo.setArguments(bundle);
        }

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentInfo, FRAGMENT_INFO);
        fTrans.commit();

        toolbar.setTitle(R.string.title_xabber_account);
        barPainter.setLiteGrey();
    }

    @Override
    protected void updateAccountInfo(XabberAccount account) {
        if (account != null) {
            XabberAccountInfoFragment fragment = (XabberAccountInfoFragment) getFragmentManager().findFragmentByTag(FRAGMENT_INFO);
            if (fragment != null && fragment.isVisible())
                ((XabberAccountInfoFragment) fragmentInfo).updateData(account);
        }
    }

    @Override
    protected void updateLastSyncTime() {
        XabberAccountInfoFragment fragment = (XabberAccountInfoFragment) getFragmentManager().findFragmentByTag(FRAGMENT_INFO);
        if (fragment != null && fragment.isVisible())
            ((XabberAccountInfoFragment) fragmentInfo).updateLastSyncTime();
    }

    /** LOGOUT */

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
        //showLoginFragment();
        hideProgress();
        Toast.makeText(this, R.string.quit_success, Toast.LENGTH_SHORT).show();
        Intent intent = ContactListActivity.createIntent(XabberAccountActivity.this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void handleErrorLogout(Throwable throwable) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.equals("Invalid token")) {
                XabberAccountManager.getInstance().onInvalidToken();
                //showLoginFragment();
            } else {
                Log.d(LOG_TAG, "Error while logout: " + message);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG, "Error while logout: " + throwable.toString());
            Toast.makeText(this, R.string.logout_error, Toast.LENGTH_LONG).show();
        }
        hideProgress();
    }

    /** ADD EMAIL */

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
        handleError(throwable, "Error while send verification email: ", LOG_TAG);
    }

    /** CONFIRM EMAIL */

    private void confirmEmail(String code) {
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
    }

    private void handleErrorConfirm(Throwable throwable) {
        hideProgress();
        handleError(throwable, "Error while confirming email: ", LOG_TAG);
    }

    /** SOCIAL AUTH */

    @Override
    protected void onSocialAuthSuccess(String provider, String credentials) {
        showProgress(getResources().getString(R.string.progress_title_bind_social));
        Subscription loginSocialSubscription = AuthManager.bindSocial(provider, credentials)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        Toast.makeText(XabberAccountActivity.this,
                                R.string.social_bind_success, Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Toast.makeText(XabberAccountActivity.this,
                                R.string.social_bind_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(loginSocialSubscription);
    }
}
