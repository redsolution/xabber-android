package com.xabber.android.ui.helper;


import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
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
import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.roster.AbstractContact;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

public class ContactTitleExpandableToolbarInflater implements ObservableScrollViewCallbacks {

    private final AppCompatActivity activity;

    private View avatarView;
    private View titleView;
    private View contactNamePanel;

    private int toolbarHeight;
    private int paddingLeftMin;
    private int actionBarSize;
    private int toolbarHeightDelta;
    private int avatarLargeSize;
    private int avatarNormalSize;
    private int avatarRadius;
    private int contactTitlePaddingBottomBig;
    private int contactTitlePaddingBottomSmall;

    public ContactTitleExpandableToolbarInflater(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void onCreate(AbstractContact abstractContact) {
        activity.setContentView(R.layout.expandable_contact_title_activity);
        activity.setSupportActionBar((Toolbar) activity.findViewById(R.id.toolbar_overlay));
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);

        avatarView = activity.findViewById(R.id.avatar);
        contactNamePanel = activity.findViewById(R.id.contact_name_panel);

        titleView = activity.findViewById(R.id.expandable_contact_title);

        int[] accountActionBarColors = activity.getResources().getIntArray(R.array.account_action_bar);

        titleView.setBackgroundDrawable(new ColorDrawable(accountActionBarColors[
                AccountManager.getInstance().getColorLevel(abstractContact.getAccount())]));

        ContactTitleInflater.updateTitle(titleView, activity, abstractContact);

        int[] accountStatusBarColors = activity.getResources().getIntArray(R.array.account_status_bar);
        int colorLevel = AccountManager.getInstance().getColorLevel(abstractContact.getAccount());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(accountStatusBarColors[colorLevel]);
        }
    }

    public void onResume() {
        Resources resources = activity.getResources();
        paddingLeftMin = resources.getDimensionPixelSize(R.dimen.contact_title_padding_left);
        avatarLargeSize = resources.getDimensionPixelSize(R.dimen.avatar_large_size);
        avatarNormalSize = resources.getDimensionPixelSize(R.dimen.avatar_normal_size);
        avatarRadius = resources.getDimensionPixelSize(R.dimen.avatar_radius);
        contactTitlePaddingBottomBig = resources.getDimensionPixelSize(R.dimen.contact_title_padding_bottom_big);
        contactTitlePaddingBottomSmall = resources.getDimensionPixelSize(R.dimen.contact_title_padding_bottom_small);
        toolbarHeight = resources.getDimensionPixelSize(R.dimen.toolbar_height);

        actionBarSize = getActionBarSize();
        toolbarHeightDelta = toolbarHeight - actionBarSize;

        final ObservableScrollView scrollView = (ObservableScrollView) activity.findViewById(R.id.scroll);
        scrollView.setScrollViewCallbacks(this);

        ScrollUtils.addOnGlobalLayoutListener(activity.findViewById(R.id.expandable_contact_title), new Runnable() {
            @Override
            public void run() {
                updateFlexibleSpaceText(scrollView.getCurrentScrollY());
            }
        });
    }

    protected int getActionBarSize() {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
        int indexOfAttrTextSize = 0;
        TypedArray a = activity.obtainStyledAttributes(typedValue.data, textSizeAttr);
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

        titleView.setPadding(paddingLeft, 0, paddingLeft, 0);



    }

}
