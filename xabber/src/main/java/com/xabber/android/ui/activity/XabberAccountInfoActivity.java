package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.XMPPAccountAdapter;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccountInfoActivity extends ManagedActivity {

    private final static String TAG = XabberAccountInfoActivity.class.getSimpleName();

    private TextView tvUsername;
    private Button btnLogout;
    private XMPPAccountAdapter adapter;
    private List<XMPPAccountSettings> xmppAccounts;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    @NonNull
    public static Intent createIntent(Context context) {
        return new Intent(context, XabberAccountInfoActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_xabber_account_info);

        tvUsername = (TextView) findViewById(R.id.tvUsername);

        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        Button btnSync = (Button) findViewById(R.id.btnSync);
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSettings();
            }
        });

        adapter = new XMPPAccountAdapter();
        xmppAccounts = new ArrayList<>();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rcvXmppUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) {
            tvUsername.setText(account.getUsername());
            adapter.setItems(xmppAccounts);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccountsFromRealm();
    }

    private void getSettings() {
        Subscription getSettingsSubscription = AuthManager.getClientSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        updateXmppAccounts(s);
                        Toast.makeText(XabberAccountInfoActivity.this, "success", Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(TAG, "Error while get settings: " + throwable.toString());
                        Toast.makeText(XabberAccountInfoActivity.this, "error", Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(getSettingsSubscription);
    }

    private void loadAccountsFromRealm() {
        Subscription loadAccountsSubscription = XabberAccountManager.getInstance().loadXMPPAccountSettingsFromRealm()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        updateXmppAccounts(s);
                        Toast.makeText(XabberAccountInfoActivity.this, "success from realm", Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(TAG, "Error while get settings: " + throwable.toString());
                        Toast.makeText(XabberAccountInfoActivity.this, "error", Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(loadAccountsSubscription);
    }

    public void updateXmppAccounts(List<XMPPAccountSettings> list) {
        xmppAccounts.clear();
        xmppAccounts.addAll(list);
        adapter.setItems(xmppAccounts);
    }

    private void logout() {
        Subscription logoutSubscription = AuthManager.logout()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        handleSuccessLogout(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorLogout(throwable);
                    }
                });
        compositeSubscription.add(logoutSubscription);
    }

    private void handleSuccessLogout(ResponseBody s) {
        XabberAccountManager.getInstance().removeAccount();
        Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show();
    }

    private void handleErrorLogout(Throwable throwable) {
        Toast.makeText(this, "Logout error", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Error while logout request: " + throwable.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }
}

