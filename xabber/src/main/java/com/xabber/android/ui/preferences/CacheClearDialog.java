package com.xabber.android.ui.preferences;

import android.app.ProgressDialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;

public class CacheClearDialog extends DialogPreference {
    public CacheClearDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            AccountManager.getInstance().setStatus(StatusMode.unavailable, null);
            Application.getInstance().requestToClear();
            Application.getInstance().requestToClose();

            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage(getContext().getString(R.string.application_state_closing));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }
    }
}
