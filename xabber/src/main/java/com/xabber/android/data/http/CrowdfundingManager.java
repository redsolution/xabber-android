package com.xabber.android.data.http;

import com.xabber.android.data.OnLoadListener;

import java.util.List;

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

        // try get from realm

        // if null -> request leader from net, than feed
        // else check cache expired
            // if expired -> request feed from net

        requestCrowdfundingLeader();
    }

    private void requestCrowdfundingLeader() {
        compositeSubscription.add(CrowdfundingClient.getLeader()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<CrowdfundingClient.Message>() {
                    @Override
                    public void call(CrowdfundingClient.Message message) {
                        requestCrowdfundingFeed(message.getTimestamp());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                }));
    }

    private void requestCrowdfundingFeed(String lastTimestamp) {
        compositeSubscription.add(CrowdfundingClient.getFeed()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<CrowdfundingClient.Message>>() {
                    @Override
                    public void call(List<CrowdfundingClient.Message> messages) {

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                }));
    }

}
