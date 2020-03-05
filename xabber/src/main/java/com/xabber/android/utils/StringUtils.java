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
import android.icu.text.Transliterator;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.log.LogManager;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
        if (timeStamp == null)
            return "";

        Calendar day = GregorianCalendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);

        Calendar hours = GregorianCalendar.getInstance();
        hours.add(Calendar.HOUR, -12);

        Calendar week = GregorianCalendar.getInstance();
        week.add(Calendar.HOUR, -168);

        Calendar year = GregorianCalendar.getInstance();
        year.add(Calendar.YEAR, -1);

        if (year.getTimeInMillis() > timeStamp.getTime())
            return new SimpleDateFormat("dd MMM yyyy", context.getResources().getConfiguration().locale).format(timeStamp);
        else if (week.getTimeInMillis() > timeStamp.getTime())
            return new SimpleDateFormat("MMM d", context.getResources().getConfiguration().locale).format(timeStamp);
        else if (hours.getTimeInMillis() > timeStamp.getTime())
            return new SimpleDateFormat("E", context.getResources().getConfiguration().locale).format(timeStamp);
        else if (day.getTimeInMillis() > timeStamp.getTime() && hours.getTimeInMillis() < timeStamp.getTime())
            return new SimpleDateFormat("HH:mm:ss", context.getResources().getConfiguration().locale).format(timeStamp);
        else if (day.getTimeInMillis() < timeStamp.getTime())
            return new SimpleDateFormat("HH:mm:ss", context.getResources().getConfiguration().locale).format(timeStamp);
        else return new SimpleDateFormat("dd MM yyyy HH:mm:ss", context.getResources().getConfiguration().locale).format(timeStamp);
    }

    public static SimpleDateFormat getLogDateTimeFormat() {
        if (logDateTimeFormat == null) {
            logDateTimeFormat = new SimpleDateFormat(LOG_DATE_TIME_FORMAT, Locale.ENGLISH);
        }

        return logDateTimeFormat;
    }

//    public static String getLastActivityString(long lastActivityTime) { //TODO REALM UPDATE
//        String result = RosterCacheManager.getInstance().getCachedLastActivityString(lastActivityTime);
//
//        if (result == null || result.isEmpty()) {
//            result = "";
//            if (lastActivityTime > 0) {
//                long timeAgo = System.currentTimeMillis()/1000 - lastActivityTime;
//                long time;
//                String sTime;
//                Date date = new Date(lastActivityTime * 1000);
//                Date today = new Date();
//                Locale locale = Application.getInstance().getResources().getConfiguration().locale;
//
//                if (timeAgo < 60) {
//                    result = Application.getInstance().getString(R.string.last_seen_now);
//
//                } else if (timeAgo < 3600) {
//                    time = TimeUnit.SECONDS.toMinutes(timeAgo);
//                    result = Application.getInstance().getString(R.string.last_seen_minutes, String.valueOf(time));
//
//                } else if (timeAgo < 7200) {
//                    result = Application.getInstance().getString(R.string.last_seen_hours);
//
//                } else if (isToday(date)) {
//                    SimpleDateFormat pattern = new SimpleDateFormat("HH:mm", locale);
//                    sTime = pattern.format(date);
//                    result = Application.getInstance().getString(R.string.last_seen_today, sTime);
//
//                } else if (isYesterday(date)) {
//                    SimpleDateFormat pattern = new SimpleDateFormat("HH:mm", locale);
//                    sTime = pattern.format(date);
//                    result = Application.getInstance().getString(R.string.last_seen_yesterday, sTime);
//
//                } else if (timeAgo < TimeUnit.DAYS.toSeconds(7)) {
//                    SimpleDateFormat pattern = new SimpleDateFormat("HH:mm", locale);
//                    sTime = pattern.format(date);
//                    result = Application.getInstance().getString(R.string.last_seen_on_week,
//                            getDayOfWeek(date, locale), sTime);
//
//                } else if (date.getYear() == today.getYear()) {
//                    SimpleDateFormat pattern = new SimpleDateFormat("d MMMM", locale);
//                    sTime = pattern.format(date);
//                    result = Application.getInstance().getString(R.string.last_seen_date, sTime);
//
//                } else if (date.getYear() < today.getYear()) {
//                    SimpleDateFormat pattern = new SimpleDateFormat("d MMMM yyyy", locale);
//                    sTime = pattern.format(date);
//                    result = Application.getInstance().getString(R.string.last_seen_date, sTime);
//                }
//
//                if (!result.isEmpty())
//                    RosterCacheManager.getInstance().putLastActivityStringToCache(lastActivityTime, result);
//            }
//        }
//
//        return result;
//    }

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

    public static String getDurationStringForVoiceMessage(@Nullable Long current, long duration) {
        StringBuilder sb = new StringBuilder();
        if (current != null) {
            sb.append(transformTimeToFormattedString(current));
            sb.append(" / ");
            sb.append(transformTimeToFormattedString(duration));
        } else {
            sb.append(transformTimeToFormattedString(duration));
        }
        return sb.toString();
    }

    private static String transformTimeToFormattedString(long time) {
        return String.format(Locale.getDefault(), "%01d:%02d",
                TimeUnit.SECONDS.toMinutes(time),
                (TimeUnit.SECONDS.toSeconds(time)) % 60);
    }

    public static String getColoredText(String text, String hexColor) {
        StringBuilder builder = new StringBuilder();
        builder.append("<font color='");
        builder.append(hexColor);
        builder.append("'>");
        builder.append(text);
        builder.append("</font> ");
        return builder.toString();
    }

    public static String getColoredText(String text, int color) {
        String hexColor = String.format("#%06X", 0xFFFFFF & color);
        return getColoredText(text, hexColor);
    }

    /**
     * Beautify XML string
     * @param xmlData
     * @return
     */
    public static String getPrettyXmlString(String xmlData){
        try {
            int xmlMarkupStartsAtIndex = xmlData.indexOf("<");
            String data = xmlData.substring(xmlMarkupStartsAtIndex-1);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);

            Source xmlInput = new StreamSource(new StringReader(data));
            transformer.transform(xmlInput, xmlOutput);

            String result = xmlOutput.getWriter().toString();
            return xmlData.substring(0, xmlMarkupStartsAtIndex) + "\n" + result.substring(0, result.length()-1);
        } catch (Exception e) {
            LogManager.e("StringUtils", e.toString());
            return xmlData;
        }
    }

    /**
     * Convert date time string to Date.
     * (Mostly for XEP-0XXX Reliable Message Delivery)
     * @param string can be three formats:
     *               or yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'
     *               or yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     *               or yyyy-MM-dd'T'HH:mm:ss'Z'
     * @return
     */
    public static Date parseReceivedReceiptTimestampString(String string){
        try {
            SimpleDateFormat simpleDateFormat;
            switch (string.length()){
                case 27: simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
                case 24: simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                case 21: simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                default: simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            }
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return simpleDateFormat.parse(string);
        } catch (Exception e) { LogManager.exception(StringUtils.class.getSimpleName(), e); }
        return null;
    }

    public static boolean isBasicLatin(String string){
        char[] array = string.toCharArray();
        boolean result = true;
        for (int i = 0; i < array.length; i++){
            if (!Character.UnicodeBlock.BASIC_LATIN.equals(Character.UnicodeBlock.of(array[i])))
                result = false;
        }
        return result;
    }

    public static String translitirateToLatin(String string){
        //TODO add detecting language and make this more flexible
        return Transliterator.getInstance("Cyrillic-Latin").transliterate(string);
    }
}
