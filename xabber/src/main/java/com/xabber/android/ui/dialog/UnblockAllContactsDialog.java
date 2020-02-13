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
import com.xabber.android.ui.activity.BlockedListActivity;

import java.util.ArrayList;

public class UnblockAllContactsDialog extends DialogFragment implements DialogInterface.OnClickListener, BlockingManager.UnblockContactListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.UnblockAllContactsDialog.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_BLOCKED_CONTACT_LIST = "com.xabber.android.ui.dialog.UnblockAllContactsDialog.ARGUMENT_BLOCKED_CONTACT_LIST";

    private AccountJid account;
    private ArrayList<UserJid> blockedContacts;

    public static UnblockAllContactsDialog newInstance(AccountJid account) {
        UnblockAllContactsDialog fragment = new UnblockAllContactsDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static UnblockAllContactsDialog newInstance(AccountJid account, ArrayList<UserJid> contacts) {
        UnblockAllContactsDialog fragment = new UnblockAllContactsDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelableArrayList(ARGUMENT_BLOCKED_CONTACT_LIST, contacts);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        blockedContacts = args.getParcelableArrayList(ARGUMENT_BLOCKED_CONTACT_LIST);

        return new AlertDialog.Builder(getActivity())
                .setMessage(String.format(getActivity().getString(R.string.unblock_all_contacts_confirm),
                        AccountManager.getInstance().getVerboseName(account)))
                .setPositiveButton(R.string.unblock_all, this)
                .setNegativeButton(android.R.string.cancel, this).create();
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            if (blockedContacts != null) {
                BlockingManager.getInstance().unblockContacts(account, blockedContacts,this);
            } else {
                BlockingManager.getInstance().unblockAll(account, this);
            }
        }
    }

    @Override
    public void onSuccessUnblock() {
        if (getActivity() instanceof BlockedListActivity) {
            ((BlockedListActivity) getActivity()).onSuccessUnblock();
        } else {
            Toast.makeText(Application.getInstance(),
                    Application.getInstance().getString(R.string.contacts_unblocked_successfully),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onErrorUnblock() {
        Toast.makeText(Application.getInstance(),
                Application.getInstance().getString(R.string.error_unblocking_contacts),
                Toast.LENGTH_SHORT).show();
    }
}
