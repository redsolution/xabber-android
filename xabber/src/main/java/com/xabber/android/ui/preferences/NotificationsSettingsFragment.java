package com.xabber.android.ui.preferences;


import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.notification.NotificationChannelUtils;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;

import static com.xabber.android.data.SettingsManager.NOTIFICATION_PREFERENCES;

public class NotificationsSettingsFragment extends android.preference.PreferenceFragment {

    protected NotificationManager notificationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(NOTIFICATION_PREFERENCES);
        addPreferencesFromResource(R.xml.preference_notifications);
        notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        PreferenceSummaryHelperActivity.updateSummary(getPreferenceScreen());

        Preference resetPreference = getPreferenceScreen().findPreference(getString(R.string.events_reset_key));
        if (resetPreference != null) setupResetPreferences(resetPreference, notificationManager);

        Preference removeCustomNotifPreference = getPreferenceScreen().findPreference(getString(R.string.events_remove_all_custom_key));
        if (removeCustomNotifPreference != null) setupRemoveCustomNotifPreferences(removeCustomNotifPreference, notificationManager);
    }

    protected void setupResetPreferences(Preference preference, final NotificationManager notificationManager) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.events_reset_alert)
                        .setPositiveButton(R.string.category_reset, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getActivity(), R.string.events_reset_toast, Toast.LENGTH_SHORT).show();
                                SettingsManager.resetPreferences(getActivity(), NOTIFICATION_PREFERENCES);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                    NotificationChannelUtils.resetMessageChannels(notificationManager);
                                ((NotificationsSettings) getActivity()).restartFragment();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
                return true;
            }
        });
    }

    protected void setupRemoveCustomNotifPreferences(Preference preference, final NotificationManager notificationManager) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.events_remove_all_custom_summary)
                        .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getActivity(), R.string.events_reset_toast, Toast.LENGTH_SHORT).show();
                                CustomNotifyPrefsManager.getInstance().deleteAllNotifyPrefs(notificationManager);
                                ((NotificationsSettings) getActivity()).restartFragment();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
                return true;
            }
        });
    }
}
