package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.roster.RosterManager;

public class GroupDeleteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.GroupDeleteDialogFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_GROUP = "com.xabber.android.ui.dialog.GroupDeleteDialogFragment.ARGUMENT_GROUP";

    private AccountJid account;
    private String group;

    /**
     * @param account can be <code>null</code> to be used for all accounts.
     */
    public static DialogFragment newInstance(AccountJid account, String group) {
        GroupDeleteDialogFragment fragment = new GroupDeleteDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_GROUP, group);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        group = args.getString(ARGUMENT_GROUP, null);

        return new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.group_remove_confirm, group))
                .setPositiveButton(R.string.group_remove, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != Dialog.BUTTON_POSITIVE) {
            return;
        }

        try {
            if (account == null)
                RosterManager.getInstance().removeGroup(group);
            else
                RosterManager.getInstance().removeGroup(account, group);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }
}
