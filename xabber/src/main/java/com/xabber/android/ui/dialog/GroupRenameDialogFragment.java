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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.roster.RosterManager;

public class GroupRenameDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.GroupRenameDialogFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_GROUP = "com.xabber.android.ui.dialog.GroupRenameDialogFragment.ARGUMENT_GROUP";

    private AccountJid account;
    private String group;

    private EditText nameView;

    /**
     * @param account can be <code>null</code> to be used for all accounts.
     * @param group   can be <code>null</code> to be used for "no group".
     */
    public static DialogFragment newInstance(AccountJid account, String group) {
        GroupRenameDialogFragment fragment = new GroupRenameDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_GROUP, group);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        group = args.getString(ARGUMENT_GROUP, null);

        View layout = getActivity().getLayoutInflater().inflate(R.layout.group_name, null);
        nameView = (EditText) layout.findViewById(R.id.group_name);
        nameView.setText(group == null ? "" : group);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.group_rename)
                .setView(layout)
                .setPositiveButton(R.string.group_rename, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != Dialog.BUTTON_POSITIVE) {
            return;
        }

        String newName = nameView.getText().toString();
        if ("".equals(newName)) {
            Toast.makeText(getActivity(), getString(R.string.group_is_empty), Toast.LENGTH_LONG).show();
            return;
        }
        if (account == null) {
            RosterManager.getInstance().renameGroup(group, newName);
        } else {
            RosterManager.getInstance().renameGroup(account, group, newName);
        }
    }
}
