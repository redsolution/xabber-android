package com.xabber.android.ui.preferences;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

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
        CharSequence name;
        String description;

        if (type == ChannelType.groupChat) {
            name = "Групповые чаты";
            description = "Уведомления о сообщениях в групповых чатах";
        } else {
            name = "Личные чаты";
            description = "Уведомления о сообщениях в личных чатах";
        }

        @SuppressLint("WrongConstant") NotificationChannel channel =
                new NotificationChannel(getChannelID(type), name,
                        android.app.NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(description);
        if (vibro != null) channel.setVibrationPattern(vibro);
        if (sound != null && audioAttrs != null) channel.setSound(sound, audioAttrs);
        notifManager.createNotificationChannel(channel);
        return getChannelID(type);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String createPresistentConnectionChannel(NotificationManager notifManager) {
        String channelName = "Persistent connection";
        String description = "Persistent connection necessary to receive messages";
        NotificationChannel channel =
                new NotificationChannel(PERSISTENT_CONNECTION_CHANNEL_ID, channelName,
                        android.app.NotificationManager.IMPORTANCE_NONE);
        channel.setShowBadge(false);
        channel.setDescription(description);
        notifManager.createNotificationChannel(channel);
        return channel.getId();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String createEventsChannel(NotificationManager notifManager) {
        String channelName = "Events";
        String description = "Events like: attention, group chat invites, subscription requests, OTR requests";
        @SuppressLint("WrongConstant") NotificationChannel channel =
                new NotificationChannel(EVENTS_CHANNEL_ID, channelName,
                        android.app.NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(description);
        channel.setShowBadge(false);
        notifManager.createNotificationChannel(channel);
        return channel.getId();
    }

}
