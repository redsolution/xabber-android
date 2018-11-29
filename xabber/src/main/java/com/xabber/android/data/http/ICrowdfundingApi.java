package com.xabber.android.data.http;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
import rx.Single;

public interface ICrowdfundingApi {

    @GET("leader/")
    Single<CrowdfundingClient.Message> getLeader(@Header("Authorization") String apiKey);

    @GET("feed/")
    Single<List<CrowdfundingClient.Message>> getFeed(@Header("Authorization") String apiKey, @Query("timestamp") String timestamp);

}
