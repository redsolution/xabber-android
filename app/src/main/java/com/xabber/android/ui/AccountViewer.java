package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.xabber.android.R;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;

public class AccountViewer extends ContactViewer implements Toolbar.OnMenuItemClickListener {

    public static final int ACCOUNT_VIEWER_MENU = R.menu.account_viewer;

    public static Intent createIntent(Context context, String account) {
        return new EntityIntentBuilder(context, AccountViewer.class)
                .setAccount(account).setUser(GroupManager.IS_ACCOUNT).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = contactTitleExpandableToolbarInflater.getToolbar();
        toolbar.inflateMenu(ACCOUNT_VIEWER_MENU);
        toolbar.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getBareAddress());
        if (rosterContact != null) {
            getMenuInflater().inflate(ACCOUNT_VIEWER_MENU, menu);
        }

        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_account_user_info:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
