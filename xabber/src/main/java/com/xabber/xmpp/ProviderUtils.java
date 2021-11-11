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
package com.xabber.xmpp;

import com.xabber.android.data.log.LogManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Set of functions commonly used by packet providers.
 *
 * @author alexander.ivanov
 */
public class ProviderUtils {

    private ProviderUtils() {
    }

    /**
     * Parse boolean from string.
     *
     * @param value
     * @return <code>null</code> if value is null or invalid.
     */
    public static Boolean parseBoolean(String value) {
        if ("1".equals(value) || "true".equalsIgnoreCase(value))
            return true;
        if ("0".equals(value) || "false".equalsIgnoreCase(value))
            return false;
        return null;
    }

    /**
     * Returns string with text from all inner elements.
     *
     * @param parser
     * @return Empty string if there is no inner text elements.
     * @throws Exception
     */
    public static String parseText(XmlPullParser parser) throws IOException, XmlPullParserException {
        return parseText(parser, -1);
    }

    /**
     * Returns string with text from all inner elements.
     *
     * @param parser
     * @param maximum maximum length of returned value. Use <code>-1</code> to
     *                disable limits.
     * @return Empty string if there is no inner text elements.
     * @throws Exception
     */
    public static String parseText(XmlPullParser parser, int maximum) throws IOException, XmlPullParserException {
        final StringBuilder text = new StringBuilder();
        int inner = 1;
        boolean overflow = false;
        while (inner > 0) {
            int eventType;
            try {
                eventType = parser.next();
            } catch (OutOfMemoryError e) {
                LogManager.exception(parser, new RuntimeException(e));
                overflow = true;
                continue;
            }
            if (eventType == XmlPullParser.TEXT) {
                if (overflow)
                    continue;
                String next = parser.getText();
                if (maximum != -1 && (text.length() + next.length()) > maximum) {
                    overflow = true;
                    continue;
                }
                try {
                    text.append(next);
                } catch (OutOfMemoryError e) {
                    LogManager.exception(parser, new RuntimeException(e));
                    overflow = true;
                }
            } else if (eventType == XmlPullParser.START_TAG) {
                inner += 1;
            } else if (eventType == XmlPullParser.END_TAG) {
                inner -= 1;
            } else if (eventType == XmlPullParser.END_DOCUMENT)
                break;
        }
        if (overflow)
            throw new IOException("Overflow");
        return text.toString();
    }

    /**
     * Skip tag and its descendants.
     *
     * @param parser
     * @throws IllegalStateException If closed tags are incompatible with opened one.
     * @throws Exception
     */
    public static void skipTag(XmlPullParser parser) throws IOException, XmlPullParserException {
        LinkedList<String> tags = new LinkedList<String>();
        tags.add(parser.getName());
        while (!tags.isEmpty()) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                tags.add(parser.getName());
            } else if (eventType == XmlPullParser.END_TAG) {
                if (!tags.removeLast().equals(parser.getName()))
                    throw new IllegalStateException();
            } else if (eventType == XmlPullParser.END_DOCUMENT)
                break;
        }
    }

}
