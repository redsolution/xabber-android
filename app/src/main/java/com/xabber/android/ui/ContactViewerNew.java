package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;


public class ContactViewerNew extends ManagedActivity implements ObservableScrollViewCallbacks {

    private int toolbarHeight;
    private View avatarView;
    private View titleView;
    private int paddingLeftMin;
    private int paddingRight;
    private int actionBarSize;
    private int toolbarHeightDelta;
    private int avatarLargeSize;
    private int avatarNormalSize;
    private int avatarRadius;
    private View contactNamePanel;
    private int contactTitlePaddingBottomBig;
    private int contactTitlePaddingBottomSmall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact_viewer);
        setSupportActionBar((Toolbar) findViewById(R.id.contact_viewer_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        int[] accountActionBarColors;
        int[] accountStatusBarColors;

        accountActionBarColors = getResources().getIntArray(R.array.account_action_bar);
        accountStatusBarColors = getResources().getIntArray(R.array.account_status_bar);

        titleView = findViewById(R.id.title);
        avatarView = findViewById(R.id.avatar);
        contactNamePanel = findViewById(R.id.contact_name_panel);

        paddingLeftMin = getResources().getDimensionPixelSize(R.dimen.contact_title_padding_left);
        paddingRight = getResources().getDimensionPixelSize(R.dimen.contact_title_padding_right);
        avatarLargeSize = getResources().getDimensionPixelSize(R.dimen.avatar_large_size);
        avatarNormalSize = getResources().getDimensionPixelSize(R.dimen.avatar_normal_size);
        avatarRadius = getResources().getDimensionPixelSize(R.dimen.avatar_radius);

        contactTitlePaddingBottomBig = getResources().getDimensionPixelSize(R.dimen.contact_title_padding_bottom_big);
        contactTitlePaddingBottomSmall = getResources().getDimensionPixelSize(R.dimen.contact_title_padding_bottom_small);

        TypedArray a = getTheme().obtainStyledAttributes(R.style.Theme, new int[]{R.attr.colorPrimary});

        AbstractContact bestContact = RosterManager.getInstance().getBestContact(getAccount(getIntent()), getUser(getIntent()));

        ContactTitleInflater.updateTitle(titleView, this, bestContact);

        int colorLevel = AccountManager.getInstance().getColorLevel(bestContact.getAccount());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(accountStatusBarColors[colorLevel]);
        }

        titleView.setBackgroundDrawable(new ColorDrawable(accountActionBarColors[colorLevel]));

        toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);

        final ObservableScrollView scrollView = (ObservableScrollView) findViewById(R.id.scroll);
        scrollView.setScrollViewCallbacks(this);

        ScrollUtils.addOnGlobalLayoutListener(findViewById(R.id.contact_viewer_toolbar), new Runnable() {
            @Override
            public void run() {
                updateFlexibleSpaceText(scrollView.getCurrentScrollY());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        actionBarSize = getActionBarSize();
        toolbarHeightDelta = toolbarHeight - actionBarSize;
    }

    protected int getActionBarSize() {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
        int indexOfAttrTextSize = 0;
        TypedArray a = obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();
        return actionBarSize;
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        updateFlexibleSpaceText(scrollY);
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
    }

    private void updateFlexibleSpaceText(final int scrollY) {
        setLeftPadding(scrollY);
        setTopPadding(scrollY);
        setAvatarSize(scrollY);
        setHeight(scrollY);
    }

    private void setTopPadding(int scrollY) {
        int paddingDelta = contactTitlePaddingBottomBig - contactTitlePaddingBottomSmall;
        int paddingBottom = contactTitlePaddingBottomBig - scrollY * paddingDelta / toolbarHeightDelta;

        if (scrollY <= 0) {
            paddingBottom = contactTitlePaddingBottomBig;
        }

        if (scrollY >= toolbarHeightDelta) {
            paddingBottom = contactTitlePaddingBottomSmall;
        }

        contactNamePanel.setPadding(0, 0, 0, paddingBottom);
    }

    private void setAvatarSize(int scrollY) {
        int newAvatarSize =  avatarLargeSize - (scrollY / 2);

        if (newAvatarSize < avatarNormalSize) {
            newAvatarSize = avatarNormalSize;
        }

        if (avatarView.getWidth() != newAvatarSize) {
            avatarView.getLayoutParams().width = newAvatarSize;
            avatarView.getLayoutParams().height = newAvatarSize;
        }
    }

    private void setHeight(int scrollY) {
        int newHeight = toolbarHeight - scrollY;
        if (newHeight < actionBarSize) {
            newHeight = actionBarSize;
        }

        titleView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, newHeight));
    }

    private void setLeftPadding(int scrollY) {
        int paddingLeft = (int) round(sqrt(pow(avatarRadius, 2) - pow(scrollY - avatarRadius, 2)));

        if (scrollY < 0) {
            paddingLeft = paddingLeftMin;
        }

        if (scrollY > avatarRadius) {
            paddingLeft = avatarRadius;
        }

        if (paddingLeft < paddingLeftMin) {
            paddingLeft = paddingLeftMin;
        }

        titleView.setPadding(paddingLeft, 0, paddingRight, 0);
    }

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactViewerNew.class).setAccount(account).setUser(user).build();
    }
    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

}
