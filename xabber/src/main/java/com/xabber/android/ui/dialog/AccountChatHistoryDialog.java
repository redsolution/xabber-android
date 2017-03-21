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

public class AccountChatHistoryDialog extends DialogFragment {
    private static final String ARGUMENT_ACCOUNT = AccountColorDialog.class.getName();

    AccountJid accountJid;

    public static DialogFragment newInstance(AccountJid account) {
        AccountChatHistoryDialog fragment = new AccountChatHistoryDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        accountJid = args.getParcelable(ARGUMENT_ACCOUNT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(getString(R.string.account_chat_history));
        dialog.setPositiveButton(null, null);
        dialog.setNegativeButton(android.R.string.cancel, null);

        dialog.setSingleChoiceItems(R.array.account_chat_history, 0, selectItemListener);
        return dialog.create();
    }

    DialogInterface.OnClickListener selectItemListener = new DialogInterface.OnClickListener() {

        @Override public void onClick(DialogInterface dialog, int which) {
            AccountManager.getInstance().onAccountChanged(accountJid);
            dialog.dismiss();
        }
    };
}
