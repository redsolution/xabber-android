package com.xabber.android.ui.helper;

import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.androiddev.R;

public class ContactTitleActionBarInflater {

    private final ActionBarActivity activity;
    private View actionBarView;

    private int[] accountActionBarColors;
    private int[] accountStatusBarColors;

    private Window window;
    private int defaultStatusBarColor;

    private Animation shakeAnimation = null;

    private ColorDrawable defaultActionBarBackground;

    public ContactTitleActionBarInflater(ActionBarActivity activity) {
        this.activity = activity;
    }

    public void setUpActionBarView() {
        accountActionBarColors = activity.getResources().getIntArray(R.array.account_action_bar);
        accountStatusBarColors = activity.getResources().getIntArray(R.array.account_status_bar);

        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);


        actionBarView = LayoutInflater.from(activity).inflate(R.layout.contact_title, null);

        actionBar.setCustomView(actionBarView, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window = this.activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            defaultStatusBarColor = window.getStatusBarColor();
        }

        TypedArray a = activity.getTheme().obtainStyledAttributes(R.style.Theme, new int[] {R.attr.colorPrimary});
        int attributeResourceId = a.getResourceId(0, 0);
        defaultActionBarBackground = new ColorDrawable(activity.getResources().getColor(attributeResourceId));

    }

    public void update(AbstractContact abstractContact) {
        activity.getSupportActionBar().setDisplayShowCustomEnabled(true);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        actionBarView.setVisibility(View.VISIBLE);

        ContactTitleInflater.updateTitle(actionBarView, activity, abstractContact);

        int colorLevel = AccountManager.getInstance().getColorLevel(abstractContact.getAccount());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(accountStatusBarColors[colorLevel]);
        }
        activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(accountActionBarColors[colorLevel]));
    }

    public void restoreDefaultTitleView(String title) {
        activity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
        actionBarView.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(defaultStatusBarColor);
        }

        activity.getSupportActionBar().setBackgroundDrawable(defaultActionBarBackground);
        activity.setTitle(title);
    }

    public ImageView getSecurityView() {
        return (ImageView)actionBarView.findViewById(R.id.security);
    }

    public void playIncomingAnimation() {
        if (shakeAnimation == null) {
            shakeAnimation = AnimationUtils.loadAnimation(activity, R.anim.shake);
        }
        actionBarView.findViewById(R.id.name_holder).startAnimation(shakeAnimation);
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        actionBarView.setOnClickListener(onClickListener);
    }

    public void setName(String name) {
        ((TextView) actionBarView.findViewById(R.id.name)).setText(name);
    }

    public void setStatusText(String user) {
        ((TextView) actionBarView.findViewById(R.id.status_text)).setText(user);
    }
}
