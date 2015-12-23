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
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.RosterManager;

import java.io.File;

public class ChatExportDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ChatExportDialogFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ChatExportDialogFragment.ARGUMENT_USER";

    private String account;
    private String user;

    private EditText nameView;
    private CheckBox sendView;

    public static ChatExportDialogFragment newInstance(String account, String user) {
        ChatExportDialogFragment fragment = new ChatExportDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getString(ARGUMENT_ACCOUNT, null);
        user = args.getString(ARGUMENT_USER, null);

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

        String name = nameView.getText().toString();
        if ("".equals(name)) {
            return;
        }
        new ChatExportAsyncTask(account, user, name, sendView.isChecked()).execute();
    }

    // TODO get rid of async task
    private class ChatExportAsyncTask extends AsyncTask<Void, Void, File> {

        private final String account;
        private final String user;
        private final String name;
        private final boolean send;

        public ChatExportAsyncTask(String account, String user, String name, boolean send) {
            this.account = account;
            this.user = user;
            this.name = name;
            this.send = send;
        }

        @Override
        protected File doInBackground(Void... params) {
            try {
                return MessageManager.getInstance().exportChat(account, user, name);
            } catch (NetworkException e) {
                Application.getInstance().onError(e);
                return null;
            }
        }

        @Override
        public void onPostExecute(File result) {
            Activity activity = getActivity();

            if (result == null || activity == null) {
                return;
            }

            // TODO: Use notification bar to notify about success.
            if (send) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                Uri uri = Uri.fromFile(result);
                intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.export_chat)));
            } else {
                Toast.makeText(activity, R.string.export_chat_done, Toast.LENGTH_LONG).show();
            }
        }

    }
}
