package com.xabber.android.utils;

import androidx.annotation.Nullable;

public class ExternalAPIs {

    @Nullable
    public static String getPushEndpointToken() {
        // Firebase pushes not used in open flavour
        return null;
    }

}