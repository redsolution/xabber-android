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

import com.xabber.android.R;
import com.xabber.android.data.Application;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to get plural forms.
 *
 * @author alexander.ivanov
 */
public class StringUtils {

    private static final DateFormat DATE_TIME;
    private static final DateFormat TIME;
    private static final String LOG_DATE_TIME_FORMAT = "HH:mm:ss yyyy-MM-dd";

    static {
        DATE_TIME = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                DateFormat.SHORT);
        TIME = new SimpleDateFormat("HH:mm:ss");
        timeFormat = android.text.format.DateFormat.getTimeFormat(Application.getInstance());
    }

    private static SimpleDateFormat logDateTimeFormat;
    private static DateFormat timeFormat;

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

    public static String getTimeText(Date timeStamp) {
        return timeFormat.format(timeStamp);
    }

    public static String getTimeTextWithSeconds(Date timeStamp) {
        return TIME.format(timeStamp);
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

        if (timeStamp.getTime() > midnight.getTimeInMillis()) {
            return timeFormat.format(timeStamp);
        } else {
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
            return dateFormat.format(timeStamp) + " " + timeFormat.format(timeStamp);
        }
    }

    public static String getSmartTimeTextForRoster(Context context, Date timeStamp) {
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

        if (timeStamp.getTime() > midnight.getTimeInMillis()) {
            return timeFormat.format(timeStamp);
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM",
                    context.getResources().getConfiguration().locale);
            return dateFormat.format(timeStamp);
        }
    }

    public static SimpleDateFormat getLogDateTimeFormat() {
        if (logDateTimeFormat == null) {
            logDateTimeFormat = new SimpleDateFormat(LOG_DATE_TIME_FORMAT, Locale.ENGLISH);
        }

        return logDateTimeFormat;
    }

    public static String getLastActivityString(long lastActivityTime) {
        if (lastActivityTime > 0) {
            long timeAgo = System.currentTimeMillis()/1000 - lastActivityTime;
            long time;

            if (timeAgo < 60) return Application.getInstance().getString(R.string.last_seen_now);
            if (timeAgo < 3600) {
                time = TimeUnit.SECONDS.toMinutes(timeAgo);
                return Application.getInstance().getString(R.string.last_seen_minutes, String.valueOf(time));
            }
            if (timeAgo < 7200) {
                time = TimeUnit.SECONDS.toHours(timeAgo);
                return Application.getInstance().getString(R.string.last_seen_hours);
            }

            String sTime;
            Date date = new Date(lastActivityTime * 1000);
            Date today = new Date();
            Locale locale = Application.getInstance().getResources().getConfiguration().locale;

            if (isToday(date)) {
                SimpleDateFormat pattern = new SimpleDateFormat("HH:mm", locale);
                sTime = pattern.format(date);
                return Application.getInstance().getString(R.string.last_seen_today, sTime);
            }

            if (isYesterday(date)) {
                SimpleDateFormat pattern = new SimpleDateFormat("HH:mm", locale);
                sTime = pattern.format(date);
                return Application.getInstance().getString(R.string.last_seen_yesterday, sTime);
            }

            if (timeAgo < TimeUnit.DAYS.toSeconds(7)) {
                SimpleDateFormat pattern = new SimpleDateFormat("HH:mm", locale);
                sTime = pattern.format(date);
                return Application.getInstance().getString(R.string.last_seen_on_week,
                        getDayOfWeek(date, locale), sTime);
            }

            if (date.getYear() == today.getYear()) {
                SimpleDateFormat pattern = new SimpleDateFormat("d MMMM", locale);
                sTime = pattern.format(date);
                return Application.getInstance().getString(R.string.last_seen_date, sTime);
            }

            if (date.getYear() < today.getYear()) {
                SimpleDateFormat pattern = new SimpleDateFormat("d MMMM yyyy", locale);
                sTime = pattern.format(date);
                return Application.getInstance().getString(R.string.last_seen_date, sTime);
            }
            return "";
        }
        else return "";
    }

    public static boolean isToday(Date date) {
        Calendar calendarOne = Calendar.getInstance();
        Calendar calendarTwo = Calendar.getInstance();
        calendarOne.setTime(date);
        calendarTwo.setTime(new Date());
        return calendarOne.get(Calendar.DAY_OF_YEAR) == calendarTwo.get(Calendar.DAY_OF_YEAR) &&
                calendarOne.get(Calendar.YEAR) == calendarTwo.get(Calendar.YEAR);
    }

    public static boolean isYesterday(Date date) {
        Calendar calendarOne = Calendar.getInstance();
        Calendar calendarTwo = Calendar.getInstance();
        calendarOne.setTime(date);
        calendarTwo.setTime(new Date());
        return calendarOne.get(Calendar.DAY_OF_YEAR) == calendarTwo.get(Calendar.DAY_OF_YEAR) - 1 &&
                calendarOne.get(Calendar.YEAR) == calendarTwo.get(Calendar.YEAR);
    }

    public static String getDayOfWeek(Date date, Locale locale) {
        DateFormatSymbols symbols = new DateFormatSymbols(locale);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return symbols.getWeekdays()[c.get(Calendar.DAY_OF_WEEK)];
    }

    public static String getDateStringForMessage(Long timestamp) {
        Date date = new Date(timestamp);
        String strPattern = "d MMMM";
        if (date.getYear() != new Date().getYear()) strPattern = "d MMMM yyyy";

        SimpleDateFormat pattern = new SimpleDateFormat(strPattern,
                Application.getInstance().getResources().getConfiguration().locale);
        return pattern.format(date);
    }

    public static String getDateStringForClipboard(Long timestamp) {
        Date date = new Date(timestamp);
        String strPattern = "EEEE, d MMMM, yyyy";

        SimpleDateFormat pattern = new SimpleDateFormat(strPattern,
                Application.getInstance().getResources().getConfiguration().locale);
        return pattern.format(date);
    }
}
