package com.xabber.android.presentation.ui.contactlist.viewobjects;

/**
 * Created by valery.miller on 06.02.18.
 */

import android.graphics.drawable.Drawable;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.AbstractContact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import eu.davidea.flexibleadapter.items.IFlexible;

public class ChatVO extends ExtContactVO {

    public ChatVO(int accountColorIndicator, boolean showOfflineShadow,
                  String name, String status, int statusId, int statusLevel, Drawable avatar,
                  int mucIndicatorLevel, UserJid userJid, AccountJid accountJid, int unreadCount,
                  boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                  boolean isOutgoing, Date time, int messageStatus, String messageOwner, boolean archived) {

        super(accountColorIndicator, showOfflineShadow, name, status, statusId, statusLevel, avatar, mucIndicatorLevel, userJid, accountJid,
                unreadCount, mute, notificationMode, messageText, isOutgoing, time, messageStatus,
                messageOwner, archived);
    }

    public static ChatVO convert(AbstractContact contact) {
        ExtContactVO contactVO = ExtContactVO.convert(contact);
        return new ChatVO(
                contactVO.getAccountColorIndicator(), contactVO.isShowOfflineShadow(),
                contactVO.getName(), contactVO.getStatus(), contactVO.getStatusId(),
                contactVO.getStatusLevel(), contactVO.getAvatar(), contactVO.getMucIndicatorLevel(),
                contactVO.getUserJid(), contactVO.getAccountJid(), contactVO.getUnreadCount(),
                contactVO.isMute(), contactVO.getNotificationMode(), contactVO.getMessageText(),
                contactVO.isOutgoing(), contactVO.getTime(), contactVO.getMessageStatus(),
                contactVO.getMessageOwner(), contactVO.isArchived());
    }

    public static ArrayList<IFlexible> convert(Collection<AbstractContact> contacts) {
        ArrayList<IFlexible> items = new ArrayList<>();
        for (AbstractContact contact : contacts) {
            items.add(ChatVO.convert(contact));
        }
        return items;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_chat_in_contact_list_new;
    }

    @Override
    public boolean isSwipeable() {
        return true;
    }
}
