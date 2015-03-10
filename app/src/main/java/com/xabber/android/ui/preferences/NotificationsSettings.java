package com.xabber.android.ui.preferences;


import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.ui.helper.PreferenceSummaryHelper;
import com.xabber.androiddev.R;

public class NotificationsSettings extends ManagedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing())
            return;

        setContentView(R.layout.activity_preferences);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_default));

        setTitle(PreferenceSummaryHelper.getPreferenceTitle(getString(R.string.preference_events)));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.preferences_activity_container, new NotificationsSettingsFragment()).commit();
        }
    }
}
