package com.xabber.android.ui.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
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
import com.google.gson.Gson;
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

import java.io.IOException;
import java.util.Collections;
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

public abstract class BaseLoginActivity extends ManagedActivity implements
        GoogleApiClient.OnConnectionFailedListener, XAccountLinksFragment.Listener,
        AddEmailDialogFragment.Listener, ConfirmEmailDialogFragment.Listener, OnSocialBindListener {

    private final static String LOG_TAG = BaseLoginActivity.class.getSimpleName();

    // facebook auth
    private CallbackManager callbackManager;

    // twitter auth
    private TwitterAuthClient twitterAuthClient;
    private Callback<TwitterSession> twitterSessionCallback;

    // google auth
    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String GOOGLE_TOKEN_SERVER = "https://www.googleapis.com/oauth2/v4/token";

    private Gson gson = new Gson();
    protected CompositeSubscription compositeSubscription = new CompositeSubscription();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableGoogleAuth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // facebook auth
        if (callbackManager != null)
            callbackManager.onActivityResult(requestCode, resultCode, data);
        // twitter auth
        if (twitterAuthClient != null)
            twitterAuthClient.onActivityResult(requestCode, resultCode, data);
        // google auth
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    public void loginFacebook() {
        FacebookSdk.setApplicationId(getString(R.string.SOCIAL_AUTH_FACEBOOK_KEY));
        FacebookSdk.sdkInitialize(this);
        initFacebookAuth();
        LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("public_profile"));
    }

    public void loginGoogle() {
        initGoogleAuth();
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void loginTwitter() {
        initTwitterAuth();
        twitterAuthClient.authorize(this, twitterSessionCallback);
    }

    private void initGoogleAuth() {
        if (mGoogleApiClient != null) return;
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(getString(R.string.SOCIAL_AUTH_GOOGLE_KEY), false)
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void disableGoogleAuth() {
        if (mGoogleApiClient == null) return;
        mGoogleApiClient.stopAutoManage(this);
        mGoogleApiClient.disconnect();
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
                                    String credentials = gson.toJson(new AuthManager.AccessToken(token));
                                    onSocialAuthSuccess(AuthManager.PROVIDER_GOOGLE, credentials);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } else Toast.makeText(this, R.string.auth_google_error, Toast.LENGTH_LONG).show();
    }

    private void initFacebookAuth() {
        if (callbackManager != null) return;
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                String token = loginResult.getAccessToken().getToken();
                if (token != null) {
                    String credentials = gson.toJson(new AuthManager.AccessToken(token));
                    onSocialAuthSuccess(AuthManager.PROVIDER_FACEBOOK, credentials);
                }
            }

            @Override
            public void onCancel() {
                Toast.makeText(BaseLoginActivity.this, R.string.auth_facebook_cancel, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(BaseLoginActivity.this, R.string.auth_facebook_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initTwitterAuth() {
        if (twitterAuthClient != null && twitterSessionCallback != null) return;
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
                if (token != null && secret != null) {
                    String credentials = gson.toJson(new AuthManager.TwitterAccessToken(
                            new AuthManager.TwitterTokens(secret, token),
                            getResources().getString(R.string.SOCIAL_AUTH_TWITTER_SECRET),
                            getResources().getString(R.string.SOCIAL_AUTH_TWITTER_KEY)));
                    onSocialAuthSuccess(AuthManager.PROVIDER_TWITTER, credentials);
                }
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(BaseLoginActivity.this, R.string.auth_twitter_error, Toast.LENGTH_SHORT).show();
            }
        };
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
