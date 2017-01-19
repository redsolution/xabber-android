package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.activity.ContactListActivity;

public class DarkThemeIntroduceDialog extends DialogFragment implements DialogInterface.OnClickListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.dark_theme_suggest_dialog_title)
        .setMessage(R.string.dark_theme_suggest_dialog_message)
        .setPositiveButton(R.string.dark_theme_suggest_dialog_join_dark_side, this)
        .setNegativeButton(R.string.dark_theme_suggest_dialog_stay_on_light_side, this)
        .setCancelable(false)
        .create();
        }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        SettingsManager.setDarkThemeSuggested();

        if (which == Dialog.BUTTON_POSITIVE) {
            joinDarkSide();
        }
    }

    private void joinDarkSide() {
        SettingsManager.setDarkTheme();
        ActivityManager.getInstance().clearStack(true);
        startActivity(ContactListActivity.createIntent(getActivity()));
    }
}