package com.xabber.android.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.helper.BatteryHelper;

public class BatteryOptimizationDisableDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public static DialogFragment newInstance() {
        return new BatteryOptimizationDisableDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.battery_optimization_disable_dialog_title)
                .setMessage(R.string.battery_optimization_disable_dialog_message)
                .setPositiveButton(android.R.string.ok, this)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        SettingsManager.setBatteryOptimizationDisableSuggested();

        BatteryHelper.sendIgnoreButteryOptimizationIntent(getActivity());
    }
}