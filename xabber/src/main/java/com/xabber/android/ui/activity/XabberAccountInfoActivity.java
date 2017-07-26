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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
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
    private EditText edtCode;
    private Button btnConfirm;
    
    // complete register
    private EditText edtUsername;
    private EditText edtPass;
    private EditText edtPass2;
    private EditText edtFirstName;
    private EditText edtLastName;
    private EditText edtHost;
    private Button btnComplete;

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

        // not verified
        tvNotVerifiedSummary = (TextView) findViewById(R.id.tvNotVerifiedSummary);
        tvNotVerified = (TextView) findViewById(R.id.tvNotVerified);
        rlResend = (RelativeLayout) findViewById(R.id.rlResend);
        rlResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(XabberAccountInfoActivity.this, "resend", Toast.LENGTH_SHORT).show();
            }
        });
        edtCode = (EditText) findViewById(R.id.edtCode);
        btnConfirm = (Button) findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConfirmClick();
            }
        });
        
        // complete register
        edtUsername = (EditText) findViewById(R.id.edtUsername); 
        edtPass = (EditText) findViewById(R.id.edtPass); 
        edtPass2 = (EditText) findViewById(R.id.edtPass2); 
        edtFirstName = (EditText) findViewById(R.id.edtFirstName); 
        edtLastName = (EditText) findViewById(R.id.edtLastName); 
        edtHost = (EditText) findViewById(R.id.edtHost);
        btnComplete = (Button) findViewById(R.id.btnComplete);
        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCompleteClick();
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
                showCompleteRegister();
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
    
    private void onCompleteClick() {
        String username = edtUsername.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        String pass2 = edtPass2.getText().toString().trim();
        String firstName = edtFirstName.getText().toString().trim();
        String lastName = edtLastName.getText().toString().trim();
        String host = edtHost.getText().toString().trim();

        // TODO: 26.07.17 Username может содержать только латинские буквы a-z, цифры и точки.
        // Причём начинаться он должен с буквы, а две точки рядом находиться не могут.
        if (username.isEmpty()) {
            edtUsername.setError(getString(R.string.empty_field));
            return;
        }

        if (pass.isEmpty()) {
            edtPass.setError(getString(R.string.empty_field));
            return;
        }

        if (pass2.isEmpty()) {
            edtPass2.setError(getString(R.string.empty_field));
            return;
        }

        if (NetworkManager.isNetworkAvailable()) {
            completeRegister(username, pass, pass2, firstName, lastName, host);
        } else
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
    }

    private void onConfirmClick() {
        String code = edtCode.getText().toString().trim();

        if (code.isEmpty()) {
            edtCode.setError(getString(R.string.empty_field));
            return;
        }

        if (NetworkManager.isNetworkAvailable()) {
            confirm(code);
        } else
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
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

    private void confirm(String code) {
        showProgress(getResources().getString(R.string.progress_title_confirm));
        Subscription confirmSubscription = AuthManager.confirmEmail(code)
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
        showCompleteRegister();
        hideProgress();
        Toast.makeText(this, R.string.confirm_success, Toast.LENGTH_SHORT).show();
    }

    private void handleErrorConfirm(Throwable throwable) {
        Log.d(LOG_TAG, "Error while confirm request: " + throwable.toString());
        hideProgress();
        Toast.makeText(this, R.string.confirm_fail, Toast.LENGTH_SHORT).show();
    }

    private void completeRegister(String username, String pass, String pass2, String firstName, String lastName, String host) {
        showProgress(getResources().getString(R.string.progress_title_complete));
        Subscription completeSubscription = AuthManager.completeRegister(username, pass, pass2, firstName, lastName, host)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount s) {
                        handleSuccessComplete(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorComplete(throwable);
                    }
                });
        compositeSubscription.add(completeSubscription);
    }

    private void handleSuccessComplete(XabberAccount response) {
        showAccountCompleted(response);
        hideProgress();
        Toast.makeText(this, R.string.complete_success, Toast.LENGTH_SHORT).show();
    }

    private void handleErrorComplete(Throwable throwable) {
        Log.d(LOG_TAG, "Error while complete register request: " + throwable.toString());
        hideProgress();
        Toast.makeText(this, R.string.complete_fail, Toast.LENGTH_SHORT).show();
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
        CardView cardConfirm = (CardView) findViewById(R.id.cardConfirmation);
        cardConfirm.setVisibility(View.GONE);

        // hide complete register
        CardView cardComplete = (CardView) findViewById(R.id.cardCompleteRegister);
        cardComplete.setVisibility(View.GONE);
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
        CardView cardConfirm = (CardView) findViewById(R.id.cardConfirmation);
        cardConfirm.setVisibility(View.VISIBLE);

        // hide complete register
        CardView cardComplete = (CardView) findViewById(R.id.cardCompleteRegister);
        cardComplete.setVisibility(View.GONE);
    }

    private void showCompleteRegister() {
        // hide other elements
        setAccountVisibility(View.GONE);

        // hide login button
        rlLogin.setVisibility(View.GONE);

        // hide not verified
        tvNotVerified.setVisibility(View.GONE);
        tvNotVerifiedSummary.setVisibility(View.GONE);
        rlResend.setVisibility(View.GONE);
        CardView cardConfirm = (CardView) findViewById(R.id.cardConfirmation);
        cardConfirm.setVisibility(View.GONE);

        // show complete register
        CardView cardComplete = (CardView) findViewById(R.id.cardCompleteRegister);
        cardComplete.setVisibility(View.VISIBLE);
    }

    private void showAccountCompleted(XabberAccount account) {
        // show other elements
        setAccountVisibility(View.VISIBLE);

        // hide login button
        rlLogin.setVisibility(View.GONE);

        // hide not verified
        tvNotVerified.setVisibility(View.GONE);
        tvNotVerifiedSummary.setVisibility(View.GONE);
        rlResend.setVisibility(View.GONE);
        CardView cardConfirm = (CardView) findViewById(R.id.cardConfirmation);
        cardConfirm.setVisibility(View.GONE);

        // show complete register
        CardView cardComplete = (CardView) findViewById(R.id.cardCompleteRegister);
        cardComplete.setVisibility(View.GONE);

        if (account != null) {
            String accountName = account.getFirstName() + " " + account.getLastName();
            tvAccountName.setText(accountName);
            tvAccountJid.setText(account.getUsername());
        } else {
            showLogin();
        }
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

