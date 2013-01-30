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
package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

public class AboutViewer extends ManagedActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_viewer);
		((TextView) findViewById(R.id.about_version))
				.setText(getString(R.string.about_version,
						getString(R.string.application_version)));
		((TextView) findViewById(R.id.about_license))
				.setMovementMethod(LinkMovementMethod.getInstance());
	}

	public static Intent createIntent(Context context) {
		return new Intent(context, AboutViewer.class);
	}

}
