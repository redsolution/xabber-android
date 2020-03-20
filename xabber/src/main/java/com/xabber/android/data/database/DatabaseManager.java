package com.xabber.android.data.database;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnClearListener;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnScreenListener;
import com.xabber.android.data.log.LogManager;

import org.jetbrains.annotations.NotNull;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import rx.Emitter;
import rx.Observable;

public class DatabaseManager implements OnClearListener, OnCloseListener, OnScreenListener {

    private static final int CURRENT_DATABASE_VERSION = 0;
    private static final String DATABASE_NAME = "realm_db_xabber";
    private static final String LOG_TAG = DatabaseManager.class.getSimpleName();

    private static DatabaseManager instance;
    private Observable<Realm> observableListenerInstance;
    private RealmConfiguration realmConfiguration;

    private Realm realmInstanceInUI;

    private int prevGlobalInstCount;
    private int prevLocalInstCount;

    private DatabaseManager(){
        Realm.init(Application.getInstance().getApplicationContext());
        realmConfiguration = createRealmConfiguration();
        Realm.setDefaultConfiguration(realmConfiguration);

        prevGlobalInstCount = 0;
        prevLocalInstCount = 0;
    }

    public static DatabaseManager getInstance(){
        if ( instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Realm getDefaultRealmInstance(){
        Realm result;

        if (Looper.myLooper() == Looper.getMainLooper()){
            if (realmInstanceInUI == null) realmInstanceInUI = Realm.getInstance(Realm.getDefaultConfiguration());
            result = realmInstanceInUI;
        } else result = Realm.getDefaultInstance();

//        int localInstances = Realm.getLocalInstanceCount(Realm.getDefaultConfiguration());
//        int instances = Realm.getGlobalInstanceCount(Realm.getDefaultConfiguration());
//        if (prevGlobalInstCount < instances || prevLocalInstCount < localInstances){
//            LogManager.e("DatabaseManager AHTUNG! Instances count was changed! ", "");
//            LogManager.exception("\t", new Exception());
//        }
//
//        prevLocalInstCount = localInstances;
//        prevGlobalInstCount = instances;

        return result;
    }

    public Observable<Realm> getObservableListener(){ //TODO pay attention!
        if (observableListenerInstance == null) observableListenerInstance = Observable.create(realmEmitter -> {
            final Realm observableRealm = DatabaseManager.getInstance().getDefaultRealmInstance();
            final RealmChangeListener<Realm> listener = realmEmitter::onNext;
            observableRealm.addChangeListener(listener);
            realmEmitter.onNext(observableRealm);
            if (Looper.myLooper() != Looper.getMainLooper()) observableRealm.close();
        }, Emitter.BackpressureMode.LATEST);
        return observableListenerInstance;
    }

    @Override
    public void onClear() { deleteRealmDatabase(); }

    @Override
    public void onClose() { Realm.compactRealm(Realm.getDefaultConfiguration()); }

    @Override
    public void onScreenStateChanged(@NotNull ScreenState screenState) {
        if (screenState == ScreenState.OFF)
            LogManager.d(LOG_TAG, "Screen state changed! Running compacting Realm.");
            Realm.compactRealm(Realm.getDefaultConfiguration());
    }

    private RealmConfiguration createRealmConfiguration(){
        return new RealmConfiguration.Builder()
                .name(DATABASE_NAME)
                .compactOnLaunch()
                .schemaVersion(CURRENT_DATABASE_VERSION)
                .deleteRealmIfMigrationNeeded() //TODO DELETE THIS
                .build();
    }

    private void deleteRealmDatabase(){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.deleteRealm(realm.getConfiguration());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null ) realm.close(); }
        });
    }

    public RealmConfiguration getDefaultRealmConfig() {
        return realmConfiguration;
    }
}
