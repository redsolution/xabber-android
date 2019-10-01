package com.xabber.android.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.xabber.android.BuildConfig;

import io.fabric.sdk.android.Fabric;

public class ExternalAPIs {

    public static void enableCrashlyticsIfNeed(Context context) {
        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build();
        Fabric.with(context, new Crashlytics.Builder().core(crashlyticsCore).build());
    }

    @Nullable
    public static String getPushEndpointToken() {
        return FirebaseInstanceId.getInstance().getToken();
    }

}