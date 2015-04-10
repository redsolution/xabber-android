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
    private String[] accountColorNames;

    private Window window;
    private int defaultStatusBarColor;
    private ColorDrawable defaultActionBarBackground;

    private final ActionBarActivity activity;

    public ActionBarPainter(ActionBarActivity activity) {
        this.activity = activity;

        accountActionBarColors = activity.getResources().getIntArray(R.array.account_action_bar);
        accountStatusBarColors = activity.getResources().getIntArray(R.array.account_status_bar);
        accountColorNames = activity.getResources().getStringArray(R.array.account_color_names);

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

    public void updateWithAccountName(String account) {
        updateWithColorLevel(AccountManager.getInstance().getColorLevel(account));
    }

    public void updateWithColorLevel(int colorLevel) {
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
