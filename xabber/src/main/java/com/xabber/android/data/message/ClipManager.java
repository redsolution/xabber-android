package com.xabber.android.data.message;

import android.content.ClipData;
import android.content.Context;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.android.utils.Utils;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class ClipManager {

    public static void copyMessagesToClipboard(AbstractChat chat, final List<String> messageIDs) {
        if (chat == null) return;

        final String accountName = AccountManager.getInstance().getNickName(chat.getAccount());
        final String userName = RosterManager.getInstance().getName(chat.getAccount(), chat.getUser());
        final boolean isMUC = chat instanceof RoomChat;
        final String[] ids = messageIDs.toArray(new String[0]);
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                RealmResults<MessageItem> items = realm.where(MessageItem.class)
                        .in(MessageItem.Fields.UNIQUE_ID, ids).findAll();

                StringBuilder stringBuilder = new StringBuilder();
                long previousTimestamp = 1;
                if (items != null && !items.isEmpty()) {
                    for (MessageItem message : items) {
                        stringBuilder.append(messageToText(message, previousTimestamp, accountName, userName, isMUC));
                        previousTimestamp = message.getTimestamp();
                    }
                }
                realm.close();

                if (!stringBuilder.toString().isEmpty()) insertDataToClipboard(stringBuilder.toString());
            }
        });
    }

    private static void insertDataToClipboard(final String text) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) Application.getInstance()
                        .getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Xabber", text);
                if (clipboard !=null) clipboard.setPrimaryClip(clip);
            }
        });
    }

    private static String messageToText(MessageItem message, long previousMessageTimestamp,
                                        String accountName, String userName, boolean isMUC) {

        StringBuilder stringBuilder = new StringBuilder();


        final String name;
        if (isMUC) name = message.getResource().toString();
        else {
            if (message.isIncoming()) name = userName;
            else name = accountName;
        }

        final String date = StringUtils.getDateStringForMessage(message.getTimestamp());
        if (!Utils.isSameDay(message.getTimestamp(), previousMessageTimestamp)) {
            stringBuilder.append("\n");
            stringBuilder.append(date);
        }

        stringBuilder.append("\n[");
        stringBuilder.append(StringUtils.getTimeText(new Date(message.getTimestamp())));
        stringBuilder.append(']');
        stringBuilder.append(' ');
        stringBuilder.append(name);
        stringBuilder.append(":\n");
        stringBuilder.append(message.getText());

        return stringBuilder.toString();
    }

}
