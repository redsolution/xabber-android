package com.xabber.android.ui.dialog;

import android.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.ui.activity.ContactAdd;
import com.xabber.xmpp.address.Jid;

public class ContactSubscriptionDialog extends BaseContactDialog {

    public static DialogFragment newInstance(String account, String contact) {
        DialogFragment fragment = new ContactSubscriptionDialog();
        setArguments(account, contact, fragment);
        return fragment;
    }

    @Override
    protected int getDialogTitleTextResource() {
        return R.string.subscription_request_message;
    }

    @Override
    protected String getMessage() {
        return getString(R.string.contact_subscribe_confirm, Jid.getBareAddress(getAccount()));
    }

    @Override
    protected int getNegativeButtonTextResource() {
        return R.string.decline_contact;
    }

    @Override
    protected int getPositiveButtonTextResource() {
        return R.string.accept_contact;
    }

    @Override
    protected Integer getNeutralButtonTextResourceOrNull() {
        return null;
    }

    @Override
    protected void onPositiveButtonClick() {
        onAccept();
    }

    @Override
    protected void onNegativeButtonClick() {
        onDecline();
    }

    @Override
    protected void onNeutralButtonClick() {

    }

    public void onAccept() {
        try {
            PresenceManager.getInstance().acceptSubscription(getAccount(), getContact());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
        startActivity(ContactAdd.createIntent(getActivity(), getAccount(), getContact()));
    }

    public void onDecline() {
        try {
            PresenceManager.getInstance().discardSubscription(getAccount(), getContact());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }
}
