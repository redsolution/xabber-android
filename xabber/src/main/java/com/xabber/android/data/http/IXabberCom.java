package com.xabber.android.data.http;

import retrofit2.http.GET;
import rx.Single;

/**
 * Created by valery.miller on 02.10.17.
 */

public interface IXabberCom {

    String SHARE_URL = "https://www.patreon.com/xabber";

    @GET("patreon.json")
    Single<XabberComClient.Patreon> getPatreon();

}
