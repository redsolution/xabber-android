package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccountInfoActivity extends ManagedActivity {

    TextView tvUsername;
    Button btnLogout;

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
                // logout
            }
        });

        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) {
            tvUsername.setText(account.getUsername());
        }
    }
}

