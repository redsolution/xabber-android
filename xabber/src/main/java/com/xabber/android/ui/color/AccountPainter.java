package com.xabber.android.ui.color;

import android.content.Context;
import android.content.res.TypedArray;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountPainter {
    private final int themeMainColor;
    private final int themeDarkColor;
    private final int themeTextColor;
    private final String[] accountColorNames;

    private final int[] accountMainColors;
    private final int[] accountDarkColors;
    private final int[] accountTextColors;
    private final int[] accountRippleColors;
    private final int[] accountSendButtonColors;

    private final int greyMain;
    private final int greyDark;

    AccountPainter(Context context) {

        accountMainColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.account_main_color));
        accountDarkColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.account_status_bar_color));
        accountTextColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.account_text_color));
        accountRippleColors = context.getResources().getIntArray(R.array.account_100);
        accountSendButtonColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.chat_send_button_color));

        accountColorNames = context.getResources().getStringArray(R.array.account_color_names);

        themeMainColor = getThemeMainColor(context);
        themeDarkColor = getThemeDarkColor(context);
        themeTextColor = getThemeTextColor(context);

        greyMain = context.getResources().getColor(R.color.grey_600);
        greyDark = context.getResources().getColor(R.color.grey_700);
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

    private int getThemeAttribute(Context context, int attr) {
        final SettingsManager.InterfaceTheme interfaceTheme = SettingsManager.interfaceTheme();
        final int theme;
        if (interfaceTheme == SettingsManager.InterfaceTheme.dark) {
            theme = R.style.ThemeDark;
        } else {
            theme = R.style.Theme;
        }

        TypedArray a = context.getTheme().obtainStyledAttributes(theme, new int[]{attr});
        int attributeResourceId = a.getResourceId(0, 0);
        a.recycle();
        return attributeResourceId;
    }


    private int getThemeMainColor(Context context) {
        return context.getResources().getColor(getThemeAttribute(context, R.attr.colorPrimary));
    }

    private int getThemeDarkColor(Context context) {
        return context.getResources().getColor(getThemeAttribute(context, R.attr.colorPrimaryDark));
    }

    private int getThemeTextColor(Context context) {
        return context.getResources().getColor(getThemeAttribute(context, android.R.attr.textColorPrimary));
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

    public int getAccountTextColor(String account) {
        return accountTextColors[getAccountColorLevel(account)];
    }

    public int getAccountRippleColor(String account) {
        return accountRippleColors[getAccountColorLevel(account)];
    }

    public int getAccountSendButtonColor(String account) {
        return accountSendButtonColors[getAccountColorLevel(account)];
    }

    public int getDefaultTextColor() {
        String firstAccount = getFirstAccount();
        if (firstAccount == null) {
            return themeTextColor;
        } else {
            return getAccountTextColor(firstAccount);
        }
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

    public int getGreyMain() {
        return greyMain;
    }

    public int getGreyDark() {
        return greyDark;
    }
}
