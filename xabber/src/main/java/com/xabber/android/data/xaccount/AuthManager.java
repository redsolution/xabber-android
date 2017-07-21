package com.xabber.android.data.xaccount;

import android.util.Base64;
import com.google.gson.Gson;
import java.util.List;

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

    public static Single<XAccountTokenDTO> login(String login, String pass) {

        String credentials = login + ":" + pass;
        byte[] data = credentials.getBytes();
        String encodedCredentials = Base64.encodeToString(data, Base64.NO_WRAP);

        return HttpApiManager.getXabberApi().login("Basic " + encodedCredentials);
    }

    public static Single<ResponseBody> logout() {

        return HttpApiManager.getXabberApi().logout(getXabberToken())
                .flatMap(new Func1<ResponseBody, Single<? extends ResponseBody>>() {
                    @Override
                    public Single<? extends ResponseBody> call(ResponseBody responseBody) {
                        if (XabberAccountManager.getInstance().deleteXabberAccountFromRealm())
                            return Single.just(responseBody);
                        else return Single.error(new Throwable("Realm deletion error"));
                    }
                });
    }

    public static Single<XAccountTokenDTO> loginSocial(String provider, String socialToken) {

        Gson gson = new Gson();
        String credentials = gson.toJson(new AccessToken(socialToken));
        return HttpApiManager.getXabberApi().loginSocial(new SocialAuthRequest(provider, credentials));
    }

    public static Single<XAccountTokenDTO> loginSocialTwitter(
           String socialToken, String twitterTokenSecret, String secret, String key) {

        Gson gson = new Gson();
        String credentials = gson.toJson(new TwitterAccessToken(new TwitterTokens(twitterTokenSecret, socialToken), secret, key));
        return HttpApiManager.getXabberApi().loginSocial(new SocialAuthRequest(PROVIDER_TWITTER, credentials));
    }

    public static Single<XabberAccount> getAccount(final String token) {
        return HttpApiManager.getXabberApi().getAccount("Token " + token)
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO, token);
                    }
                });
    }

    public static Single<List<XMPPAccountSettings>> getClientSettings() {
        return HttpApiManager.getXabberApi().getClientSettings(getXabberToken())
                .flatMap(new Func1<ListClientSettingsDTO, Single<? extends List<XMPPAccountSettings>>>() {
                    @Override
                    public Single<? extends List<XMPPAccountSettings>> call(ListClientSettingsDTO listClientSettingsDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXMPPAccountSettingsToRealm(listClientSettingsDTO);
                    }
                });
    }

    public static Single<ResponseBody> updateClientSettings(UpdateClientSettings updateClientSettings) {
        return HttpApiManager.getXabberApi().updateClientSettings(getXabberToken(), updateClientSettings);
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

    public static class UpdateClientSettings {
        final String jid;
        final UpdateSettingsValues settings;

        public UpdateClientSettings(String jid, UpdateSettingsValues settings) {
            this.jid = jid;
            this.settings = settings;
        }
    }

    public static class UpdateSettingsValues {
        final int order;

        public UpdateSettingsValues(int order) {
            this.order = order;
        }
    }

    public static class ListClientSettingsDTO {
        final List<ClientSettingsDTO> settings;

        public ListClientSettingsDTO(List<ClientSettingsDTO> settings) {
            this.settings = settings;
        }

        public List<ClientSettingsDTO> getSettings() {
            return settings;
        }
    }

    public static class ClientSettingsDTO {
        final String jid;
        final SettingsValuesDTO settings;

        public ClientSettingsDTO(String jid, SettingsValuesDTO settings) {
            this.jid = jid;
            this.settings = settings;
        }

        public String getJid() {
            return jid;
        }

        public SettingsValuesDTO getSettings() {
            return settings;
        }
    }

    public static class SettingsValuesDTO {
        final int order;
        final String color;
        final String token;

        public SettingsValuesDTO(int order, String color, String token) {
            this.order = order;
            this.color = color;
            this.token = token;
        }

        public int getOrder() {
            return order;
        }

        public String getColor() {
            return color;
        }

        public String getToken() {
            return token;
        }
    }
}
