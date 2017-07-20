package com.xabber.android.data.xaccount;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;

import okhttp3.Cookie;
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

    public static Single<ResponseBody> login(Context context, String login, String pass) {

        String credentials = login + ":" + pass;
        byte[] data = credentials.getBytes();
        String encodedCredentials = Base64.encodeToString(data, Base64.NO_WRAP);

        return HttpApiManager.getXabberApi(context).login("Basic " + encodedCredentials);
    }

    public static Single<ResponseBody> logout(Context context) {
        // TODO: 20.07.17 delete setting referer and csrftoken
        String csrftoken = "";
        for (Cookie cookie : HttpApiManager.getCookieCache()) {
            if (cookie.domain().equals("api.xabber.com") && cookie.name().equals("csrftoken"))
                csrftoken = cookie.value();
        }
        return HttpApiManager.getXabberApi(context).logout("https://api.xabber.com", csrftoken);
    }

    public static Single<ResponseBody> loginSocial(Context context, String provider, String token) {

        Gson gson = new Gson();
        String credentials = gson.toJson(new AccessToken(token));
        // TODO: 20.07.17 delete setting referer and csrftoken
        String csrftoken = "";
        for (Cookie cookie : HttpApiManager.getCookieCache()) {
            if (cookie.domain().equals("api.xabber.com") && cookie.name().equals("csrftoken"))
                csrftoken = cookie.value();
        }
        return HttpApiManager.getXabberApi(context).loginSocial(new SocialAuthRequest(provider, credentials), "https://api.xabber.com", csrftoken);
    }

    public static Single<XabberAccount> getAccount(Context context) {
        return HttpApiManager.getXabberApi(context).getAccount()
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO);
                    }
                });
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

}
