package com.xabber.android.utils;

import android.content.Context;

import androidx.annotation.Nullable;

public class ExternalAPIs {

    public static void enableCrashlyticsIfNeed(Context context) {
        // Crashlytics not used in open flavour
    }

    @Nullable
    public static String getPushEndpointToken() {
        // Firebase pushes not used in open flavour
        return null;
    }

}