package com.xabber.android.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleActionBarInflater;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.xmpp.address.Jid;

import java.util.Collection;

public class AccountInfoEditor extends ManagedActivity implements OnAccountChangedListener {

    public static final String ARG_VCARD = "com.xabber.android.ui.AccountInfoEditor.ARG_VCARD";

    ContactTitleActionBarInflater contactTitleActionBarInflater;
    private String account;

    public static Intent createIntent(Context context, String account, String vCard) {
        Intent intent = new EntityIntentBuilder(context, AccountInfoEditor.class).setAccount(account).build();
        intent.putExtra(ARG_VCARD, vCard);
        return intent;
    }

    private static String getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_with_toolbar_and_container);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        contactTitleActionBarInflater = new ContactTitleActionBarInflater(this, toolbar);
        contactTitleActionBarInflater.setUpActionBarView();

        Intent intent = getIntent();
        account = getAccount(intent);
        String vCard = intent.getStringExtra(ARG_VCARD);

        if (AccountManager.getInstance().getAccount(account) == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
        }


        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, AccountInfoEditorFragment.newInstance(account, vCard)).commit();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    private void update() {
        AbstractContact bestContact = RosterManager.getInstance().getBestContact(account, Jid.getBareAddress(account));
        contactTitleActionBarInflater.update(bestContact);
        contactTitleActionBarInflater.hideStatusIcon();
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(account)) {
            update();
        }
    }
}
