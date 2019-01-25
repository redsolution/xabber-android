package com.xabber.android.data.message;

import android.content.ClipData;
import android.content.Context;

import com.xabber.android.R;
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
                String text = messagesToText(realm, ids, accountName, userName, isMUC, 0);
                realm.close();
                if (!text.isEmpty()) insertDataToClipboard(text);
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

    private static String messagesToText(Realm realm, String[] messagesIDs,
                                         String accountName, String userName, boolean isMUC, int level) {

        RealmResults<MessageItem> items = realm.where(MessageItem.class)
                .in(MessageItem.Fields.UNIQUE_ID, messagesIDs).findAll();

        StringBuilder stringBuilder = new StringBuilder();
        long previousTimestamp = 1;
        if (items != null && !items.isEmpty()) {
            for (MessageItem message : items) {
                stringBuilder.append(messageToText(realm, message, previousTimestamp, accountName, userName, isMUC, level));
                previousTimestamp = message.getTimestamp();
            }
        }

        return stringBuilder.toString().trim();
    }

    private static String messageToText(Realm realm, MessageItem message, long previousMessageTimestamp,
                                        String accountName, String userName, boolean isMUC, int level) {

        Context context = Application.getInstance().getApplicationContext();

        String space = getSpace(level);
        StringBuilder stringBuilder = new StringBuilder();

        final String name;
        if (isMUC) name = message.getResource().toString();
        else {
            if (message.isIncoming()) name = userName;
            else name = accountName;
        }

        final String date = StringUtils.getDateStringForClipboard(message.getTimestamp());
        if (!Utils.isSameDay(message.getTimestamp(), previousMessageTimestamp)) {
            stringBuilder.append("\n");
            stringBuilder.append(space);
            stringBuilder.append(date);
        }

        stringBuilder.append("\n");
        stringBuilder.append(space);
        stringBuilder.append('[');
        stringBuilder.append(StringUtils.getTimeTextWithSeconds(new Date(message.getTimestamp())));
        stringBuilder.append("] ");
        stringBuilder.append(name);
        stringBuilder.append(":\n");

        if (!message.getText().isEmpty()) {
            stringBuilder.append(space);
            stringBuilder.append(message.getText());
        }

        if (message.haveForwardedMessages()) {
            stringBuilder.append(space);
            stringBuilder.append(context.getResources().getString(R.string.forwarded_messages_count, message.getForwardedIds().size()));
            stringBuilder.append(":\n");
            stringBuilder.append(messagesToText(realm, message.getForwardedIdsAsArray(),
                    accountName, userName, isMUC, level + 1));
        }

        return stringBuilder.toString();
    }

    private static String getSpace(int level) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stringBuilder.append("> ");
        }
        return stringBuilder.toString();
    }

}
