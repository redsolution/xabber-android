package com.xabber.android.data.extension.chat_markers;

import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;


/** Groups messages to send displayed. To avoid
 * too often sending displayed chat markers.
 * */
public class BackpressureMessageReader {

    private static BackpressureMessageReader instance;
    private Map<AbstractContact, PublishSubject<MessageHolder>> queries = new HashMap<>();

    public static BackpressureMessageReader getInstance() {
        if (instance == null) {
            instance = new BackpressureMessageReader();
        }
        return instance;
    }

    public void markAsRead(MessageItem messageItem, boolean trySendDisplayed) {
        AbstractContact contact = RosterManager.getInstance().getAbstractContact(messageItem.getAccount(), messageItem.getUser());
        PublishSubject<MessageHolder> subject = queries.get(contact);
        if (subject == null) subject = createSubject(contact);
        subject.onNext(new MessageHolder(messageItem, trySendDisplayed));
    }

    private PublishSubject<MessageHolder> createSubject(final AbstractContact contact) {
        PublishSubject<MessageHolder> subject = PublishSubject.create();
        subject.debounce(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<MessageHolder>() {
                    @Override
                    public void call(MessageHolder holder) {
                        MessageItem message = holder.messageItem;
                        if (holder.trySendDisplayed)
                            ChatMarkerManager.getInstance().sendDisplayed(message);

                        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
                        RealmResults<MessageItem> messages = getPreviousUnreadMessages(realm, message);
                        realm.beginTransaction();
                        List<String> ids = new ArrayList<>();
                        for (MessageItem mes : messages) {
                            mes.setRead(true);
                            ids.add(mes.getUniqueId());
                        }
                        realm.commitTransaction();

                        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(message.getAccount(), message.getUser());
                        if (chat != null) chat.approveRead(ids);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        LogManager.exception(this, throwable);
                        LogManager.d(this, "Exception is thrown. Subject was deleted.");
                        queries.remove(contact);
                    }
                });
        queries.put(contact, subject);
        return subject;
    }

    private RealmResults<MessageItem> getPreviousUnreadMessages(Realm realm, MessageItem messageItem) {
        return realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, messageItem.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, messageItem.getUser().toString())
                .equalTo(MessageItem.Fields.READ, false)
                .lessThanOrEqualTo(MessageItem.Fields.TIMESTAMP, messageItem.getTimestamp())
                .findAll();
    }

    private class MessageHolder {
        final MessageItem messageItem;
        final boolean trySendDisplayed;

        public MessageHolder(MessageItem messageItem, boolean trySendDisplayed) {
            this.messageItem = messageItem;
            this.trySendDisplayed = trySendDisplayed;
        }
    }

}
