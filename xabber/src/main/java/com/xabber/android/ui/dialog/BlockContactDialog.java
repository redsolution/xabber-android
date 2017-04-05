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
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.roster.RosterManager;

public class BlockContactDialog extends DialogFragment implements DialogInterface.OnClickListener, BlockingManager.BlockContactListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ContactBlocker.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ContactBlocker.ARGUMENT_USER";

    private AccountJid account;
    private UserJid user;

    public static BlockContactDialog newInstance(AccountJid account, UserJid user) {
        BlockContactDialog fragment = new BlockContactDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);

        return new AlertDialog.Builder(getActivity())
                .setMessage(String.format(getActivity().getString(R.string.block_contact_confirm),
                        RosterManager.getInstance().getBestContact(account, user).getName(),
                        AccountManager.getInstance().getVerboseName(account)))
                .setPositiveButton(R.string.contact_block, this)
                .setNegativeButton(android.R.string.cancel, this).create();
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            BlockingManager.getInstance().blockContact(account, user, this);
        }
    }

    @Override
    public void onSuccess() {
        Toast.makeText(Application.getInstance(), R.string.contact_blocked_successfully, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError() {
        Toast.makeText(Application.getInstance(), R.string.error_blocking_contact, Toast.LENGTH_SHORT).show();
    }
}
