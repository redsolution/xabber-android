package com.xabber.android.data.http;

import android.util.Log;

import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.CrowdfundingMessage;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CrowdfundingManager implements OnLoadListener {

    private static CrowdfundingManager instance;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    // TODO: 29.11.18 composite subscription must depends on application lifecycle

    public static CrowdfundingManager getInstance() {
        if (instance == null)
            instance = new CrowdfundingManager ();
        return instance;
    }

    @Override
    public void onLoad() {
        CrowdfundingMessage lastMessage = getLastMessageFromRealm();
        if (lastMessage == null) requestLeaderAndFeed();
        else if (isCacheExpired()) requestFeed(lastMessage.getTimestamp());
    }

    private void requestLeaderAndFeed() {
        compositeSubscription.add(CrowdfundingClient.getLeaderAndFeed()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<CrowdfundingMessage>>() {
                @Override
                public void call(List<CrowdfundingMessage> crowdfundingMessages) {
                    Log.d("crowd", "ok");
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d("crowd", throwable.toString());
                }
            }));
    }

    private void requestFeed(String timestamp) {
        compositeSubscription.add(CrowdfundingClient.getFeed(timestamp)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<CrowdfundingMessage>>() {
                @Override
                public void call(List<CrowdfundingMessage> crowdfundingMessages) {
                    Log.d("crowd", "ok");
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d("crowd", throwable.toString());
                }
            }));
    }

    public Single<CrowdfundingMessage> saveCrowdfundingMessageToRealm(CrowdfundingClient.Message message) {
        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();
        CrowdfundingMessage result = realm.copyToRealmOrUpdate(messageToRealm(message));
        realm.commitTransaction();

        return Single.just(result);
    }

    public Single<List<CrowdfundingMessage>> saveCrowdfundingMessageToRealm(List<CrowdfundingClient.Message> messages) {

        RealmList<CrowdfundingMessage> realmMessages = new RealmList<>();
        for (CrowdfundingClient.Message message : messages) {
            realmMessages.add(messageToRealm(message));
        }

        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();
        List<CrowdfundingMessage> result = realm.copyToRealmOrUpdate(realmMessages);
        realm.commitTransaction();

        return Single.just(result);
    }

    private CrowdfundingMessage messageToRealm(CrowdfundingClient.Message message) {
        CrowdfundingMessage realmMessage = new CrowdfundingMessage(message.getUuid());
        realmMessage.setRead(false);
        realmMessage.setTimestamp(message.getTimestamp());

        for (CrowdfundingClient.Locale locale : message.getFeed()) {
            if ("en".equals(locale.getLocale())) realmMessage.setMessageEn(locale.getMessage());
            if ("ru".equals(locale.getLocale())) realmMessage.setMessageRu(locale.getMessage());
        }

        return realmMessage;
    }

    public RealmResults<CrowdfundingMessage> getAllMessages() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        return realm.where(CrowdfundingMessage.class).findAllSorted("timestamp");
    }

    public CrowdfundingMessage getLastMessageFromRealm() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = realm.where(CrowdfundingMessage.class).findAllSorted("timestamp");
        return messages.last();
    }

    public int getUnreadMessageCount() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        Long count = realm.where(CrowdfundingMessage.class).equalTo("read", false).count();
        return count.intValue();
    }

    public void reloadMessages() {
        removeAllMessages();
        requestLeaderAndFeed();
    }

    private void removeAllMessages() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = realm.where(CrowdfundingMessage.class).findAllSorted("timestamp");
        realm.beginTransaction();
        for (CrowdfundingMessage message : messages)
            message.deleteFromRealm();
        realm.commitTransaction();
    }

    private boolean isCacheExpired() {
        // TODO: 29.11.18 implement
        // expire date is 1 day
        return true;
    }

}
