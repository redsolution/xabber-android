package com.xabber.android.data.http;

import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.PatreonGoalRealmObject;
import com.xabber.android.data.database.realmobjects.PatreonRealmObject;

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

        RealmList<PatreonGoalRealmObject> patreonGoals = new RealmList<>();
        for (XabberComClient.PatreonGoal patreonGoal : patreon.getGoals()) {
            PatreonGoalRealmObject patreonGoalRealmObject = new PatreonGoalRealmObject();
            patreonGoalRealmObject.setGoal(patreonGoal.getGoal());
            patreonGoalRealmObject.setTitle(patreonGoal.getTitle());

            patreonGoals.add(patreonGoalRealmObject);
        }

        PatreonRealmObject patreonRealmObject = new PatreonRealmObject("1");
        patreonRealmObject.setPledged(patreon.getPledged());
        patreonRealmObject.setString(patreon.getString());
        patreonRealmObject.setGoals(patreonGoals);

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        realm.beginTransaction();
        PatreonRealmObject resultRealm = realm.copyToRealmOrUpdate(patreonRealmObject);
        realm.commitTransaction();
        XabberComClient.Patreon result = patreonRealmToDTO(resultRealm);
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        Log.d(LOG_TAG, "Patreon was saved to Realm");
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        return Single.just(result);
    }

    @Nullable
    private XabberComClient.Patreon loadPatreonFromRealm() {
        XabberComClient.Patreon patreon = null;
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        RealmResults<PatreonRealmObject> patreonRealmObjects = realm
                .where(PatreonRealmObject.class)
                .findAll();

        if (patreonRealmObjects.size() > 0)
            patreon = patreonRealmToDTO(patreonRealmObjects.get(0));

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        return patreon;
    }

    private XabberComClient.Patreon patreonRealmToDTO(PatreonRealmObject realmItem) {

        List<XabberComClient.PatreonGoal> patreonGoals = new ArrayList<>();
        for (PatreonGoalRealmObject patreonGoalRealmObject : realmItem.getGoals()) {
            XabberComClient.PatreonGoal patreonGoal =
                    new XabberComClient.PatreonGoal(patreonGoalRealmObject.getTitle(), patreonGoalRealmObject.getGoal());

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
