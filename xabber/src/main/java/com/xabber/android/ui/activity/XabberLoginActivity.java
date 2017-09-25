package com.xabber.android.ui.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.xaccount.HttpApiManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.XabberLoginFragment;


/**
 * Created by valery.miller on 14.07.17.
 */

public class XabberLoginActivity extends BaseLoginActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = XabberLoginActivity.class.getSimpleName();
    public final static String CURRENT_FRAGMENT = "current_fragment";
    public final static String FRAGMENT_LOGIN = "fragment_login";
    public final static String FRAGMENT_SIGNUP = "fragment_signup";

    private Fragment fragmentLogin;
    private FragmentTransaction fTrans;
    private String currentFragment = FRAGMENT_LOGIN;

    private ProgressDialog progressDialog;

    private ImageView ivFacebook;
    private ImageView ivGoogle;
    private ImageView ivTwitter;
    private ImageView ivGithub;

    private BarPainter barPainter;

    public static Intent createIntent(Context context) {
        return new Intent(context, XabberLoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            currentFragment = savedInstanceState.getString(CURRENT_FRAGMENT);
        else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) currentFragment = extras.getString(CURRENT_FRAGMENT);
        }

        setContentView(R.layout.activity_xabber_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setTitle(R.string.title_login_xabber_account);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        barPainter = new BarPainter(this, toolbar);

        ivFacebook = (ImageView) findViewById(R.id.ivFacebook);
        ivGoogle = (ImageView) findViewById(R.id.ivGoogle);
        ivTwitter = (ImageView) findViewById(R.id.ivTwitter);
        ivGithub = (ImageView) findViewById(R.id.ivGithub);

        ivFacebook.setOnClickListener(this);
        ivGoogle.setOnClickListener(this);
        ivTwitter.setOnClickListener(this);
        ivGithub.setOnClickListener(this);
    }

    public void showLoginFragment() {
        if (fragmentLogin == null)
            fragmentLogin = new XabberLoginFragment();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentLogin);
        fTrans.commit();
        currentFragment = FRAGMENT_LOGIN;
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLoginFragment();
        barPainter.setBlue(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FRAGMENT, currentFragment);
    }

    @Override
    public void onClick(View v) {
        if (NetworkManager.isNetworkAvailable()) {
            switch (v.getId()) {
                case R.id.ivFacebook:
                    loginFacebook();
                    break;
                case R.id.ivGoogle:
                    loginGoogle();
                    break;
                case R.id.ivGithub:
                    loginGithub();
                    break;
                case R.id.ivTwitter:
                    loginTwitter();
                    break;
            }
        } else
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
    }

    public void onForgotPassClick() {
        String url = HttpApiManager.XABBER_FORGOT_PASS_URL;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
