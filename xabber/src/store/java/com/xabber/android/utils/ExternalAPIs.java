package com.xabber.android.utils;

import androidx.annotation.Nullable;
import com.google.firebase.iid.FirebaseInstanceId;

public class ExternalAPIs {

    @Nullable
    public static String getPushEndpointToken() {
        return FirebaseInstanceId.getInstance().getToken();
    }

}