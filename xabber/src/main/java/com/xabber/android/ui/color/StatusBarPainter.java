package com.xabber.android.ui.color;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;

import com.xabber.android.data.entity.AccountJid;


public class StatusBarPainter {

    private final AccountPainter accountPainter;
    private Window window;

    /** StatusBarPainter Constructor
     * @param activity in most cases should be "this"
     */
    public StatusBarPainter(FragmentActivity activity) {
        accountPainter = new AccountPainter(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /** Colorize status bar with Account Main Color
     * @param account Status bar will be colorized with Main Color of this account*/
    public void updateWithAccountName(AccountJid account) {
        updateWithColor(accountPainter.getAccountMainColor(account));
    }

    /** Static colorize status bar with Account Main Color
     * @param activity at most cases should be "this"
     * @param accountJid Status bar will be colorized with Main Color of this account*/
    static public void instanceUpdateWithAccountName(FragmentActivity activity, AccountJid accountJid){
        new StatusBarPainter(activity).updateWithAccountName(accountJid);
    }

    /** Static colorize status bar with standartColor
     * @param activity at most cases should be "this"
     * @param color colorize into this standartColor
     */
    static public void instanceUpdateWIthColor(FragmentActivity activity, int color){
        new StatusBarPainter(activity).updateWithColor(color);
    }

    /** Colorize status bat into custom standartColor
     * @param color use for colorizing
     */
    public void updateWithColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            window.setStatusBarColor(color);
        }
    }

    /** Colorize status bar with Default Main Color */
    public void updateWithDefaultColor() {
        updateWithColor(accountPainter.getDefaultMainColor());
    }

    /** Static colorize status bar with Account Main Color
     * @param activity in most cases should be "this" */
    static public void instanceUpdateWithDefaultColor(FragmentActivity activity){
        new StatusBarPainter(activity).updateWithDefaultColor();
    }
}
