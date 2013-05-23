package com.xabber.android.ui.dialog;

import android.app.AlertDialog.Builder;
import android.support.v4.app.DialogFragment;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.androiddev.R;

public class ContactDeleteDialogFragment extends ConfirmDialogFragment {

	private static final String ACCOUNT = "ACCOUNT";
	private static final String USER = "USER";

	/**
	 * @param account
	 *            can be <code>null</code> to be used for all accounts.
	 * @param user
	 * @return
	 */
	public static DialogFragment newInstance(String account, String user) {
		return new ContactDeleteDialogFragment().putAgrument(ACCOUNT, account)
				.putAgrument(USER, user);
	}

	private String user;
	private String account;
	private boolean isRoom;

	@Override
	protected Builder getBuilder() {
		user = getArguments().getString(USER);
		account = getArguments().getString(ACCOUNT);
		isRoom = MUCManager.getInstance().hasRoom(account, user);
		return new Builder(getActivity()).setMessage(getString(
				isRoom ? R.string.muc_delete_confirm
						: R.string.contact_delete_confirm, RosterManager
						.getInstance().getName(account, user), AccountManager
						.getInstance().getVerboseName(account)));
	}

	@Override
	protected boolean onPositiveClick() {
		if (isRoom) {
			MUCManager.getInstance().removeRoom(account, user);
			MessageManager.getInstance().closeChat(account, user);
			NotificationManager.getInstance().removeMessageNotification(
					account, user);
		} else {
			try {
				RosterManager.getInstance().removeContact(account, user);
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
			}
		}
		return true;
	}

}
