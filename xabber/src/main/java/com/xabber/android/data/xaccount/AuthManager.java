package com.xabber.android.data.xaccount;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;

import okhttp3.ResponseBody;
import rx.Single;
import rx.functions.Func1;

/**
 * Created by valery.miller on 17.07.17.
 */

public class AuthManager {

    public static final String PROVIDER_FACEBOOK = "facebook";
    public static final String PROVIDER_TWITTER = "twitter";
    public static final String PROVIDER_GITHUB = "github";
    public static final String PROVIDER_GOOGLE = "google";

    public static Single<XAccountTokenDTO> login(Context context, String login, String pass) {

        String credentials = login + ":" + pass;
        byte[] data = credentials.getBytes();
        String encodedCredentials = Base64.encodeToString(data, Base64.NO_WRAP);

        return HttpApiManager.getXabberApi(context).login("Basic " + encodedCredentials);
    }

    public static Single<ResponseBody> logout(Context context) {

        return HttpApiManager.getXabberApi(context).logout(getXabberToken())
                .flatMap(new Func1<ResponseBody, Single<? extends ResponseBody>>() {
                    @Override
                    public Single<? extends ResponseBody> call(ResponseBody responseBody) {
                        if (XabberAccountManager.getInstance().deleteXabberAccountFromRealm())
                            return Single.just(responseBody);
                        else return Single.error(new Throwable("Realm deletion error"));
                    }
                });
    }

    public static Single<XAccountTokenDTO> loginSocial(Context context, String provider, String socialToken) {

        Gson gson = new Gson();
        String credentials = gson.toJson(new AccessToken(socialToken));
        return HttpApiManager.getXabberApi(context).loginSocial(new SocialAuthRequest(provider, credentials));
    }

    public static Single<XAccountTokenDTO> loginSocialTwitter(
            Context context, String socialToken, String twitterTokenSecret, String secret, String key) {

        Gson gson = new Gson();
        String credentials = gson.toJson(new TwitterAccessToken(new TwitterTokens(twitterTokenSecret, socialToken), secret, key));
        return HttpApiManager.getXabberApi(context).loginSocial(new SocialAuthRequest(PROVIDER_TWITTER, credentials));
    }

    public static Single<XabberAccount> getAccount(Context context, final String token) {
        return HttpApiManager.getXabberApi(context).getAccount("Token " + token)
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO, token);
                    }
                });
    }

    private static String getXabberToken() {
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null && account.getToken() != null)
            return "Token " + account.getToken();
        else return null;
    }

    public static class SocialAuthRequest {
        final String provider;
        final String credentials;

        public SocialAuthRequest(String provider, String credentials) {
            this.provider = provider;
            this.credentials = credentials;
        }
    }

    public static class AccessToken {
        final String access_token;

        public AccessToken(String access_token) {
            this.access_token = access_token;
        }
    }

    public static class TwitterAccessToken {
        final TwitterTokens access_token;
        final String secret;
        final String key;

        public TwitterAccessToken(TwitterTokens access_token, String secret, String key) {
            this.access_token = access_token;
            this.secret = secret;
            this.key = key;
        }
    }

    public static class TwitterTokens {
        final String oauth_token_secret;
        final String oauth_token;

        public TwitterTokens(String oauth_token_secret, String oauth_token) {
            this.oauth_token_secret = oauth_token_secret;
            this.oauth_token = oauth_token;
        }
    }

}
