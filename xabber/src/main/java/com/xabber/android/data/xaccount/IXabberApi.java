package com.xabber.android.data.xaccount;

import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import rx.Single;

/**
 * Created by valery.miller on 17.07.17.
 */

public interface IXabberApi {

    @POST("accounts/login/")
    Single<XAccountTokenDTO> login(@Header("Authorization") String credentials, @Body AuthManager.Source source);

    @POST("accounts/logout/")
    Single<ResponseBody> logout(@Header("Authorization") String token);

    @POST("accounts/social_auth/")
    Single<XAccountTokenDTO> loginSocial(@Body AuthManager.SocialAuthRequest body);

    @POST("accounts/signup/")
    Single<XAccountTokenDTO> signup(@Body AuthManager.Email email);

    @GET("accounts/current")
    Single<XabberAccountDTO> getAccount(@Header("Authorization") String token);

    @GET("accounts/current/client-settings/")
    Single<AuthManager.ListClientSettingsDTO> getClientSettings(@Header("Authorization") String token);

    @PATCH("accounts/current/client-settings/")
    Single<AuthManager.ListClientSettingsDTO> updateClientSettings(@Header("Authorization") String token, @Body AuthManager.ClientSettingsWithoutOrderDTO body);

    @PATCH("accounts/current/client-settings/")
    Single<AuthManager.ListClientSettingsDTO> updateClientSettings(@Header("Authorization") String token, @Body AuthManager.ClientSettingsOrderDTO body);

    @HTTP(method = "DELETE", path = "accounts/current/client-settings/", hasBody = true)
    Single<AuthManager.ListClientSettingsDTO> deleteClientSettings(@Header("Authorization") String token, @Body AuthManager.Jid jid);

    @POST("accounts/email_confirmation/")
    Single<XabberAccountDTO> confirmEmail(@Header("Authorization") String token, @Body AuthManager.Code code);

    @POST("accounts/email_confirmation/")
    Single<XabberAccountDTO> confirmEmail(@Body AuthManager.Key key);

    @POST("accounts/current/complete_registration/")
    Single<XabberAccountDTO> completeRegister(@Header("Authorization") String token, @Body AuthManager.CompleteRegister register);

    @POST("accounts/current/email_list/")
    Single<ResponseBody> addEmail(@Header("Authorization") String token, @Body AuthManager.Email email);

}

