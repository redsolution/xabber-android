package com.xabber.android.ui.dialog;

import android.app.AlertDialog.Builder;
import android.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.roster.RosterManager;

public class GroupDeleteDialogFragment extends ConfirmDialogFragment {

    private static final String ACCOUNT = "ACCOUNT";
    private static final String GROUP = "GROUP";
    private String group;
    private String account;

    /**
     * @param account can be <code>null</code> to be used for all accounts.
     * @param group
     * @return
     */
    public static DialogFragment newInstance(String account, String group) {
        return new GroupDeleteDialogFragment().putAgrument(ACCOUNT, account)
                .putAgrument(GROUP, group);
    }

    @Override
    protected Builder getBuilder() {
        group = getArguments().getString(GROUP);
        account = getArguments().getString(ACCOUNT);
        return new Builder(getActivity()).setMessage(getString(
                R.string.group_remove_confirm, group));
    }

    @Override
    protected boolean onPositiveClick() {
        try {
            if (account == null)
                RosterManager.getInstance().removeGroup(group);
            else
                RosterManager.getInstance().removeGroup(account, group);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
        return true;
    }

}
