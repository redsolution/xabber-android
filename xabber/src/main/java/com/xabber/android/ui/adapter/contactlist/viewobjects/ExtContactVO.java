package com.xabber.android.ui.adapter.contactlist.viewobjects;

import android.graphics.drawable.Drawable;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.color.ColorManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Created by valery.miller on 06.12.17.
 */

public class ExtContactVO extends ChatVO {

    public ExtContactVO(int accountColorIndicator, boolean showOfflineShadow, String name, String status,
                  int statusId, int statusLevel, Drawable avatar, int mucIndicatorLevel,
                  UserJid userJid, AccountJid accountJid, String messageText, boolean isOutgoing,
                  Date time, int messageStatus, int unreadCount, String messageOwner, boolean mute,
                  NotificationState.NotificationMode notificationMode, boolean archived) {

        super(accountColorIndicator, showOfflineShadow, name, status, statusId, statusLevel,
                avatar, mucIndicatorLevel, userJid, accountJid, messageText, isOutgoing, time,
                messageStatus, unreadCount, messageOwner, mute, notificationMode, archived);
    }

    public static ExtContactVO convert(AbstractContact contact) {
        boolean showOfflineShadow;
        int accountColorIndicator;
        Drawable avatar;
        int statusLevel;
        int mucIndicatorLevel;
        boolean isOutgoing = false;
        Date time = null;
        int messageStatus = 0;
        int unreadCount = 0;
        String messageOwner = null;

        AccountItem accountItem = AccountManager.getInstance().getAccount(contact.getAccount());
        if (accountItem != null && accountItem.getState() == ConnectionState.connected) {
            showOfflineShadow = false;
        } else {
            showOfflineShadow = true;
        }

        accountColorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(contact.getAccount());
        avatar = contact.getAvatarForContactList();


        String name = contact.getName();

        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            mucIndicatorLevel = 1;
        } else if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            mucIndicatorLevel = 2;
        } else {
            mucIndicatorLevel = 0;
        }

        statusLevel = contact.getStatusMode().getStatusLevel();
        String messageText;
        String statusText = contact.getStatusText().trim();
        int statusId = contact.getStatusMode().getStringID();

        MessageManager messageManager = MessageManager.getInstance();
        AbstractChat chat = messageManager.getOrCreateChat(contact.getAccount(), contact.getUser());
        MessageItem lastMessage = chat.getLastMessage();

        if (lastMessage == null) {
            messageText = statusText;
        } else {
            if (lastMessage.getFilePath() != null) {
                messageText = new File(lastMessage.getFilePath()).getName();
            } else {
                messageText = lastMessage.getText().trim();
            }

            time = new Date(lastMessage.getTimestamp());

            isOutgoing = !lastMessage.isIncoming();

            if ((mucIndicatorLevel == 1 || mucIndicatorLevel == 2) && lastMessage.isIncoming()
                    && lastMessage.getText() != null && !lastMessage.getText().trim().isEmpty())
                messageOwner = lastMessage.getResource().toString();

            // message status
            if (lastMessage.isForwarded()) {
                messageStatus = 1;
            } else if (lastMessage.isReceivedFromMessageArchive()) {
                messageStatus = 2;
            } else if (lastMessage.isError()) {
                messageStatus = 3;
            } else if (!lastMessage.isDelivered()) {
                if (lastMessage.isAcknowledged()) {
                    messageStatus = 4;
                } else {
                    messageStatus = 5;
                }
            }
        }

        if (!isOutgoing) unreadCount = chat.getUnreadMessageCount();

        // notification icon
        NotificationState.NotificationMode mode = NotificationState.NotificationMode.bydefault;
        boolean defaultValue = mucIndicatorLevel == 0 ? SettingsManager.eventsOnChat() : SettingsManager.eventsOnMuc();
        if (chat.getNotificationState().getMode() == NotificationState.NotificationMode.enabled && !defaultValue)
            mode = NotificationState.NotificationMode.enabled;
        if (chat.getNotificationState().getMode() == NotificationState.NotificationMode.disabled && defaultValue)
            mode = NotificationState.NotificationMode.disabled;

        return new ExtContactVO(accountColorIndicator, showOfflineShadow, name, statusText, statusId,
                statusLevel, avatar, mucIndicatorLevel, contact.getUser(), contact.getAccount(),
                messageText, isOutgoing, time, messageStatus, unreadCount, messageOwner,
                !chat.notifyAboutMessage(), mode, chat.isArchived());
    }

    public static ArrayList<ContactVO> convert(Collection<AbstractContact> contacts) {
        ArrayList<ContactVO> items = new ArrayList<>();
        for (AbstractContact contact : contacts) {
            items.add(convert(contact));
        }
        return items;
    }

}
