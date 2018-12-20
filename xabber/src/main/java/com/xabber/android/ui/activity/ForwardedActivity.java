package com.xabber.android.ui.activity;


import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.fragment.ForwardedFragment;

public class ForwardedActivity extends ManagedActivity {

    private String messageId;
    private UserJid user;
    private AccountJid account;

    private Toolbar toolbar;
    private Fragment fragment;

    private StatusBarPainter statusBarPainter;

    private final static String KEY_MESSAGE_ID = "messageId";
    private final static String KEY_ACCOUNT = "account";
    private final static String KEY_USER = "user";

    public static Intent createIntent(Context context, String messageId, UserJid user, AccountJid account) {
        Intent intent = new Intent(context, ForwardedActivity.class);
        intent.putExtra(KEY_MESSAGE_ID, messageId);
        intent.putExtra(KEY_ACCOUNT, (Parcelable) account);
        intent.putExtra(KEY_USER, user);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forwarded);

        Intent intent = getIntent();
        if (intent != null) {
            messageId = intent.getStringExtra(KEY_MESSAGE_ID);
            account = intent.getParcelableExtra(KEY_ACCOUNT);
            user = intent.getParcelableExtra(KEY_USER);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(ForwardedActivity.this);
            }
        });
        statusBarPainter = new StatusBarPainter(this);
        statusBarPainter.updateWithDefaultColor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFragment(account, user);
    }

    private void initFragment(AccountJid account, UserJid user) {
        if (fragment == null)
            fragment = ForwardedFragment.newInstance(account, user, messageId);

        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragment);
        fTrans.commit();
    }

    public void setToolbar(int forwardedCount) {
        toolbar.setTitle(String.format(getString(R.string.forwarded_messages_count), forwardedCount));
    }

}
