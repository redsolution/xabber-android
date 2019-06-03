package com.xabber.android.data.http;

import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.POST;
import rx.Single;

public interface IPushApi {

    @POST("jid/endpoints/")
    Single<ResponseBody> registerEndpoint(@Header("Authorization") String key, @Body PushApiClient.Endpoint endpoint);

    @HTTP(method = "DELETE", path = "jid/endpoints/", hasBody = true)
    Single<ResponseBody> deleteEndpoint(@Header("Authorization") String key, @Body PushApiClient.Endpoint endpoint);

}
