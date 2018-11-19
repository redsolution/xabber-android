/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.RosterManager;

import java.io.File;

public class ChatExportDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ChatExportDialogFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ChatExportDialogFragment.ARGUMENT_USER";

    AccountJid account;
    UserJid user;

    private EditText nameView;
    CheckBox sendView;

    public static ChatExportDialogFragment newInstance(AccountJid account, UserJid user) {
        ChatExportDialogFragment fragment = new ChatExportDialogFragment();

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

        View layout = getActivity().getLayoutInflater().inflate(R.layout.export_chat, null);
        nameView = (EditText) layout.findViewById(R.id.name);
        sendView = (CheckBox) layout.findViewById(R.id.send);
        nameView.setText(getString(R.string.export_chat_mask,
                AccountManager.getInstance().getVerboseName(account),
                RosterManager.getInstance().getName(account, user)));

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.export_chat_title)
                .setView(layout)
                .setPositiveButton(R.string.export_chat, this)
                .setNegativeButton(android.R.string.cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != Dialog.BUTTON_POSITIVE) {
            return;
        }

        final String name = nameView.getText().toString();
        if ("".equals(name)) {
            return;
        }

        final boolean send = sendView.isChecked();

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                try {
                    final File file = MessageManager.getInstance().exportChat(account, user, name);

                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: Use notification bar to notify about success.
                            if (send) {
                                Activity activity = getActivity();
                                if (activity != null) {
                                    Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                                    intent.setType("text/plain");
                                    Uri uri = Uri.fromFile(file);
                                    intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.export_chat)));
                                }
                            } else {
                                Toast.makeText(Application.getInstance(), R.string.export_chat_done, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
            }
        });

    }
}
