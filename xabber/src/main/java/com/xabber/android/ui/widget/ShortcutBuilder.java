package com.xabber.android.ui.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.CrowdfundingChat;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.activity.ChatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShortcutBuilder {

    public static void updateShortcuts(Context context, List<AbstractContact> contacts) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            List<ShortcutInfo> shortcuts = new ArrayList<>();
            int added = 0;
            for (AbstractContact contact : contacts) {
                if (added == 4) break;
                else if (!contact.getUser().equals(CrowdfundingChat.getDefaultUser())) {
                    shortcuts.add(createShortcutInfo(context, contact));
                    added++;
                }
            }

            ShortcutManager manager = context.getSystemService(ShortcutManager.class);
            if (manager != null && !shortcuts.isEmpty()) {
                manager.setDynamicShortcuts(shortcuts);
            }
        }
    }

    public static Intent createPinnedShortcut(Context context, AbstractContact contact) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager manager = context.getSystemService(ShortcutManager.class);
            if (manager != null && manager.isRequestPinShortcutSupported()) {
                ShortcutInfo shortcutInfo = createShortcutInfo(context, contact);
                Intent callbackIntent = manager.createShortcutResultIntent(shortcutInfo);
                PendingIntent successCallback = PendingIntent.getBroadcast(context, 0, callbackIntent, 0);
                manager.requestPinShortcut(shortcutInfo, successCallback.getIntentSender());
                return null;
            }
        }

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, ChatActivity.createClearTopIntent(context,
                contact.getAccount(), contact.getUser()));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, contact.getName());
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, getAvatar(contact));
        return intent;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private static ShortcutInfo createShortcutInfo(Context context, AbstractContact abstractContact) {
        return new ShortcutInfo.Builder(context, UUID.randomUUID().toString())
                .setShortLabel(abstractContact.getName())
                .setLongLabel(abstractContact.getName())
                .setIcon(Icon.createWithBitmap(getAvatar(abstractContact)))
                .setIntent(ChatActivity.createClearTopIntent(context,
                        abstractContact.getAccount(), abstractContact.getUser()))
                .build();
    }

    private static Bitmap getAvatar(AbstractContact abstractContact) {
        Bitmap bitmap;
        if (MUCManager.getInstance().hasRoom(abstractContact.getAccount(), abstractContact.getUser()))
            bitmap = AvatarManager.getInstance().getRoomBitmap(abstractContact.getUser());
        else bitmap = AvatarManager.getInstance().getUserBitmap(abstractContact.getUser(), abstractContact.getName());
        return AvatarManager.getInstance().createShortcutBitmap(bitmap);
    }

}
