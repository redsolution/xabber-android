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
    private Map<AbstractContact, PublishSubject<MessageDataHolder>> queriesNew = new HashMap<>();

    public static BackpressureMessageReader getInstance() {
        if (instance == null) {
            instance = new BackpressureMessageReader();
        }
        return instance;
    }

    public void markAsRead(MessageItem messageItem, boolean trySendDisplayed) {
        PublishSubject<MessageDataHolder> subject = createSubjectIfNeeded(messageItem.getAccount(), messageItem.getUser());
        subject.onNext(new MessageDataHolder(messageItem, trySendDisplayed));
    }

    public void markAsRead(String messageId, @Nullable ArrayList<String> stanzaId, AccountJid accountJid, UserJid userJid, boolean trySendDisplayed) {
        PublishSubject<MessageDataHolder> subject = createSubjectIfNeeded(accountJid, userJid);
        subject.onNext(new MessageDataHolder(messageId, null, stanzaId, accountJid, userJid, trySendDisplayed));
    }

    private PublishSubject<MessageDataHolder> createSubjectIfNeeded(AccountJid accountJid, UserJid userJid) {
        AbstractContact contact = RosterManager.getInstance().getAbstractContact(accountJid, userJid);
        PublishSubject<MessageDataHolder> subject = queriesNew.get(contact);
        if (subject == null) subject = createSubject(contact);
        return subject;
    }

    private PublishSubject<MessageDataHolder> createSubject(final AbstractContact contact) {
        PublishSubject<MessageDataHolder> subject = PublishSubject.create();
        subject.debounce(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<MessageDataHolder>() {
                    @Override
                    public void call(final MessageDataHolder holder) {
                        final List<String> ids = new ArrayList<>();

                        Application.getInstance().runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                Realm realm = null;
                                try {
                                    realm = Realm.getDefaultInstance();
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            MessageItem message = getMessageById(realm, holder);
                                            if (message != null) {
                                                if (holder.trySendDisplayed)
                                                    ChatMarkerManager.getInstance().sendDisplayed(message);

                                                RealmResults<MessageItem> messages = getPreviousUnreadMessages(realm, message);
                                                for (MessageItem mes : messages) {
                                                    mes.setRead(true);
                                                    ids.add(mes.getUniqueId());
                                                }
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    LogManager.exception(BackpressureMessageReader.class.getSimpleName(), e);
                                } finally { if (realm != null) realm.close(); }
                            }
                        });

                        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(holder.account, holder.user);
                        if (chat != null) chat.approveRead(ids);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        LogManager.exception(this, throwable);
                        LogManager.d(this, "Exception is thrown. Subject was deleted.");
                        queriesNew.remove(contact);
                    }
                });
        queriesNew.put(contact, subject);
        return subject;
    }

/*
    private PublishSubject<MessageDataHolder> createSubjectTest(final AbstractContact contact) {
        PublishSubject<MessageDataHolder> subject = PublishSubject.create();
        subject.debounce(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<MessageDataHolder>() {
                    @Override
                    public void call(MessageDataHolder holder) {
                        String messageId = holder.messageId;
                        String uniqueId = holder.uniqueId;
                        ArrayList<String> stanzaIds = holder.stanzaId;
                        AccountJid accountJid = holder.account;

                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();

                        MessageItem message = getMessageById(realm, messageId, uniqueId, stanzaIds, accountJid);
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
                        queriesNew.remove(contact);
                    }
                });
        queriesNew.put(contact, subject);
        return subject;
    }
*/

    private MessageItem getMessageById(Realm realm, MessageDataHolder data) {
        return getMessageById(realm, data.messageId, data.uniqueId, data.stanzaId, data.account);
    }

    private MessageItem getMessageById(Realm realm, String id, String uniqueId, ArrayList<String> stanzaIds, AccountJid accountJid) {
        MessageItem message;
        RealmQuery<MessageItem> realmQuery = RealmQuery.createQuery(realm, MessageItem.class);
        realmQuery.equalTo(MessageItem.Fields.ACCOUNT, accountJid.toString());
        if (stanzaIds != null && stanzaIds.size()>0) {
            realmQuery.beginGroup();
            if (uniqueId != null && !uniqueId.isEmpty()) {
                realmQuery.equalTo(MessageItem.Fields.UNIQUE_ID, uniqueId);
                realmQuery.or();
            }
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
            realmQuery.beginGroup();
            if (uniqueId != null && !uniqueId.isEmpty()) {
                realmQuery.equalTo(MessageItem.Fields.UNIQUE_ID, uniqueId);
                realmQuery.or();
            }
            realmQuery.equalTo(MessageItem.Fields.ORIGIN_ID, id);
            realmQuery.or();
            realmQuery.equalTo(MessageItem.Fields.STANZA_ID, id);
            realmQuery.endGroup();
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

    private class MessageDataHolder {
        final String messageId;
        final String uniqueId;
        final ArrayList<String> stanzaId;
        final AccountJid account;
        final UserJid user;
        final boolean trySendDisplayed;

        MessageDataHolder(MessageItem messageItem, boolean trySendDisplayed) {
            this.messageId = messageItem.getOriginId();
            this.uniqueId = messageItem.getUniqueId();
            this.stanzaId = getStanzaIds(messageItem);
            this.account = messageItem.getAccount();
            this.user = messageItem.getUser();
            this.trySendDisplayed = trySendDisplayed;
        }

        MessageDataHolder(String messageId, @Nullable String uniqueId, @Nullable ArrayList<String> stanzaId,
                          AccountJid account, UserJid user, boolean trySendDisplayed) {
            this.messageId = messageId;
            this.uniqueId = uniqueId;
            this.stanzaId = stanzaId;
            this.account = account;
            this.user = user;
            this.trySendDisplayed = trySendDisplayed;
        }

        private ArrayList<String> getStanzaIds(MessageItem messageItem) {
            ArrayList<String> stanzaIds = new ArrayList<>();
            if (messageItem.getStanzaId() != null) {
                stanzaIds.add(messageItem.getStanzaId());
            }
            if (messageItem.getArchivedId() != null && !messageItem.getArchivedId().equals(messageItem.getStanzaId())) {
                stanzaIds.add(messageItem.getArchivedId());
            }
            return stanzaIds;
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
