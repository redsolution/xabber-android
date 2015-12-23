package com.xabber.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.AccountChooseAdapter;

import java.util.ArrayList;

public class AccountChooseDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.AccountChooseDialogFragment.ARGUMENT_USER";
    public static final String ARGUMENT_TEXT = "com.xabber.android.ui.dialog.AccountChooseDialogFragment.ARGUMENT_TEXT";

    private String user;
    private String text;
    private Adapter adapter;

    public static DialogFragment newInstance(String user, String text) {
        AccountChooseDialogFragment fragment = new AccountChooseDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_USER, user);
        arguments.putString(ARGUMENT_TEXT, text);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        user = args.getString(ARGUMENT_USER, null);
        text = args.getString(ARGUMENT_TEXT, null);

        adapter = new Adapter(getActivity());

        return new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(adapter, -1, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String account = (String) adapter.getItem(which);
        OnChooseListener listener = (OnChooseListener) getActivity();
        listener.onChoose(account, user, text);
    }

    private class Adapter extends AccountChooseAdapter {

        public Adapter(Activity activity) {
            super(activity);
            ArrayList<String> available = new ArrayList<>();
            for (RosterContact check : RosterManager.getInstance().getContacts()) {
                if (check.isEnabled() && check.getUser().equals(user)) {
                    available.add(check.getAccount());
                }
            }
            if (!available.isEmpty()) {
                accounts.clear();
                accounts.addAll(available);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }

    }

    public interface OnChooseListener {
        void onChoose(String account, String user, String text);
    }

}
