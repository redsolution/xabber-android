package com.xabber.android.data.xaccount;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
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

    @GET("accounts/current/")
    Single<XabberAccountDTO> getAccount(@Header("Authorization") String token);

    @PATCH("accounts/current/")
    Single<XabberAccountDTO> updateAccount(@Header("Authorization") String token, @Body AuthManager.Account account);

    @POST("accounts/current/phone/")
    Single<ResponseBody> setPhoneNumber(@Header("Authorization") String token, @Body AuthManager.SetPhoneNumber setPhoneNumber);

    @POST("accounts/current/phone/")
    Single<ResponseBody> confirmPhoneNumber(@Header("Authorization") String token, @Body AuthManager.ConfirmPhoneNumber confirmPhoneNumber);

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

    @DELETE("accounts/current/email_list/{email_id}/")
    Single<ResponseBody> deleteEmail(@Header("Authorization") String token, @Path(value = "email_id", encoded = true) int emailId);

    /* Xabber API v2 */

    @POST("accounts/xmpp_code_request/")
    Single<AuthManager.XMPPCode> requestXMPPCode(@Body AuthManager.Jid jid);

    @POST("accounts/xmpp_auth/")
    Single<XabberAccountDTO> confirmXMPP(@Body AuthManager.CodeConfirm codeConfirm);

    @GET("accounts/xmpp/hosts/")
    Single<List<AuthManager.Domain>> getHosts();

    @POST("accounts/signup/")
    Single<XabberAccountDTO> signupv2(@Body AuthManager.SignUpFields fields);

    @POST("accounts/current/social_bind/")
    Single<ResponseBody> bindSocial(@Header("Authorization") String token, @Body AuthManager.SocialAuthRequest body);

    @POST("accounts/current/social_unbind/")
    Single<ResponseBody> unbindSocial(@Header("Authorization") String token, @Body AuthManager.Provider provider);
}

