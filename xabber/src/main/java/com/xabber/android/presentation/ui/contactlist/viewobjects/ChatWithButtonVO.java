package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.graphics.drawable.Drawable;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.AbstractContact;

import java.util.Date;

/**
 * Created by valery.miller on 12.02.18.
 */

public class ChatWithButtonVO extends ExtContactVO {

    public ChatWithButtonVO(int accountColorIndicator, int accountColorIndicatorBack,
                            boolean showOfflineShadow,
                            String name, String status, int statusId, int statusLevel, Drawable avatar,
                            int mucIndicatorLevel, UserJid userJid, AccountJid accountJid, int unreadCount,
                            boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                            boolean isOutgoing, Date time, int messageStatus, String messageOwner,
                            boolean archived, String lastActivity, ContactClickListener listener, int forwardedCount,
                            boolean isCustomNotification) {

        super(accountColorIndicator, accountColorIndicatorBack, showOfflineShadow, name, status,
                statusId, statusLevel, avatar, mucIndicatorLevel, userJid, accountJid,
                unreadCount, mute, notificationMode, messageText, isOutgoing, time, messageStatus,
                messageOwner, archived, lastActivity, listener, forwardedCount, isCustomNotification);
    }

    public static ChatWithButtonVO convert(AbstractContact contact, ContactClickListener listener) {
        ExtContactVO contactVO = ExtContactVO.convert(contact, listener);
        return new ChatWithButtonVO(
                contactVO.getAccountColorIndicator(), contactVO.getAccountColorIndicatorBack(),
                contactVO.isShowOfflineShadow(),
                contactVO.getName(), contactVO.getStatus(), contactVO.getStatusId(),
                contactVO.getStatusLevel(), contactVO.getAvatar(), contactVO.getMucIndicatorLevel(),
                contactVO.getUserJid(), contactVO.getAccountJid(), contactVO.getUnreadCount(),
                contactVO.isMute(), contactVO.getNotificationMode(), contactVO.getMessageText(),
                contactVO.isOutgoing(), contactVO.getTime(), contactVO.getMessageStatus(),
                contactVO.getMessageOwner(), contactVO.isArchived(), contactVO.getLastActivity(),
                contactVO.listener, contactVO.forwardedCount, contactVO.isCustomNotification());
    }

    public static ChatWithButtonVO convert(ChatVO chat) {
        return new ChatWithButtonVO(
                chat.getAccountColorIndicator(), chat.getAccountColorIndicatorBack(),
                chat.isShowOfflineShadow(),
                chat.getName(), chat.getStatus(), chat.getStatusId(),
                chat.getStatusLevel(), chat.getAvatar(), chat.getMucIndicatorLevel(),
                chat.getUserJid(), chat.getAccountJid(), chat.getUnreadCount(),
                chat.isMute(), chat.getNotificationMode(), chat.getMessageText(),
                chat.isOutgoing(), chat.getTime(), chat.getMessageStatus(),
                chat.getMessageOwner(), chat.isArchived(), chat.getLastActivity(), chat.listener,
                chat.forwardedCount, chat.isCustomNotification());
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_chat_with_button;
    }

}
