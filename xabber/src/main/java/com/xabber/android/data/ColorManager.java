package com.xabber.android.data;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;

public class ColorManager {

    private static ColorManager instance = null;
    private ColorStateList[] chatIncomingBalloonColorStateLists;

    public static ColorManager getInstance() {
        if (instance == null) {
            instance = new ColorManager();
        }

        return instance;
    }

    private ColorManager() {
        loadResources();


    }

    public static int getThemeColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] { attr });
        final int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    private void loadResources() {
        final Context applicationContext = Application.getInstance().getApplicationContext();
        final Resources resources = applicationContext.getResources();

        int[] chatIncomingBalloonColors = resources.getIntArray(getThemeResource(applicationContext, R.attr.chat_incoming_balloon));
        int[] chatIncomingBalloonPressedColors = resources.getIntArray(getThemeResource(applicationContext, R.attr.chat_incoming_balloon_pressed));

        final int length = chatIncomingBalloonColors.length;

        chatIncomingBalloonColorStateLists = new ColorStateList[length];

        for (int i = 0; i < length; i++) {
            chatIncomingBalloonColorStateLists[i] = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_pressed},
                            new int[]{},

                    },
                    new int[] {
                            chatIncomingBalloonPressedColors[i],
                            chatIncomingBalloonColors[i],
                    }
            );
        }

    }

    private int getThemeResource(Context context, int themeResourceId) {

        int themeId;
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            themeId = R.style.ThemeDark;
        } else {
            themeId = R.style.Theme;
        }

        TypedArray a = context.obtainStyledAttributes(themeId, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }

    public ColorStateList getChatIncomingBalloonColorsStateList(String account) {
        return chatIncomingBalloonColorStateLists[getAccountColorLevel(account)];
    }

    private static int getAccountColorLevel(String account) {
        return AccountManager.getInstance().getColorLevel(account);
    }


    public void onSettingsChanged() {
        loadResources();
    }
}
