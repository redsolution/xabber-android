package com.xabber.android.ui.helper;

import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;
import android.view.WindowManager;

import com.xabber.android.data.account.AccountManager;
import com.xabber.androiddev.R;

public class ActionBarPainter {

    private int[] accountActionBarColors;
    private int[] accountStatusBarColors;

    private Window window;
    private int defaultStatusBarColor;
    private ColorDrawable defaultActionBarBackground;

    private final ActionBarActivity activity;

    public ActionBarPainter(ActionBarActivity activity) {
        this.activity = activity;

        accountActionBarColors = activity.getResources().getIntArray(R.array.account_action_bar);
        accountStatusBarColors = activity.getResources().getIntArray(R.array.account_status_bar);

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

    public void update(String account) {
        int colorLevel = AccountManager.getInstance().getColorLevel(account);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(accountStatusBarColors[colorLevel]);
        }
        activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(accountActionBarColors[colorLevel]));
    }

    public void restore() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(defaultStatusBarColor);
        }

        activity.getSupportActionBar().setBackgroundDrawable(defaultActionBarBackground);
    }
}
