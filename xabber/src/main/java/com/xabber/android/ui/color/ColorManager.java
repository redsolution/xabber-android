package com.xabber.android.ui.color;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

import java.util.HashMap;

public class ColorManager {

    private static ColorManager instance = null;
    private ColorStateList[] chatIncomingBalloonColorStateLists;
    private int themeId;
    private AccountPainter accountPainter;

    private int colorMucPrivateChatText;
    private int colorMain;
    private int activeChatTextColor;
    private int activeChatBackgroundColor;
    private int contactBackground;
    private int contactSeparatorColor;
    private int activeChatSeparatorColor;
    private int contactLargeClientIconColor;
    private int activeChatLargeClientIconColor;

    private int contactListBackgroundColor;

    private int chatBackgroundColor;
    private int chatInputBackgroundColor;
    private int navigationDrawerBackgroundColor;

    public static ColorManager getInstance() {
        if (instance == null) {
            instance = new ColorManager();
        }

        return instance;
    }

    private ColorManager() {
        loadResources();
    }

    public AccountPainter getAccountPainter() {
        return accountPainter;
    }

    private void loadResources() {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            themeId = R.style.ThemeDark;
        } else {
            themeId = R.style.Theme;
        }

        accountPainter = new AccountPainter(Application.getInstance().getApplicationContext());

        final Context context = Application.getInstance().getApplicationContext();
        final Resources resources = context.getResources();

        int[] chatIncomingBalloonColors = resources.getIntArray(getThemeResource(context, R.attr.chat_incoming_balloon));
        int[] chatIncomingBalloonPressedColors = resources.getIntArray(getThemeResource(context, R.attr.chat_incoming_balloon_pressed));

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

        colorMucPrivateChatText = getThemeColor(context, R.attr.contact_list_contact_muc_private_chat_name_text_color);
        colorMain = getThemeColor(context, R.attr.contact_list_contact_name_text_color);
        activeChatTextColor = getThemeColor(context, R.attr.contact_list_active_chat_text_color);
        activeChatBackgroundColor = getThemeColor(context, R.attr.contact_list_active_chat_background);
        contactBackground = getThemeColor(context, R.attr.contact_list_contact_background);
        contactSeparatorColor = getThemeColor(context, R.attr.contact_list_contact_separator);
        activeChatSeparatorColor = getThemeColor(context, R.attr.contact_list_active_chat_separator);
        contactLargeClientIconColor = getThemeColor(context, R.attr.contact_list_contact_client_large_icon_color);
        activeChatLargeClientIconColor = getThemeColor(context, R.attr.contact_list_active_chat_client_large_icon_color);

        contactListBackgroundColor = getThemeColor(context, R.attr.contact_list_background);

        chatBackgroundColor = getThemeColor(context, R.attr.chat_background);
        chatInputBackgroundColor = getThemeColor(context, R.attr.chat_input_background);

        navigationDrawerBackgroundColor = getThemeColor(context, R.attr.navigation_drawer_background);
    }

    private int getThemeResource(Context context, int themeResourceId) {
        TypedArray a = context.obtainStyledAttributes(themeId, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }

    public int getThemeColor(Context context, int attr) {
        TypedArray a = context.obtainStyledAttributes(themeId, new int[] { attr });
        final int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    public ColorStateList getChatIncomingBalloonColorsStateList(AccountJid account) {
        return chatIncomingBalloonColorStateLists[getAccountColorLevel(account)];
    }

    private static int getAccountColorLevel(AccountJid account) {
        return AccountManager.getInstance().getColorLevel(account);
    }


    public void onSettingsChanged() {
        loadResources();
    }

    public int getColorMucPrivateChatText() {
        return colorMucPrivateChatText;
    }

    public int getColorMain() {
        return colorMain;
    }

    public int getActiveChatTextColor() {
        return activeChatTextColor;
    }

    public int getActiveChatBackgroundColor() {
        return activeChatBackgroundColor;
    }

    public int getContactBackground() {
        return contactBackground;
    }

    public int getContactSeparatorColor() {
        return contactSeparatorColor;
    }

    public int getActiveChatSeparatorColor() {
        return activeChatSeparatorColor;
    }

    public int getContactLargeClientIconColor() {
        return contactLargeClientIconColor;
    }

    public int getActiveChatLargeClientIconColor() {
        return activeChatLargeClientIconColor;
    }

    public int getChatBackgroundColor() {
        return chatBackgroundColor;
    }

    public int getChatInputBackgroundColor() {
        return chatInputBackgroundColor;
    }

    public int getContactListBackgroundColor() {
        return contactListBackgroundColor;
    }

    public int getNavigationDrawerBackgroundColor() {
        return navigationDrawerBackgroundColor;
    }

    public int convertColorNameToId(String colorName) {
        final Context context = Application.getInstance().getApplicationContext();

        HashMap<String, Integer> colors = new HashMap<>();
        colors.put("green", R.color.green_500);
        colors.put("orange", R.color.orange_500);
        colors.put("red", R.color.red_500);
        colors.put("blue", R.color.blue_500);
        colors.put("indigo", R.color.indigo_500);
        colors.put("blue-grey", R.color.blue_grey_500);
        colors.put("cyan", R.color.cyan_500);
        colors.put("teal", R.color.teal_500);

        Integer colorId = colors.get(colorName);
        if (colorId != null)
            return ContextCompat.getColor(context, colors.get(colorName));
        else return ContextCompat.getColor(context, colors.get("green"));
    }

    public int convertColorNameToIndex(String colorName) {
        HashMap<String, Integer> colors = new HashMap<>();
        colors.put("green", 0);
        colors.put("orange", 1);
        colors.put("red", 2);
        colors.put("blue", 3);
        colors.put("indigo", 4);
        colors.put("blue-grey", 5);
        colors.put("cyan", 6);
        colors.put("teal", 7);

        Integer colorId = colors.get(colorName);
        if (colorId != null) return colorId;
        else return 1;
    }

    public String convertIndexToColorName(int colorIndex) {
        SparseArray<String> colors = new SparseArray<>();
        colors.put(0, "green");
        colors.put(1, "orange");
        colors.put(2, "red");
        colors.put(3, "blue");
        colors.put(4, "indigo");
        colors.put(5, "blue-grey");
        colors.put(6, "cyan");
        colors.put(7, "teal");

        String colorName = colors.get(colorIndex);
        if (colorName != null) return colorName;
        else return "green";
    }

}
