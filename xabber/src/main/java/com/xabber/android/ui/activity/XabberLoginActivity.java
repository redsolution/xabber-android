package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XabberAccount;

import java.util.Collections;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 14.07.17.
 */

public class XabberLoginActivity extends ManagedActivity {

    private final static String TAG = XabberLoginActivity.class.getSimpleName();

    private EditText edtLogin;
    private EditText edtPass;
    private Button btnLogin;
    private RelativeLayout rlForgotPass;
    private ProgressBar progressBar;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static Intent createIntent(Context context) {
        return new Intent(context, XabberLoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_xabber_login);
        setStatusBarTranslucent();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        ImageView backgroundImage = (ImageView) findViewById(R.id.intro_background_image);
        edtLogin = (EditText) findViewById(R.id.edtLogin);
        edtPass = (EditText) findViewById(R.id.edtPass);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        rlForgotPass = (RelativeLayout) findViewById(R.id.rlForgotPass);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.red_700), PorterDuff.Mode.MULTIPLY);

        Glide.with(this)
                .load(R.drawable.intro_background)
                .centerCrop()
                .into(backgroundImage);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginClick();
            }
        });

        rlForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    private void onLoginClick() {
        String login = edtLogin.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if (login.isEmpty()) {
            edtLogin.setError("empty field");
            return;
        }

        if (pass.isEmpty()) {
            edtPass.setError("empty field");
            return;
        }

        if (NetworkManager.isNetworkAvailable()) {
            showProgress(true);
            login(login, pass);
        } else
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
    }

//    private void loginSocial(String provider, String token) {
//        Subscription loginSocialSubscription = AuthManager.loginSocial(this, provider, token)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Action1<ResponseBody>() {
//                    @Override
//                    public void call(ResponseBody s) {
//                        handleSuccessLogin(s);
//                    }
//                }, new Action1<Throwable>() {
//                    @Override
//                    public void call(Throwable throwable) {
//                        handleErrorSocialLogin(throwable);
//                    }
//                });
//        compositeSubscription.add(loginSocialSubscription);
//    }

    private void login(String login, String pass) {
        Subscription loginSubscription = AuthManager.login(this, login, pass)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        handleSuccessLogin(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorLogin(throwable);
                    }
                });
        compositeSubscription.add(loginSubscription);
    }

    private void handleSuccessLogin(@NonNull ResponseBody response) {
        getAccount();
    }

    private void handleErrorLogin(Throwable throwable) {
        Log.d(TAG, "Error while login request: " + throwable.toString());
        Toast.makeText(this, "Username or password is incorrect", Toast.LENGTH_SHORT).show();
        showProgress(false);
    }

//    private void handleErrorSocialLogin(Throwable throwable) {
//        Log.d(TAG, "Error while social login request: " + throwable.toString());
//        Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
//        // TODO: 19.07.17 временный костыль
//        getAccount();
//        showProgress(false);
//    }

    private void getAccount() {
        Subscription getAccountSubscription = AuthManager.getAccount(this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<XabberAccount>() {
                    @Override
                    public void call(XabberAccount xabberAccount) {
                        handleSuccessGetAccount(xabberAccount);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorLogin(throwable);
                    }
                });
        compositeSubscription.add(getAccountSubscription);
    }

    private void handleSuccessGetAccount(@NonNull XabberAccount xabberAccount) {
        Toast.makeText(this, xabberAccount.getId() + " " + xabberAccount.getFirstName() + " " + xabberAccount.getLastName(), Toast.LENGTH_LONG).show();
        showProgress(false);

        Intent intent = ContactListActivity.createIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finish();
        startActivity(intent);
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
        }
    }

    void setStatusBarTranslucent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
