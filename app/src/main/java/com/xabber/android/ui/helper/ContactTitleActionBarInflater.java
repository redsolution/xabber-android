package com.xabber.android.ui.helper;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.data.roster.AbstractContact;
import com.xabber.androiddev.R;

public class ContactTitleActionBarInflater {

    private final ActionBarActivity activity;
    private View actionBarView;



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
        actionBarPainter.updateWithAccountName(abstractContact.getAccount());

        activity.getSupportActionBar().setDisplayShowCustomEnabled(true);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        actionBarView.setVisibility(View.VISIBLE);

        ContactTitleInflater.updateTitle(actionBarView, activity, abstractContact);
    }

    public void setStatusText(String user) {
        ((TextView) actionBarView.findViewById(R.id.status_text)).setText(user);
    }

    public void hideStatusIcon() {
        actionBarView.findViewById(R.id.status_icon).setVisibility(View.GONE);
    }
}
