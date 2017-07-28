package com.xabber.android.data.xaccount;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xabber.android.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by valery.miller on 17.07.17.
 */

public class HttpApiManager {

    private static final String XABBER_API_URL = "https://api.xabber.com/api/v1/";
    private static final String XABBER_API_URL_TEST = "http://c0014.soni.redsolution.ru:9001/api/v1/";
    private static IXabberApi xabberApi;
    private static Retrofit retrofit;

    public static IXabberApi getXabberApi() {
        if (xabberApi == null)
            xabberApi = getRetrofit().create(IXabberApi.class);
        return xabberApi;
    }

    public static Retrofit getRetrofit() {
        if (retrofit == null) {

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

            // if debug enable http logging
            if (BuildConfig.DEBUG)
                httpClientBuilder.addInterceptor(loggingInterceptor);

            OkHttpClient httpClient = httpClientBuilder.build();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(AuthManager.ListClientSettingsDTO.class, new ClientSettingsDeserializer())
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                .baseUrl(XABBER_API_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build();
        }
        return retrofit;
    }
}

