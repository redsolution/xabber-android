package com.xabber.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.HttpApiManager;
import com.xabber.android.data.xaccount.XAccountTokenDTO;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.presentation.mvp.signup.SignUpRepo;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.XAccountLoginFragment;
import com.xabber.android.ui.fragment.XAccountSignUpFragment1;
import com.xabber.android.ui.fragment.XAccountSignUpFragment2;
import com.xabber.android.utils.RetrofitErrorConverter;

import java.util.ArrayList;
import java.util.List;

import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * Created by valery.miller on 14.07.17.
 */

public class XabberLoginActivity extends BaseLoginActivity implements XAccountSignUpFragment1.Listener,
        XAccountSignUpFragment2.Listener, XAccountLoginFragment.Listener {

    private final static String LOG_TAG = XabberLoginActivity.class.getSimpleName();
    public final static String CURRENT_FRAGMENT = "current_fragment";
    public final static String FRAGMENT_LOGIN = "fragment_login";
    public final static String FRAGMENT_SIGNUP_STEP1 = "fragment_signup_step1";
    public final static String FRAGMENT_SIGNUP_STEP2 = "fragment_signup_step2";
    public final static String FRAGMENT_SIGNUP_STEP3 = "fragment_signup_step3";

    private FragmentTransaction fTrans;
    private Fragment fragmentLogin;
    private Fragment fragmentSignUpStep1;
    private Fragment fragmentSignUpStep2;
    private Fragment fragmentSignUpStep3;
    private String currentFragment = FRAGMENT_LOGIN;

    private ProgressDialog progressDialog;
    private BarPainter barPainter;
    private Toolbar toolbar;

    private List<AuthManager.Host> hosts = new ArrayList<>();

    public static Intent createIntent(Context context, @Nullable String currentFragment) {
        Intent intent = new Intent(context, XabberLoginActivity.class);
        intent.putExtra(CURRENT_FRAGMENT, currentFragment);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            currentFragment = savedInstanceState.getString(CURRENT_FRAGMENT);
        else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) currentFragment = extras.getString(CURRENT_FRAGMENT);
        }

        setContentView(R.layout.activity_xabber_login);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setTitle(R.string.title_login_xabber_account);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitleTextColor(getResources().getColor(R.color.black_text));
        barPainter = new BarPainter(this, toolbar);
    }

    public void showLoginFragment() {
        if (fragmentLogin == null)
            fragmentLogin = XAccountLoginFragment.newInstance(this);

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentLogin);
        fTrans.commit();
        currentFragment = FRAGMENT_LOGIN;

        toolbar.setTitle(R.string.title_login_xabber_account);
        barPainter.setLiteGrey();
    }

    public void showSignUpStep1Fragment() {
        if (fragmentSignUpStep1 == null)
            fragmentSignUpStep1 = XAccountSignUpFragment1.newInstance(this);

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentSignUpStep1, FRAGMENT_SIGNUP_STEP1);
        fTrans.commit();
        currentFragment = FRAGMENT_SIGNUP_STEP1;

        toolbar.setTitle(R.string.title_register_xabber_account);
        barPainter.setLiteGrey();
    }

    public void showSignUpStep2Fragment() {
        if (fragmentSignUpStep2 == null)
            fragmentSignUpStep2 = XAccountSignUpFragment2.newInstance(this);

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentSignUpStep2, FRAGMENT_SIGNUP_STEP2);
        fTrans.commit();
        currentFragment = FRAGMENT_SIGNUP_STEP2;

        toolbar.setTitle(R.string.title_register_xabber_account);
        barPainter.setLiteGrey();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentFragment != null) {
            switch (currentFragment) {
                case FRAGMENT_SIGNUP_STEP1:
                    showSignUpStep1Fragment();
                    break;
                case FRAGMENT_SIGNUP_STEP2:
                    showSignUpStep2Fragment();
                    break;
                default:
                    showLoginFragment();
            }
        } else showLoginFragment();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FRAGMENT, currentFragment);
    }

    public void onForgotPassClick() {
        String url = HttpApiManager.XABBER_FORGOT_PASS_URL;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onSocialAuthSuccess(String provider, String credentials) {
        socialLogin(provider, credentials);
    }

    @Override
    public void onGetHosts() {
        getHosts();
    }

    @Override
    public void onStep1Completed(String username, String host) {
        SignUpRepo signUpRepo = SignUpRepo.getInstance();
        signUpRepo.setUsername(username);
        signUpRepo.setHost(host);

        showSignUpStep2Fragment();
    }

    @Override
    public void on2StepCompleted(String pass) {
        SignUpRepo.getInstance().setPass(pass);
        //showSignUpStep3Fragment();
    }

    //    @Override
//    public void onSignupClick(String username, String host, String pass, String captchaToken) {
//        signUp(username, host, pass, captchaToken,null, null);
//    }
//
//    @Override
//    public void onSignupClick(String username, String host, String pass, String socialToken, String socialProvider) {
//        signUp(username, host, pass, null, socialToken, socialProvider);
//    }

    @Override
    public void onGoogleClick() {
        loginGoogle();
    }

    @Override
    public void onFacebookClick() {
        loginFacebook();
    }

    @Override
    public void onTwitterClick() {
        loginTwitter();
    }

    /** GET HOSTS */

    private void getHosts() {
        if (hosts != null && !hosts.isEmpty()) {
            if (fragmentSignUpStep1 != null) ((XAccountSignUpFragment1)fragmentSignUpStep1).setupHosts(hosts);
        } else {
            if (fragmentSignUpStep1 != null) ((XAccountSignUpFragment1) fragmentSignUpStep1).showHostsProgress(true);
            Subscription requestHosts = AuthManager.getHosts()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<AuthManager.HostResponse>() {
                        @Override
                        public void call(AuthManager.HostResponse response) {
                            handleSuccessGetHosts(response);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            handleErrorGetHosts(throwable);
                        }
                    });
            compositeSubscription.add(requestHosts);
        }
    }

    private void handleSuccessGetHosts(AuthManager.HostResponse response) {
        List<AuthManager.Host> hosts = response.getHosts();
        if (hosts == null) return;

        this.hosts.clear();
        this.hosts.addAll(hosts);

        if (fragmentSignUpStep1 != null) {
            ((XAccountSignUpFragment1) fragmentSignUpStep1).showHostsProgress(false);
            ((XAccountSignUpFragment1) fragmentSignUpStep1).setupHosts(this.hosts);
        }
    }

    private void handleErrorGetHosts(Throwable throwable) {
        if (fragmentSignUpStep1 != null) ((XAccountSignUpFragment1) fragmentSignUpStep1).showHostsProgress(false);
        handleError(throwable, "Error while request hosts: ", LOG_TAG);
    }

    /** SIGN UP */

    private void signUp(String username, String host, String pass, String captchaToken,
                        String credentials, String socialProvider) {
        showProgress(getResources().getString(R.string.progress_title_signup));

        Single<XabberAccount> signUpSingle;
        if (credentials != null && socialProvider != null)
            signUpSingle = AuthManager.signupv2(username, host, pass, socialProvider, credentials);
        else signUpSingle = AuthManager.signupv2(username, host, pass, captchaToken);

        Subscription signUpSubscription = signUpSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount account) {
                        handleSuccessSignUp();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorSignUp(throwable);
                    }
                });
        compositeSubscription.add(signUpSubscription);
    }

    private void handleSuccessSignUp() {
        synchronize(true);
    }

    private void handleErrorSignUp(Throwable throwable) {
        hideProgress();
        handleError(throwable, "Error while signup: ", LOG_TAG);
    }

    /** SOCIAL LOGIN */

    private void socialLogin(final String provider, final String credentials) {
        showProgress(getString(R.string.progress_title_login));
        Subscription loginSocialSubscription = AuthManager.loginSocial(provider, credentials)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XAccountTokenDTO>() {
                    @Override
                    public void call(XAccountTokenDTO s) {
                        handleSuccessSocialLogin(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorSocialLogin(throwable, credentials, provider);
                    }
                });
        compositeSubscription.add(loginSocialSubscription);
    }

    private void handleSuccessSocialLogin(@NonNull XAccountTokenDTO response) {
        getAccount(response.getToken());
    }

    private void handleErrorSocialLogin(Throwable throwable, String credentials, String provider) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.contains("is not attached to any Xabber account")) {
                // go to sign up
                SignUpRepo.getInstance().setSocialCredentials(credentials);
                SignUpRepo.getInstance().setSocialProvider(provider);
                hideProgress();
                showSignUpStep1Fragment();

            } else {
                Log.d(LOG_TAG, "Error while social login request: " + message);
                Toast.makeText(this, R.string.social_auth_fail, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(LOG_TAG, "Error while social login request: " + throwable.toString());
            Toast.makeText(this, R.string.social_auth_fail, Toast.LENGTH_LONG).show();
        }
        hideProgress();
    }

    /** GET ACCOUNT */

    private void getAccount(String token) {
        Subscription getAccountSubscription = AuthManager.getAccount(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount xabberAccount) {
                        handleSuccessGetAccount(xabberAccount);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorGetAccount(throwable);
                    }
                });
        compositeSubscription.add(getAccountSubscription);
    }

    private void handleSuccessGetAccount(@NonNull XabberAccount xabberAccount) {
        hideProgress();
        Intent intent = XabberAccountActivity.createIntent(this, true);
        finish();
        startActivity(intent);
    }

    private void handleErrorGetAccount(Throwable throwable) {
        hideProgress();
        handleError(throwable, "Error while login: ", LOG_TAG);
    }

}
