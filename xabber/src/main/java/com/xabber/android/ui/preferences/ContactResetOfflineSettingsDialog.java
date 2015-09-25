package com.xabber.android.ui.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.xabber.android.data.roster.GroupManager;

public class ContactResetOfflineSettingsDialog extends DialogPreference {
    public ContactResetOfflineSettingsDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            GroupManager.getInstance().resetShowOfflineModes();
        }
    }
}
