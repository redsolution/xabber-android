package com.xabber.android.ui.helper;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.xabber.android.data.roster.AbstractContact;
import com.xabber.androiddev.R;

public class ContactTitleActionBarInflater {

    private final ActionBarActivity activity;
    private View actionBarView;

    private Animation shakeAnimation = null;

    private ActionBarPainter actionBarPainter;

    public ContactTitleActionBarInflater(ActionBarActivity activity) {
        this.activity = activity;
    }

    public void setUpActionBarView() {

        actionBarPainter = new ActionBarPainter(activity);

        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBarView = LayoutInflater.from(activity).inflate(R.layout.contact_title, null);

        actionBar.setCustomView(actionBarView, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
    }

    public void update(AbstractContact abstractContact) {
        actionBarPainter.update(abstractContact.getAccount());

        activity.getSupportActionBar().setDisplayShowCustomEnabled(true);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        actionBarView.setVisibility(View.VISIBLE);

        ContactTitleInflater.updateTitle(actionBarView, activity, abstractContact);
    }

    public void restoreDefaultTitleView(String title) {
        actionBarPainter.restore();

        activity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
        actionBarView.setVisibility(View.GONE);
        activity.setTitle(title);
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

    public void setOnAvatarClickListener(View.OnClickListener onClickListener) {
        actionBarView.findViewById(R.id.avatar).setOnClickListener(onClickListener);
    }

    public void setName(String name) {
        ((TextView) actionBarView.findViewById(R.id.name)).setText(name);
    }

    public void setStatusText(String user) {
        ((TextView) actionBarView.findViewById(R.id.status_text)).setText(user);
    }

    public void hideStatusIcon() {
        actionBarView.findViewById(R.id.status_icon).setVisibility(View.GONE);
    }
}
