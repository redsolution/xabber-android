package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XAccountTokenDTO;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;

import java.util.Collections;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 14.07.17.
 */

public class XabberLoginActivity extends ManagedActivity implements View.OnClickListener {

    private final static String TAG = XabberLoginActivity.class.getSimpleName();

    private EditText edtLogin;
    private EditText edtPass;
    private Button btnLogin;
    private RelativeLayout rlForgotPass;
    private ProgressBar progressBar;

    private ImageView ivFacebook;
    private ImageView ivGoogle;
    private ImageView ivTwitter;
    private ImageView ivGithub;

    // facebook auth
    private CallbackManager callbackManager;

    // twitter auth
    private TwitterAuthClient twitterAuthClient;
    private Callback<TwitterSession> twitterSessionCallback;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static Intent createIntent(Context context) {
        return new Intent(context, XabberLoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_xabber_login);
        setStatusBarTranslucent();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        ImageView backgroundImage = (ImageView) findViewById(R.id.intro_background_image);
        edtLogin = (EditText) findViewById(R.id.edtLogin);
        edtPass = (EditText) findViewById(R.id.edtPass);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        rlForgotPass = (RelativeLayout) findViewById(R.id.rlForgotPass);

        ivFacebook = (ImageView) findViewById(R.id.ivFacebook);
        ivGoogle = (ImageView) findViewById(R.id.ivGoogle);
        ivTwitter = (ImageView) findViewById(R.id.ivTwitter);
        ivGithub = (ImageView) findViewById(R.id.ivGithub);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.red_700), PorterDuff.Mode.MULTIPLY);

        Glide.with(this)
                .load(R.drawable.intro_background)
                .centerCrop()
                .into(backgroundImage);

        btnLogin.setOnClickListener(this);
        rlForgotPass.setOnClickListener(this);
        ivFacebook.setOnClickListener(this);
        ivGoogle.setOnClickListener(this);
        ivTwitter.setOnClickListener(this);
        ivGithub.setOnClickListener(this);

        // social auth
        initFacebookAuth();
        initTwitterAuth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // facebook auth
        callbackManager.onActivityResult(requestCode, resultCode, data);
        // twitter auth
        twitterAuthClient.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin:
                onLoginClick();
                break;
            case R.id.rlForgotPass:
                break;
            case R.id.ivFacebook:
                onSocialLoginClick(R.id.ivFacebook);
                break;
            case R.id.ivGoogle:
                onSocialLoginClick(R.id.ivGoogle);
                break;
            case R.id.ivGithub:
                onSocialLoginClick(R.id.ivGithub);
                break;
            case R.id.ivTwitter:
                onSocialLoginClick(R.id.ivTwitter);
                break;
        }
    }

    private void onLoginClick() {
        String login = edtLogin.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if (login.isEmpty()) {
            edtLogin.setError("empty field");
            return;
        }

        if (pass.isEmpty()) {
            edtPass.setError("empty field");
            return;
        }

        if (NetworkManager.isNetworkAvailable()) {
            showProgress(true);
            login(login, pass);
        } else
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
    }

    private void onSocialLoginClick(int id) {
        if (NetworkManager.isNetworkAvailable()) {
            switch (id) {
                case R.id.ivFacebook:
                    LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("public_profile"));
                    break;
                case R.id.ivGoogle:
                    break;
                case R.id.ivGithub:
                    break;
                case R.id.ivTwitter:
                    twitterAuthClient.authorize(this, twitterSessionCallback);
                    break;
            }
        } else
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
    }

    private void initFacebookAuth() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                String token = loginResult.getAccessToken().getToken();
                if (token != null)
                    loginSocial(AuthManager.PROVIDER_FACEBOOK, token);
            }

            @Override
            public void onCancel() {
                Toast.makeText(XabberLoginActivity.this, "fcb cancel", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(XabberLoginActivity.this, "fcb error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initTwitterAuth() {
        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(
                        getResources().getString(R.string.SOCIAL_AUTH_TWITTER_KEY),
                        getResources().getString(R.string.SOCIAL_AUTH_TWITTER_SECRET)))
                .debug(true)
                .build();
        Twitter.initialize(config);
        twitterAuthClient = new TwitterAuthClient();
        twitterSessionCallback = new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                String token = result.data.getAuthToken().token;
                String secret = result.data.getAuthToken().secret;
                if (token != null && secret != null)
                    loginSocialTwitter(token, secret,
                            getResources().getString(R.string.SOCIAL_AUTH_TWITTER_SECRET),
                            getResources().getString(R.string.SOCIAL_AUTH_TWITTER_KEY));
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(XabberLoginActivity.this, "twitter error", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void loginSocial(String provider, String token) {
        showProgress(true);
        Subscription loginSocialSubscription = AuthManager.loginSocial(provider, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XAccountTokenDTO>() {
                    @Override
                    public void call(XAccountTokenDTO s) {
                        handleSuccessLogin(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorSocialLogin(throwable);
                    }
                });
        compositeSubscription.add(loginSocialSubscription);
    }

    private void loginSocialTwitter(String token, String twitterTokenSecret, String secret, String key) {
        showProgress(true);
        Subscription loginSocialSubscription = AuthManager.loginSocialTwitter(token, twitterTokenSecret, secret, key)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XAccountTokenDTO>() {
                    @Override
                    public void call(XAccountTokenDTO s) {
                        handleSuccessLogin(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorSocialLogin(throwable);
                    }
                });
        compositeSubscription.add(loginSocialSubscription);
    }

    private void login(String login, String pass) {
        Subscription loginSubscription = AuthManager.login(login, pass)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XAccountTokenDTO>() {
                    @Override
                    public void call(XAccountTokenDTO s) {
                        handleSuccessLogin(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorLogin(throwable);
                    }
                });
        compositeSubscription.add(loginSubscription);
    }

    private void handleSuccessLogin(@NonNull XAccountTokenDTO response) {
        getAccount(response.getToken());
    }

    private void handleErrorLogin(Throwable throwable) {
        Log.d(TAG, "Error while login request: " + throwable.toString());
        Toast.makeText(this, "Username or password is incorrect", Toast.LENGTH_SHORT).show();
        showProgress(false);
    }

    private void handleErrorSocialLogin(Throwable throwable) {
        Log.d(TAG, "Error while social login request: " + throwable.toString());
        Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
        showProgress(false);
    }

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
                        handleErrorLogin(throwable);
                    }
                });
        compositeSubscription.add(getAccountSubscription);
    }

    private void handleSuccessGetAccount(@NonNull XabberAccount xabberAccount) {
        getSettings();
    }

    private void getSettings() {
        Subscription getAccountSubscription = AuthManager.getClientSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        handleSuccessGetSettings(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorLogin(throwable);
                    }
                });
        compositeSubscription.add(getAccountSubscription);
    }

    private void handleSuccessGetSettings(List<XMPPAccountSettings> settings) {
        showProgress(false);

        Intent intent = ContactListActivity.createIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finish();
        startActivity(intent);
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
        }
    }

    void setStatusBarTranslucent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
