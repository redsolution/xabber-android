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
    Single<ResponseBody> login(@Header("Authorization") String credentials);

    // TODO: 20.07.17 delete additional headers
    @POST("accounts/logout/")
    Single<ResponseBody> logout(@Header("Referer") String referer, @Header("X-CSRFToken") String token);

    @POST("accounts/social_auth/")
    Single<ResponseBody> loginSocial(@Body AuthManager.SocialAuthRequest body, @Header("Referer") String referer, @Header("X-CSRFToken") String token);

    @GET("accounts/current")
    Single<XabberAccountDTO> getAccount();

}

