package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.activity.ContactActivity;

public class ContactDeleteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ContactDeleteDialogFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ContactDeleteDialogFragment.ARGUMENT_USER";

    private UserJid user;
    private AccountJid account;

    public static ContactDeleteDialogFragment newInstance(AccountJid account, UserJid user) {
        ContactDeleteDialogFragment fragment = new ContactDeleteDialogFragment();

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
                .setMessage(String.format(getActivity().getString(R.string.contact_delete_confirm),
                        RosterManager.getInstance().getName(account, user),
                        AccountManager.getInstance().getVerboseName(account)))
                .setPositiveButton(R.string.contact_delete, this)
                .setNegativeButton(android.R.string.cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            MessageManager.getInstance().closeChat(account, user);

            try {
                // discard subscription
                PresenceManager.getInstance().discardSubscription(account, user);
            } catch (NetworkException e) {
                Application.getInstance().onError(R.string.CONNECTION_FAILED);
            }

            // delete chat
            AbstractChat chat = MessageManager.getInstance().getChat(account, user);
            if (chat != null)
                MessageManager.getInstance().removeChat(chat);

            // remove roster contact
            RosterManager.getInstance().removeContact(account, user);

            if (getActivity() instanceof ContactActivity) {
                startActivity(ContactListActivity.createIntent(getActivity()));
            }
        }
    }
}
