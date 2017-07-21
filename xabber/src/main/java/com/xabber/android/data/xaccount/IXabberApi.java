package com.xabber.android.data.xaccount;

import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import rx.Single;

/**
 * Created by valery.miller on 17.07.17.
 */

public interface IXabberApi {

    @POST("accounts/login/")
    Single<XAccountTokenDTO> login(@Header("Authorization") String credentials);

    @POST("accounts/logout/")
    Single<ResponseBody> logout(@Header("Authorization") String token);

    @POST("accounts/social_auth/")
    Single<XAccountTokenDTO> loginSocial(@Body AuthManager.SocialAuthRequest body);

    @GET("accounts/current")
    Single<XabberAccountDTO> getAccount(@Header("Authorization") String token);

    @GET("accounts/current/client-settings/")
    Single<AuthManager.ListClientSettingsDTO> getClientSettings(@Header("Authorization") String token);

    @POST("accounts/current/client-settings/")
    Single<ResponseBody> updateClientSettings(@Header("Authorization") String token, @Body AuthManager.UpdateClientSettings body);

}

