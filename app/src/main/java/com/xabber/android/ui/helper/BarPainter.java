package com.xabber.android.ui.helper;

import android.content.res.TypedArray;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.xabber.android.data.account.AccountManager;
import com.xabber.androiddev.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BarPainter {

    private final Toolbar toolbar;
    private final int defaultSystemColor;
    private int[] accountActionBarColors;
    private String[] accountColorNames;
    private StatusBarPainter statusBarPainter;

    public BarPainter(AppCompatActivity activity, Toolbar toolbar) {
        this.toolbar = toolbar;
        statusBarPainter = new StatusBarPainter(activity);
        accountActionBarColors = activity.getResources().getIntArray(R.array.account_action_bar);
        accountColorNames = activity.getResources().getStringArray(R.array.account_color_names);

        TypedArray a = activity.getTheme().obtainStyledAttributes(R.style.Theme, new int[]{R.attr.colorPrimary});
        int attributeResourceId = a.getResourceId(0, 0);
        a.recycle();
        defaultSystemColor = activity.getResources().getColor(attributeResourceId);
    }

    public static String getFirstAccount() {
        List<String> list = new ArrayList<>();
        list.addAll(AccountManager.getInstance().getAccounts());
        Collections.sort(list);
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public void updateWithAccountName(String account) {
        updateWithColorLevel(AccountManager.getInstance().getColorLevel(account));
    }

    public void updateWithColorLevel(int colorLevel) {
        toolbar.setBackgroundColor(accountActionBarColors[colorLevel]);
        statusBarPainter.updateWithColorLevel(colorLevel);
    }

    public void setDefaultColor() {
        String firstAccount = getFirstAccount();
        if (firstAccount == null) {
            toolbar.setBackgroundColor(defaultSystemColor);
            statusBarPainter.restore();
        } else {
            updateWithAccountName(firstAccount);
        }
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

    public int getDefaultColor() {
        String firstAccount = getFirstAccount();
        if (firstAccount == null) {
            return defaultSystemColor;
        } else {
            return accountActionBarColors[AccountManager.getInstance().getColorLevel(firstAccount)];
        }
    }
}
