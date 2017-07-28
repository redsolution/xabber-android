package com.xabber.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XAccountTokenDTO;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.ui.fragment.XabberLoginFragment;
import com.xabber.android.ui.fragment.XabberSignUpFragment;
import com.xabber.android.utils.RetrofitErrorConverter;

import java.io.IOException;
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

public class XabberLoginActivity extends ManagedActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = XabberLoginActivity.class.getSimpleName();
    public final static String CURRENT_FRAGMENT = "current_fragment";
    public final static String FRAGMENT_LOGIN = "fragment_login";
    public final static String FRAGMENT_SIGNUP = "fragment_signup";

    private Fragment fragmentLogin;
    private Fragment fragmentSignUp;
    private FragmentTransaction fTrans;
    private String currentFragment = FRAGMENT_LOGIN;

    private ProgressDialog progressDialog;

    private ImageView ivFacebook;
    private ImageView ivGoogle;
    private ImageView ivTwitter;
    private ImageView ivGithub;

    // facebook auth
    private CallbackManager callbackManager;

    // twitter auth
    private TwitterAuthClient twitterAuthClient;
    private Callback<TwitterSession> twitterSessionCallback;

    // google auth
    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String GOOGLE_TOKEN_SERVER = "https://www.googleapis.com/oauth2/v4/token";

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static Intent createIntent(Context context) {
        return new Intent(context, XabberLoginActivity.class);
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
        setStatusBarTranslucent();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        ImageView backgroundImage = (ImageView) findViewById(R.id.intro_background_image);

        ivFacebook = (ImageView) findViewById(R.id.ivFacebook);
        ivGoogle = (ImageView) findViewById(R.id.ivGoogle);
        ivTwitter = (ImageView) findViewById(R.id.ivTwitter);
        ivGithub = (ImageView) findViewById(R.id.ivGithub);

        Glide.with(this)
                .load(R.drawable.intro_background)
                .centerCrop()
                .into(backgroundImage);

        ivFacebook.setOnClickListener(this);
        ivGoogle.setOnClickListener(this);
        ivTwitter.setOnClickListener(this);
        ivGithub.setOnClickListener(this);

        // social auth
        initFacebookAuth();
        initTwitterAuth();
        initGoogleAuth();
    }

    public void showSignUpFragment() {
        if (fragmentSignUp == null)
            fragmentSignUp = new XabberSignUpFragment();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentSignUp);
        fTrans.commit();
        currentFragment = FRAGMENT_SIGNUP;
    }

    public void showLoginFragment() {
        if (fragmentLogin == null)
            fragmentLogin = new XabberLoginFragment();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentLogin);
        fTrans.commit();
        currentFragment = FRAGMENT_LOGIN;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentFragment.equals(FRAGMENT_LOGIN)) showLoginFragment();
        else showSignUpFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FRAGMENT, currentFragment);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // facebook auth
        callbackManager.onActivityResult(requestCode, resultCode, data);
        // twitter auth
        twitterAuthClient.onActivityResult(requestCode, resultCode, data);
        // google auth
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
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

    private void onSocialLoginClick(int id) {
        if (NetworkManager.isNetworkAvailable()) {
            switch (id) {
                case R.id.ivFacebook:
                    LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("public_profile"));
                    break;
                case R.id.ivGoogle:
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);
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

    private void initGoogleAuth() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(getString(R.string.SOCIAL_AUTH_GOOGLE_KEY), false)
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();

            if (acct != null) {
                final String googleAuthCode = acct.getServerAuthCode();

                Application.getInstance().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                                new NetHttpTransport(),
                                JacksonFactory.getDefaultInstance(),
                                GOOGLE_TOKEN_SERVER,
                                getString(R.string.SOCIAL_AUTH_GOOGLE_KEY),
                                getString(R.string.SOCIAL_AUTH_GOOGLE_SECRET),
                                googleAuthCode,
                                "").execute();
                            final String token = tokenResponse.getAccessToken();
                            Application.getInstance().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loginSocial(AuthManager.PROVIDER_GOOGLE, token);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } else Toast.makeText(this, "google error", Toast.LENGTH_LONG).show();
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
        showProgress(getString(R.string.progress_title_login));
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
        showProgress(getString(R.string.progress_title_login));
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

    public void login(String login, String pass) {
        showProgress(getString(R.string.progress_title_login));
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
        hideProgress();

        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            Log.d(TAG, "Error while login: " + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Error while login: " + throwable.toString());
            Toast.makeText(this, "Error while login: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleErrorSocialLogin(Throwable throwable) {
        Log.d(TAG, "Error while social login request: " + throwable.toString());
        Toast.makeText(this, R.string.social_auth_fail, Toast.LENGTH_LONG).show();
        hideProgress();
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
        if (!XabberAccount.STATUS_REGISTERED.equals(xabberAccount.getAccountStatus()))
            handleSuccessGetAccountAfterSignUp(xabberAccount);
        else
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
        hideProgress();

        Intent intent = XabberAccountInfoActivity.createIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(XabberAccountInfoActivity.CALL_FROM, XabberAccountInfoActivity.CALL_FROM_LOGIN);
        finish();
        startActivity(intent);
    }

    public void signup(String email) {
        showProgress(getString(R.string.progress_title_signup));
        Subscription signupSubscription = AuthManager.signup(email)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XAccountTokenDTO>() {
                    @Override
                    public void call(XAccountTokenDTO s) {
                        handleSuccessSignUp(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorSignUp(throwable);
                    }
                });
        compositeSubscription.add(signupSubscription);
    }

    private void handleSuccessSignUp(XAccountTokenDTO s) {
        getAccountAfterSignUp(s.getToken());
    }

    private void handleErrorSignUp(Throwable throwable) {
        hideProgress();

        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            Log.d(TAG, "Error while registration: " + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Error while registration: " + throwable.toString());
            Toast.makeText(this, "Error while registration: " + throwable.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void getAccountAfterSignUp(String token) {
        Subscription getAccountSubscription = AuthManager.getAccount(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount xabberAccount) {
                        handleSuccessGetAccountAfterSignUp(xabberAccount);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorSignUp(throwable);
                    }
                });
        compositeSubscription.add(getAccountSubscription);
    }

    private void handleSuccessGetAccountAfterSignUp(XabberAccount account) {
        hideProgress();

        Intent intent = ContactListActivity.createIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finish();
        startActivity(intent);
    }

    private void showProgress(String title) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title);
        progressDialog.setMessage(getResources().getString(R.string.progress_message));
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
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
