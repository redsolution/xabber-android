package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class ConfirmDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_MESSAGE = "com.xabber.android.ui.dialog.ConfirmDialog.ARGUMENT_MESSAGE";

    public static DialogFragment newInstance(String message) {
        ConfirmDialog fragment = new ConfirmDialog();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_MESSAGE, message);
        fragment.setArguments(arguments);
        return fragment;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String message = args.getString(ARGUMENT_MESSAGE, null);

        return new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            ((Listener)getActivity()).onConfirm();
        }
    }

    public interface Listener {
        void onConfirm();
    }
}
