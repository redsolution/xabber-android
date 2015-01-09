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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/**
 * Dialog builder that provides listeners for dialog to be accepted, declined
 * and canceled.
 * 
 * @author alexander.ivanov
 * 
 */
public class ListenableDialogBuilder extends DialogBuilder {

	private OnCancelListener onCancelListener;
	private OnDeclineListener onDeclineListener;
	private OnAcceptListener onAcceptListener;

	public ListenableDialogBuilder(Activity activity, int dialogId) {
		super(activity, dialogId);
	}

	/**
	 * Sets listener for dialog to be canceled.
	 * 
	 * @param listener
	 * @return
	 */
	public ListenableDialogBuilder setOnCancelListener(OnCancelListener listener) {
		onCancelListener = listener;
		setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				ListenableDialogBuilder.this.onCancel(dialog);
			}
		});
		return this;
	}

	protected void onCancel(DialogInterface dialog) {
		dialog.dismiss();
		onCancelListener.onCancel(this);
	}

	/**
	 * @return Title for the negative button.
	 */
	protected String getNegativeTitle() {
		return activity.getString(android.R.string.no);
	}

	/**
	 * Sets listener for dialog to be declined by pushing on negative button.
	 * 
	 * @param listener
	 * @return
	 */
	public ListenableDialogBuilder setOnDeclineListener(
			OnDeclineListener listener) {
		onDeclineListener = listener;
		setNegativeButton(getNegativeTitle(), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ListenableDialogBuilder.this.onDecline(dialog);
			}
		});
		return this;
	}

	protected void onDecline(DialogInterface dialog) {
		dialog.dismiss();
		onDeclineListener.onDecline(this);
	}

	/**
	 * @return Title for the positive button.
	 */
	protected String getPositiveTitle() {
		return activity.getString(android.R.string.yes);
	}

	/**
	 * Sets listener for dialog to be accepted by pushing on positive button.
	 * 
	 * @param listener
	 * @return
	 */
	public ListenableDialogBuilder setOnAcceptListener(OnAcceptListener listener) {
		onAcceptListener = listener;
		setPositiveButton(getPositiveTitle(), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ListenableDialogBuilder.this.onAccept(dialog);
			}
		});
		return this;
	}

	public void onAccept(DialogInterface dialog) {
		dialog.dismiss();
		onAcceptListener.onAccept(this);
	}

}
