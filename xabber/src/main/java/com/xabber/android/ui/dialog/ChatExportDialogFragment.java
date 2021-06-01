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
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;

import java.io.File;

public class ChatExportDialogFragment extends DialogFragment implements View.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ChatExportDialogFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ChatExportDialogFragment.ARGUMENT_USER";

    AccountJid account;
    ContactJid user;

    private EditText nameView;
    CheckBox sendView;

    public static ChatExportDialogFragment newInstance(AccountJid account, ContactJid user) {
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
        int colorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account);


        View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_export_chat, null);
        nameView = (EditText) layout.findViewById(R.id.name);
        sendView = (CheckBox) layout.findViewById(R.id.send);
        nameView.setText(getString(R.string.export_chat_mask,
                AccountManager.getInstance().getVerboseName(account),
                RosterManager.getInstance().getName(account, user)));

        ((Button) layout.findViewById(R.id.export)).setTextColor(colorIndicator);

        layout.findViewById(R.id.cancel_export).setOnClickListener(this);
        layout.findViewById(R.id.export).setOnClickListener(this);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.export_chat_title)
                .setView(layout).create();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.cancel_export) {
            dismiss();
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
                    final File file = ChatManager.getInstance().exportChat(account, user, name);

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
