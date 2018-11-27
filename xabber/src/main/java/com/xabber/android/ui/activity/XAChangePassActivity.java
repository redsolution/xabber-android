package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.ui.color.BarPainter;

import okhttp3.ResponseBody;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class XAChangePassActivity extends ManagedActivity {

    private Toolbar toolbar;

    private EditText edtOldPass;
    private TextInputLayout tilOldPass;
    private EditText edtPass;
    private TextInputLayout tilPass;
    private EditText edtConfirmPass;
    private TextInputLayout tilConfirmPass;
    private Button btnChange;
    private ProgressBar progressBar;

    protected CompositeSubscription compositeSubscription = new CompositeSubscription();

    @NonNull
    public static Intent createIntent(Context context) {
        return new Intent(context, XAChangePassActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_LightToolbar);
        setContentView(R.layout.activity_change_xa_pass);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitleTextColor(getResources().getColor(R.color.black_text));
        toolbar.setTitle(R.string.button_change_pass);
        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setLiteGrey();

        progressBar = findViewById(R.id.progressBar);
        edtOldPass = findViewById(R.id.edtOldPass);
        tilOldPass = findViewById(R.id.tilOldPass);
        edtPass = findViewById(R.id.edtPass);
        tilPass = findViewById(R.id.tilPass);
        edtConfirmPass = findViewById(R.id.edtConfirmPass);
        tilConfirmPass = findViewById(R.id.tilConfirmPass);
        btnChange = findViewById(R.id.btnChange);
        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChangePassClick(edtOldPass, edtPass, edtConfirmPass);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        compositeSubscription.clear();
    }

    public void onChangePassClick(EditText edtOldPass, EditText edtPass, EditText edtConfirmPass) {
        String oldPass = edtOldPass.getText().toString();
        oldPass = oldPass.trim();

        String pass = edtPass.getText().toString();
        pass = pass.trim();

        String confirmPass = edtConfirmPass.getText().toString();
        confirmPass = confirmPass.trim();

        if (oldPass.isEmpty()) {
            tilOldPass.setError(getString(R.string.empty_field));
            return;
        } else tilOldPass.setError(null);

        if (oldPass.length() < 4) {
            tilOldPass.setError(getString(R.string.pass_too_short));
            return;
        } else tilOldPass.setError(null);

        if (pass.isEmpty()) {
            tilPass.setError(getString(R.string.empty_field));
            return;
        } else tilPass.setError(null);

        if (pass.length() < 4) {
            tilPass.setError(getString(R.string.pass_too_short));
            return;
        } else tilPass.setError(null);

        if (confirmPass.isEmpty()) {
            tilConfirmPass.setError(getString(R.string.empty_field));
            return;
        } else tilConfirmPass.setError(null);

        if (confirmPass.length() < 4) {
            tilConfirmPass.setError(getString(R.string.pass_too_short));
            return;
        } else tilConfirmPass.setError(null);

        if (!pass.equals(confirmPass)) {
            tilConfirmPass.setError(getString(R.string.passwords_not_match));
            return;
        } else tilConfirmPass.setError(null);

        if (checkInternetOrShowError()) changePass(oldPass, pass, confirmPass);
    }

    private void changePass(String oldPass, String pass, String confirmPass) {
        showProgress(true);
        compositeSubscription.add(AuthManager.changePassword(oldPass, pass, confirmPass)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        handleSuccessChangePass();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorChangePass();
                    }
                }));
    }

    private void handleSuccessChangePass() {
        Toast.makeText(this, R.string.password_changed_success, Toast.LENGTH_SHORT).show();
        showProgress(false);
        finish();
    }

    private void handleErrorChangePass() {
        Toast.makeText(this, R.string.password_changed_fail, Toast.LENGTH_SHORT).show();
        showProgress(false);
    }

    private boolean checkInternetOrShowError() {
        if (NetworkManager.isNetworkAvailable()) return true;
        else {
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnChange.setEnabled(!show);
    }
}
