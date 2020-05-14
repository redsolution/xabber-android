package com.xabber.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.QuoteSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.Surface;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.push.SyncManager;
import com.xabber.android.service.XabberService;
import com.xabber.android.ui.text.CustomQuoteSpan;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Utils {

    public static int dipToPx(float dip, Context context) {
        return (int) dipToPxFloat(dip, context);
    }

    public static float dipToPxFloat(float dip, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dip, context.getResources().getDisplayMetrics());
    }

    public static int spToPx(float sp, Context context) {
        return (int) spToPxFloat(sp, context);
    }

    public static float spToPxFloat(float sp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                sp, context.getResources().getDisplayMetrics());
    }

    public static int longToInt(long number) {
        if (number > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else if (number < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        else return (int) number;
    }

    public static boolean isSameDay(Long date1, Long date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(new Date(date1));
        cal2.setTime(new Date(date2));
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    public static void startXabberServiceCompat(Context context) {
        startXabberServiceCompat(context, XabberService.createIntent(context));
    }

    public static void startXabberServiceCompatWithSyncMode(Context context, String pushNode) {
        startXabberServiceCompat(context,
                SyncManager.createXabberServiceIntentWithSyncMode(context, pushNode));
    }

    public static void startXabberServiceCompatWithSyncMode(Context context, AccountJid accountJid) {
        startXabberServiceCompat(context,
                SyncManager.createXabberServiceIntentWithSyncMode(context, accountJid));
    }

    private static void startXabberServiceCompat(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static String xmlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;"); //$NON-NLS-1$
                    break;
                case '>':
                    sb.append("&gt;"); //$NON-NLS-1$
                    break;
                case '&':
                    sb.append("&amp;"); //$NON-NLS-1$
                    break;
                case '\'':
                    // In this implementation we use &apos; instead of &#39; because we encode XML, not HTML.
                    sb.append("&apos;"); //$NON-NLS-1$
                    break;
                case '"':
                    sb.append("&quot;"); //$NON-NLS-1$
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static Spannable getDecodedSpannable(String text) {
        Editable.Factory factory = Editable.Factory.getInstance();
        SpannableStringBuilder originalSpannable = (SpannableStringBuilder) factory.newEditable(text);
        if (Linkify.addLinks(originalSpannable, Linkify.WEB_URLS)) {
            // get all url spans if addLinks() returned true, meaning that it found web urls
            URLSpan[] originalURLSpans = originalSpannable.getSpans(0, originalSpannable.length(), URLSpan.class);
            ArrayList<URLSpanContainer> urlSpanContainers = new ArrayList<URLSpanContainer>(originalURLSpans.length);
            for (URLSpan originalUrl : originalURLSpans) {
                // save the original url span data
                urlSpanContainers.add(new URLSpanContainer(originalUrl, originalSpannable.getSpanStart(originalUrl), originalSpannable.getSpanEnd(originalUrl)));
                // remove original url span from spannable
                originalSpannable.removeSpan(originalUrl);
            }
            // iterate over each available url span from last to first, to properly
            // manage start positions in cases when the size of the text changes on decoding
            for (int i = urlSpanContainers.size() - 1; i >= 0; i--) {
                URLSpanContainer spanContainer = urlSpanContainers.get(i);
                try {
                    String originalURL = spanContainer.span.getURL();
                    String decodedURL = URLDecoder.decode(originalURL, StandardCharsets.UTF_8.name());
                    URLSpan decodedSpan = new URLSpan(decodedURL);
                    if (decodedURL.length() < originalURL.length()) {
                        // replace the text with the decoded url
                        originalSpannable.replace(spanContainer.start,
                                spanContainer.end,
                                decodedURL,
                                0,
                                decodedURL.length());
                        // set a new span range
                        originalSpannable.setSpan(decodedSpan, spanContainer.start, decodedSpan.getURL().length() + spanContainer.start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        // restore the old span range
                        originalSpannable.setSpan(spanContainer.span, spanContainer.start, spanContainer.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } catch (UnsupportedEncodingException e) {
                    originalSpannable.setSpan(spanContainer.span, spanContainer.start, spanContainer.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    e.printStackTrace();
                }
            }
            return originalSpannable;
        } else {
            return originalSpannable;
        }
    }

    public static void modifySpannableWithCustomQuotes(SpannableStringBuilder spannable, DisplayMetrics displayMetrics, int color) {
        QuoteSpan[] quoteSpans = spannable.getSpans(0, spannable.length(), QuoteSpan.class);
        if (quoteSpans.length > 0) {
            for (int i = quoteSpans.length - 1; i >= 0; i--) {
                QuoteSpan span = quoteSpans[i];
                int spanEnd = spannable.getSpanEnd(span);
                int spanStart = spannable.getSpanStart(span);
                spannable.removeSpan(span);
                if (spanEnd < 0 || spanStart < 0) break;

                int newlineCount = 0;
                if ('\n' == spannable.charAt(spanEnd)) {
                    newlineCount++;
                    if (spanEnd + 1 < spannable.length() && '\n' == spannable.charAt(spanEnd + 1)) {
                        newlineCount++;
                    }
                    if ('\n' == spannable.charAt(spanEnd - 1)) {
                        newlineCount++;
                    }
                }
                switch (newlineCount) {
                    case 3:
                        spannable.delete(spanEnd - 1, spanEnd + 1);
                        spanEnd = spanEnd - 2;
                        break;
                    case 2:
                        spannable.delete(spanEnd, spanEnd + 1);
                        spanEnd--;
                }

                if (spanStart > 1 && '\n' == spannable.charAt(spanStart - 1)) {
                    if ('\n' == spannable.charAt(spanStart - 2)) {
                        spannable.delete(spanStart - 2, spanStart - 1);
                        spanStart--;
                    }
                }

                spannable.setSpan(new CustomQuoteSpan(color, displayMetrics), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                char current;
                boolean waitForNewLine = false;
                for (int j = spanStart; j < spanEnd; j++) {
                    if (j >= spannable.length()) break;
                    current = spannable.charAt(j);

                    if (waitForNewLine && current != '\n') continue;
                    else waitForNewLine = false;

                    if (current == '>') {
                        spannable.delete(j, j + 1);
                        j--;
                        waitForNewLine = true;
                    }
                }
            }
        }
    }

    public static void lockScreenRotation(Activity activity, boolean lockOrientation) {
        int lock = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (lockOrientation) {
            Display display = activity.getWindowManager().getDefaultDisplay();
            int rotation = display.getRotation();

            Point size = new Point();
            display.getSize(size);

            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                if (size.x > size.y) {
                    //rotation is 0 or 180 deg, and the size of x is greater than y,
                    //so we have a tablet
                    if (rotation == Surface.ROTATION_0) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    }
                } else {
                    //rotation is 0 or 180 deg, and the size of y is greater than x,
                    //so we have a phone
                    if (rotation == Surface.ROTATION_0) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    }
                }
            } else {
                if (size.x > size.y) {
                    //rotation is 90 or 270, and the size of x is greater than y,
                    //so we have a phone
                    if (rotation == Surface.ROTATION_90) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    }
                } else {
                    //rotation is 90 or 270, and the size of y is greater than x,
                    //so we have a tablet
                    if (rotation == Surface.ROTATION_90) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    }
                }
            }
        }
        activity.setRequestedOrientation(lock);
    }

    /**
     * Haptic feedback helper methods.
     *
     * */
    public static void performHapticFeedback(View view) {
        performHapticFeedback(view, HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public static void performHapticFeedback(View view, int feedbackType) {
        performHapticFeedback(view, feedbackType, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    public static void performHapticFeedback(View view, int feedbackType, int flag) {
        if (view != null)
            view.performHapticFeedback(feedbackType, flag);
    }

    @ColorInt
    public static int getAttrColor(Context context, int attrId) {
        if (context == null) {
            return 0;
        }
        TypedValue value = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attrId, value, true);
        return value.data;
    }
}

class URLSpanContainer {
    public URLSpanContainer(URLSpan span, int start, int end) {
        this.span = span;
        this.start = start;
        this.end = end;
    }
    int start;
    int end;
    URLSpan span;
}
