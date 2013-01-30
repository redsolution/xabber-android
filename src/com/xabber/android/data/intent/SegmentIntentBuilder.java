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
package com.xabber.android.data.intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class SegmentIntentBuilder<T extends SegmentIntentBuilder<?>> extends
		BaseIntentBuilder<T> {

	private final List<String> segments;

	public SegmentIntentBuilder(Context context, Class<?> cls) {
		super(context, cls);
		segments = new ArrayList<String>();
	}

	protected int getSegmentCount() {
		return segments.size();
	}

	@SuppressWarnings("unchecked")
	public T addSegment(String segment) {
		segments.add(segment);
		return (T) this;
	}

	void preBuild() {
	}

	@Override
	public Intent build() {
		preBuild();
		Intent intent = super.build();
		Uri.Builder builder = new Uri.Builder();
		for (String segment : segments)
			builder.appendPath(segment);
		Uri uri = builder.build();
		uri = Uri.parse(uri.toString()); // Workaround for android 1.5
		intent.setData(uri);
		return intent;
	}

	/**
	 * Parse segments from the intent.
	 * 
	 * @param intent
	 * @return
	 */
	static List<String> getSegments(Intent intent) {
		Uri uri = intent.getData();
		if (uri == null) {
			List<String> emptyList = Collections.emptyList();
			return emptyList;
		}
		return uri.getPathSegments();
	}

	/**
	 * @param intent
	 * @param index
	 * @return Segment from the intent data uri or <code>null</code>.
	 */
	public static String getSegment(Intent intent, int index) {
		Uri uri = intent.getData();
		if (uri == null)
			return null;
		List<String> list = uri.getPathSegments();
		if (list.size() <= index)
			return null;
		return list.get(index);
	}

}