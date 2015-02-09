package com.xabber.android.ui.dialog;

import android.app.AlertDialog.Builder;
import android.app.DialogFragment;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.androiddev.R;

public class ContactDeleteDialogFragment extends ConfirmDialogFragment {

    private static final String ACCOUNT = "ACCOUNT";
    private static final String USER = "USER";

    /**
     * @param account
     * @param user
     * @return
     */
    public static DialogFragment newInstance(String account, String user) {
        return new ContactDeleteDialogFragment().putAgrument(ACCOUNT, account)
                .putAgrument(USER, user);
    }

    private String user;
    private String account;

    @Override
    protected Builder getBuilder() {
        user = getArguments().getString(USER);
        account = getArguments().getString(ACCOUNT);
        return new Builder(getActivity()).setMessage(getString(
                R.string.contact_delete_confirm, RosterManager.getInstance()
                        .getName(account, user), AccountManager.getInstance()
                        .getVerboseName(account)));
    }

    @Override
    protected boolean onPositiveClick() {
        try {
            RosterManager.getInstance().removeContact(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
        return true;
    }

}
