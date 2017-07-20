package com.xabber.android.data.xaccount;

import android.content.Context;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.CookieCache;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xabber.android.BuildConfig;

import okhttp3.CookieJar;
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
    private static IXabberApi xabberApi;
    private static CookieCache cookieCache;

    public static IXabberApi getXabberApi(Context mContext) {
        if (xabberApi == null) {

            // TODO: 20.07.17 delete cookie jar
            CookieJar cookieJar = new PersistentCookieJar(getCookieCache(), new SharedPrefsCookiePersistor(mContext));

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                    .cookieJar(cookieJar);

            // if debug enable http logging
            if (BuildConfig.DEBUG)
                httpClientBuilder.addInterceptor(loggingInterceptor);

            OkHttpClient httpClient = httpClientBuilder.build();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(XABBER_API_URL)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClient)
                    .build();

            xabberApi = retrofit.create(IXabberApi.class);
        }
        return xabberApi;
    }

    // TODO: 20.07.17 delete
    public static CookieCache getCookieCache() {
        if (cookieCache == null)
            cookieCache = new SetCookieCache();
        return cookieCache;
    }

}

