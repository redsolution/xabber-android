package com.xabber.android.data.http;

import android.util.Log;

import com.xabber.android.BuildConfig;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.message.NewMessageEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CrowdfundingManager {

    private static final int CACHE_LIFETIME = (int) TimeUnit.DAYS.toSeconds(1);
    private static final int CROWDFUNDING_DELAY = BuildConfig.FLAVOR.equals("dev") ?
            (int) TimeUnit.MINUTES.toSeconds(1) : (int) TimeUnit.HOURS.toSeconds(1);

    private static CrowdfundingManager instance;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private Timer timer;

    public static CrowdfundingManager getInstance() {
        if (instance == null)
            instance = new CrowdfundingManager ();
        return instance;
    }

    public void onLoad() {
        CrowdfundingMessage lastMessage = getLastMessageFromRealm();
        if (lastMessage == null) {
            if (SettingsManager.getFirstAppRunTimestamp() == 0)
                SettingsManager.setFirstAppRunTimestamp(getCurrentTime());
            else if (isTimeToCrowdfunding() && isLeaderCacheExpired()) requestLeader();
        }
        else if (!CrowdfundingManager.getInstance().haveDelayedMessages() && isCacheExpired())
            requestFeed(lastMessage.getReceivedTimestamp());
    }

    private void requestLeader() {
        compositeSubscription.add(CrowdfundingClient.getLeader()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<CrowdfundingMessage>>() {
                @Override
                public void call(List<CrowdfundingMessage> crowdfundingMessages) {
                    Log.d("crowd", "ok");
                    SettingsManager.setLastLeaderCrowdfundingLoadTimestamp(getCurrentTime());
                    EventBus.getDefault().post(new NewMessageEvent());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d("crowd", throwable.toString());
                }
            }));
    }

    public void startUpdateTimer(final int delay, final int step) {
        if (timer != null) timer.cancel();
        if (!CrowdfundingManager.getInstance().haveDelayedMessages()) {
            CrowdfundingMessage lastMessage = getLastMessageFromRealm();
            if (lastMessage != null && isCacheExpired()) requestFeed(lastMessage.getReceivedTimestamp());
            return;
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                CrowdfundingManager.getInstance().removeDelay(delay + step);
                EventBus.getDefault().post(new NewMessageEvent());
                startUpdateTimer(delay + step, step);
            }
        }, step * 1000);
    }

    private void requestFeed(int timestamp) {
        compositeSubscription.add(CrowdfundingClient.getFeed(timestamp)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<CrowdfundingMessage>>() {
                @Override
                public void call(List<CrowdfundingMessage> crowdfundingMessages) {
                    Log.d("crowd", "ok");
                    SettingsManager.setLastCrowdfundingLoadTimestamp(getCurrentTime());
                    EventBus.getDefault().post(new NewMessageEvent());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d("crowd", throwable.toString());
                }
            }));
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
        realmMessage.setDelay(message.getDelay());
        realmMessage.setLeader(message.isLeader());
        realmMessage.setTimestamp(message.getTimestamp());
        realmMessage.setReceivedTimestamp(getCurrentTime());
        realmMessage.setAuthorAvatar(message.getAuthor().getAvatar());
        realmMessage.setAuthorJid(message.getAuthor().getJabberId());

        for (CrowdfundingClient.LocalizedMessage locale : message.getFeed()) {
            if ("en".equals(locale.getLocale())) realmMessage.setMessageEn(locale.getMessage());
            if ("ru".equals(locale.getLocale())) realmMessage.setMessageRu(locale.getMessage());
        }

        for (CrowdfundingClient.LocalizedName name : message.getAuthor().getName()) {
            if ("en".equals(name.getLocale())) realmMessage.setAuthorNameEn(name.getName());
            if ("ru".equals(name.getLocale())) realmMessage.setAuthorNameRu(name.getName());
        }

        return realmMessage;
    }

    public boolean haveDelayedMessages() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        CrowdfundingMessage message = realm.where(CrowdfundingMessage.class)
                .notEqualTo("delay", 0).findFirst();
        return message != null;
    }

    public RealmResults<CrowdfundingMessage> getMessagesWithDelay(int delay) {
        Realm realm = RealmManager.getInstance().getNewRealm();
        return realm.where(CrowdfundingMessage.class)
                .lessThanOrEqualTo("delay", delay)
                .findAllSorted("timestamp");
    }

    public void removeDelay(int delay) {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = getMessagesWithDelay(delay);
        realm.beginTransaction();
        for (CrowdfundingMessage message : messages) {
            // remove delay and update received time
            message.setDelay(0);
            message.setReceivedTimestamp(getCurrentTime());
        }
        realm.commitTransaction();
        realm.close();
    }

    public CrowdfundingMessage getLastMessageFromRealm() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = realm.where(CrowdfundingMessage.class).findAllSorted("timestamp");
        if (messages != null && !messages.isEmpty()) return messages.last();
        else return null;
    }

    public CrowdfundingMessage getLastNotDelayedMessageFromRealm() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = realm.where(CrowdfundingMessage.class)
                .equalTo("delay", 0)
                .findAllSorted("timestamp");
        if (messages != null && !messages.isEmpty()) return messages.last();
        else return null;
    }

    public int getUnreadMessageCount() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        Long count = realm.where(CrowdfundingMessage.class).equalTo("read", false)
                .equalTo("delay", 0).count();
        return count.intValue();
    }

    public Observable<RealmResults<CrowdfundingMessage>> getUnreadMessageCountAsObservable() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        return realm.where(CrowdfundingMessage.class).equalTo("read", false)
                .equalTo("delay", 0).findAll().asObservable();
    }

    public void reloadMessages() {
        removeAllMessages();
        requestLeader();
        SettingsManager.setFirstAppRunTimestamp(0);
        SettingsManager.setLastCrowdfundingLoadTimestamp(1);
        SettingsManager.setLastLeaderCrowdfundingLoadTimestamp(1);
    }

    /** Ignore business rules. Use only for debug */
    public void fetchFeedForDebug() {
        CrowdfundingMessage lastMessage = getLastMessageFromRealm();
        if (lastMessage != null) requestFeed(lastMessage.getReceivedTimestamp());
    }

    public void markMessagesAsRead(String[] ids) {
        if (ids.length == 0) return;
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = realm.where(CrowdfundingMessage.class)
                .equalTo("read", false).in("id", ids).findAll();

        realm.beginTransaction();
        for (CrowdfundingMessage message : messages) {
            message.setRead(true);
        }
        realm.commitTransaction();
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
        return getCurrentTime() > SettingsManager.getLastCrowdfundingLoadTimestamp() + CACHE_LIFETIME;
    }

    private boolean isLeaderCacheExpired() {
        if (SettingsManager.getLastLeaderCrowdfundingLoadTimestamp() == 1) return true;
        else return getCurrentTime() > SettingsManager.getLastLeaderCrowdfundingLoadTimestamp() + CACHE_LIFETIME;
    }

    private boolean isTimeToCrowdfunding() {
        return getCurrentTime() > SettingsManager.getFirstAppRunTimestamp() + CROWDFUNDING_DELAY;
    }

    public int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

}
