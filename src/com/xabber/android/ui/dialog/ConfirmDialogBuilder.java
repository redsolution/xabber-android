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

/**
 * Yes / No dialog builder.
 * 
 * @author alexander.ivanov
 * 
 */
public class ConfirmDialogBuilder extends ListenableDialogBuilder {

	/**
	 * Yes / No dialog builder.
	 * 
	 * @param activity
	 *            parent activity.
	 * @param dialogId
	 *            ID of the dialog.
	 * @param listener
	 *            listener of actions.
	 */
	public ConfirmDialogBuilder(Activity activity, int dialogId,
			ConfirmDialogListener listener) {
		super(activity, dialogId);
		setOnCancelListener(listener);
		setOnDeclineListener(listener);
		setOnAcceptListener(listener);
	}

}
