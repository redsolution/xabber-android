package com.xabber.android.utils;

/**
 * Created by valery.miller on 19.03.18.
 */

public class ColorTransparentUtils {

    public static String convertIntoColor(int colorCode, int transCode) {

        // convert color code into hex string and remove starting 2 digit
        String color = Integer.toHexString(colorCode).toUpperCase().substring(2);
        if (!color.isEmpty() && transCode < 100) {
            if (color.trim().length() == 6) {
                return "#" + convert(transCode) + color;
            } else {
                // Color is already with transparency
                return convert(transCode) + color;
            }
        }
        return color;
    }

    public static String convert(int trans) {
        String hexString = Integer.toHexString(Math.round(255 * trans/100));
        return (hexString.length() < 2 ? "0" : "") + hexString;
    }
}
