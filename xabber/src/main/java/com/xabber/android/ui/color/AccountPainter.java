package com.xabber.android.ui.color;

import android.content.Context;
import android.content.res.TypedArray;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

public class AccountPainter {
    private final int themeMainColor;
    private final int themeDarkColor;
    private final int themeTextColor;
    private final String[] accountColorNames;

    private final int[] accountMainColors;
    private final int[] accountIndicatorBackColors;
    private final int[] accountDarkColors;
    private final int[] accountTextColors;
    private final int[] accountRippleColors;
    private final int[] accountSendButtonColors;

    private final int[] account50;
    private final int[] account100;
    private final int[] account200;
    private final int[] account300;
    private final int[] account400;
    private final int[] account500;
    private final int[] account600;
    private final int[] account700;
    private final int[] account800;
    private final int[] account900;

    private final int greyMain;
    private final int greyDark;

    AccountPainter(Context context) {

        accountMainColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.account_main_color));
        accountIndicatorBackColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.contact_list_account_group_background));
        accountDarkColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.account_status_bar_color));
        accountTextColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.account_text_color));
        accountRippleColors = context.getResources().getIntArray(R.array.account_100);
        accountSendButtonColors = context.getResources().getIntArray(getThemeAttribute(context, R.attr.chat_send_button_color));

        account50 = context.getResources().getIntArray(R.array.account_50);
        account100 = context.getResources().getIntArray(R.array.account_100);
        account200 = context.getResources().getIntArray(R.array.account_200);
        account300 = context.getResources().getIntArray(R.array.account_300);
        account400 = context.getResources().getIntArray(R.array.account_400);
        account500 = context.getResources().getIntArray(R.array.account_500);
        account600 = context.getResources().getIntArray(R.array.account_600);
        account700 = context.getResources().getIntArray(R.array.account_700);
        account800 = context.getResources().getIntArray(R.array.account_800);
        account900 = context.getResources().getIntArray(R.array.account_900);

        accountColorNames = context.getResources().getStringArray(R.array.account_color_names);

        themeMainColor = getThemeMainColor(context);
        themeDarkColor = getThemeDarkColor(context);
        themeTextColor = getThemeTextColor(context);

        greyMain = context.getResources().getColor(R.color.grey_600);
        greyDark = context.getResources().getColor(R.color.grey_700);
    }

    public static int getAccountColorLevel(AccountJid account) {
        return AccountManager.getInstance().getColorLevel(account);
    }

    public static int getDefaultAccountColorLevel() {
        AccountJid firstAccount = AccountManager.getInstance().getFirstAccount();
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

    public int getAccountMainColor(AccountJid account) {
        return accountMainColors[getAccountColorLevel(account)];
    }

    public int getAccountIndicatorBackColor(AccountJid account) {
        return accountIndicatorBackColors[getAccountColorLevel(account)];
    }

    /**
     * Returns the specified tint of the standardColor of the account.
     * @param account
     * @param tint may be 50, 100, 200.. 900; else will return 0
     * @return account standartColor with tint
     */
    public int getAccountColorWithTint(AccountJid account, int tint){
        switch (tint){
            case 50 : return account50[getAccountColorLevel(account)];
            case 100 : return account100[getAccountColorLevel(account)];
            case 200 : return account200[getAccountColorLevel(account)];
            case 300 : return account300[getAccountColorLevel(account)];
            case 400 : return account400[getAccountColorLevel(account)];
            case 500 : return account500[getAccountColorLevel(account)];
            case 600 : return account600[getAccountColorLevel(account)];
            case 700 : return account700[getAccountColorLevel(account)];
            case 800 : return account800[getAccountColorLevel(account)];
            case 900 : return account900[getAccountColorLevel(account)];
        }
        return 0;
    }

    /**
     * Returns the specified tint of the default standartColor
     * @param tint may be 50, 100, 200.. 900; else will return 0
     * @return default standartColor with tint
     */
    public int getDefaultColorWithTint(int tint){
        return getAccountColorWithTint(AccountManager.getInstance().getFirstAccount(), tint);
    }
    public int getDefaultMainColor() {
        AccountJid firstAccount = AccountManager.getInstance().getFirstAccount();
        if (firstAccount == null) {
            return accountMainColors[SettingsManager.getMainAccountColorLevel()];
        } else {
            return getAccountMainColor(firstAccount);
        }
    }

    public int getDefaultRippleColor() {
        AccountJid firstAccount = AccountManager.getInstance().getFirstAccount();
        if (firstAccount == null) {
            return accountRippleColors[SettingsManager.getMainAccountColorLevel()];
        } else {
            return getAccountRippleColor(firstAccount);
        }
    }

    public int getDefaultIndicatorBackColor() {
        AccountJid firstAccount = AccountManager.getInstance().getFirstAccount();
        if (firstAccount == null) {
            return themeMainColor;
        } else {
            return getAccountIndicatorBackColor(firstAccount);
        }
    }

    public int getAccountDarkColor(AccountJid account) {
        return accountDarkColors[getAccountColorLevel(account)];
    }

    public int getAccountTextColor(AccountJid account) {
        return accountTextColors[getAccountColorLevel(account)];
    }

    public int getAccountRippleColor(AccountJid account) {
        return accountRippleColors[getAccountColorLevel(account)];
    }

    public int getAccountSendButtonColor(AccountJid account) {
        return accountSendButtonColors[getAccountColorLevel(account)];
    }

    public int getDefaultTextColor() {
        AccountJid firstAccount = AccountManager.getInstance().getFirstAccount();
        if (firstAccount == null) {
            return themeTextColor;
        } else {
            return getAccountTextColor(firstAccount);
        }
    }

    public int getDefaultDarkColor() {
        AccountJid firstAccount = AccountManager.getInstance().getFirstAccount();
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

    public String getAccountColorName(AccountJid accountJid) {
        return accountColorNames[getAccountColorLevel(accountJid)];
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
