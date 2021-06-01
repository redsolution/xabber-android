package com.xabber.android.ui.helper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.RosterManager.SubscriptionState;
import com.xabber.android.ui.color.ColorManager;


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

        boolean isGroupchat = false;
        boolean isServer = false;
        boolean isBlocked = false;
        boolean isConnected = false;
        AbstractChat chat = ChatManager.getInstance().getOrCreateChat(abstractContact.getAccount(), abstractContact.getUser());
        if (chat != null) {
            isGroupchat = chat.isGroupchat();
            isServer = abstractContact.getUser().getJid().isDomainBareJid();
            isBlocked = BlockingManager.getInstance()
                    .contactIsBlockedLocally(abstractContact.getAccount(), abstractContact.getUser());
            isConnected = AccountManager.getInstance().getConnectedAccounts()
                    .contains(abstractContact.getAccount());
        }
        int statusLevel = abstractContact.getStatusMode().getStatusLevel();

        if (isBlocked) statusLevel = 11;
        else if (isServer) statusLevel = 10;
        else if (isGroupchat) statusLevel = 9;

        statusModeView.setImageLevel(statusLevel);

        if (isContactOffline(statusLevel)) {
            statusModeView.setVisibility(View.GONE);
        } else {
            statusModeView.setVisibility(View.VISIBLE);
        }

        if ((isServer || isGroupchat) && !isConnected){
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
            statusModeView.setColorFilter(colorFilter);
        } else
            statusModeView.setColorFilter(0);

        final TextView statusTextView = (TextView) titleView.findViewById(R.id.status_text);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark){
            statusTextView.setAlpha(1);
            switch (statusLevel) {
                case 6:
                    statusTextView.setTextColor(context.getResources().getColor(R.color.contact_item_text_second_dark));
                    break;
                case 11:
                    statusTextView.setTextColor(Color.RED);
                    break;
                default:
                    statusTextView.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountColorWithTint(abstractContact.getAccount(), 800));
            }
        } else {
            statusTextView.setTextColor(context.getResources().getColor(R.color.grey_800));
            if (isContactOffline(statusLevel)) {
                statusTextView.setAlpha(0.5f);
            } else {
                statusTextView.setAlpha(0.9f);
            }
        }

        CharSequence statusText;
        if (isBlocked) statusText = "Blocked";
        else if (isServer) statusText = "Server";
        else {
            statusText = ChatStateManager.getInstance().getFullChatStateString(
                    abstractContact.getAccount(), abstractContact.getUser());
            if (statusText == null) {
                if (abstractContact instanceof ChatContact) {
                    if (PresenceManager.getInstance().hasSubscriptionRequest(abstractContact.getAccount(), abstractContact.getUser())) {
                        //Contact not in our roster, but we have an incoming subscription request
                        statusText = context.getString(R.string.contact_state_incoming_request);
                    } else {
                        if (VCardManager.getInstance().isRosterOrHistoryLoaded(abstractContact.getAccount())) {
                            //Contact not in our roster, and no subscription requests.
                            statusText = context.getString(R.string.contact_state_not_in_contact_list);
                        } else {
                            //Contact state is undefined since roster is not loaded yet
                            statusText = context.getString(R.string.waiting_for_network);
                        }
                    }
                } else {
                    //TODO this is way too messy, should do some cleanup later.
                    if (abstractContact instanceof RosterContact) {
                        SubscriptionState state = RosterManager.getInstance().getSubscriptionState(abstractContact.getAccount(), abstractContact.getUser());
                        switch (state.getSubscriptionType()) {
                            case SubscriptionState.BOTH:
                            case SubscriptionState.TO:
                                //Contact is in our roster, and we have an accepted subscription to their status(online/offline/busy/etc.)
                                statusText = getNormalStatus(abstractContact);
                                break;
                            case SubscriptionState.FROM:
                                //Contact is in our roster, and has an accepted subscription to our status
                                if (state.hasOutgoingSubscription()) {
                                    //And we have an outgoing subscription request to contact's status
                                    statusText = context.getString(R.string.contact_state_outgoing_request);
                                } else {
                                    //And there is no outgoing subscription request to contact's status
                                    statusText = context.getString(R.string.contact_state_subscribed_to_account);
                                }
                                break;
                            default:
                                //Contact is in our roster, no one has a subscription
                                if (state.hasOutgoingSubscription()) {
                                    //And we have an outgoing subscription request to contact's status
                                    statusText = context.getString(R.string.contact_state_outgoing_request);
                                } else {
                                    if (state.hasIncomingSubscription()) {
                                        //And we have an incoming subscription request to our status
                                        statusText = context.getString(R.string.contact_state_incoming_request);
                                    } else {
                                        //And there is no outgoing subscription request to contact's status
                                        statusText = context.getString(R.string.contact_state_no_subscriptions);
                                    }
                                }
                                break;
                        }
                    }

                    if (statusText == null)
                        statusText = getNormalStatus(abstractContact);


                    if (statusText.toString().isEmpty())
                        statusText = context.getString(abstractContact.getStatusMode().getStringID());

                    if (!isConnected)
                        statusText = Application.getInstance().getResources()
                                .getText(R.string.waiting_for_network);
                }
            }
        }
        statusTextView.setText(statusText);
    }

    private static String getNormalStatus(AbstractContact contact) {
        if (StatusMode.unavailable == contact.getStatusMode()) {
            return getLastActivity(contact);
        } else {
            return contact.getStatusText().trim();
        }
    }

    private static boolean isContactOffline(int statusLevel) {
        return statusLevel == 6;
    }

    private static String getLastActivity(AbstractContact contact) {
//        if (contact instanceof RosterContact) {
//            RosterContact rosterContact = (RosterContact) contact;
//            if (!rosterContact.getStatusMode().isOnline())
//                return rosterContact.getLastActivity();
//        } //TODO REALM UPDATE
        return "";
    }

}
