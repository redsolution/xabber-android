package com.xabber.android.data.extension.chat_markers;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageUpdateEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class BackpressureMessageMarker {
    private static BackpressureMessageMarker instance;
    private PublishSubject<MessageIdAndMarker> subject;

    public static BackpressureMessageMarker getInstance() {
        if (instance == null) instance = new BackpressureMessageMarker();
        return instance;
    }

    public void markMessage(String messageId, @Nullable ArrayList<String> stanzaId, ChatMarkersState marker, AccountJid accountJid) {
        if (messageId != null && marker != null && accountJid != null)
            subject.onNext(new MessageIdAndMarker(messageId, stanzaId, marker, accountJid));
    }

    private BackpressureMessageMarker() {
        createSubject();
    }

    private void createSubject() {
        subject = PublishSubject.create();
        subject.buffer(1000, TimeUnit.MILLISECONDS)
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messageIdAndMarkers -> {
                    if (messageIdAndMarkers == null || messageIdAndMarkers.isEmpty()) return;
                    Realm realm = null;
                    try{
                        realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                        realm.executeTransaction(realm1 -> {
                            for (MessageIdAndMarker messageAndMarker : messageIdAndMarkers) {

                                String headMessageId = messageAndMarker.messageId;
                                ArrayList<String> altHeadMessageId = messageAndMarker.stanzaId;
                                ChatMarkersState marker = messageAndMarker.marker;
                                AccountJid accountJid = messageAndMarker.accountJid;

                                MessageRealmObject headMessage = getMessageById(realm1, headMessageId,altHeadMessageId, accountJid);
                                if (headMessage != null && marker != null) {
                                    RealmResults<MessageRealmObject> messages = getPreviousUnmarkedMessages(realm1, headMessage, marker);
                                    List<String> ids = new ArrayList<>();
                                    if (messages != null) {
                                        for (MessageRealmObject mes : messages) {
                                            setMarkedState(mes, marker);
                                        }
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        LogManager.exception(BackpressureMessageMarker.class.getSimpleName(), e);
                    } finally {
                        if (realm != null && Looper.myLooper() != Looper.getMainLooper())
                            realm.close();
                    }
                    EventBus.getDefault().post(new MessageUpdateEvent());
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        LogManager.exception(this, throwable);
                        LogManager.d(this, "Exception is thrown. Created new publish subject.");
                        createSubject();
                    }
                });
    }

    private void setMarkedState(MessageRealmObject item, ChatMarkersState marker) {
        switch (marker) {
            case received:
                item.setDelivered(true);
                break;
            case displayed:
                item.setDisplayed(true);
                break;
        }
    }

    private RealmResults<MessageRealmObject> getPreviousUnmarkedMessages(Realm realm, MessageRealmObject messageRealmObject, ChatMarkersState marker) {
        String chatMarkerFieldState = null;
        switch (marker) {
            case received:
                chatMarkerFieldState = MessageRealmObject.Fields.DELIVERED;
                break;
            case displayed:
                chatMarkerFieldState = MessageRealmObject.Fields.DISPLAYED;
                break;
        }
        if (chatMarkerFieldState == null) return null;
        return realm.where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, messageRealmObject.getAccount().toString())
                .equalTo(MessageRealmObject.Fields.USER, messageRealmObject.getUser().toString())
                .equalTo(MessageRealmObject.Fields.INCOMING, false)
                .equalTo(chatMarkerFieldState, false)
                .equalTo(MessageRealmObject.Fields.IS_IN_PROGRESS, false)
                .lessThanOrEqualTo(MessageRealmObject.Fields.TIMESTAMP, messageRealmObject.getTimestamp())
                .findAll();
    }

    private MessageRealmObject getMessageById(Realm realm, String id, ArrayList<String> stanzaIds, AccountJid accountJid) {
        MessageRealmObject message;

        RealmQuery<MessageRealmObject> realmQuery = realm.where(MessageRealmObject.class);

        realmQuery.equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString());
        if (stanzaIds != null && stanzaIds.size()>0) {
            realmQuery.beginGroup();
            realmQuery.equalTo(MessageRealmObject.Fields.ORIGIN_ID, id);
            for (String stanzaId : stanzaIds) {
                realmQuery.or();
                realmQuery.equalTo(MessageRealmObject.Fields.STANZA_ID, stanzaId);
            }
            realmQuery.endGroup();
            message = realmQuery.findFirst();
        } else {
            realmQuery.equalTo(MessageRealmObject.Fields.ORIGIN_ID, id);
            message = realmQuery.findFirst();
        }
        return message;
    }

    private class MessageIdAndMarker {
        final String messageId;
        final ArrayList<String> stanzaId;
        final ChatMarkersState marker;
        final AccountJid accountJid;

        public MessageIdAndMarker(String messageId, @Nullable ArrayList<String> stanzaId, ChatMarkersState marker, AccountJid accountJid) {
            this.messageId = messageId;
            this.stanzaId = stanzaId;
            this.marker = marker;
            this.accountJid = accountJid;
        }
    }
}