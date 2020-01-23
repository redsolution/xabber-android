package com.xabber.android.data.extension.chat_markers;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
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
import io.realm.RealmQuery;
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
    private Map<AbstractContact, PublishSubject<MessageHolderTest>> queriesTest = new HashMap<>();

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
                        final MessageItem message = holder.messageItem;
                        final List<String> ids = new ArrayList<>();
                        if (holder.trySendDisplayed)
                            ChatMarkerManager.getInstance().sendDisplayed(message);
                        Application.getInstance().runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                Realm realm = null;
                                try {
                                    realm = Realm.getDefaultInstance();
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            RealmResults<MessageItem> messages = getPreviousUnreadMessages(realm, message);
                                            for (MessageItem mes : messages) {
                                                mes.setRead(true);
                                                ids.add(mes.getUniqueId());
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    LogManager.exception(BackpressureMessageReader.class.getSimpleName(), e);
                                } finally { if (realm != null) realm.close(); }
                            }
                        });

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

    public void markAsReadTest(String messageId, @Nullable ArrayList<String> stanzaId, AccountJid accountJid, UserJid userJid, boolean trySendDisplayed) {
        AbstractContact contact = RosterManager.getInstance().getAbstractContact(accountJid, userJid);
        PublishSubject<MessageHolderTest> subject = queriesTest.get(contact);
        if (subject == null) subject = createSubjectTest(contact);
        subject.onNext(new MessageHolderTest(messageId, stanzaId, trySendDisplayed, accountJid));
    }

    private PublishSubject<MessageHolderTest> createSubjectTest(final AbstractContact contact) {
        PublishSubject<MessageHolderTest> subject = PublishSubject.create();
        subject.debounce(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<MessageHolderTest>() {
                    @Override
                    public void call(MessageHolderTest holder) {
                        String messageId = holder.messageId;
                        ArrayList<String> stanzaId = holder.stanzaId;
                        AccountJid accountJid = holder.account;

                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();

                        MessageItem message = getMessageById(realm, messageId, stanzaId, accountJid);
                        if (message != null) {
                            if (holder.trySendDisplayed) {
                                ChatMarkerManager.getInstance().sendDisplayed(message);
                            }
                            RealmResults<MessageItem> messages = getPreviousUnreadMessages(realm, message);
                            List<String> ids = new ArrayList<>();
                            for (MessageItem mes : messages) {
                                mes.setRead(true);
                                ids.add(mes.getUniqueId());
                            }
                            realm.commitTransaction();
                            AbstractChat chat = MessageManager.getInstance().getOrCreateChat(message.getAccount(), message.getUser());
                            if (chat != null) chat.approveRead(ids);
                        }
                        if (realm.isInTransaction())
                            realm.commitTransaction();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        LogManager.exception(this, throwable);
                        LogManager.d(this, "Exception is thrown. Subject was deleted.");
                        queriesTest.remove(contact);
                    }
                });
        queriesTest.put(contact, subject);
        return subject;
    }

    private MessageItem getMessageById(Realm realm, String id, ArrayList<String> stanzaIds, AccountJid accountJid) {
        MessageItem message;
        RealmQuery<MessageItem> realmQuery = RealmQuery.createQuery(realm, MessageItem.class);
        realmQuery.equalTo(MessageItem.Fields.ACCOUNT, accountJid.toString());
        if (stanzaIds != null && stanzaIds.size()>0) {
            realmQuery.beginGroup();
            realmQuery.equalTo(MessageItem.Fields.ORIGIN_ID, id);
            realmQuery.or();
            realmQuery.equalTo(MessageItem.Fields.STANZA_ID, id);
            for (String stanzaId : stanzaIds) {
                realmQuery.or();
                realmQuery.equalTo(MessageItem.Fields.STANZA_ID, stanzaId);
            }
            realmQuery.endGroup();
            message = realmQuery.findFirst();

        } else {
            realmQuery.equalTo(MessageItem.Fields.ORIGIN_ID, id);
            realmQuery.or();
            realmQuery.equalTo(MessageItem.Fields.STANZA_ID, id);
            message = realmQuery.findFirst();
        }
        return message;
    }

    private RealmResults<MessageItem> getPreviousUnreadMessages(Realm realm, MessageItem messageItem) {
        return realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, messageItem.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, messageItem.getUser().toString())
                .equalTo(MessageItem.Fields.READ, false)
                .lessThanOrEqualTo(MessageItem.Fields.TIMESTAMP, messageItem.getTimestamp())
                .findAll();
    }

    private class MessageHolderTest {
        final String messageId;
        final ArrayList<String> stanzaId;
        final boolean trySendDisplayed;
        final AccountJid account;

        MessageHolderTest(String messageId, @Nullable ArrayList<String> stanzaId, boolean trySendDisplayed, AccountJid account) {
            this.messageId = messageId;
            this.stanzaId = stanzaId;
            this.trySendDisplayed = trySendDisplayed;
            this.account = account;
        }
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
