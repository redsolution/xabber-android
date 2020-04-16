package com.xabber.android.data.extension.chat_markers;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
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

    public void markAsRead(MessageRealmObject messageRealmObject, boolean trySendDisplayed) {
        PublishSubject<MessageDataHolder> subject = createSubjectIfNeeded(messageRealmObject.getAccount(), messageRealmObject.getUser());
        subject.onNext(new MessageDataHolder(messageRealmObject, trySendDisplayed));
    }

    public void markAsRead(String messageId, @Nullable ArrayList<String> stanzaId, AccountJid accountJid, ContactJid contactJid, boolean trySendDisplayed) {
        PublishSubject<MessageDataHolder> subject = createSubjectIfNeeded(accountJid, contactJid);
        subject.onNext(new MessageDataHolder(messageId, null, stanzaId, accountJid, contactJid, trySendDisplayed));
    }

    private PublishSubject<MessageDataHolder> createSubjectIfNeeded(AccountJid accountJid, ContactJid contactJid) {
        AbstractContact contact = RosterManager.getInstance().getAbstractContact(accountJid, contactJid);
        PublishSubject<MessageDataHolder> subject = queriesNew.get(contact);
        if (subject == null) subject = createSubject(contact);
        return subject;
    }

    private PublishSubject<MessageDataHolder> createSubject(final AbstractContact contact) {
        PublishSubject<MessageDataHolder> subject = PublishSubject.create();
        subject.debounce(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(holder -> { // TODO maybe move to background network
                    Application.getInstance().runInBackground(() -> {
                        Realm realm = null;
                        try {
                            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                            realm.executeTransaction(realm1 -> {
                                MessageRealmObject message = getMessageById(realm1, holder);
                                if (message != null) {
                                    if (holder.trySendDisplayed)
                                        ChatMarkerManager.getInstance().sendDisplayed(message);

                                    RealmResults<MessageRealmObject> messages = getPreviousUnreadMessages(realm1, message);
                                    messages.setBoolean(MessageRealmObject.Fields.READ, true);
                                    LogManager.d("BackpressureReader", "Finished setting the 'read' state to messages");
                                }
                            });
                        } catch (Exception e) {
                            LogManager.exception(BackpressureMessageReader.class.getSimpleName(), e);
                        } finally { if (realm != null && Looper.myLooper() != Looper.getMainLooper())
                            realm.close(); }
                    });
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

    private MessageRealmObject getMessageById(Realm realm, MessageDataHolder data) {
        return getMessageById(realm, data.messageId, data.uniqueId, data.stanzaId, data.account);
    }

    private MessageRealmObject getMessageById(Realm realm, String id, String uniqueId, ArrayList<String> stanzaIds, AccountJid accountJid) {
        MessageRealmObject message;
        RealmQuery<MessageRealmObject> realmQuery = realm.where(MessageRealmObject.class);
        realmQuery.equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString());
        int queryFieldCounter = 0;
        realmQuery.beginGroup();
        if (uniqueId != null && !uniqueId.isEmpty()) {
            realmQuery.equalTo(MessageRealmObject.Fields.UNIQUE_ID, uniqueId);
            queryFieldCounter++;
        }
        if (id != null && !id.isEmpty()) {
            if (queryFieldCounter > 0) realmQuery.or();
            realmQuery.equalTo(MessageRealmObject.Fields.ORIGIN_ID, id);
            realmQuery.or();
            realmQuery.equalTo(MessageRealmObject.Fields.STANZA_ID, id);
            queryFieldCounter++;
        }
        if (stanzaIds != null && stanzaIds.size() > 0) {
            for (String stanzaId : stanzaIds) {
                if (queryFieldCounter > 0) realmQuery.or();
                realmQuery.equalTo(MessageRealmObject.Fields.STANZA_ID, stanzaId);
                queryFieldCounter++;
            }
        }
        realmQuery.endGroup();
        message = realmQuery.findFirst();
        return message;
    }

    private RealmResults<MessageRealmObject> getPreviousUnreadMessages(Realm realm, MessageRealmObject messageRealmObject) {
        return realm.where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, messageRealmObject.getAccount().toString())
                .equalTo(MessageRealmObject.Fields.USER, messageRealmObject.getUser().toString())
                .equalTo(MessageRealmObject.Fields.READ, false)
                .lessThanOrEqualTo(MessageRealmObject.Fields.TIMESTAMP, messageRealmObject.getTimestamp())
                .findAll();
    }

    private static class MessageDataHolder {
        final String messageId;
        final String uniqueId;
        final ArrayList<String> stanzaId;
        final AccountJid account;
        final ContactJid user;
        final boolean trySendDisplayed;

        MessageDataHolder(MessageRealmObject messageRealmObject, boolean trySendDisplayed) {
            this.messageId = messageRealmObject.getOriginId();
            this.uniqueId = messageRealmObject.getUniqueId();
            this.stanzaId = getStanzaIds(messageRealmObject);
            this.account = messageRealmObject.getAccount();
            this.user = messageRealmObject.getUser();
            this.trySendDisplayed = trySendDisplayed;
        }

        MessageDataHolder(String messageId, @Nullable String uniqueId, @Nullable ArrayList<String> stanzaId,
                          AccountJid account, ContactJid user, boolean trySendDisplayed) {
            this.messageId = messageId;
            this.uniqueId = uniqueId;
            this.stanzaId = stanzaId;
            this.account = account;
            this.user = user;
            this.trySendDisplayed = trySendDisplayed;
        }

        private ArrayList<String> getStanzaIds(MessageRealmObject messageRealmObject) {
            ArrayList<String> stanzaIds = new ArrayList<>();
            if (messageRealmObject.getStanzaId() != null) {
                stanzaIds.add(messageRealmObject.getStanzaId());
            }
            if (messageRealmObject.getArchivedId() != null && !messageRealmObject.getArchivedId().equals(messageRealmObject.getStanzaId())) {
                stanzaIds.add(messageRealmObject.getArchivedId());
            }
            return stanzaIds;
        }
    }

    private static class MessageHolder {
        final MessageRealmObject messageRealmObject;
        final boolean trySendDisplayed;

        public MessageHolder(MessageRealmObject messageRealmObject, boolean trySendDisplayed) {
            this.messageRealmObject = messageRealmObject;
            this.trySendDisplayed = trySendDisplayed;
        }
    }

}
