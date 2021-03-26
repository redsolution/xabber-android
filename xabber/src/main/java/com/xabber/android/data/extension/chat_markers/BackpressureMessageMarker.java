package com.xabber.android.data.extension.chat_markers;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageStatus;
import com.xabber.android.ui.OnMessageUpdatedListener;

import java.util.ArrayList;
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

    public void markMessage(String messageId, @Nullable ArrayList<String> stanzaId, ChatMarkersState marker,
                            AccountJid accountJid) {

        if (messageId != null && marker != null && accountJid != null){
            subject.onNext(new MessageIdAndMarker(messageId, stanzaId, marker, accountJid));
        }
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

                                MessageRealmObject headMessage = getMessageById(realm1, headMessageId,altHeadMessageId,
                                        accountJid);
                                if (headMessage != null && marker != null) {
                                    RealmResults<MessageRealmObject> messages = getPreviousUnmarkedMessages(
                                            realm1, marker, headMessage);
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
                        if (realm != null && Looper.myLooper() != Looper.getMainLooper()) realm.close();
                    }
                    for (OnMessageUpdatedListener listener :
                            Application.getInstance().getUIListeners(OnMessageUpdatedListener.class)){
                        listener.onMessageUpdated();
                    }
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
                item.setMessageStatus(MessageStatus.RECEIVED);
                break;
            case displayed:
                item.setMessageStatus(MessageStatus.DISPLAYED);
                break;
        }
    }

    private RealmResults<MessageRealmObject> getPreviousUnmarkedMessages(Realm realm, ChatMarkersState marker,
                                                                         MessageRealmObject messageRealmObject) {
        String chatMarkerFieldState = null;
        switch (marker) {
            case received:
                chatMarkerFieldState = MessageStatus.RECEIVED.toString();
                break;
            case displayed:
                chatMarkerFieldState = MessageStatus.DISPLAYED.toString();
                break;
        }
        if (chatMarkerFieldState == null) return null;

        return realm.where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.ACCOUNT, messageRealmObject.getAccount().toString())
                .equalTo(MessageRealmObject.Fields.USER, messageRealmObject.getUser().toString())
                .equalTo(MessageRealmObject.Fields.INCOMING, false)
                .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS, chatMarkerFieldState) //todo check this
                .notEqualTo(MessageRealmObject.Fields.MESSAGE_STATUS, MessageStatus.UPLOADING.toString()) //todo check this
                .lessThanOrEqualTo(MessageRealmObject.Fields.TIMESTAMP, messageRealmObject.getTimestamp())
                .findAll();
    }

    private MessageRealmObject getMessageById(Realm realm, String id, ArrayList<String> stanzaIds,
                                              AccountJid accountJid) {
        int idFieldCounter = 0;
        RealmQuery<MessageRealmObject> realmQuery = realm.where(MessageRealmObject.class);
        realmQuery.equalTo(MessageRealmObject.Fields.ACCOUNT, accountJid.toString());
        realmQuery.beginGroup();
        if (id != null && !id.isEmpty()) {
            realmQuery.equalTo(MessageRealmObject.Fields.ORIGIN_ID, id);
            realmQuery.or();
            realmQuery.equalTo(MessageRealmObject.Fields.STANZA_ID, id);
            idFieldCounter++;
        }
        if (stanzaIds != null && stanzaIds.size() > 0) {
            for (String stanzaId : stanzaIds) {

                if (idFieldCounter > 0) realmQuery.or();

                realmQuery.equalTo(MessageRealmObject.Fields.STANZA_ID, stanzaId);
                idFieldCounter++;
            }
        }
        if (idFieldCounter == 0) {
            return null;
        } else {
            realmQuery.endGroup();
            return realmQuery.findFirst();
        }
    }

    private static class MessageIdAndMarker {
        final String messageId;
        final ArrayList<String> stanzaId;
        final ChatMarkersState marker;
        final AccountJid accountJid;

        MessageIdAndMarker(String messageId, @Nullable ArrayList<String> stanzaId, ChatMarkersState marker,
                           AccountJid accountJid) {
            this.messageId = messageId;
            this.stanzaId = stanzaId;
            this.marker = marker;
            this.accountJid = accountJid;
        }
    }

}