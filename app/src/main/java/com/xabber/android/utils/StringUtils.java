/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.utils;

import android.content.Context;
import android.content.res.Resources;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Helper class to get plural forms.
 *
 * @author alexander.ivanov
 */
public class StringUtils {

    private static final DateFormat DATE_TIME;
    private static final DateFormat TIME;

    static {
        DATE_TIME = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                DateFormat.SHORT);
        TIME = new SimpleDateFormat("H:mm");
    }

    private StringUtils() {
    }

    /**
     * @param resources
     * @param stringArrayResourceId
     * @param quantity
     * @return Plural string for the given quantity.
     */
    public static String getQuantityString(Resources resources,
                                           int stringArrayResourceId, long quantity) {
        String[] strings = resources.getStringArray(stringArrayResourceId);
        String lang = resources.getConfiguration().locale.getLanguage();
        if ("ru".equals(lang) && strings.length == 3) {
            quantity = quantity % 100;
            if (quantity >= 20)
                quantity = quantity % 10;
            if (quantity == 1)
                return strings[0];
            if (quantity >= 2 && quantity < 5)
                return strings[1];
            return strings[2];
        } else if (("cs".equals(lang) || "pl".equals(lang))
                && strings.length == 3) {
            if (quantity == 1) {
                return strings[0];
            } else if (quantity >= 2 && quantity <= 4) {
                return strings[1];
            } else {
                return strings[2];
            }
        } else {
            if (quantity == 1) {
                return strings[0];
            } else {
                return strings[1];
            }
        }
    }

    /**
     * Escape input chars to be shown in html.
     *
     * @param input
     * @return
     */
    public static String escapeHtml(String input) {
        StringBuilder builder = new StringBuilder();
        int pos = 0;
        int len = input.length();
        while (pos < len) {
            int codePoint = Character.codePointAt(input, pos);
            if (codePoint == '"')
                builder.append("&quot;");
            else if (codePoint == '&')
                builder.append("&amp;");
            else if (codePoint == '<')
                builder.append("&lt;");
            else if (codePoint == '>')
                builder.append("&gt;");
            else if (codePoint == '\n')
                builder.append("<br />");
            else if (codePoint >= 0 && codePoint < 160)
                builder.append(Character.toChars(codePoint));
            else
                builder.append("&#").append(codePoint).append(';');
            pos += Character.charCount(codePoint);
        }
        return builder.toString();
    }

    /**
     * @param timeStamp
     * @return String with date and time to be display.
     */
    public static String getDateTimeText(Date timeStamp) {
        synchronized (DATE_TIME) {
            return DATE_TIME.format(timeStamp);
        }
    }

    /**
     * @param timeStamp
     * @return String with time or with date and time depend on current time.
     */
    public static String getSmartTimeText(Context context, Date timeStamp) {
        if (timeStamp == null) {
            return "";
        }

        // today
        Calendar midnight = new GregorianCalendar();
        // reset hour, minutes, seconds and millis
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);

        if (timeStamp.getTime() > midnight.getTimeInMillis()) {
            synchronized (TIME) {
                return timeFormat.format(timeStamp);
            }
        } else {
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
            return dateFormat.format(timeStamp) + " " + timeFormat.format(timeStamp);
        }
    }

}
