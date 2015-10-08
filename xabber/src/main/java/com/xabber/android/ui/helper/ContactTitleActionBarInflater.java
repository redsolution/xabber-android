package com.xabber.android.ui.helper;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.roster.AbstractContact;

public class ContactTitleActionBarInflater {

    private final AppCompatActivity activity;
    private final Toolbar toolbar;
    private View actionBarView;


    private BarPainter barPainter;

    public ContactTitleActionBarInflater(AppCompatActivity activity, Toolbar toolbar) {
        this.activity = activity;
        this.toolbar = toolbar;
    }

    public void setUpActionBarView() {

        barPainter = new BarPainter(activity, toolbar);

        activity.setSupportActionBar(toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBarView = LayoutInflater.from(activity).inflate(R.layout.contact_title, null);

        actionBar.setCustomView(actionBarView, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
    }

    public void update(AbstractContact abstractContact) {
        barPainter.updateWithAccountName(abstractContact.getAccount());

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
