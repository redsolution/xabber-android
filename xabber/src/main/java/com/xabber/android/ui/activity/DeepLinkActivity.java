package com.xabber.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.HttpApiManager;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class DeepLinkActivity extends BaseLoginActivity {

    protected CompositeSubscription compositeSubscription = new CompositeSubscription();
    private final static String LOG_TAG = DeepLinkActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deeplink);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        compositeSubscription.clear();
    }

    @Override
    protected void onSocialAuthSuccess(String provider, String credentials) { }

    @Override
    protected void showProgress(String title) { }

    @Override
    protected void hideProgress() { }

    @Override
    protected void onSynchronized() {

    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String appLinkAction = intent.getAction();
        Uri appLinkData = intent.getData();
        if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null) {
            String uri = appLinkData.toString();
            String key = uri.replace(HttpApiManager.getXabberEmailConfirmUrl(), "");
            key = key.replace("/", "");

            XabberAccount account = XabberAccountManager.getInstance().getAccount();
            if (account != null) confirmEmailWithKey(key);
            else Toast.makeText(this, R.string.need_login_to_continue,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** CONFIRM EMAIL */

    protected void confirmEmailWithKey(String key) {
        showProgress(getResources().getString(R.string.progress_title_confirm));
        Subscription confirmSubscription = AuthManager.confirmEmailWithKey(key)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount s) {
                        handleSuccessConfirm(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorConfirm(throwable);
                    }
                });
        compositeSubscription.add(confirmSubscription);
    }

    private void handleSuccessConfirm(XabberAccount response) {
        Toast.makeText(this, R.string.confirm_success, Toast.LENGTH_SHORT).show();
        synchronize(false);
        finish();
    }

    private void handleErrorConfirm(Throwable throwable) {
        handleError(throwable, "Error while confirming email: ", LOG_TAG);
        finish();
    }
}
