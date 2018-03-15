package com.xabber.android.data.http;

import android.support.annotation.Nullable;
import android.util.Log;

import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.PatreonGoalRealm;
import com.xabber.android.data.database.realm.PatreonRealm;
import com.xabber.android.data.log.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 03.10.17.
 */

public class PatreonManager implements OnLoadListener {

    private static final String LOG_TAG = PatreonManager.class.getSimpleName();
    private static final int CACHE_LIFETIME = (int) TimeUnit.DAYS.toSeconds(1);

    private static PatreonManager instance;
    private XabberComClient.Patreon patreon;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static PatreonManager getInstance() {
        if (instance == null)
            instance = new PatreonManager();
        return instance;
    }

    public XabberComClient.Patreon getPatreon() {
        return patreon;
    }

    @Override
    public void onLoad() {
        this.patreon = loadPatreonFromRealm();
        if (patreon == null || isCacheExpired()) loadFromNet();
    }

    public void updatePatreonIfNeed() {
        if (isCacheExpired()) loadFromNet();
    }

    public void loadFromNet() {
        Subscription subscription = XabberComClient.getPatreon()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberComClient.Patreon>() {
                    @Override
                    public void call(XabberComClient.Patreon patreon) {
                        if (patreon != null)
                            SettingsManager.setLastPatreonLoadTimestamp(getCurrentTime());
                        handleSuccessGetPatreon(patreon);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorGetPatreon(throwable);
                    }
                });
        compositeSubscription.add(subscription);
    }

    private void handleSuccessGetPatreon(XabberComClient.Patreon patreon) {
        this.patreon = patreon;
    }

    private void handleErrorGetPatreon(Throwable throwable) {
        Log.d(LOG_TAG, "Error while loading patreon.json from net: " + throwable.toString());
    }

    public Single<XabberComClient.Patreon> savePatreonToRealm(XabberComClient.Patreon patreon) {

        RealmList<PatreonGoalRealm> patreonGoals = new RealmList<>();
        for (XabberComClient.PatreonGoal patreonGoal : patreon.getGoals()) {
            PatreonGoalRealm patreonGoalRealm = new PatreonGoalRealm();
            patreonGoalRealm.setGoal(patreonGoal.getGoal());
            patreonGoalRealm.setTitle(patreonGoal.getTitle());

            patreonGoals.add(patreonGoalRealm);
        }

        PatreonRealm patreonRealm = new PatreonRealm("1");
        patreonRealm.setPledged(patreon.getPledged());
        patreonRealm.setString(patreon.getString());
        patreonRealm.setGoals(patreonGoals);

        // TODO: 13.03.18 ANR - WRITE
        final long startTime = System.currentTimeMillis();
        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();
        PatreonRealm resultRealm = realm.copyToRealmOrUpdate(patreonRealm);
        realm.commitTransaction();
        XabberComClient.Patreon result = patreonRealmToDTO(resultRealm);
        realm.close();
        LogManager.d("REALM", Thread.currentThread().getName()
                + " save patreon data: " + (System.currentTimeMillis() - startTime));

        Log.d(LOG_TAG, "Patreon was saved to Realm");

        return Single.just(result);
    }

    @Nullable
    private XabberComClient.Patreon loadPatreonFromRealm() {
        XabberComClient.Patreon patreon = null;

        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<PatreonRealm> patreonRealms = realm.where(PatreonRealm.class).findAll();

        if (patreonRealms.size() > 0)
            patreon = patreonRealmToDTO(patreonRealms.get(0));

        realm.close();
        return patreon;
    }

    private XabberComClient.Patreon patreonRealmToDTO(PatreonRealm realmItem) {

        List<XabberComClient.PatreonGoal> patreonGoals = new ArrayList<>();
        for (PatreonGoalRealm patreonGoalRealm : realmItem.getGoals()) {
            XabberComClient.PatreonGoal patreonGoal =
                    new XabberComClient.PatreonGoal(patreonGoalRealm.getTitle(), patreonGoalRealm.getGoal());

            patreonGoals.add(patreonGoal);
        }

        return new XabberComClient.Patreon(
                realmItem.getString(),
                realmItem.getPledged(),
                patreonGoals);
    }

    private boolean isCacheExpired() {
        return getCurrentTime() > SettingsManager.getLastPatreonLoadTimestamp() + CACHE_LIFETIME;
    }

    public int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }
}
