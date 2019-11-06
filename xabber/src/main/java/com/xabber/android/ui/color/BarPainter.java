package com.xabber.android.ui.color;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;

public class BarPainter {

    private final Toolbar toolbar;
    private StatusBarPainter statusBarPainter;
    private TypedValue typedValue = new TypedValue();
    private Resources.Theme theme;
    int color;
    private AccountPainter accountPainter;
    Context context;

    public BarPainter(AppCompatActivity activity, Toolbar toolbar) {
        this.toolbar = toolbar;
        statusBarPainter = new StatusBarPainter(activity);
        accountPainter = new AccountPainter(activity);
        theme = activity.getTheme();
        color = typedValue.data;
        context = toolbar.getContext();
        theme.resolveAttribute(R.attr.bars_color, typedValue, true);
    }

    public void updateWithAccountName(AccountJid account) {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
            toolbar.setBackgroundColor(accountPainter.getAccountRippleColor(accountPainter.getFirstAccount()));
            statusBarPainter.updateWithAccountName(account);
        } else {
            toolbar.setBackgroundColor(color);
            statusBarPainter.updateWithColor(color);
        }

    }

    public void setDefaultColor() {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
            toolbar.setBackgroundColor(accountPainter.getAccountRippleColor(accountPainter.getFirstAccount()));
            statusBarPainter.updateWithDefaultColor();
        } else {
            toolbar.setBackgroundColor(color);
            statusBarPainter.updateWithColor(color);
        }

    }

    public void updateWithColorName(String targetColorName) {
        toolbar.setBackgroundColor(accountPainter.getAccountMainColorByColorName(targetColorName));
        statusBarPainter.updateWithColor(accountPainter.getAccountDarkColorByColorName(targetColorName));
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
