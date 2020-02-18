package com.xabber.android.data.extension.chat_markers;

import androidx.annotation.Nullable;

import com.xabber.android.data.database.realmobjects.MessageItem;
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
                .subscribe(new Action1<List<MessageIdAndMarker>>() {
                    @Override
                    public void call(final List<MessageIdAndMarker> messageAndMarkers) {
                        if (messageAndMarkers == null || messageAndMarkers.isEmpty()) return;
                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();
                        for (MessageIdAndMarker messageAndMarker : messageAndMarkers) {

                            String headMessageId = messageAndMarker.messageId;
                            ArrayList<String> altHeadMessageId = messageAndMarker.stanzaId;
                            ChatMarkersState marker = messageAndMarker.marker;
                            AccountJid accountJid = messageAndMarker.accountJid;

                            MessageItem headMessage = getMessageById(realm, headMessageId,altHeadMessageId, accountJid);
                            if (headMessage != null && marker != null) {
                                RealmResults<MessageItem> messages = getPreviousUnmarkedMessages(realm, headMessage, marker);
                                List<String> ids = new ArrayList<>();
                                if (messages != null) {
                                    for (MessageItem mes : messages) {
                                        setMarkedState(mes, marker);
                                    }
                                }

                            }
                        }
                        if (realm.isInTransaction())
                            realm.commitTransaction();
                        EventBus.getDefault().post(new MessageUpdateEvent());
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

    private void setMarkedState(MessageItem item, ChatMarkersState marker) {
        switch (marker) {
            case received:
                item.setDelivered(true);
                break;
            case displayed:
                item.setDisplayed(true);
                break;
        }
    }

    private RealmResults<MessageItem> getPreviousUnmarkedMessages(Realm realm, MessageItem messageItem, ChatMarkersState marker) {
        String chatMarkerFieldState = null;
        switch (marker) {
            case received:
                chatMarkerFieldState = MessageItem.Fields.DELIVERED;
                break;
            case displayed:
                chatMarkerFieldState = MessageItem.Fields.DISPLAYED;
                break;
        }
        if (chatMarkerFieldState == null) return null;
        return realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, messageItem.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, messageItem.getUser().toString())
                .equalTo(MessageItem.Fields.INCOMING, false)
                .equalTo(chatMarkerFieldState, false)
                .equalTo(MessageItem.Fields.IS_IN_PROGRESS, false)
                .lessThanOrEqualTo(MessageItem.Fields.TIMESTAMP, messageItem.getTimestamp())
                .findAll();
    }

    private MessageItem getMessageById(Realm realm, String id, ArrayList<String> stanzaIds, AccountJid accountJid) {
        MessageItem message;

        RealmQuery<MessageItem> realmQuery = realm.where(MessageItem.class);

        realmQuery.equalTo(MessageItem.Fields.ACCOUNT, accountJid.toString());
        if (stanzaIds != null && stanzaIds.size()>0) {
            realmQuery.beginGroup();
            realmQuery.equalTo(MessageItem.Fields.ORIGIN_ID, id);
            for (String stanzaId : stanzaIds) {
                realmQuery.or();
                realmQuery.equalTo(MessageItem.Fields.STANZA_ID, stanzaId);
            }
            realmQuery.endGroup();
            message = realmQuery.findFirst();
        } else {
            realmQuery.equalTo(MessageItem.Fields.ORIGIN_ID, id);
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