package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;


public class AccountDeleteDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.AccountDeleteDialog.ARGUMENT_ACCOUNT";

    private AccountJid account;

    public static DialogFragment newInstance(AccountJid account) {
        AccountDeleteDialog fragment = new AccountDeleteDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);

        return new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.account_delete_confirm,
                        AccountManager.getInstance().getVerboseName(account)))
                .setPositiveButton(R.string.account_delete, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != Dialog.BUTTON_POSITIVE) {
            return;
        }

        AccountManager.getInstance().removeAccount(account);
    }
}
