package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.PrivateMucChatBlockingManager;

public class UnblockAllContactsDialog extends DialogFragment implements DialogInterface.OnClickListener, BlockingManager.BlockContactListener, BlockingManager.UnblockContactListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.UnblockAllContactsDialog.ARGUMENT_ACCOUNT";

    private String account;

    public static UnblockAllContactsDialog newInstance(String account) {
        UnblockAllContactsDialog fragment = new UnblockAllContactsDialog();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getString(ARGUMENT_ACCOUNT, null);

        return new AlertDialog.Builder(getActivity())
                .setMessage(String.format(getActivity().getString(R.string.unblock_all_contacts_confirm),
                        AccountManager.getInstance().getVerboseName(account)))
                .setPositiveButton(R.string.unblock_all, this)
                .setNegativeButton(android.R.string.cancel, this).create();
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            BlockingManager.getInstance().unblockAll(account, this);
            PrivateMucChatBlockingManager.getInstance().unblockAll(account);
        }
    }

    @Override
    public void onSuccess() {
        Toast.makeText(Application.getInstance(),
                Application.getInstance().getString(R.string.contacts_unblocked_successfully),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError() {
        Toast.makeText(Application.getInstance(),
                Application.getInstance().getString(R.string.error_unblocking_contacts),
                Toast.LENGTH_SHORT).show();
    }
}
