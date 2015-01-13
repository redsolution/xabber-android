package com.xabber.android.ui.preferences;

import android.app.Activity;
import android.os.Bundle;

import com.xabber.androiddev.R;

public class ContactListSettings extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        setContentView(R.layout.activity_preferences);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.preferences_activity_container, new ContactListSettingsFragment()).commit();
        }
    }
}
