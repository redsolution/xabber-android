package com.xabber.android.data.http;

import android.util.Log;

import com.xabber.android.data.OnLoadListener;

import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 03.10.17.
 */

public class PatreonManager implements OnLoadListener {

    private static final String LOG_TAG = PatreonManager.class.getSimpleName();

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
        loadFromNet();
    }

    private void loadFromNet() {
        Subscription subscription = XabberComClient.getPatreon()
                .subscribe(new Action1<XabberComClient.Patreon>() {
                    @Override
                    public void call(XabberComClient.Patreon patreon) {
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
}
