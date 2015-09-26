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
import android.app.AlertDialog.Builder;
import android.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.roster.RosterManager;

public class GroupRenameDialogFragment extends ConfirmDialogFragment {

    private static final String ACCOUNT = "ACCOUNT";
    private static final String GROUP = "GROUP";
    private String group;
    private String account;
    private EditText nameView;

    /**
     * @param account can be <code>null</code> to be used for all accounts.
     * @param group   can be <code>null</code> to be used for "no group".
     * @return
     */
    public static DialogFragment newInstance(String account, String group) {
        return new GroupRenameDialogFragment().putAgrument(ACCOUNT, account)
                .putAgrument(GROUP, group);
    }

    @Override
    protected Builder getBuilder() {
        group = getArguments().getString(GROUP);
        account = getArguments().getString(ACCOUNT);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.group_rename);
        View layout = getActivity().getLayoutInflater().inflate(
                R.layout.group_name, null);
        nameView = (EditText) layout.findViewById(R.id.group_name);
        nameView.setText(group == null ? "" : group);
        builder.setView(layout);
        return builder;
    }

    @Override
    protected boolean onPositiveClick() {
        String newName = nameView.getText().toString();
        if ("".equals(newName)) {
            Toast.makeText(getActivity(), getString(R.string.group_is_empty),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        try {
            if (account == null)
                RosterManager.getInstance().renameGroup(group, newName);
            else
                RosterManager.getInstance()
                        .renameGroup(account, group, newName);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
        return true;
    }

}
