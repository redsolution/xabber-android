package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.xaccount.HttpApiManager;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.TutorialAdapter;
import com.xabber.android.ui.preferences.PreferenceEditor;

import me.relex.circleindicator.CircleIndicator;

/**
 * Created by valery.miller on 14.09.17.
 */

public class TutorialActivity extends ManagedActivity {

    private Button btnLogin;
    private Button btnRegister;
    private ImageView ivSettings;

    public static Intent createIntent(Context context) {
        return new Intent(context, TutorialActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_tutorial);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

        FragmentPagerAdapter pagerAdapter = new TutorialAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);

        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(XabberLoginActivity.createIntent(TutorialActivity.this, XabberLoginActivity.FRAGMENT_LOGIN));
            }
        });

        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.FLAVOR_destribution.equals("store")) {
                    startActivity(XabberLoginActivity.createIntent(
                            TutorialActivity.this, XabberLoginActivity.FRAGMENT_SIGNUP_STEP1));
                } else {
                    String url = HttpApiManager.XABBER_SIGNUP_URL;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                }
            }
        });

        ivSettings = (ImageView) findViewById(R.id.ivSettings);
        ivSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PreferenceEditor.createIntent(TutorialActivity.this));
            }
        });
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
