package com.xabber.android.ui.color;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.util.SparseArray;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

import java.util.HashMap;

public class ColorManager {

    private static ColorManager instance = null;
    private ColorStateList[] chatIncomingBalloonColorStateLists;
    private int[] unreadMessagesBackground;
    private int themeId;
    private AccountPainter accountPainter;

    public static final int defaultAccountColorIndex = 10;
    public static final String defaultAccountColorName = "blue";

    private int colorContactSecondLine;

    private int colorMain;
    private int activeChatTextColor;
    private int activeChatBackgroundColor;
    private int contactBackground;
    private int contactSeparatorColor;
    private int activeChatSeparatorColor;
    private int contactLargeClientIconColor;
    private int activeChatLargeClientIconColor;

    private int contactListBackgroundColor;
    private int archivedContactBackgroundColor;

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
        unreadMessagesBackground = resources.getIntArray(getThemeResource(context, R.attr.chat_unread_messages_background));

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

        colorContactSecondLine = getThemeColor(context, R.attr.contact_list_contact_second_line_text_color);
        colorMain = getThemeColor(context, R.attr.contact_list_contact_name_text_color);
        activeChatTextColor = getThemeColor(context, R.attr.contact_list_active_chat_text_color);
        activeChatBackgroundColor = getThemeColor(context, R.attr.contact_list_active_chat_background);
        contactBackground = getThemeColor(context, R.attr.contact_list_contact_background);
        contactSeparatorColor = getThemeColor(context, R.attr.contact_list_contact_separator);
        activeChatSeparatorColor = getThemeColor(context, R.attr.contact_list_active_chat_separator);
        contactLargeClientIconColor = getThemeColor(context, R.attr.contact_list_contact_client_large_icon_color);
        activeChatLargeClientIconColor = getThemeColor(context, R.attr.contact_list_active_chat_client_large_icon_color);
        archivedContactBackgroundColor = getThemeColor(context, R.attr.contact_list_contact_archived_background);

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

    public int getUnreadMessageBackground(AccountJid account) {
        return unreadMessagesBackground[getAccountColorLevel(account)];
    }

    public static int getAccountColorLevel(AccountJid account) {
        return AccountManager.getInstance().getColorLevel(account);
    }


    public void onSettingsChanged() {
        loadResources();
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

    public int getColorContactSecondLine() {
        return colorContactSecondLine;
    }

    public int getArchivedContactBackgroundColor() {
        return archivedContactBackgroundColor;
    }

    public int convertColorNameToId(String colorName) {
        final Context context = Application.getInstance().getApplicationContext();

        HashMap<String, Integer> colors = new HashMap<>();
        colors.put("red", R.color.red_500);
        colors.put("deep-orange", R.color.deep_orange_500);
        colors.put("orange", R.color.orange_500);
        colors.put("amber", R.color.amber_500);
        colors.put("lime", R.color.lime_500);
        colors.put("light-green", R.color.light_green_500);
        colors.put("green", R.color.green_500);
        colors.put("teal", R.color.teal_500);
        colors.put("cyan", R.color.cyan_500);
        colors.put("light-blue", R.color.light_blue_500);
        colors.put("blue", R.color.blue_500);
        colors.put("indigo", R.color.indigo_500);
        colors.put("deep-purple", R.color.dark_purple_500);
        colors.put("purple", R.color.purple_500);
        colors.put("pink", R.color.pink_500);
        colors.put("blue-grey", R.color.blue_grey_500);
        colors.put("brown", R.color.brown_500);

        Integer colorId = colors.get(colorName);
        if (colorId != null)
            return ContextCompat.getColor(context, colors.get(colorName));
        else return ContextCompat.getColor(context, R.color.grey_800);
    }

    public int convertColorNameToIndex(String colorName) {
        HashMap<String, Integer> colors = new HashMap<>();
        colors.put("red", 0);
        colors.put("deep-orange", 1);
        colors.put("orange", 2);
        colors.put("amber", 3);
        colors.put("lime", 4);
        colors.put("light-green", 5);
        colors.put("green", 6);
        colors.put("teal", 7);
        colors.put("cyan", 8);
        colors.put("light-blue", 9);
        colors.put("blue", 10);
        colors.put("indigo", 11);
        colors.put("deep-purple", 12);
        colors.put("purple", 13);
        colors.put("pink", 14);
        colors.put("blue-grey", 15);
        colors.put("brown", 16);

        Integer colorId = colors.get(colorName);
        if (colorId != null) return colorId;
        else return defaultAccountColorIndex;
    }

    public String convertIndexToColorName(int colorIndex) {
        SparseArray<String> colors = new SparseArray<>();
        colors.put(0, "red");
        colors.put(1, "deep-orange");
        colors.put(2, "orange");
        colors.put(3, "amber");
        colors.put(4, "lime");
        colors.put(5, "light-green");
        colors.put(6, "green");
        colors.put(7, "teal");
        colors.put(8, "cyan");
        colors.put(9, "light-blue");
        colors.put(10, "blue");
        colors.put(11, "indigo");
        colors.put(12, "deep-purple");
        colors.put(13, "purple");
        colors.put(14, "pink");
        colors.put(15, "blue-grey");
        colors.put(16, "brown");

        String colorName = colors.get(colorIndex);
        if (colorName != null) return colorName;
        else return defaultAccountColorName;
    }

    public static int changeColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r,255),
                Math.min(g,255),
                Math.min(b,255));
    }

    public static int getColorWithAlpha(int color, float alpha) {
        return Color.argb(
                Math.round(Color.alpha(color) * alpha),
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }

    public static void setGrayScaleFilter(ImageView v) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        v.setColorFilter(new ColorMatrixColorFilter(matrix));
    }
}
