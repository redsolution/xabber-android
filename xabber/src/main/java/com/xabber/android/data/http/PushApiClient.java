package com.xabber.android.data.http;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.xaccount.HttpApiManager;

import okhttp3.ResponseBody;
import rx.Single;

public class PushApiClient {

    private static final String DEFAULT_PUSH_PROVIDER = "fcm";

    private static PushApiClient instance;

    public static PushApiClient getInstance() {
        if (instance == null)
            instance = new PushApiClient();
        return instance;
    }

    public static Single<ResponseBody> registerEndpoint(String endpoint, String jid) {
        if (getAPIKey().length() < 36) return Single.error(new Throwable("API key not provided"));
        return HttpApiManager.getPushApi().registerEndpoint(getAPIKey(),
                new Endpoint(endpoint, DEFAULT_PUSH_PROVIDER, jid));
    }

    public static Single<ResponseBody> deleteEndpoint(String endpoint, String jid) {
        if (getAPIKey().length() < 36) return Single.error(new Throwable("API key not provided"));
        return HttpApiManager.getPushApi().deleteEndpoint(getAPIKey(),
                new Endpoint(endpoint, DEFAULT_PUSH_PROVIDER, jid));
    }

    private static String getAPIKey() {
        return "Key " + Application.getInstance().getResources().getString(R.string.PUSH_API_KEY);
    }

    public static class Endpoint {
        final String endpoint_key;
        final String provider;
        final String target;

        public Endpoint(String endpoint_key, String provider, String target) {
            this.endpoint_key = endpoint_key;
            this.provider = provider;
            this.target = target;
        }
    }

}
