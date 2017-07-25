package com.xabber.android.ui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.XMPPAccountAdapter;
import com.xabber.android.ui.color.BarPainter;

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

    private final static String LOG_TAG = XabberAccountInfoActivity.class.getSimpleName();

    private Toolbar toolbar;
    private BarPainter barPainter;
    private TextView tvAccountName;
    private TextView tvAccountJid;
    private RelativeLayout rlLogout;
    private RelativeLayout rlLogin;
    private RelativeLayout rlSync;
    private XMPPAccountAdapter adapter;
    private List<XMPPAccountSettings> xmppAccounts;
    private ProgressDialog progressDialog;

    // not verified
    private TextView tvNotVerified;
    private TextView tvNotVerifiedSummary;
    private RelativeLayout rlResend;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    @NonNull
    public static Intent createIntent(Context context) {
        return new Intent(context, XabberAccountInfoActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_xabber_account_info);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(R.string.xabber_account_title);
        barPainter = new BarPainter(this, toolbar);

        tvAccountName = (TextView) findViewById(R.id.tvAccountName);
        tvAccountJid = (TextView) findViewById(R.id.tvAccountJid);

        rlLogin = (RelativeLayout) findViewById(R.id.rlLogin);
        rlLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = XabberLoginActivity.createIntent(XabberAccountInfoActivity.this);
                startActivity(intent);
            }
        });

        rlLogout = (RelativeLayout) findViewById(R.id.rlLogout);
        rlLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        rlSync = (RelativeLayout) findViewById(R.id.rlSync);
        rlSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronize();
            }
        });

        tvNotVerifiedSummary = (TextView) findViewById(R.id.tvNotVerifiedSummary);
        tvNotVerified = (TextView) findViewById(R.id.tvNotVerified);
        rlResend = (RelativeLayout) findViewById(R.id.rlResend);
        rlResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(XabberAccountInfoActivity.this, "resend", Toast.LENGTH_SHORT).show();
            }
        });

        adapter = new XMPPAccountAdapter();
        xmppAccounts = new ArrayList<>();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rcvXmppUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
        adapter.setItems(xmppAccounts);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        barPainter.setDefaultColor();
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) {
            if (XabberAccount.STATUS_NOT_CONFIRMED.equals(account.getAccountStatus())) {
                showNotVerified();
            }
            if (XabberAccount.STATUS_CONFIRMED.equals(account.getAccountStatus())) {
                // show finish signup
            }
            if (XabberAccount.STATUS_REGISTERED.equals(account.getAccountStatus())) {
                String accountName = account.getFirstName() + " " + account.getLastName();
                tvAccountName.setText(accountName);
                tvAccountJid.setText(account.getUsername());
            }
        } else {
            showLogin();
        }
        List<XMPPAccountSettings> items = XabberAccountManager.getInstance().getXmppAccounts();
        if (items != null) {
            updateXmppAccounts(items);
        }
    }

    private void synchronize() {
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null && account.getToken() != null) {
            showProgress(getResources().getString(R.string.progress_title_sync));
            getAccount(account.getToken());
        } else {
            Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void getSettings() {
        Subscription getSettingsSubscription = AuthManager.getClientSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> s) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: successfully");
                        updateXmppAccounts(s);
                        hideProgress();
                        Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_success, Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "XMPP accounts loading from net: error: " + throwable.toString());
                        hideProgress();
                        Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
                    }
                });
        compositeSubscription.add(getSettingsSubscription);
    }

    private void getAccount(String token) {
        Subscription loadAccountsSubscription = AuthManager.getAccount(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount s) {
                        Log.d(LOG_TAG, "Xabber account loading from net: successfully");
                        String accountName = s.getFirstName() + " " + s.getLastName();
                        tvAccountName.setText(accountName);
                        tvAccountJid.setText(s.getUsername());
                        getSettings();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(LOG_TAG, "Xabber account loading from net: error: " + throwable.toString());
                        hideProgress();
                        Toast.makeText(XabberAccountInfoActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
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
        showProgress(getResources().getString(R.string.progress_title_logout));
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
        showLogin();
        hideProgress();
        Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
    }

    private void handleErrorLogout(Throwable throwable) {
        hideProgress();
        Toast.makeText(this, R.string.logout_fail, Toast.LENGTH_SHORT).show();
        Log.d(LOG_TAG, "Error while logout request: " + throwable.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    private void showProgress(String title) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title);
        progressDialog.setMessage(getResources().getString(R.string.progress_message));
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void showLogin() {
        // hide other elements
        setAccountVisibility(View.GONE);

        // show login button
        rlLogin.setVisibility(View.VISIBLE);

        // hide not verified
        tvNotVerified.setVisibility(View.GONE);
        tvNotVerifiedSummary.setVisibility(View.GONE);
        rlResend.setVisibility(View.GONE);
        CardView cardList = (CardView) findViewById(R.id.cardConfirmation);
        cardList.setVisibility(View.GONE);
    }

    private void showNotVerified() {
        // hide other elements
        setAccountVisibility(View.GONE);

        // hide login button
        rlLogin.setVisibility(View.GONE);

        // show not verified
        tvNotVerified.setVisibility(View.VISIBLE);
        tvNotVerifiedSummary.setVisibility(View.VISIBLE);
        rlResend.setVisibility(View.VISIBLE);
        CardView cardList = (CardView) findViewById(R.id.cardConfirmation);
        cardList.setVisibility(View.VISIBLE);
    }

    private void setAccountVisibility(int visibility) {
        // show account
        CardView cardList = (CardView) findViewById(R.id.cardList);
        LinearLayout llAccount = (LinearLayout) findViewById(R.id.llAccount);

        rlLogout.setVisibility(visibility);
        rlSync.setVisibility(visibility);
        cardList.setVisibility(visibility);
        llAccount.setVisibility(visibility);
    }
}

