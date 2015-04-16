package com.xabber.android.ui.helper;

import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import android.view.WindowManager;

import com.xabber.android.data.account.AccountManager;
import com.xabber.androiddev.R;


public class StatusBarPainter {

    private Window window;
    private int defaultStatusBarColor;

    private int[] accountStatusBarColors;

    public StatusBarPainter(FragmentActivity activity) {
        accountStatusBarColors = activity.getResources().getIntArray(R.array.account_status_bar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            defaultStatusBarColor = window.getStatusBarColor();
        }

    }

    public void updateWithAccountName(String account) {
        updateWithColorLevel(AccountManager.getInstance().getColorLevel(account));
    }

    public void updateWithColorLevel(int colorLevel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(accountStatusBarColors[colorLevel]);
        }
    }

    public void restore() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(defaultStatusBarColor);
        }
    }
}
