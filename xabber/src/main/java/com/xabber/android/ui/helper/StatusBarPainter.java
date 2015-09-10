package com.xabber.android.ui.helper;

import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import android.view.WindowManager;


public class StatusBarPainter {

    private final AccountPainter accountPainter;
    private Window window;

    public StatusBarPainter(FragmentActivity activity) {
        accountPainter = new AccountPainter(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    public void updateWithAccountName(String account) {
        updateWithColor(accountPainter.getAccountDarkColor(account));
    }


    public void updateWithColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(color);
        }
    }

    public void updateWithDefaultColor() {
        updateWithColor(accountPainter.getDefaultDarkColor());
    }
}
