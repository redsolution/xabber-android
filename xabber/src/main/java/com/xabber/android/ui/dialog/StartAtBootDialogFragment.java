package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;

public class StartAtBootDialogFragment extends DialogFragment implements DialogInterface.OnClickListener  {

    public static DialogFragment newInstance() {
        return new StartAtBootDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.start_at_boot_suggest)
                .setPositiveButton(R.string.start_at_boot, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        SettingsManager.setStartAtBootSuggested();
        if (which == Dialog.BUTTON_POSITIVE) {
            SettingsManager.setConnectionStartAtBoot(true);
        }
    }
}
