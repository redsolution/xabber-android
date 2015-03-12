package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.widget.RelativeLayout;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleActionBarInflater;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;


public class ContactViewerNew extends ManagedActivity implements ObservableScrollViewCallbacks {

    private Toolbar toolbar;
    private int toolbarHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_contact_viewer);

        setSupportActionBar((Toolbar) findViewById(R.id.contact_viewer_toolbar));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ContactTitleActionBarInflater contactTitleActionBarInflater = new ContactTitleActionBarInflater(this);
        contactTitleActionBarInflater.setUpActionBarView();
        contactTitleActionBarInflater.update(RosterManager.getInstance().getBestContact(getAccount(getIntent()), getUser(getIntent())));


        toolbar = (Toolbar)findViewById(R.id.contact_viewer_toolbar);

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
        int actionBarSize = getActionBarSize();

        int newHeight = toolbarHeight - scrollY;
        if (newHeight < actionBarSize) {
            newHeight = actionBarSize;
        }

        toolbar.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, newHeight));
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
