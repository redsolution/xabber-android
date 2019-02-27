package com.xabber.android.data.message;

import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.ForwardId;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.forward.ForwardComment;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class ForwardManager {

    public static void forwardMessage(List<String> messages, AccountJid account, UserJid user, String text) {
        final AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        final MessageItem messageItem = chat.createNewMessageItem(text);

        RealmList<ForwardId> ids = new RealmList<>();

        for (String message : messages) {
            ids.add(new ForwardId(message));
        }

        messageItem.setForwardedIds(ids);

        MessageDatabaseManager.getInstance().getRealmUiThread()
                .executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealm(messageItem);
                        chat.sendMessages();
                    }
                });
        EventBus.getDefault().post(new NewMessageEvent());
    }

    public static String parseForwardComment(Stanza packet) {
        ExtensionElement comment = packet.getExtension(ForwardComment.ELEMENT, ForwardComment.NAMESPACE);
        if (comment instanceof ForwardComment) {
            return ((ForwardComment) comment).getComment();
        }
        return null;
    }

}
