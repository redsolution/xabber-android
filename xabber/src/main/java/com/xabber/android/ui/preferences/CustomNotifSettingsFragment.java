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
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.data.notification.custom_notification.NotifyPrefs;

public class CustomNotifSettingsFragment extends BaseSoundPrefFragment<CustomSettingsRingtoneHolder>
        implements Preference.OnPreferenceChangeListener {

    private Key key;

    private SwitchPreference prefEnableCustomNotif;
    private SwitchPreference prefMessagePreview;
    private RingtonePreference prefSound;
    private ListPreference prefVibro;

    private NotificationManager notificationManager;

    public static CustomNotifSettingsFragment createInstance(Context context, Key key) {
        CustomNotifSettingsFragment fragment = new CustomNotifSettingsFragment();
        fragment.key = key;
        fragment.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_custom_notify);

        prefEnableCustomNotif = (SwitchPreference) getPreferenceScreen().findPreference(getString(R.string.custom_notification_enable_key));
        prefMessagePreview = (SwitchPreference) getPreferenceScreen().findPreference(getString(R.string.custom_notification_preview_key));
        prefSound = (RingtonePreference) getPreferenceScreen().findPreference(getString(R.string.custom_notification_sound_key));
        prefVibro = (ListPreference) getPreferenceScreen().findPreference(getString(R.string.custom_notification_vibro_key));

        prefEnableCustomNotif.setOnPreferenceChangeListener(this);
        prefMessagePreview.setOnPreferenceChangeListener(this);
        prefSound.setOnPreferenceChangeListener(this);
        prefVibro.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final NotifyPrefs notifyPrefs = CustomNotifyPrefsManager.getInstance().findPrefs(key);

        if (preference.getKey().equals(getString(R.string.custom_notification_enable_key))) {
            if ((Boolean) newValue && key != null)
                CustomNotifyPrefsManager.getInstance().createNotifyPrefs(getActivity(),
                        notificationManager, key, "", true, "");
            else if (notifyPrefs != null)
                CustomNotifyPrefsManager.getInstance().deleteNotifyPrefs(notificationManager, notifyPrefs.getId());
            updateSummaries();

        } else if (preference.getKey().equals(getString(R.string.custom_notification_preview_key))) {
            if (notifyPrefs == null) return false;
            CustomNotifyPrefsManager.getInstance().createNotifyPrefs(getActivity(),
                    notificationManager, key, notifyPrefs.getVibro(),
                    (Boolean) newValue, notifyPrefs.getSound());

        } else if (preference.getKey().equals(getString(R.string.custom_notification_sound_key))) {
            if (notifyPrefs == null) return false;
            return trySetNewRingtone(new CustomSettingsRingtoneHolder(newValue.toString(),
                    notifyPrefs.getVibro(), notifyPrefs.isShowPreview()));

        } else if (preference.getKey().equals(getString(R.string.custom_notification_vibro_key))) {
            if (notifyPrefs == null) return false;
            CustomNotifyPrefsManager.getInstance().createNotifyPrefs(getActivity(),
                    notificationManager, key, newValue.toString(),
                    notifyPrefs.isShowPreview(), notifyPrefs.getSound());
            updateSummaries();

        }
        return true;
    }

    @Override
    protected void setNewRingtone(CustomSettingsRingtoneHolder ringtoneHolder) {
        CustomNotifyPrefsManager.getInstance().createNotifyPrefs(getActivity(),
                notificationManager, key, ringtoneHolder.vibro,
                ringtoneHolder.showPreview, ringtoneHolder.uri);
        updateSummaries();
    }

    private void updateSummaries() {
        final NotifyPrefs notifyPrefs = CustomNotifyPrefsManager.getInstance().findPrefs(key);
        prefEnableCustomNotif.setChecked(notifyPrefs != null);

        if (notifyPrefs != null) {
            prefMessagePreview.setChecked(notifyPrefs.isShowPreview());
            prefVibro.setSummary(getVibroSummary(getActivity(), notifyPrefs));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = notificationManager.getNotificationChannel(notifyPrefs.getChannelID());
                prefSound.setSummary(getSoundTitle(channel));
            } else prefSound.setSummary(getSoundTitle(notifyPrefs));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getSoundTitle(NotificationChannel channel) {
        if (channel == null) return null;
        Uri uri = channel.getSound();
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
        return ringtone.getTitle(getActivity());
    }

    private String getSoundTitle(NotifyPrefs notifyPrefs) {
        Uri uri = Uri.parse(notifyPrefs.getSound());
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
