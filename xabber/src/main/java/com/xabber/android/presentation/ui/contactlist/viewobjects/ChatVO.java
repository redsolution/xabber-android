package com.xabber.android.presentation.ui.contactlist.viewobjects;

/**
 * Created by valery.miller on 06.02.18.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class ChatVO extends ExtContactVO {

    private IsCurrentChatListener currentChatListener;

    public interface IsCurrentChatListener {
        boolean isCurrentChat(String account, String user);
    }

    public ChatVO(int accountColorIndicator, int accountColorIndicatorBack, boolean showOfflineShadow,
                  String name, String status, int statusId, int statusLevel, Drawable avatar,
                  int mucIndicatorLevel, UserJid userJid, AccountJid accountJid, int unreadCount,
                  boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                  boolean isOutgoing, Date time, int messageStatus, String messageOwner,
                  boolean archived, String lastActivity, ContactClickListener listener,
                  @Nullable IsCurrentChatListener currentChatListener, int forwardedCount,
                  boolean isCustomNotification) {

        super(accountColorIndicator, accountColorIndicatorBack, showOfflineShadow, name, status,
                statusId, statusLevel, avatar, mucIndicatorLevel, userJid, accountJid,
                unreadCount, mute, notificationMode, messageText, isOutgoing, time, messageStatus,
                messageOwner, archived, lastActivity, listener, forwardedCount, isCustomNotification);

        this.currentChatListener = currentChatListener;
    }

    public static ChatVO convert(AbstractContact contact, ContactClickListener listener,
                                 @Nullable IsCurrentChatListener currentChatListener) {
        ExtContactVO contactVO = ExtContactVO.convert(contact, listener);
        return new ChatVO(
                contactVO.getAccountColorIndicator(), contactVO.getAccountColorIndicatorBack(),
                contactVO.isShowOfflineShadow(),
                contactVO.getName(), contactVO.getStatus(), contactVO.getStatusId(),
                contactVO.getStatusLevel(), contactVO.getAvatar(), contactVO.getMucIndicatorLevel(),
                contactVO.getUserJid(), contactVO.getAccountJid(), contactVO.getUnreadCount(),
                contactVO.isMute(), contactVO.getNotificationMode(), contactVO.getMessageText(),
                contactVO.isOutgoing(), contactVO.getTime(), contactVO.getMessageStatus(),
                contactVO.getMessageOwner(), contactVO.isArchived(), contactVO.getLastActivity(),
                contactVO.listener, currentChatListener, contactVO.forwardedCount, contactVO.isCustomNotification());
    }

    public static ArrayList<IFlexible> convert(Collection<AbstractContact> contacts,
                                               ContactClickListener listener,
                                               @Nullable IsCurrentChatListener currentChatListener) {
        ArrayList<IFlexible> items = new ArrayList<>();
        for (AbstractContact contact : contacts) {
            items.add(ChatVO.convert(contact, listener, currentChatListener));
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

        /** set up BACKGROUND for ARCHIVED CHAT*/
        viewHolder.foregroundView.setBackgroundColor(archived ? ColorManager.getInstance().getArchivedContactBackgroundColor()
                : ColorManager.getInstance().getContactBackground());

        /** set up BACKGROUND for CURRENT CHAT */
        if (currentChatListener != null && currentChatListener.isCurrentChat(getAccountJid().toString(),
                getUserJid().toString())) {

            final int[] accountGroupColors = context.getResources().getIntArray(
                    getThemeResource(context, R.attr.current_chat_background));
            final int level = AccountManager.getInstance().getColorLevel(getAccountJid());
            viewHolder.foregroundView.setBackgroundColor(accountGroupColors[level]);
        }
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_chat_in_contact_list;
    }

    @Override
    public boolean isSwipeable() {
        return true;
    }

    private int getThemeResource(Context context, int themeResourceId) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }
}
