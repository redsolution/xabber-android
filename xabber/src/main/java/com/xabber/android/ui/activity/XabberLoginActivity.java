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
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.XAccountSignUpFragment;
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

public class XabberLoginActivity extends BaseLoginActivity implements XAccountSignUpFragment.Listener {

    private final static String LOG_TAG = XabberLoginActivity.class.getSimpleName();
    public final static String CURRENT_FRAGMENT = "current_fragment";
    public final static String FRAGMENT_LOGIN = "fragment_login";
    public final static String FRAGMENT_SIGNUP = "fragment_signup";

    private FragmentTransaction fTrans;
    private Fragment fragmentLogin;
    private Fragment fragmentSignUp;
    private String currentFragment = FRAGMENT_LOGIN;

    private ProgressDialog progressDialog;
    private BarPainter barPainter;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setTitle(R.string.title_login_xabber_account);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        barPainter = new BarPainter(this, toolbar);
    }

    public void showLoginFragment() {
//        if (fragmentLogin == null)
//            fragmentLogin = new XabberLoginFragment();
//
//        fTrans = getFragmentManager().beginTransaction();
//        fTrans.replace(R.id.container, fragmentLogin);
//        fTrans.commit();
//        currentFragment = FRAGMENT_LOGIN;
    }

    public void showSignUpFragment(String socialToken, String socialProvider) {
        if (fragmentSignUp == null)
            fragmentSignUp = XAccountSignUpFragment.newInstance(this, socialToken, socialProvider);

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentSignUp, FRAGMENT_SIGNUP);
        fTrans.commit();

        //toolbar.setTitle(R.string.title_register_xabber_account);
        barPainter.setBlue(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentFragment != null) {
            switch (currentFragment) {
                case FRAGMENT_SIGNUP:
                    showSignUpFragment(null, null);
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
    protected void onSocialAuthSuccess(String provider, String token) {
        socialLogin(provider, token);
    }

    @Override
    protected void onTwitterAuthSuccess(String token, String twitterTokenSecret, String secret, String key) {
        socialLogin(token, twitterTokenSecret, secret, key);
    }

    @Override
    public void onGetHosts() {
        getHosts();
    }

    @Override
    public void onSignupClick(String username, String host, String pass, String captchaToken) {
        signUp(username, host, pass, captchaToken,null, null);
    }

    @Override
    public void onSignupClick(String username, String host, String pass, String socialToken, String socialProvider) {
        signUp(username, host, pass, null, socialToken, socialProvider);
    }

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
        showProgress("Request hosts..");
        Subscription requestHosts = AuthManager.getHosts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<AuthManager.Domain>>() {
                    @Override
                    public void call(List<AuthManager.Domain> domains) {
                        handleSuccessGetHosts(domains);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorGetHosts(throwable);
                    }
                });
        compositeSubscription.add(requestHosts);
    }

    private void handleSuccessGetHosts(List<AuthManager.Domain> domains) {
        hideProgress();

        List<String> hosts = new ArrayList<>();
        for (AuthManager.Domain domain : domains) {
            hosts.add(domain.getDomain());
        }

        if (fragmentSignUp != null)
            ((XAccountSignUpFragment)fragmentSignUp).setupSpinner(hosts);
    }

    private void handleErrorGetHosts(Throwable throwable) {
        hideProgress();

        // TODO: 03.08.18 implement error parsing

        String error = "Error while request hosts: " + throwable.toString();
        Log.d(LOG_TAG, error);
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    /** SIGN UP */

    private void signUp(String username, String host, String pass, String captchaToken,
                        String socialToken, String socialProvider) {
        showProgress("Sign Up..");

        Single<XabberAccount> signUpSingle;
        if (socialToken != null && socialProvider != null)
            signUpSingle = AuthManager.signupv2(username, host, pass, socialProvider, socialToken);
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
        // TODO: 15.08.18 go to xabber account
    }

    private void handleErrorSignUp(Throwable throwable) {
        hideProgress();

        // TODO: 03.08.18 implement error parsing

        String error = "Error while signup: " + throwable.toString();
        Log.d(LOG_TAG, error);
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    /** SOCIAL LOGIN */

    private void socialLogin(final String provider, final String token) {
        showProgress(getString(R.string.progress_title_login));
        Subscription loginSocialSubscription = AuthManager.loginSocial(provider, token)
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
                        handleErrorSocialLogin(throwable, token, provider);
                    }
                });
        compositeSubscription.add(loginSocialSubscription);
    }

    private void socialLogin(String socialToken, String twitterTokenSecret, String secret, String key) {
        showProgress(getString(R.string.progress_title_login));
        Subscription loginSocialSubscription = AuthManager.loginSocialTwitter(socialToken,
                twitterTokenSecret, secret, key)
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
                        //handleErrorSocialLogin(throwable, token, provider);
                    }
                });
        compositeSubscription.add(loginSocialSubscription);
    }

    private void handleSuccessSocialLogin(@NonNull XAccountTokenDTO response) {
        getAccount(response.getToken());
    }

    private void handleErrorSocialLogin(Throwable throwable, String token, String provider) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.contains("is not attached to any Xabber account")) {
                // go to sign up
                showSignUpFragment(token, provider);
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

        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            Log.d(LOG_TAG, "Error while login: " + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(LOG_TAG, "Error while login: " + throwable.toString());
            Toast.makeText(this, "Error while login: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
    }

}
