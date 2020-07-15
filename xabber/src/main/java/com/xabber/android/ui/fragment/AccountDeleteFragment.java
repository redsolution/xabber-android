package com.xabber.android.ui.fragment;


import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.xaccount.XabberAccountManager;

public class AccountDeleteFragment extends Fragment {

    public static final String ACCOUNT = "com.xabber.android.ui.fragment.AccountDeleteFragment.ACCOUNT";
    private AccountJid account;
    private String jid;
    private CheckBox chbDeleteSettings;

    public static Fragment newInstance(AccountJid account) {
        AccountDeleteFragment fragment = new AccountDeleteFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getParcelable(ACCOUNT);
        if (account != null)
            jid = account.getFullJid().asBareJid().toString();

        View view = inflater.inflate(R.layout.fragment_delete_account, null);

        chbDeleteSettings = (CheckBox) view.findViewById(R.id.chbDeleteSettings);
        chbDeleteSettings.setChecked(XabberAccountManager.getInstance().isAccountSynchronize(jid));
        if (XabberAccountManager.getInstance().getAccountSyncState(jid) == null)
            chbDeleteSettings.setVisibility(View.GONE);

        TextView deleteMessage = (TextView) view.findViewById(R.id.accountDeleteMessage);
        StringBuilder deleteMessageText = new StringBuilder();
        deleteMessageText.append(getString(R.string.account_delete_confirmation_question, AccountManager.getInstance().getVerboseName(account)));
        deleteMessageText.append(getString(R.string.account_delete_confirmation_explanation));
        deleteMessage.setText(deleteMessageText);

        return view;
    }

    public void deleteAccount() {
        AlertDialog alertDialog = new AlertDialog.Builder(chbDeleteSettings.getContext()).create();
        alertDialog.setTitle("Delete this account");
        alertDialog.setMessage("Are you sure you want to delete this account?");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Delete", (dialog, which) -> {

            AccountManager.getInstance().removeAccount(account);
            if (chbDeleteSettings != null && chbDeleteSettings.isChecked())
                XabberAccountManager.getInstance().deleteAccountSettings(jid);
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) ->
                dialog.dismiss());
        alertDialog.show();
    }
}