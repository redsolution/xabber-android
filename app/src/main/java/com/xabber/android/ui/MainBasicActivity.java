package com.xabber.android.ui;


import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.ui.preferences.AboutViewer;
import com.xabber.android.ui.preferences.AccountEditor;
import com.xabber.android.ui.preferences.PreferenceEditor;

public class MainBasicActivity extends ManagedActivity implements ContactListDrawerFragment.ContactListDrawerListener {

    protected ActionBarDrawerToggle drawerToggle;
    protected DrawerLayout drawerLayout;
    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contact_list);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    }


    @Override
    public void onContactListDrawerListener(int viewId) {
        drawerLayout.closeDrawers();
        switch (viewId) {
            case R.id.drawer_action_settings:
                startActivity(PreferenceEditor.createIntent(this));
                break;
            case R.id.drawer_action_about:
                startActivity(AboutViewer.createIntent(this));
                break;
            case R.id.drawer_action_exit:
                exit();
                break;

        }
    }

    protected void exit() {
    }

    @Override
    public void onAccountSelected(String account) {
        drawerLayout.closeDrawers();
        startActivity(AccountEditor.createIntent(this, account));
    }
}
