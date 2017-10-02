package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.bookmarks.BookmarksManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterManager;

public class MUCDeleteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.MUCDeleteDialogFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.MUCDeleteDialogFragment.ARGUMENT_USER";

    private AccountJid account;
    private UserJid user;

    public static MUCDeleteDialogFragment newInstance(AccountJid account, UserJid user) {
        MUCDeleteDialogFragment fragment = new MUCDeleteDialogFragment();

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
                .setMessage(String.format(getActivity().getString(R.string.muc_delete_confirm),
                        RosterManager.getInstance().getName(account, user),
                        AccountManager.getInstance().getVerboseName(account)))
                .setPositiveButton(R.string.muc_delete, this)
                .setNegativeButton(android.R.string.cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != Dialog.BUTTON_POSITIVE) {
            return;
        }

        MUCManager.getInstance().removeRoom(account, user.getJid().asEntityBareJidIfPossible());
        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                BookmarksManager.getInstance().removeConferenceFromBookmarks(account, user.getJid().asEntityBareJidIfPossible());
            }
        });
    }
}
