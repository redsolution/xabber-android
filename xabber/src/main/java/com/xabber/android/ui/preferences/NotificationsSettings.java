package com.xabber.android.ui.preferences;


import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.ToolbarHelper;

public class NotificationsSettings extends ManagedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing())
            return;

        setContentView(R.layout.activity_with_toolbar_and_container);

        String title = PreferenceSummaryHelperActivity.getPreferenceTitle(getString(R.string.preference_events));
        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(this, title);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                getFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, new ChannelSettingsFragment()).commit();
            else getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new NotificationsSettingsFragment()).commit();
        }
    }

    public void restartFragment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChannelSettingsFragment()).commit();
        else getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new NotificationsSettingsFragment()).commit();
    }
}
