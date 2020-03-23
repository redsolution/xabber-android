package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.graphics.drawable.Drawable;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.AbstractContact;

import java.util.Date;

/**
 * Created by valery.miller on 12.02.18.
 */

public class ChatWithButtonVO extends ExtContactVO {

    public ChatWithButtonVO(int accountColorIndicator, int accountColorIndicatorBack,
                            String name, String status, int statusId, int statusLevel, Drawable avatar,
                            int mucIndicatorLevel, ContactJid contactJid, AccountJid accountJid, int unreadCount,
                            boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                            boolean isOutgoing, Date time, int messageStatus, String messageOwner,
                            boolean archived, String lastActivity, ContactClickListener listener, int forwardedCount,
                            boolean isCustomNotification, boolean isGroupchat, boolean isServer) {

        super(accountColorIndicator, accountColorIndicatorBack, name, status,
                statusId, statusLevel, avatar, mucIndicatorLevel, contactJid, accountJid,
                unreadCount, mute, notificationMode, messageText, isOutgoing, time, messageStatus,
                messageOwner, archived, lastActivity, listener, forwardedCount, isCustomNotification, isGroupchat, isServer);
    }

    public static ChatWithButtonVO convert(AbstractContact contact, ContactClickListener listener) {
        ExtContactVO contactVO = ExtContactVO.convert(contact, listener);
        return new ChatWithButtonVO(
                contactVO.getAccountColorIndicator(), contactVO.getAccountColorIndicatorBack(),
                contactVO.getName(), contactVO.getStatus(), contactVO.getStatusId(),
                contactVO.getStatusLevel(), contactVO.getAvatar(), contactVO.getMucIndicatorLevel(),
                contactVO.getContactJid(), contactVO.getAccountJid(), contactVO.getUnreadCount(),
                contactVO.isMute(), contactVO.getNotificationMode(), contactVO.getMessageText(),
                contactVO.isOutgoing(), contactVO.getTime(), contactVO.getMessageStatus(),
                contactVO.getMessageOwner(), contactVO.isArchived(), contactVO.getLastActivity(),
                contactVO.listener, contactVO.forwardedCount, contactVO.isCustomNotification(), contactVO.isGroupchat(), contactVO.isServer());
    }

    public static ChatWithButtonVO convert(ChatVO chat) {
        return new ChatWithButtonVO(
                chat.getAccountColorIndicator(), chat.getAccountColorIndicatorBack(),
                chat.getName(), chat.getStatus(), chat.getStatusId(),
                chat.getStatusLevel(), chat.getAvatar(), chat.getMucIndicatorLevel(),
                chat.getContactJid(), chat.getAccountJid(), chat.getUnreadCount(),
                chat.isMute(), chat.getNotificationMode(), chat.getMessageText(),
                chat.isOutgoing(), chat.getTime(), chat.getMessageStatus(),
                chat.getMessageOwner(), chat.isArchived(), chat.getLastActivity(), chat.listener,
                chat.forwardedCount, chat.isCustomNotification(), chat.isGroupchat(), chat.isServer());
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_chat_with_button;
    }

}
