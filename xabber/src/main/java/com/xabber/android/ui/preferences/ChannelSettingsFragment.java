package com.xabber.android.ui.preferences;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.notification.MessageNotificationCreator;
import com.xabber.android.data.notification.NotificationChannelUtils;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;

import static com.xabber.android.data.SettingsManager.NOTIFICATION_PREFERENCES;

@RequiresApi(api = Build.VERSION_CODES.O)
public class ChannelSettingsFragment extends BaseSoundPrefFragment<ChannelRingtoneHolder> {

    private NotificationManager notificationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(NOTIFICATION_PREFERENCES);
        addPreferencesFromResource(R.xml.preference_notifications);
        notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        Preference resetPreference = getPreferenceScreen().findPreference(getString(R.string.events_reset_key));
        if (resetPreference != null) {
            resetPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.events_reset_alert)
                            .setPositiveButton(R.string.category_reset, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(), R.string.events_reset_toast, Toast.LENGTH_SHORT).show();
                                    SettingsManager.resetPreferences(getActivity(), NOTIFICATION_PREFERENCES);
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

        Preference removeCustomNotifPreference = getPreferenceScreen()
                .findPreference(getString(R.string.events_remove_all_custom_key));
        if (removeCustomNotifPreference != null) {
            removeCustomNotifPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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

    @Override
    public void onResume() {
        super.onResume();

        loadSound(R.string.events_sound_key, NotificationChannelUtils.ChannelType.privateChat);
        loadSound(R.string.events_sound_muc_key, NotificationChannelUtils.ChannelType.groupChat);
        loadSound(R.string.chats_attention_sound_key, NotificationChannelUtils.ChannelType.attention);

        loadVibro(R.string.events_vibro_chat_key, NotificationChannelUtils.ChannelType.privateChat);
        loadVibro(R.string.events_vibro_muc_key, NotificationChannelUtils.ChannelType.groupChat);
    }

    private void loadVibro(@StringRes int resid, final NotificationChannelUtils.ChannelType type) {
        ListPreference preference = (ListPreference) getPreferenceScreen().findPreference(getString(resid));
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                NotificationChannelUtils.updateMessageChannel(notificationManager, type, null,
                        MessageNotificationCreator.getVibroValue(((String)newValue), getActivity()),
                        null);
                return true;
            }
        });
    }

    private void loadSound(@StringRes int resid, final NotificationChannelUtils.ChannelType type) {
        NotificationChannel channel = NotificationChannelUtils.getMessageChannel(notificationManager, type);
        RingtonePreference preference = (RingtonePreference) getPreferenceScreen().findPreference(getString(resid));

        preference.setSummary(getSoundTitle(channel));
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return trySetNewRingtone(new ChannelRingtoneHolder(newValue.toString(), type));
            }
        });
    }

    private String getSoundTitle(NotificationChannel channel) {
        if (channel == null) return null;
        Uri uri = channel.getSound();
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
        return ringtone.getTitle(getActivity());
    }

    @Override
    protected void setNewRingtone(ChannelRingtoneHolder ringtoneHolder) {
        NotificationChannelUtils.updateMessageChannel(notificationManager, ringtoneHolder.type,
                Uri.parse(ringtoneHolder.uri), null, null);
    }
}



