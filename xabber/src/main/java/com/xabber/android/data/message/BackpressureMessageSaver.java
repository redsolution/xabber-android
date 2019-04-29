package com.xabber.android.data.message;

import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.log.LogManager;

import org.greenrobot.eventbus.EventBus;

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

    private static BackpressureMessageSaver instance;
    private PublishSubject<MessageItem> subject;

    public static BackpressureMessageSaver getInstance() {
        if (instance == null) instance = new BackpressureMessageSaver();
        return instance;
    }

    public void saveMessageItem(MessageItem messageItem) {
        subject.onNext(messageItem);
    }

    private BackpressureMessageSaver() {
        createSubject();
    }

    private void createSubject() {
        subject = PublishSubject.create();
        subject.buffer(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<MessageItem>>() {
                @Override
                public void call(final List<MessageItem> messageItems) {
                    if (messageItems == null || messageItems.isEmpty()) return;
                    try {
                        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.copyToRealm(messageItems);
                            }
                        }, new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
                                EventBus.getDefault().post(new NewMessageEvent());
                            }
                        });
                    } catch (Exception e) {
                        LogManager.exception(this, e);
                    }
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

}
