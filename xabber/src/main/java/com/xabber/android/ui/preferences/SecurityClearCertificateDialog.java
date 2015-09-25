package com.xabber.android.ui.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.connection.ConnectionManager;


public class SecurityClearCertificateDialog extends DialogPreference {
    public SecurityClearCertificateDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            CertificateManager.getInstance().removeCertificates();
            ConnectionManager.getInstance().updateConnections(true);
        }
    }
}
