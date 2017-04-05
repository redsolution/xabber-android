package com.xabber.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.AccountChooseAdapter;

import java.util.ArrayList;
import java.util.Collection;

public class AccountChooseDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.AccountChooseDialogFragment.ARGUMENT_USER";
    public static final String ARGUMENT_TEXT = "com.xabber.android.ui.dialog.AccountChooseDialogFragment.ARGUMENT_TEXT";

    UserJid user;
    private String text;
    private Adapter adapter;

    public static DialogFragment newInstance(UserJid user, String text) {
        AccountChooseDialogFragment fragment = new AccountChooseDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_USER, user);
        arguments.putString(ARGUMENT_TEXT, text);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        user = args.getParcelable(ARGUMENT_USER);
        text = args.getString(ARGUMENT_TEXT, null);

        adapter = new Adapter(getActivity());

        return new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(adapter, -1, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        AccountJid account = (AccountJid) adapter.getItem(which);
        OnChooseListener listener = (OnChooseListener) getActivity();
        listener.onChoose(account, user, text);
    }

    private class Adapter extends AccountChooseAdapter {

        public Adapter(Activity activity) {
            super(activity);
            ArrayList<AccountJid> available = new ArrayList<>();
            Collection<AccountJid> enabledAccounts = AccountManager.getInstance().getEnabledAccounts();

            RosterManager rosterManager = RosterManager.getInstance();

            for (AccountJid accountJid : enabledAccounts) {
                RosterContact rosterContact = rosterManager.getRosterContact(accountJid, user);
                if (rosterContact != null && rosterContact.isEnabled()) {
                    available.add(accountJid);
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
        void onChoose(AccountJid account, UserJid user, String text);
    }

}
