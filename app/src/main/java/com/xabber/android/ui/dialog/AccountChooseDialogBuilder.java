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

import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.AccountChooseAdapter;

public class AccountChooseDialogBuilder extends ListenableDialogBuilder {

	private final String user;
	private String selected;

	public AccountChooseDialogBuilder(Activity activity, int dialogId,
			final ConfirmDialogListener listener, String user) {
		super(activity, dialogId);
		this.user = user;
		this.selected = null;
		setOnCancelListener(listener);
		setOnDeclineListener(listener);
		final Adapter adapter = new Adapter(activity);
		setSingleChoiceItems(adapter, -1, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				selected = (String) adapter.getItem(which);
				dialog.dismiss();
				listener.onAccept(AccountChooseDialogBuilder.this);
			}
		});
	}

	/**
	 * @return <code>null</code> can be returned.
	 */
	public String getSelected() {
		return selected;
	}

	private class Adapter extends AccountChooseAdapter {

		public Adapter(Activity activity) {
			super(activity);
			ArrayList<String> available = new ArrayList<String>();
			for (RosterContact check : RosterManager.getInstance()
					.getContacts())
				if (check.isEnabled() && check.getUser().equals(user))
					available.add(check.getAccount());
			if (!available.isEmpty()) {
				accounts.clear();
				accounts.addAll(available);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getDropDownView(position, convertView, parent);
		}

	}

}
