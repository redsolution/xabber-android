package com.xabber.android.ui.preferences;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.NotifyPrefs;

public class CustomNotifSettingsFragment extends android.preference.PreferenceFragment {

    private AccountJid account;
    private UserJid user;

    private SwitchPreference prefEnableCustomNotif;
    private Preference prefMessagePreview;
    private RingtonePreference prefSound;
    private ListPreference prefVibro;

    private NotificationManager notificationManager;

    public static CustomNotifSettingsFragment createInstance(Context context, AccountJid account, UserJid user) {
        CustomNotifSettingsFragment fragment = new CustomNotifSettingsFragment();
        fragment.account = account;
        fragment.user = user;
        fragment.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_custom_notify);

        prefEnableCustomNotif = (SwitchPreference) getPreferenceScreen().findPreference("custom_notification_enable");
        prefMessagePreview = getPreferenceScreen().findPreference("custom_notification_preview");
        prefSound = (RingtonePreference) getPreferenceScreen().findPreference("custom_notification_sound");
        prefVibro = (ListPreference) getPreferenceScreen().findPreference("custom_notification_vibro");
    }

    @Override
    public void onResume() {
        super.onResume();
        final NotifyPrefs notifyPrefs = CustomNotifyPrefsManager.getInstance().findChatNotifyPrefs(account, user);
        prefEnableCustomNotif.setChecked(notifyPrefs != null);
        prefEnableCustomNotif.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue)
                    CustomNotifyPrefsManager.getInstance().createChatNotifyPrefs(getActivity(),
                            notificationManager, account, user, true, "", true, "");
                else if (notifyPrefs != null)
                    CustomNotifyPrefsManager.getInstance().deleteChatNotifyPrefs(notificationManager,
                            notifyPrefs.getId());
                return true;
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && notifyPrefs != null) {
            NotificationChannel channel = notificationManager.getNotificationChannel(notifyPrefs.getChannelID());

            // sound
            prefSound.setSummary(getSoundTitle(channel));
            prefSound.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    CustomNotifyPrefsManager.getInstance().createChatNotifyPrefs(getActivity(),
                            notificationManager, account, user, true, "", true, newValue.toString());
                    return true;
                }
            });
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getSoundTitle(NotificationChannel channel) {
        if (channel == null) return null;
        Uri uri = channel.getSound();
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
        return ringtone.getTitle(getActivity());
    }


}
