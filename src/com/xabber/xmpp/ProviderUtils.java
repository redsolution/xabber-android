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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

import com.xabber.android.data.LogManager;

/**
 * Set of functions commonly used by packet providers.
 * 
 * @author alexander.ivanov
 * 
 */
public class ProviderUtils {

	private ProviderUtils() {
	}

	/**
	 * Pattern to remove microseconds.
	 */
	private static final Pattern pattern = Pattern
			.compile("^(\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d{1,3})\\d+(Z)$");

	/**
	 * Date format without milliseconds.
	 */
	private static final DateFormat XEP_0082_UTC_FORMAT_WITHOUT_MILLIS = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

	static {
		XEP_0082_UTC_FORMAT_WITHOUT_MILLIS.setTimeZone(TimeZone
				.getTimeZone("UTC"));
	}

	/**
	 * Parse date time from string.
	 * 
	 * @param dateString
	 * @return <code>null</code> if dateString is null or contains invalid data.
	 */
	public static Date parseDateTime(String dateString) {
		if (dateString == null)
			return null;
		Matcher matcher = pattern.matcher(dateString);
		if (matcher.matches())
			dateString = matcher.group(1) + matcher.group(2);
		try {
			return StringUtils.parseXEP0082Date(dateString);
		} catch (ParseException e) {
			synchronized (XEP_0082_UTC_FORMAT_WITHOUT_MILLIS) {
				try {
					return XEP_0082_UTC_FORMAT_WITHOUT_MILLIS.parse(dateString);
				} catch (ParseException e2) {
					return null;
				}
			}
		}
	}

	/**
	 * Parse integer.
	 * 
	 * @param value
	 * @return <code>null</code> if inner value is <code>null</code> or invalid.
	 */
	public static Integer parseInteger(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
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
	public static String parseText(XmlPullParser parser) throws Exception {
		return parseText(parser, -1);
	}

	/**
	 * Returns string with text from all inner elements.
	 * 
	 * @param parser
	 * @param maximum
	 *            maximum length of returned value. Use <code>-1</code> to
	 *            disable limits.
	 * @return Empty string if there is no inner text elements.
	 * @throws OverflowReceiverBufferException
	 *             If more than maximum chars have been read. Though parser
	 *             position will be at the and of the tag.
	 * @throws Exception
	 */
	public static String parseText(XmlPullParser parser, int maximum)
			throws OverflowReceiverBufferException, Exception {
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
			throw new OverflowReceiverBufferException();
		return text.toString();
	}

	/**
	 * Skip tag and its descendants.
	 * 
	 * @param parser
	 * @throws IllegalStateException
	 *             If closed tags are incompatible with opened one.
	 * @throws Exception
	 */
	public static void skipTag(XmlPullParser parser)
			throws IllegalStateException, Exception {
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

	/**
	 * Parse big decimal.
	 * 
	 * @param parser
	 * @return <code>null</code> if inner text elements contains no or invalid
	 *         data.
	 * @throws Exception
	 */
	public static BigDecimal parseBigDecimal(XmlPullParser parser)
			throws Exception {
		try {
			return new BigDecimal(parseText(parser, -1));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Parse integer.
	 * 
	 * @param parser
	 * @return <code>null</code> if inner text elements contains no or invalid
	 *         data.
	 * @throws Exception
	 */
	public static Integer parseInteger(XmlPullParser parser) throws Exception {
		return parseInteger(parseText(parser, -1));
	}

	/**
	 * Parse boolean.
	 * 
	 * @param parser
	 * @return <code>null</code> if inner text elements contains no or invalid
	 *         data.
	 * @throws Exception
	 */
	public static Integer parseBoolean(XmlPullParser parser) throws Exception {
		return parseInteger(parseText(parser, -1));
	}

	/**
	 * Parse date time.
	 * 
	 * @param parser
	 * @return <code>null</code> if inner text elements contains no or invalid
	 *         data.
	 * @throws Exception
	 */
	public static Date parseDateTime(XmlPullParser parser) throws Exception {
		return parseDateTime(parseText(parser, -1));
	}

}
