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
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.xabber.android.data.account.StatusMode;
import com.xabber.androiddev.R;

/**
 * Preference to show status mode icon.
 * 
 * @author alexander.ivanov
 * 
 */
public class StatusPreference extends Preference {

	private StatusMode statusMode;

	public StatusPreference(Context context) {
		super(context);
		init();
	}

	public StatusPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public StatusPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setWidgetLayoutResource(R.layout.preference_status_widget);
	}

	public void setStatusMode(StatusMode statusMode) {
		this.statusMode = statusMode;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		((ImageView) view.findViewById(R.id.status_mode))
				.setImageLevel(statusMode.getStatusLevel());
	}

}
