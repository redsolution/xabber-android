package com.xabber.android.data.message;

import android.content.ClipData;
import android.content.Context;
import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.extension.groups.GroupMemberManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.text.DatesUtilsKt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;

public class ClipManager {

    public static void copyMessagesToClipboard(final List<String> messageIDs) {
        final String[] ids = messageIDs.toArray(new String[0]);
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        String text = messagesToText(realm, ids, 0);
        if (!text.isEmpty()) insertDataToClipboard(text);
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    public static String createMessageTree(Realm realm, String id) {
        String[] str = {id};
        return messagesToText(realm, str, 1);
    }

    private static void insertDataToClipboard(final String text) {
        Application.getInstance().runOnUiThread(() -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    Application.getInstance().getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Xabber", text);
            if (clipboard !=null) clipboard.setPrimaryClip(clip);
        });
    }

    private static String messagesToText(Realm realm, String[] messagesIDs, int level) {

        RealmResults<MessageRealmObject> items = realm
                .where(MessageRealmObject.class)
                .in(MessageRealmObject.Fields.PRIMARY_KEY, messagesIDs)
                .findAll();

        StringBuilder stringBuilder = new StringBuilder();
        long previousTimestamp = 1;
        if (items != null && !items.isEmpty()) {
            for (MessageRealmObject message : items) {
                stringBuilder.append(messageToText(realm, message, previousTimestamp, level));
                previousTimestamp = message.getTimestamp();
            }
        }

        return stringBuilder.toString().trim();
    }

    private static String messageToText(Realm realm, MessageRealmObject message,
                                        long previousMessageTimestamp, int level) {

        String space = getSpace(level);
        StringBuilder stringBuilder = new StringBuilder();

        final String name = (message.getGroupchatUserId() != null)
                ? GroupMemberManager.INSTANCE.getGroupMemberById(
                        message.getAccount(), message.getUser(), message.getGroupchatUserId()
        ).getNickname() : RosterManager.getDisplayAuthorName(message);

        final String date = getDateStringForClipboard(message.getTimestamp());
        if (!DatesUtilsKt.isSameDayWith(message.getTimestamp(), previousMessageTimestamp)) {
            stringBuilder.append("\n");
            stringBuilder.append(space);
            stringBuilder.append(date);
        }

        stringBuilder.append("\n");
        stringBuilder.append(space);
        stringBuilder.append('[');

        stringBuilder.append(new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(message.getTimestamp()));
        stringBuilder.append("] ");
        stringBuilder.append(name);
        stringBuilder.append(":\n");

        if (message.hasForwardedMessages()) {
            stringBuilder.append(messagesToText(realm, message.getForwardedIdsAsArray(), level + 1));
            stringBuilder.append("\n");
        }

        if (message.hasAttachments()) {
            for (AttachmentRealmObject attachmentRealmObject : message.getAttachmentRealmObjects()) {
                stringBuilder.append(space);
                stringBuilder.append(attachmentRealmObject.getFileUrl());
                stringBuilder.append("\n");
            }
        }

        if (!message.getText().isEmpty()) {
            stringBuilder.append(space);
            stringBuilder.append(message.getText());
        }

        return stringBuilder.toString();
    }

    private static String getDateStringForClipboard(Long timestamp) {
        Date date = new Date(timestamp);
        String strPattern = "EEEE, d MMMM, yyyy";

        SimpleDateFormat pattern =
                new SimpleDateFormat(strPattern, Application.getInstance().getResources().getConfiguration().locale);
        return pattern.format(date);
    }

    private static String getSpace(int level) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stringBuilder.append("> ");
        }
        return stringBuilder.toString();
    }

}
