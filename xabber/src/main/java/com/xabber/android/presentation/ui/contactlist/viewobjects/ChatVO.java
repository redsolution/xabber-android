package com.xabber.android.presentation.ui.contactlist.viewobjects;

/**
 * Created by valery.miller on 06.02.18.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class ChatVO extends ExtContactVO {

    public ChatVO(int accountColorIndicator, boolean showOfflineShadow,
                  String name, String status, int statusId, int statusLevel, Drawable avatar,
                  int mucIndicatorLevel, UserJid userJid, AccountJid accountJid, int unreadCount,
                  boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                  boolean isOutgoing, Date time, int messageStatus, String messageOwner,
                  boolean archived, ContactClickListener listener) {

        super(accountColorIndicator, showOfflineShadow, name, status, statusId, statusLevel, avatar, mucIndicatorLevel, userJid, accountJid,
                unreadCount, mute, notificationMode, messageText, isOutgoing, time, messageStatus,
                messageOwner, archived, listener);
    }

    public static ChatVO convert(AbstractContact contact, ContactClickListener listener) {
        ExtContactVO contactVO = ExtContactVO.convert(contact, listener);
        return new ChatVO(
                contactVO.getAccountColorIndicator(), contactVO.isShowOfflineShadow(),
                contactVO.getName(), contactVO.getStatus(), contactVO.getStatusId(),
                contactVO.getStatusLevel(), contactVO.getAvatar(), contactVO.getMucIndicatorLevel(),
                contactVO.getUserJid(), contactVO.getAccountJid(), contactVO.getUnreadCount(),
                contactVO.isMute(), contactVO.getNotificationMode(), contactVO.getMessageText(),
                contactVO.isOutgoing(), contactVO.getTime(), contactVO.getMessageStatus(),
                contactVO.getMessageOwner(), contactVO.isArchived(), contactVO.listener);
    }

    public static ArrayList<IFlexible> convert(Collection<AbstractContact> contacts,
                                               ContactClickListener listener) {
        ArrayList<IFlexible> items = new ArrayList<>();
        for (AbstractContact contact : contacts) {
            items.add(ChatVO.convert(contact, listener));
        }
        return items;
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder viewHolder, int position, List<Object> payloads) {
        super.bindViewHolder(adapter, viewHolder, position, payloads);
        Context context = viewHolder.itemView.getContext();

        /** set up SWIPE BACKGROUND */
        boolean archived = isArchived();
        viewHolder.tvAction.setText(archived ? R.string.unarchive_chat : R.string.archive_chat);
        viewHolder.tvActionLeft.setText(archived ? R.string.unarchive_chat : R.string.archive_chat);
        Drawable drawable = archived ? context.getResources().getDrawable(R.drawable.ic_unarchived)
                : context.getResources().getDrawable(R.drawable.ic_arcived);
        viewHolder.tvAction.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
        viewHolder.tvActionLeft.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        viewHolder.foregroundView.setBackgroundColor(archived ? ColorManager.getInstance().getArchivedContactBackgroundColor()
                : ColorManager.getInstance().getContactBackground());
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
