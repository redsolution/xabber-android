package com.xabber.android.ui.helper;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.androiddev.R;

public class ContactTitleActionBarInflater {

    private final ActionBarActivity activity;
    private View actionBarView;

    private int[] accountActionBarColors;
    private int[] accountStatusBarColors;
    private Window window;

    public ContactTitleActionBarInflater(ActionBarActivity activity) {
        this.activity = activity;
    }

    public void setActionBarView() {
        window = this.activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        accountActionBarColors = activity.getResources().getIntArray(R.array.account_action_bar);
        accountStatusBarColors = activity.getResources().getIntArray(R.array.account_status_bar);

        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        actionBarView = LayoutInflater.from(activity).inflate(R.layout.contact_title, null);

        actionBar.setCustomView(actionBarView, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
    }

    public void update(AbstractContact abstractContact) {
        ContactTitleInflater.updateTitle(actionBarView, activity, abstractContact);

        int colorLevel = AccountManager.getInstance().getColorLevel(abstractContact.getAccount());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(accountStatusBarColors[colorLevel]);
        }
        activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(accountActionBarColors[colorLevel]));
    }

    public View getActionBarView() {
        return actionBarView;
    }
}
