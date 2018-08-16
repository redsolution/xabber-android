package com.xabber.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

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
import com.xabber.android.data.xaccount.AuthManager;

import java.io.IOException;
import java.util.Collections;

import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 31.07.17.
 */

public abstract class BaseLoginActivity extends ManagedActivity implements GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = BaseLoginActivity.class.getSimpleName();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // social auth
        initFacebookAuth();
        initTwitterAuth();
        initGoogleAuth();
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
        LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("public_profile"));
    }

    public void loginGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void loginTwitter() {
        twitterAuthClient.authorize(this, twitterSessionCallback);
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
        } else Toast.makeText(this, "google error", Toast.LENGTH_LONG).show();
    }

    private void initFacebookAuth() {
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
                Toast.makeText(BaseLoginActivity.this, "fcb cancel", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(BaseLoginActivity.this, "fcb error", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(BaseLoginActivity.this, "twitter error", Toast.LENGTH_SHORT).show();
            }
        };
    }

    protected abstract void onSocialAuthSuccess(final String provider, final String credentials);

    protected abstract void showProgress(String title);

    protected abstract void hideProgress();
}
