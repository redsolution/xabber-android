package com.xabber.android.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.xaccount.XabberAccountManager;

public class AccountColorDialog extends DialogFragment {
    private static final String ARGUMENT_ACCOUNT = AccountColorDialog.class.getName();

    AccountJid accountJid;

    public static DialogFragment newInstance(AccountJid account) {
        AccountColorDialog fragment = new AccountColorDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        accountJid = args.getParcelable(ARGUMENT_ACCOUNT);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(getString(R.string.account_color));
        dialog.setPositiveButton(null, null);
        dialog.setNegativeButton(android.R.string.cancel, null);
        int colorIndex = AccountManager.getInstance().getColorLevel(accountJid);

        dialog.setSingleChoiceItems(R.array.account_color_names, colorIndex, selectItemListener);
        return dialog.create();
    }

    DialogInterface.OnClickListener selectItemListener = new DialogInterface.OnClickListener() {

        @Override public void onClick(DialogInterface dialog, int which) {
            AccountManager.getInstance().setColor(accountJid, which);
            AccountManager.getInstance().setTimestamp(accountJid, XabberAccountManager.getInstance().getCurrentTime());
            AccountManager.getInstance().onAccountChanged(accountJid);

            if (XabberAccountManager.getInstance().getAccount() != null)
                XabberAccountManager.getInstance().updateAccountSettings();
            dialog.dismiss();
        }
    };
}
