package com.xabber.android.ui.color;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

public class BarPainter {

    private final Toolbar toolbar;
    private final StatusBarPainter statusBarPainter;
    int standardColor;
    private final AccountPainter accountPainter;
    Context context;

    public BarPainter(AppCompatActivity activity, Toolbar toolbar) {
        this.toolbar = toolbar;
        statusBarPainter = new StatusBarPainter(activity);
        accountPainter = new AccountPainter(activity);
        context = toolbar.getContext();
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.bars_color, typedValue, true);
        standardColor = typedValue.data;
    }

    public void updateWithAccountName(AccountJid account) {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
            if (account == null) {
                toolbar.setBackgroundColor(standardColor);
            } else {
                toolbar.setBackgroundColor(accountPainter.getAccountRippleColor(account));
            }
            toolbar.setTitleTextColor(Color.BLACK);
            statusBarPainter.updateWithAccountName(account);
        } else {
            toolbar.setBackgroundColor(standardColor);
            toolbar.setTitleTextColor(Color.WHITE);
            statusBarPainter.updateWithColor(standardColor);
        }

    }

    public void setDefaultColor() {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
            if (AccountManager.getInstance().getFirstAccount() == null)
            {
                toolbar.setBackgroundColor(standardColor);
            } else {
                toolbar.setBackgroundColor(accountPainter.getAccountRippleColor(AccountManager.getInstance().getFirstAccount()));
            }
            toolbar.setTitleTextColor(Color.BLACK);
            statusBarPainter.updateWithDefaultColor();
        } else {
            toolbar.setBackgroundColor(standardColor);
            toolbar.setTitleTextColor(Color.WHITE);
            statusBarPainter.updateWithColor(standardColor);
        }

    }

    public void setLiteGrey() {
        toolbar.setBackgroundColor(Application.getInstance().getResources().getColor(R.color.grey_200));
    }

    public void setGrey() {
        toolbar.setBackgroundColor(accountPainter.getGreyMain());
        statusBarPainter.updateWithColor(accountPainter.getGreyDark());
    }

    public void setBlue(Context context) {
        toolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.account_register_blue));
        statusBarPainter.updateWithColor(ContextCompat.getColor(context, R.color.account_register_blue_dark));
    }

    public AccountPainter getAccountPainter() {
        return accountPainter;
    }

}
