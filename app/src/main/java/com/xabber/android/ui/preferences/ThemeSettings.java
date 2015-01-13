package com.xabber.android.ui.preferences;

import android.os.Bundle;

import com.xabber.android.data.ActivityManager;
import com.xabber.android.ui.ContactList;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

public class ThemeSettings extends ManagedActivity
        implements ThemeSettingsFragment.OnThemeSettingsFragmentInteractionListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing())
            return;

        setContentView(R.layout.activity_preferences);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.preferences_activity_container, new ThemeSettingsFragment()).commit();
        }
    }

    @Override
    public void onThemeChanged() {
        ActivityManager.getInstance().clearStack(true);
        startActivity(ContactList.createIntent(this));
    }
}
