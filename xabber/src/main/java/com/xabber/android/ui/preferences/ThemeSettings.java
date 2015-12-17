package com.xabber.android.ui.preferences;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.ui.activity.ContactList;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.activity.PreferenceSummaryHelper;
import com.xabber.android.ui.color.BarPainter;

public class ThemeSettings extends ManagedActivity
        implements ThemeSettingsFragment.OnThemeSettingsFragmentInteractionListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing())
            return;

        setContentView(R.layout.activity_with_toolbar_and_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        setSupportActionBar(toolbar);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        setTitle(PreferenceSummaryHelper.getPreferenceTitle(getString(R.string.preference_interface)));

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new ThemeSettingsFragment()).commit();
        }
    }

    @Override
    public void onThemeChanged() {
        ActivityManager.getInstance().clearStack(true);
        startActivity(ContactList.createIntent(this));
    }
}