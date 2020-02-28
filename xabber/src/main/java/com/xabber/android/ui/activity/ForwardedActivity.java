package com.xabber.android.ui.activity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.TypedValue;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
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
        statusBarPainter = new StatusBarPainter(this);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
            toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountRippleColor(account));
            toolbar.setTitleTextColor(Color.BLACK);
            statusBarPainter.updateWithAccountName(account);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
            TypedValue darkColor = new TypedValue();
            getTheme().resolveAttribute(R.attr.bars_color, darkColor, true);
            toolbar.setBackgroundColor(darkColor.data);
            toolbar.setTitleTextColor(Color.WHITE);
            statusBarPainter.updateWithColor(Color.BLACK);
        }
        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(ForwardedActivity.this));
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
        toolbar.setTitle(String.format(forwardedCount > 1 ?
                getString(R.string.forwarded_messages_count_plural) : getString(R.string.forwarded_messages_count) ,
                forwardedCount));
    }
}
