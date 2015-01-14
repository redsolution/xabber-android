package com.xabber.android.ui.preferences;

import android.os.Bundle;

import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

public class SecuritySettings extends ManagedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing())
            return;

        setContentView(R.layout.activity_preferences);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.preferences_activity_container, new SecuritySettingsFragment()).commit();
        }
    }
}
