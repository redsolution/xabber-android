package com.xabber.android.data.database;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnClearListener;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import rx.Emitter;
import rx.Observable;

public class DatabaseManager implements OnClearListener, OnCloseListener {

    private static final int CURRENT_DATABASE_VERSION = 0;
    private static final String DATABASE_NAME = "realm_db_xabber";
    private static final String LOG_TAG = DatabaseManager.class.getSimpleName();

    private static DatabaseManager instance;
    private Observable<Realm> observableListenerInstance;
    private Observable<Realm> observableChatListenerInstance;
    private RealmConfiguration realmConfiguration;

    private Realm realmInstanceInUI;

    private DatabaseManager(){
        Realm.init(Application.getInstance().getApplicationContext());
        realmConfiguration = createRealmConfiguration();
        Realm.setDefaultConfiguration(realmConfiguration);
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

        return result;
    }

    public Observable<Realm> getObservableListener(){ //TODO pay attention!
        if (observableListenerInstance == null)
            observableListenerInstance = Observable.create(realmEmitter -> {
                final Realm observableRealm = DatabaseManager.getInstance().getDefaultRealmInstance();
                final RealmChangeListener<Realm> listener = realmEmitter::onNext;
                observableRealm.addChangeListener(listener);
                realmEmitter.onNext(observableRealm);
                if (Looper.myLooper() != Looper.getMainLooper()) observableRealm.close();
            }, Emitter.BackpressureMode.LATEST);
        return observableListenerInstance;
    }

    public Observable<Realm> getObservableChatListener(){ //TODO pay attention!
        if (observableChatListenerInstance == null)
            observableChatListenerInstance = Observable.create(realmEmitter -> {
                final Realm observableRealm = DatabaseManager.getInstance().getDefaultRealmInstance();
                final RealmChangeListener<Realm> listener = realmEmitter::onNext;
                observableRealm.addChangeListener(listener);
                realmEmitter.onNext(observableRealm);
                if (Looper.myLooper() != Looper.getMainLooper()) observableRealm.close();
            }, Emitter.BackpressureMode.LATEST);
        return observableChatListenerInstance;
    }

    @Override
    public void onClear() { deleteRealmDatabase(); }

    @Override
    public void onClose() { Realm.compactRealm(Realm.getDefaultConfiguration()); }

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
