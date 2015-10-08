package com.xabber.android.ui.dialog;

import android.app.AlertDialog.Builder;
import android.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;

public class ContactIntegrationDialogFragment extends ConfirmDialogFragment {

    public static DialogFragment newInstance() {
        return new ContactIntegrationDialogFragment();
    }

    @Override
    protected Builder getBuilder() {
        return new Builder(getActivity())
                .setMessage(R.string.contact_integration_suggest);
    }

    @Override
    protected boolean onPositiveClick() {
        SettingsManager.setContactIntegrationSuggested();
        for (String account : AccountManager.getInstance().getAllAccounts())
            AccountManager.getInstance().setSyncable(account, true);
        return true;
    }

    @Override
    protected boolean onNegativeClicked() {
        SettingsManager.setContactIntegrationSuggested();
        return true;
    }

}
