package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.preferences.DebugSettings;


public class CrashReportDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public static DialogFragment newInstance() {
        return new CrashReportDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.debug_crash_reports_dialog_title)
                .setMessage(R.string.debug_crash_reports_dialog_message)
                .setPositiveButton(R.string.debug_crash_reports_dialog_settings_button, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        SettingsManager.setCrashReportsDialogShown();

        if (which == Dialog.BUTTON_POSITIVE) {
            startActivity(new Intent(getActivity(), DebugSettings.class));
        }
    }
}
