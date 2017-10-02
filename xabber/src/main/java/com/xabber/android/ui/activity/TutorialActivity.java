package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.xabber.android.R;
import com.xabber.android.ui.adapter.TutorialAdapter;

import me.relex.circleindicator.CircleIndicator;

/**
 * Created by valery.miller on 14.09.17.
 */

public class TutorialActivity extends AppCompatActivity {

    private Button btnLogin;
    private Button btnRegister;

    public static Intent createIntent(Context context) {
        return new Intent(context, TutorialActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tutorial);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

        FragmentPagerAdapter pagerAdapter = new TutorialAdapter(getFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);

        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(XabberLoginActivity.createIntent(TutorialActivity.this));
            }
        });

        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = XabberAccountInfoActivity.createIntent(TutorialActivity.this);
                intent.putExtra(XabberAccountInfoActivity.CALL_FROM, XabberAccountInfoActivity.CALL_FROM_LOGIN);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
