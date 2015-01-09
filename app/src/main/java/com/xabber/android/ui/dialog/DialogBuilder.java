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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * Builder for auto removed dialog on dismiss.
 * 
 * @author alexander.ivanov
 * 
 */
public class DialogBuilder extends AlertDialog.Builder implements
		DialogInterface.OnDismissListener {

	protected final Activity activity;
	protected final int dialogId;

	/**
	 * @param activity
	 *            Parent activity.
	 * @param dialogId
	 *            Dialog ID to be removed.
	 */
	public DialogBuilder(Activity activity, int dialogId) {
		super(activity);
		this.activity = activity;
		this.dialogId = dialogId;
	}

	@Override
	public AlertDialog create() {
		AlertDialog alertDialog = super.create();
		alertDialog.setOnDismissListener(this);
		return alertDialog;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		activity.removeDialog(dialogId);
	}

	/**
	 * Returns dialog ID.
	 * 
	 * @return
	 */
	public int getDialogId() {
		return dialogId;
	}

}
