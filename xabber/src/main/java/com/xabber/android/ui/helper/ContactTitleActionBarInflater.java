package com.xabber.android.ui.helper;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.color.BarPainter;

public class ContactTitleActionBarInflater {

    private final ManagedActivity activity;
    private View contactView;


    private BarPainter barPainter;

    public ContactTitleActionBarInflater(ManagedActivity activity) {
        this.activity = activity;
    }

    public void setUpActionBarView() {

        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(activity);

        barPainter = new BarPainter(activity, toolbar);

        contactView = LayoutInflater.from(activity).inflate(R.layout.contact_title, null);

        toolbar.addView(contactView, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
    }

    public void update(AbstractContact abstractContact) {
        barPainter.updateWithAccountName(abstractContact.getAccount());
        contactView.setVisibility(View.VISIBLE);

        ContactTitleInflater.updateTitle(contactView, activity, abstractContact);
    }

    public void setStatusText(String test) {
        ((TextView) contactView.findViewById(R.id.status_text)).setText(test);
    }

    public void hideStatusIcon() {
        contactView.findViewById(R.id.ivStatus).setVisibility(View.GONE);
    }

    public void hideStatusGroupIcon() {
        contactView.findViewById(R.id.ivStatusGroupchat).setVisibility(View.GONE);
    }

    public void showStatusIcon() {
        contactView.findViewById(R.id.ivStatus).setVisibility(View.VISIBLE);
    }

    public void showStatusGroupIcon() {
        contactView.findViewById(R.id.ivStatusGroupchat).setVisibility(View.VISIBLE);
    }
}
