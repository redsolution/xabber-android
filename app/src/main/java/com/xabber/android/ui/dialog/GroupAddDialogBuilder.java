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
package com.xabber.android.ui.dialog;

import java.util.Collection;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.xabber.androiddev.R;

public class GroupAddDialogBuilder extends ConfirmDialogBuilder {

	private final Collection<String> groups;
	private final EditText nameView;

	public GroupAddDialogBuilder(Activity activity, int dialogId,
			ConfirmDialogListener listener, Collection<String> groups) {
		super(activity, dialogId, listener);
		setTitle(R.string.group_add);
		this.groups = groups;
		View layout = activity.getLayoutInflater().inflate(R.layout.group_name,
				null);
		nameView = (EditText) layout.findViewById(R.id.group_name);
		setView(layout);
	}

	@Override
	public void onAccept(DialogInterface dialog) {
		String name = nameView.getText().toString();
		if ("".equals(name)) {
			Toast.makeText(activity,
					activity.getString(R.string.group_is_empty),
					Toast.LENGTH_LONG).show();
			return;
		}
		if (groups.contains(name)) {
			Toast.makeText(activity, activity.getString(R.string.group_exists),
					Toast.LENGTH_LONG).show();
			return;
		}
		super.onAccept(dialog);
	}

	public String getName() {
		return nameView.getText().toString();
	}

}
