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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.xabber.androiddev.R;

public class GroupAddDialogFragment extends ConfirmDialogFragment {

    private static final String GROUPS = "GROUPS";

    /**
     * @param account can be <code>null</code> to be used for all accounts.
     * @param group   can be <code>null</code> to be used for "no group".
     * @return
     */
    public static DialogFragment newInstance(ArrayList<String> groups) {
        return new GroupAddDialogFragment().putAgrument(GROUPS, groups);
    }

    private ArrayList<String> groups;
    private EditText nameView;

    @Override
    protected Builder getBuilder() {
        groups = getArguments().getStringArrayList(GROUPS);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.group_add);
        View layout = getActivity().getLayoutInflater().inflate(
                R.layout.group_name, null);
        nameView = (EditText) layout.findViewById(R.id.group_name);
        builder.setView(layout);
        return builder;
    }

    @Override
    protected boolean onPositiveClick() {
        String group = nameView.getText().toString();
        if ("".equals(group)) {
            Toast.makeText(getActivity(), getString(R.string.group_is_empty),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (groups.contains(group)) {
            Toast.makeText(getActivity(), getString(R.string.group_exists),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        ((OnGroupAddConfirmed) getActivity()).onGroupAddConfirmed(group);
        return true;
    }

    public interface OnGroupAddConfirmed {

        void onGroupAddConfirmed(String group);

    }

}
