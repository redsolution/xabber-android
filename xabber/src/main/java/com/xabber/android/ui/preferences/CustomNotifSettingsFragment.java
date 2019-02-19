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
    private String group;

    private SwitchPreference prefEnableCustomNotif;
    private SwitchPreference prefMessagePreview;
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

    public static CustomNotifSettingsFragment createInstance(Context context, AccountJid account, String group) {
        CustomNotifSettingsFragment fragment = new CustomNotifSettingsFragment();
        fragment.account = account;
        fragment.group = group;
        fragment.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_custom_notify);

        prefEnableCustomNotif = (SwitchPreference) getPreferenceScreen().findPreference("custom_notification_enable");
        prefMessagePreview = (SwitchPreference) getPreferenceScreen().findPreference("custom_notification_preview");
        prefSound = (RingtonePreference) getPreferenceScreen().findPreference("custom_notification_sound");
        prefVibro = (ListPreference) getPreferenceScreen().findPreference("custom_notification_vibro");
    }

    @Override
    public void onResume() {
        super.onResume();
        final NotifyPrefs notifyPrefs = user != null ? CustomNotifyPrefsManager.getInstance().findPrefsByChat(account, user)
                : CustomNotifyPrefsManager.getInstance().findPrefsByGroup(account, group);
        prefEnableCustomNotif.setChecked(notifyPrefs != null);
        prefEnableCustomNotif.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue)
                    if (user != null) CustomNotifyPrefsManager.getInstance().createChatNotifyPrefs(getActivity(),
                            notificationManager, account, user,"", true, "");
                    else CustomNotifyPrefsManager.getInstance().createGroupNotifyPrefs(getActivity(),
                            notificationManager, account, group,"", true, "");
                else if (notifyPrefs != null)
                    CustomNotifyPrefsManager.getInstance().deleteChatNotifyPrefs(notificationManager,
                            notifyPrefs.getId());
                return true;
            }
        });

        if (notifyPrefs != null) {
            prefMessagePreview.setChecked(notifyPrefs.isShowPreview());
            prefMessagePreview.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (user != null) CustomNotifyPrefsManager.getInstance().createChatNotifyPrefs(getActivity(),
                            notificationManager, account, user, notifyPrefs.getVibro(),
                            (Boolean) newValue, notifyPrefs.getSound());
                    else CustomNotifyPrefsManager.getInstance().createGroupNotifyPrefs(getActivity(),
                            notificationManager, account, group, notifyPrefs.getVibro(),
                            (Boolean) newValue, notifyPrefs.getSound());
                    return true;
                }
            });

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = notificationManager.getNotificationChannel(notifyPrefs.getChannelID());

                // sound
                prefSound.setSummary(getSoundTitle(channel));
                prefSound.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (user != null) CustomNotifyPrefsManager.getInstance().createChatNotifyPrefs(getActivity(),
                                notificationManager, account, user, notifyPrefs.getVibro(),
                                notifyPrefs.isShowPreview(), newValue.toString());
                        else CustomNotifyPrefsManager.getInstance().createGroupNotifyPrefs(getActivity(),
                                notificationManager, account, group, notifyPrefs.getVibro(),
                                notifyPrefs.isShowPreview(), newValue.toString());
                        return true;
                    }
                });

                // vibro
                prefVibro.setSummary(getVibroSummary(getActivity(), notifyPrefs));
                prefVibro.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (user != null) CustomNotifyPrefsManager.getInstance().createChatNotifyPrefs(getActivity(),
                                notificationManager, account, user, newValue.toString(),
                                notifyPrefs.isShowPreview(), notifyPrefs.getSound());
                        else CustomNotifyPrefsManager.getInstance().createGroupNotifyPrefs(getActivity(),
                                notificationManager, account, group, newValue.toString(),
                                notifyPrefs.isShowPreview(), notifyPrefs.getSound());
                        prefVibro.setSummary(getVibroSummary(getActivity(), notifyPrefs));
                        return true;
                    }
                });
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getSoundTitle(NotificationChannel channel) {
        if (channel == null) return null;
        Uri uri = channel.getSound();
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
        return ringtone.getTitle(getActivity());
    }

    private String getVibroSummary(Context context, NotifyPrefs notifyPrefs) {
        if (notifyPrefs == null) return null;
        else {
            switch (notifyPrefs.getVibro()) {
                case "disable":
                    return context.getString(R.string.disabled);
                case "short":
                    return context.getString(R.string.vibro_short);
                case "long":
                    return context.getString(R.string.vibro_long);
                case "if silent":
                    return context.getString(R.string.vibro_if_silent);
                default:
                    return context.getString(R.string.vibro_default);
            }
        }
    }
}
