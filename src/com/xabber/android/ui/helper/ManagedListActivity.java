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
package com.xabber.android.ui.helper;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;

import com.xabber.android.data.ActivityManager;

/**
 * Base class for all ListActivities.
 * 
 * Adds custom activity logic.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class ManagedListActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ActivityManager.getInstance().onCreate(this);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		ActivityManager.getInstance().onResume(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		ActivityManager.getInstance().onPause(this);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		ActivityManager.getInstance().onDestroy(this);
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		ActivityManager.getInstance().onNewIntent(this, intent);
		super.onNewIntent(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ActivityManager.getInstance().onActivityResult(this, requestCode,
				resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void startActivity(Intent intent) {
		ActivityManager.getInstance().updateIntent(this, intent);
		super.startActivity(intent);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		ActivityManager.getInstance().updateIntent(this, intent);
		super.startActivityForResult(intent, requestCode);
	}

}
