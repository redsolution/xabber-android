package com.xabber.android.ui.helper;

import android.content.Context;
import android.content.res.TypedArray;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountPainter {
    private final int themeMainColor;
    private final int themeDarkColor;
    private final String[] accountColorNames;

    private int[] accountMainColors;
    private int[] accountDarkColors;

    public AccountPainter(Context context) {

        accountMainColors = context.getResources().getIntArray(R.array.account_action_bar);
        accountDarkColors = context.getResources().getIntArray(R.array.account_status_bar);

        accountColorNames = context.getResources().getStringArray(R.array.account_color_names);

        themeMainColor = getThemeMainColor(context);
        themeDarkColor = getThemeDarkColor(context);
    }

    private static String getFirstAccount() {
        List<String> list = new ArrayList<>();
        list.addAll(AccountManager.getInstance().getAccounts());
        Collections.sort(list);
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    private static int getAccountColorLevel(String account) {
        return AccountManager.getInstance().getColorLevel(account);
    }

    public static int getDefaultAccountColorLevel() {
        String firstAccount = getFirstAccount();
        if (firstAccount == null) {
            return 5;
        } else {
            return getAccountColorLevel(firstAccount);
        }
    }

    private int getThemeMainColor(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(R.style.Theme, new int[]{R.attr.colorPrimary});
        int attributeResourceId = a.getResourceId(0, 0);
        a.recycle();
        return context.getResources().getColor(attributeResourceId);
    }

    private int getThemeDarkColor(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(R.style.Theme, new int[]{R.attr.colorPrimaryDark});
        int attributeResourceId = a.getResourceId(0, 0);
        a.recycle();
        return context.getResources().getColor(attributeResourceId);
    }

    public int getAccountMainColor(String account) {
        return accountMainColors[getAccountColorLevel(account)];
    }

    public int getDefaultMainColor() {
        String firstAccount = getFirstAccount();
        if (firstAccount == null) {
            return themeMainColor;
        } else {
            return getAccountMainColor(firstAccount);
        }
    }

    public int getAccountDarkColor(String account) {
        return accountDarkColors[getAccountColorLevel(account)];
    }

    public int getDefaultDarkColor() {
        String firstAccount = getFirstAccount();
        if (firstAccount == null) {
            return themeDarkColor;
        } else {
            return getAccountDarkColor(firstAccount);
        }
    }

    public int getAccountMainColorByColorName(String targetColorName) {
        return accountMainColors[getColorIndexByName(targetColorName)];
    }

    public int getAccountDarkColorByColorName(String targetColorName) {
        return accountDarkColors[getColorIndexByName(targetColorName)];
    }

    private Integer getColorIndexByName(String targetColorName) {
        for (int i = 0; i < accountColorNames.length; i++) {
            String accountColorName = accountColorNames[i];
            if (accountColorName.equals(targetColorName)) {
                return i;
            }
        }
        return null;
    }
}
