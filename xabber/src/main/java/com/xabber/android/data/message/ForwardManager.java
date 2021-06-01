package com.xabber.android.data.message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.ForwardIdRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.forward.ForwardComment;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class ForwardManager {

    private static final String LOG_TAG = ForwardManager.class.getSimpleName();

    public static void forwardMessage(List<String> messages, AccountJid account, ContactJid user, String text, @Nullable String markupText) {
        final AbstractChat chat = ChatManager.getInstance().getOrCreateChat(account, user);
        final MessageRealmObject messageRealmObject = chat.createNewMessageItem(text);

        if (markupText != null) messageRealmObject.setMarkupText(markupText);

        RealmList<ForwardIdRealmObject> ids = new RealmList<>();

        for (String message : messages) {
            ids.add(new ForwardIdRealmObject(message));
        }

        messageRealmObject.setForwardedIds(ids);

        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 ->  {
                    realm1.copyToRealm(messageRealmObject);
                    EventBus.getDefault().post(new NewMessageEvent());
                    chat.sendMessages();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }

    public static String parseForwardComment(Stanza packet) {
        ExtensionElement comment = packet.getExtension(ForwardComment.ELEMENT, ForwardComment.NAMESPACE);
        if (comment instanceof ForwardComment) {
            return ((ForwardComment) comment).getComment();
        }
        return null;
    }

    @NonNull
    public static List<Forwarded> getForwardedFromStanza(Stanza packet) {
        List<ExtensionElement> elements = packet.getExtensions(Forwarded.ELEMENT, Forwarded.NAMESPACE);
        if (elements == null || elements.size() == 0) return Collections.emptyList();

        List<Forwarded> forwarded = new ArrayList<>();
        for (ExtensionElement element : elements) {
            if (element instanceof Forwarded) {
                forwarded.add((Forwarded)element);
            }
        }
        return forwarded;
    }

}
