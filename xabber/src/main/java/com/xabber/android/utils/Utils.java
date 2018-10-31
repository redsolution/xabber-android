package com.xabber.android.utils;

import android.content.Context;
import android.util.TypedValue;

public class Utils {

    public static int dipToPx(float dip, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dip, context.getResources().getDisplayMetrics());
    }

}
