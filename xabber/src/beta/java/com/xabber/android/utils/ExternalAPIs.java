package com.xabber.android.utils;

import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.xabber.android.BuildConfig;

import io.fabric.sdk.android.Fabric;

public class ExternalAPIs {

    public static void enableCrashlyticsIfNeed(Context context) {
        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build();
        Fabric.with(context, new Crashlytics.Builder().core(crashlyticsCore).build());
    }

}