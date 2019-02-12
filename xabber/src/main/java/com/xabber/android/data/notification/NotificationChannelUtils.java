package com.xabber.android.data.notification;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;

import com.xabber.android.R;
import com.xabber.android.data.Application;

import java.util.UUID;

public class NotificationChannelUtils {

    private static final String PRIVATE_MESSAGE_CHANNEL_ID_KEY = "PRIVATE_MESSAGE_CHANNEL_ID_KEY";
    private static final String GROUP_MESSAGE_CHANNEL_ID_KEY = "GROUP_MESSAGE_CHANNEL_ID_KEY";

    private static final String DEFAULT_PRIVATE_MESSAGE_CHANNEL_ID = "DEFAULT_PRIVATE_MESSAGE_CHANNEL_ID";
    private static final String DEFAULT_GROUP_MESSAGE_CHANNEL_ID = "DEFAULT_GROUP_MESSAGE_CHANNEL_ID";
    public static final String PERSISTENT_CONNECTION_CHANNEL_ID = "PERSISTENT_CONNECTION_CHANNEL_ID";
    public static final String EVENTS_CHANNEL_ID = "EVENTS_CHANNEL_ID";

    public enum ChannelType {
        privateChat,
        groupChat
    }

    public static String getChannelID(ChannelType type) {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(Application.getInstance());

        if (type == ChannelType.groupChat)
            return sPref.getString(GROUP_MESSAGE_CHANNEL_ID_KEY, DEFAULT_GROUP_MESSAGE_CHANNEL_ID);
        else return sPref.getString(PRIVATE_MESSAGE_CHANNEL_ID_KEY, DEFAULT_PRIVATE_MESSAGE_CHANNEL_ID);
    }

    private static String updateChannelID(ChannelType type) {
        String newID = UUID.randomUUID().toString();
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(Application.getInstance());

        if (type == ChannelType.groupChat)
            sPref.edit().putString(GROUP_MESSAGE_CHANNEL_ID_KEY, newID).apply();
        else sPref.edit().putString(PRIVATE_MESSAGE_CHANNEL_ID_KEY, newID).apply();

        return newID;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static NotificationChannel getMessageChannel(NotificationManager notifManager, ChannelType type) {
        if (notifManager == null) return null;
        else return notifManager.getNotificationChannel(getChannelID(type));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String updateMessageChannel(NotificationManager notifManager, ChannelType type,
                                              Uri newSound, long[] newVibro, AudioAttributes newAudioAttrs) {

        // settings
        NotificationChannel channel = getMessageChannel(notifManager, type);
        Uri sound = (newSound != null) ? newSound : channel.getSound();
        long[] vibro = (newVibro != null) ? newVibro : channel.getVibrationPattern();
        AudioAttributes audioAttrs = (newAudioAttrs != null) ? newAudioAttrs : channel.getAudioAttributes();

        // delete old channel
        notifManager.deleteNotificationChannel(getChannelID(type));

        // need to change channel settings
        updateChannelID(type);
        return createMessageChannel(notifManager, type, sound, vibro, audioAttrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String createMessageChannel(NotificationManager notifManager, ChannelType type,
                                              Uri sound, long[] vibro, AudioAttributes audioAttrs) {
        @SuppressLint("WrongConstant") NotificationChannel channel =
                new NotificationChannel(getChannelID(type),
                        getString(type == ChannelType.groupChat ? R.string.channel_group_chat_title
                                : R.string.channel_private_chat_title),
                        android.app.NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(getString(type == ChannelType.groupChat ? R.string.channel_group_chat_description
                : R.string.channel_private_chat_description));
        if (vibro != null) channel.setVibrationPattern(vibro);
        if (sound != null && audioAttrs != null) channel.setSound(sound, audioAttrs);
        notifManager.createNotificationChannel(channel);
        return getChannelID(type);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String createPresistentConnectionChannel(NotificationManager notifManager) {
        NotificationChannel channel =
                new NotificationChannel(PERSISTENT_CONNECTION_CHANNEL_ID,
                        getString(R.string.channel_persistent_connection_title),
                        android.app.NotificationManager.IMPORTANCE_NONE);
        channel.setShowBadge(false);
        channel.setDescription(getString(R.string.channel_persistent_connection_description));
        notifManager.createNotificationChannel(channel);
        return channel.getId();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String createEventsChannel(NotificationManager notifManager) {
        @SuppressLint("WrongConstant") NotificationChannel channel =
                new NotificationChannel(EVENTS_CHANNEL_ID,
                        getString(R.string.channel_events_title),
                        android.app.NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(getString(R.string.channel_events_description));
        channel.setShowBadge(false);
        notifManager.createNotificationChannel(channel);
        return channel.getId();
    }

    private static String getString(@StringRes int resid) {
        return Application.getInstance().getString(resid);
    }
}
