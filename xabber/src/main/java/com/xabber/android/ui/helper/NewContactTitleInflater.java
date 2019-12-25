package com.xabber.android.ui.helper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.xmpp.uuu.ChatState;
import com.xabber.xmpp.uuu.ChatStateSubtype;


/**
 * Created by valery.miller on 26.10.17.
 */

public class NewContactTitleInflater {

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact,
                                   NotificationState.NotificationMode mode) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);
        final ImageView avatarView = (ImageView) titleView.findViewById(R.id.ivAvatar);

        nameView.setText(abstractContact.getName());
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.contact_list_contact_name_text_color, typedValue, true);
        nameView.setTextColor(typedValue.data);

        // notification mute
        Resources resources = context.getResources();
        int resID = 0;
        if (mode == NotificationState.NotificationMode.enabled) resID = R.drawable.ic_unmute_large;
        else if (mode == NotificationState.NotificationMode.disabled) resID = R.drawable.ic_mute_large;
        else if (mode != NotificationState.NotificationMode.bydefault) resID = R.drawable.ic_snooze_toolbar;
        Drawable drawable = null;
        if (resID != 0){
            drawable = resources.getDrawable(resID);
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                drawable.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                drawable.setAlpha(144);
            }
            else {
                drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                drawable.setAlpha(128);
            }
        }
        nameView.setCompoundDrawablesWithIntrinsicBounds(null, null,
                drawable, null);

        // custom notification
        boolean isCustomNotification = CustomNotifyPrefsManager.getInstance().
                isPrefsExist(Key.createKey(abstractContact.getAccount(), abstractContact.getUser()));
        if (isCustomNotification && (mode == NotificationState.NotificationMode.enabled
                || mode == NotificationState.NotificationMode.bydefault))
            nameView.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    resources.getDrawable(R.drawable.ic_notif_custom_large), null);

        // if it is account, not simple user contact
        if (abstractContact.getUser().getJid().asBareJid().equals(abstractContact.getAccount().getFullJid().asBareJid())) {
            avatarView.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(abstractContact.getAccount()));
        } else {
            avatarView.setImageDrawable(abstractContact.getAvatar());
        }
        setStatus(context, titleView, abstractContact);
    }

    private static void setStatus(Context context, View titleView, AbstractContact abstractContact) {
        final ImageView statusModeView = (ImageView) titleView.findViewById(R.id.ivStatus);
        final ImageView groupchatStatusView = (ImageView) titleView.findViewById(R.id.ivStatusGroupchat);

        boolean isGroupchat = false;
        boolean isServer = false;
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(abstractContact.getAccount(), abstractContact.getUser());
        if (chat != null) {
            isGroupchat = chat.isGroupchat();
            isServer = abstractContact.getUser().getJid().isDomainBareJid();
        }
        int statusLevel = abstractContact.getStatusMode().getStatusLevel();
        statusModeView.setVisibility(View.GONE);
        if (isServer) {
            groupchatStatusView.setImageResource(R.drawable.ic_server_14_border);
            groupchatStatusView.setVisibility(View.VISIBLE);
        } else if (isContactOffline(statusLevel)) {
            statusModeView.setVisibility(View.GONE);
            groupchatStatusView.setVisibility(View.GONE);
        } else {
            if (isGroupchat) {
                statusModeView.setVisibility(View.GONE);
                groupchatStatusView.setVisibility(View.VISIBLE);
            } else {
                statusModeView.setVisibility(View.VISIBLE);
                statusModeView.setImageLevel(statusLevel);
                groupchatStatusView.setVisibility(View.GONE);
            }
        }

        final TextView statusTextView = (TextView) titleView.findViewById(R.id.status_text);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark){
            statusTextView.setAlpha(1);
            if (statusLevel == 0 || statusLevel == 1 || statusLevel == 2 || statusLevel == 3 || statusLevel == 4)
                statusTextView.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountColorWithTint(abstractContact.getAccount(), 800));
            else statusTextView.setTextColor(context.getResources().getColor(R.color.contact_item_text_second_dark));
        } else {
            statusTextView.setTextColor(context.getResources().getColor(R.color.grey_800));
            if (statusLevel == 0 || statusLevel == 1 || statusLevel == 2 || statusLevel == 3 || statusLevel == 4){
                statusTextView.setAlpha(0.9f);
            }
            else {
                statusTextView.setAlpha(0.5f);
            }
        }

        ChatState chatState = ChatStateManager.getInstance().getChatState(
                abstractContact.getAccount(), abstractContact.getUser());
        ChatStateSubtype type = ChatStateManager.getInstance().getChatSubstate(
                abstractContact.getAccount(), abstractContact.getUser());

        CharSequence statusText = null;
        if (isServer) statusText = "Server";
        else if (chatState == ChatState.composing) {
            if (type == null) {
                statusText = context.getString(R.string.chat_state_composing);
            } else {
                switch (type) {
                    case voice:
                        statusText = context.getString(R.string.chat_state_composing_voice);
                        break;
                    case video:
                        statusText = context.getString(R.string.chat_state_composing_video);
                        break;
                    case upload:
                        statusText = context.getString(R.string.chat_state_composing_upload);
                        break;
                    default:
                        statusText = context.getString(R.string.chat_state_composing);
                        break;
                }
            }
        } else if (chatState == ChatState.paused) {
            if (type == null) {
                statusText = context.getString(R.string.chat_state_paused);
            } else {
                switch (type) {
                    case voice:
                    case video:
                        statusText = context.getString(R.string.chat_state_paused_voice_and_video);
                        break;
                    case upload:
                        statusText = context.getString(R.string.chat_state_composing_upload);
                        break;
                    default:
                        statusText = context.getString(R.string.chat_state_paused);
                        break;
                }
            }
        } else {
            if (StatusMode.unavailable == abstractContact.getStatusMode())
                statusText = getLastActivity(abstractContact);
            else statusText = abstractContact.getStatusText().trim();

            if (statusText.toString().isEmpty())
                statusText = context.getString(abstractContact.getStatusMode().getStringID());
        }
        statusTextView.setText(statusText);
    }

    private static boolean isContactOffline(int statusLevel) {
        return statusLevel == 6;
    }

    private static String getLastActivity(AbstractContact contact) {
        if (contact instanceof RosterContact) {
            RosterContact rosterContact = (RosterContact) contact;
            if (!rosterContact.getStatusMode().isOnline())
                return rosterContact.getLastActivity();
        }
        return "";
    }

}
