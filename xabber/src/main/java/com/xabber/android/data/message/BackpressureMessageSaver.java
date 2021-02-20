package com.xabber.android.data.message;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.push.SyncManager;
import com.xabber.android.ui.OnNewMessageListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;


/** Groups messages to save. It is necessary to avoid
 * java.util.concurrent.RejectedExecutionException error,
 * which can occur if you frequently save messages one at a time in Realm.
 *
 * Issue in crashlytics: https://www.fabric.io/redsolution/android/apps/com.xabber.android/issues/5c55e9edf8b88c29636f3fd3
 * */
public class BackpressureMessageSaver {

    private static final String LOG_TAG = BackpressureMessageSaver.class.getSimpleName();

    private static BackpressureMessageSaver instance;
    private PublishSubject<MessageRealmObject> subject;

    public static BackpressureMessageSaver getInstance() {
        if (instance == null) instance = new BackpressureMessageSaver();
        return instance;
    }

    public void saveMessageItem(MessageRealmObject messageRealmObject) {
        if (hasCopyInRealm(messageRealmObject)) return;
        subject.onNext(messageRealmObject);
    }

    private BackpressureMessageSaver() {
        createSubject();
    }

    private void createSubject() {
        subject = PublishSubject.create();
        subject.buffer(250, TimeUnit.MILLISECONDS)
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<MessageRealmObject>>() {
                @Override
                public void call(final List<MessageRealmObject> messageRealmObjects) {
                    if (messageRealmObjects == null || messageRealmObjects.isEmpty()) return;
                    Realm realm = null;
                    try {
                        realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                        realm.executeTransactionAsync(realm1 -> {
                            realm1.copyToRealm(messageRealmObjects);
                        }, () ->  {
                            for (OnNewMessageListener listener :
                                    Application.getInstance().getUIListeners(OnNewMessageListener.class)){
                                listener.onNewMessage();
                            }
                            SyncManager.getInstance().onMessageSaved();
                            checkForAttachmentsAndDownload(messageRealmObjects);
                        });
                    } catch (Exception e) {
                        LogManager.exception(this, e);
                    } finally { if ( realm != null && Looper.myLooper() != Looper.getMainLooper())
                        realm.close(); }
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

    //TODO refactor this method before releasing
    private boolean hasCopyInRealm(final MessageRealmObject newIncomingMessageRealmObject){
        boolean result = false;
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        MessageRealmObject item;

        if (newIncomingMessageRealmObject.getUniqueId() != null) {
            item = realm.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.UNIQUE_ID, newIncomingMessageRealmObject.getUniqueId())
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, newIncomingMessageRealmObject.getAccount().toString())
                    .findFirst();
            if (item != null && !newIncomingMessageRealmObject.isForwarded()) {
                result = true;
                LogManager.d(LOG_TAG,
                        "Received message, but we already have message with same ID! \n Message stanza: "
                                + newIncomingMessageRealmObject.getOriginalStanza() + "\nMessage already in database stanza: "
                                + item.getOriginalStanza());
            }
        }

        if (newIncomingMessageRealmObject.getStanzaId() != null) {
            item = realm.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.STANZA_ID, newIncomingMessageRealmObject.getStanzaId())
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, newIncomingMessageRealmObject.getAccount().toString())
                    .findFirst();
            if (item != null && !newIncomingMessageRealmObject.isForwarded()) {
                result = true;
                LogManager.d(LOG_TAG,
                        "Received message, but we already have message with same ID! \n Message stanza: "
                                + newIncomingMessageRealmObject.getOriginalStanza() + "\nMessage already in database stanza: "
                                + item.getOriginalStanza());
            }
        }

        if (newIncomingMessageRealmObject.getOriginId() != null) {
            item = realm.where(MessageRealmObject.class)
                    .equalTo(MessageRealmObject.Fields.ORIGIN_ID, newIncomingMessageRealmObject.getOriginId())
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, newIncomingMessageRealmObject.getAccount().toString())
                    .findFirst();
            if (item != null && !newIncomingMessageRealmObject.isForwarded()) {
                result = true;
                LogManager.d(LOG_TAG,
                        "Received message, but we already have message with same ID! \n Message stanza: "
                                + newIncomingMessageRealmObject.getOriginalStanza() + "\nMessage already in database stanza: "
                                + item.getOriginalStanza());
            }
        }

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        return result;
    }

    private void checkForAttachmentsAndDownload(List<MessageRealmObject> messageRealmObjects) {
        if (SettingsManager.chatsAutoDownloadVoiceMessage()) {
            for (MessageRealmObject message : messageRealmObjects) {
                if (message.haveAttachments()) {
                    for (AttachmentRealmObject attachmentRealmObject : message.getAttachmentRealmObjects()) {
                        if (attachmentRealmObject.isVoice() && attachmentRealmObject.getFilePath() == null) {
                            DownloadManager.getInstance().downloadFile(attachmentRealmObject, message.getAccount(), Application.getInstance());
                        }
                    }
                }
            }
        }
    }
}
