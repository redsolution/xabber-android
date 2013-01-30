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
package com.xabber.android.ui.widget;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

/**
 * Ringtone preference that store and retrieve its data from internal property.
 * 
 * @author alexander.ivanov
 * 
 */
public class RingtonePreference extends android.preference.RingtonePreference {

	private Uri uri;

	public RingtonePreference(Context context) {
		super(context);
	}

	public RingtonePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RingtonePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected Uri onRestoreRingtone() {
		return uri;
	}

	@Override
	protected void onSaveRingtone(Uri ringtoneUri) {
		uri = ringtoneUri;
	}

	public Uri getUri() {
		return uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}

}
