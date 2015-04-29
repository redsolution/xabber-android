package com.xabber.android.ui.helper;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class BarPainter {

    private final Toolbar toolbar;
    private StatusBarPainter statusBarPainter;

    private AccountPainter accountPainter;

    public BarPainter(AppCompatActivity activity, Toolbar toolbar) {
        this.toolbar = toolbar;
        statusBarPainter = new StatusBarPainter(activity);

        accountPainter = new AccountPainter(activity);
    }

    public void updateWithAccountName(String account) {
        toolbar.setBackgroundColor(accountPainter.getAccountMainColor(account));
        statusBarPainter.updateWithAccountName(account);
    }

    public void setDefaultColor() {
        toolbar.setBackgroundColor(accountPainter.getDefaultMainColor());
        statusBarPainter.updateWithDefaultColor();
    }

    public void updateWithColorName(String targetColorName) {
        toolbar.setBackgroundColor(accountPainter.getAccountMainColorByColorName(targetColorName));
        statusBarPainter.updateWithColor(accountPainter.getAccountDarkColorByColorName(targetColorName));
    }
}
