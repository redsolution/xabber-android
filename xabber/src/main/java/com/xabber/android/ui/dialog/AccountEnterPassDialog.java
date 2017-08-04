package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountManager;

/**
 * Created by valery.miller on 04.08.17.
 */

public class AccountEnterPassDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARGUMENT_ERROR_EVENT = AccountErrorDialogFragment.class.getName() + "ARGUMENT_ERROR_EVENT";
    private AccountErrorEvent accountErrorEvent;
    private EditText edtPass;

    public static DialogFragment newInstance(AccountErrorEvent accountErrorEvent) {
        AccountEnterPassDialog fragment = new AccountEnterPassDialog();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARGUMENT_ERROR_EVENT, accountErrorEvent);

        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        accountErrorEvent = (AccountErrorEvent) args.getSerializable(ARGUMENT_ERROR_EVENT);

        return new AlertDialog.Builder(getActivity())
                .setTitle(AccountManager.getInstance().getVerboseName(accountErrorEvent.getAccount()))
                .setMessage(R.string.enter_password)
                .setView(setUpDialogView())
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    @NonNull
    private View setUpDialogView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_account_enter_pass, null);
        edtPass = (EditText) view.findViewById(R.id.edtPass);
        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            if (edtPass != null) {
                String password = edtPass.getText().toString();
                AccountManager.getInstance().updateAccountPassword(accountErrorEvent.getAccount(), password);
            }
        }
        if (which == Dialog.BUTTON_NEGATIVE) {
            dialog.dismiss();
        }
    }
}
