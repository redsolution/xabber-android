package com.xabber.android.ui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.xaccount.XabberAccountManager;

public class IntroActivity extends BaseLoginActivity implements View.OnClickListener {

    private ProgressDialog progressDialog;

    public static Intent createIntent(Context context) {
        return new Intent(context, IntroActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_intro);

        Button btnSignIn = (Button) findViewById(R.id.btnSignIn);
        Button btnSignUp = (Button) findViewById(R.id.btnSignUp);
        ImageView ivFacebook = (ImageView) findViewById(R.id.ivFacebook);
        ImageView ivGoogle = (ImageView) findViewById(R.id.ivGoogle);
        ImageView ivTwitter = (ImageView) findViewById(R.id.ivTwitter);

        btnSignIn.setOnClickListener(this);
        btnSignUp.setOnClickListener(this);
        ivFacebook.setOnClickListener(this);
        ivGoogle.setOnClickListener(this);
        ivTwitter.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AccountManager.getInstance().hasAccounts() || XabberAccountManager.getInstance().getAccount() != null) {
            Intent intent = ContactListActivity.createIntent(this);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            finish();
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSignUp:
                startActivity(TutorialActivity.createIntent(IntroActivity.this));
                break;
            case R.id.btnSignIn:
                startActivity(AccountAddActivity.createIntent(IntroActivity.this));
                break;
            case R.id.ivFacebook:
                loginFacebook();
                break;
            case R.id.ivGoogle:
                loginGoogle();
                break;
            case R.id.ivTwitter:
                loginTwitter();
                break;
        }
    }

    @Override
    protected void showProgress(String title) {
        if (!isFinishing()) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(title);
            progressDialog.setMessage(getResources().getString(R.string.progress_message));
            progressDialog.show();
        }
    }

    @Override
    protected void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
