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
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.xabber.androiddev.R;

/**
 * Preference to validate xmpp priority input and to show related hint.
 * 
 * @author alexander.ivanov
 * 
 */
public class PriorityPreference extends EditTextPreference {

	private final Context context;

	public PriorityPreference(Context context) {
		super(context);
		this.context = context;
	}

	public PriorityPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public PriorityPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}

	@Override
	protected boolean callChangeListener(Object newValue) {
		try {
			int value = Integer.parseInt((String) newValue);
			if (value < -128 || value > 128)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
			Toast.makeText(context,
					context.getString(R.string.account_invalid_priority),
					Toast.LENGTH_LONG).show();
			return false;
		}
		return super.callChangeListener(newValue);
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		String summary = text;
		try {
			if (Integer.parseInt(text) < 0)
				summary = context.getString(R.string.negative_priotiry_summary,
						text);
		} catch (NumberFormatException e) {
		}
		setSummary(summary);
	}

}
