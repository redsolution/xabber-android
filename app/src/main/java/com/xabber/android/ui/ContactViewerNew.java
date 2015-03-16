package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleExpandableToolbarInflater;
import com.xabber.android.ui.helper.ManagedActivity;


public class ContactViewerNew extends ManagedActivity {


    private ContactTitleExpandableToolbarInflater contactTitleExpandableToolbarInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contactTitleExpandableToolbarInflater = new ContactTitleExpandableToolbarInflater(this);
        AbstractContact bestContact = RosterManager.getInstance().getBestContact(getAccount(getIntent()), getUser(getIntent()));
        contactTitleExpandableToolbarInflater.onCreate(bestContact);

    }

    @Override
    protected void onResume() {
        super.onResume();

        contactTitleExpandableToolbarInflater.onResume();

    }

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactViewerNew.class).setAccount(account).setUser(user).build();
    }
    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

}
