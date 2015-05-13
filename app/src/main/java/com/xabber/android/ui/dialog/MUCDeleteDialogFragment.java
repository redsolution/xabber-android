package com.xabber.android.ui.dialog;

import android.app.AlertDialog.Builder;
import android.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterManager;

public class MUCDeleteDialogFragment extends ConfirmDialogFragment {

    private static final String ACCOUNT = "ACCOUNT";
    private static final String USER = "USER";
    private String user;
    private String account;

    /**
     * @param account
     * @param user
     * @return
     */
    public static DialogFragment newInstance(String account, String user) {
        return new MUCDeleteDialogFragment().putAgrument(ACCOUNT, account)
                .putAgrument(USER, user);
    }

    @Override
    protected Builder getBuilder() {
        user = getArguments().getString(USER);
        account = getArguments().getString(ACCOUNT);
        return new Builder(getActivity()).setMessage(getString(
                R.string.muc_delete_confirm, RosterManager.getInstance()
                        .getName(account, user), AccountManager.getInstance()
                        .getVerboseName(account)));
    }

    @Override
    protected boolean onPositiveClick() {
        MUCManager.getInstance().removeRoom(account, user);
        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account,
                user);
        return true;
    }

}
