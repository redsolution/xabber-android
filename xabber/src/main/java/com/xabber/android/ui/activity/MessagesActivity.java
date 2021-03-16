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
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.fragment.ForwardedFragment;

public class MessagesActivity extends ManagedActivity {

    private String messageId;
    private ContactJid user;
    private AccountJid account;
    private String action;

    private Toolbar toolbar;
    private Fragment fragment;

    public static final String ACTION_SHOW_FORWARDED = "com.xabber.android.ui.activity.ACTION_SHOW_FORWARDED";
    public static final String ACTION_SHOW_PINNED = "com.xabber.android.ui.activity.ACTION_SHOW_PINNED";

    private final static String KEY_MESSAGE_ID = "messageId";
    private final static String KEY_ACCOUNT = "account";
    private final static String KEY_USER = "user";

    public static Intent createIntentShowForwarded(Context context, String messageId, ContactJid user, AccountJid account) {
        Intent intent = new Intent(context, MessagesActivity.class);
        intent.putExtra(KEY_MESSAGE_ID, messageId);
        intent.putExtra(KEY_ACCOUNT, (Parcelable) account);
        intent.putExtra(KEY_USER, user);
        intent.setAction(ACTION_SHOW_FORWARDED);
        return intent;
    }

    public static Intent createIntentShowPinned(Context context, String messageId, ContactJid user, AccountJid account) {
        Intent intent = new Intent(context, MessagesActivity.class);
        intent.putExtra(KEY_MESSAGE_ID, messageId);
        intent.putExtra(KEY_ACCOUNT, (Parcelable) account);
        intent.putExtra(KEY_USER, user);
        intent.setAction(ACTION_SHOW_PINNED);
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
            action = intent.getAction();
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        StatusBarPainter statusBarPainter = new StatusBarPainter(this);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
            toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter()
                    .getAccountRippleColor(account));
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
        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(MessagesActivity.this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFragment(account, user);
    }

    private void initFragment(AccountJid account, ContactJid user) {
        if (fragment == null)
            fragment = ForwardedFragment.newInstance(account, user, messageId, action);

        FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragment);
        fTrans.commit();
    }

    public void setToolbar(int forwardedCount) {
        if (action.equals(ACTION_SHOW_FORWARDED)) {
            toolbar.setTitle(getResources().getQuantityString(
                    R.plurals.forwarded_messages_count, forwardedCount, forwardedCount));
        }
        else if (action.equals(ACTION_SHOW_PINNED)) toolbar.setTitle(getString(R.string.pinned_message));
    }
}
