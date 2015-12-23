package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;

public class ContactIntegrationDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static DialogFragment newInstance() {
        return new ContactIntegrationDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.contact_integration_suggest)
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        SettingsManager.setContactIntegrationSuggested();
        if (which == Dialog.BUTTON_POSITIVE) {
            for (String account : AccountManager.getInstance().getAllAccounts())
                AccountManager.getInstance().setSyncable(account, true);
        }
    }

}
