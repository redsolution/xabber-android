package com.xabber.android.ui.helper;

import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBarActivity;

import com.xabber.android.data.account.AccountManager;
import com.xabber.androiddev.R;

public class ActionBarPainter {

    private int[] accountActionBarColors;

    private String[] accountColorNames;

    private ColorDrawable defaultActionBarBackground;

    private final ActionBarActivity activity;

    private StatusBarPainter statusBarPainter;

    public ActionBarPainter(ActionBarActivity activity) {
        this.activity = activity;

        statusBarPainter = new StatusBarPainter(activity);

        accountActionBarColors = activity.getResources().getIntArray(R.array.account_action_bar);

        accountColorNames = activity.getResources().getStringArray(R.array.account_color_names);


        TypedArray a = activity.getTheme().obtainStyledAttributes(R.style.Theme, new int[] {R.attr.colorPrimary});
        int attributeResourceId = a.getResourceId(0, 0);
        defaultActionBarBackground = new ColorDrawable(activity.getResources().getColor(attributeResourceId));
    }

    public void updateWithAccountName(String account) {
        updateWithColorLevel(AccountManager.getInstance().getColorLevel(account));
    }

    public void updateWithColorLevel(int colorLevel) {
        activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(accountActionBarColors[colorLevel]));
        statusBarPainter.updateWithColorLevel(colorLevel);
    }

    public void restore() {
        activity.getSupportActionBar().setBackgroundDrawable(defaultActionBarBackground);
        statusBarPainter.restore();
    }

    public void updateWithColorName(String targetColorName) {
        for (int i = 0; i < accountColorNames.length; i++) {
            String accountColorName = accountColorNames[i];
            if (accountColorName.equals(targetColorName)) {
                updateWithColorLevel(i);
            }
        }
    }

    public int getAccountColor(String account) {
        return accountActionBarColors[AccountManager.getInstance().getColorLevel(account)];
    }

}
