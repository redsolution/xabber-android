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
package com.xabber.xmpp.uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.net.Uri;
import android.text.Spannable;
import android.text.util.Linkify;
import android.util.Patterns;

/**
 * Helper class to parse xmpp uri.
 * 
 * http://xmpp.org/extensions/xep-0147.html
 * 
 * @author alexander.ivanov
 * 
 */
public class XMPPUri {

	private static final String XMPP_SCHEME = "xmpp";

	private static final Pattern XMPP_PATTERN = Pattern
			.compile("xmpp\\:(?:(?:["
					+ Patterns.GOOD_IRI_CHAR
					+ "\\;\\/\\?\\@\\&\\=\\#\\~\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])"
					+ "|(?:\\%[a-fA-F0-9]{2}))+");

	private final String authority;
	private final String path;
	private final String queryType;
	private final HashMap<String, ArrayList<String>> values;

	public static XMPPUri parse(Uri uri) throws IllegalArgumentException {
		return new XMPPUri(uri);
	}

	private XMPPUri(Uri uri) throws IllegalArgumentException {
		if (uri == null)
			throw new IllegalArgumentException();
		if (!XMPP_SCHEME.equals(uri.getScheme()))
			throw new IllegalArgumentException();
		// Fix processing path without leading slash
		uri = Uri.parse(uri.getEncodedSchemeSpecificPart());
		authority = uri.getAuthority();
		if (uri.getPath() == null)
			throw new IllegalArgumentException();
		if (uri.getPath().startsWith("/"))
			path = uri.getPath().substring(1);
		else
			path = uri.getPath();
		values = new HashMap<String, ArrayList<String>>();
		String query = uri.getEncodedQuery();
		String action = null;
		if (query != null) {
			String parts[] = query.split(";");
			for (String part : parts)
				if (action == null) {
					if (part.contains("="))
						throw new IllegalArgumentException();
					action = part;
				} else {
					int index = part.indexOf("=");
					if (index == -1)
						continue;
					String key = part.substring(0, index);
					String value = part.substring(index + 1);
					ArrayList<String> list = values.get(key);
					if (list == null) {
						list = new ArrayList<String>();
						values.put(key, list);
					}
					list.add(Uri.decode(value));
				}
		}
		queryType = action;
	}

	public String getAuthority() {
		return authority;
	}

	public String getPath() {
		return path;
	}

	public String getQueryType() {
		return queryType;
	}

	public ArrayList<String> getValues(String queryKey) {
		return values.get(queryKey);
	}

	@Override
	public String toString() {
		return path + " : " + queryType + " : " + values;
	}

	/**
	 * Update spannable with XMPP URI links.
	 * 
	 * @param spannable
	 * @return Where spannable was modified.
	 */
	public static boolean addLinks(Spannable spannable) {
		return Linkify.addLinks(spannable, XMPP_PATTERN, "xmpp");
	}

}
