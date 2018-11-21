package com.xabber.android.data.message;

import com.xabber.android.data.database.messagerealm.ForwardId;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.realm.RealmList;

public class ForwardManager {

    public static void forwardMessage(List<String> messages, AccountJid account, UserJid user, String text) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        MessageItem messageItem = chat.createNewMessageItem(text);

        RealmList<ForwardId> ids = new RealmList<>();

        for (String message : messages) {
            ids.add(new ForwardId(message));
        }

        messageItem.setForwardedIds(ids);
        chat.saveMessageItem(true, messageItem);
        chat.sendMessages();
        EventBus.getDefault().post(new NewMessageEvent());
    }

}
