package com.xabber.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.AccountSettingsActivity;


public class AccountAuthErrorDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = AccountAuthErrorDialogFragment.class.getName() + "ARGUMENT_ACCOUNT";
    private AccountJid account;

    public static DialogFragment newInstance(AccountJid account) {
        AccountAuthErrorDialogFragment fragment = new AccountAuthErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);


        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.AUTHENTICATION_FAILED)
                .setMessage(AccountManager.getInstance().getVerboseName(account))
                .setPositiveButton(R.string.account_connection_settings, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            Activity activity = getActivity();

            if (activity instanceof AccountActivity) {
                activity.finish();
            }

            if (activity instanceof  AccountSettingsActivity) {
                AccountManager.getInstance().removeAuthorizationError(account);
            } else {
                startActivity(AccountActivity.createConnectionSettingsIntent(activity, account));
            }
        }
    }

}
