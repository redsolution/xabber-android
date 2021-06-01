package com.xabber.android.ui.fragment.contactListFragment.viewObjects;

/**
 * Created by valery.miller on 06.02.18.
 */

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.utils.StringUtils;
import com.xabber.android.utils.Utils;

import java.util.Date;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class ExtContactVO extends ContactVO {

    public ExtContactVO(int accountColorIndicator, int accountColorIndicatorBack,
                        String name,
                        String status, int statusId, int statusLevel, Drawable avatar,
                        int mucIndicatorLevel, ContactJid contactJid, AccountJid accountJid, int unreadCount,
                        boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                        boolean isOutgoing, Date time, int messageStatus, String messageOwner,
                        boolean archived, String lastActivity, ContactClickListener listener, int forwardedCount,
                        boolean isCustomNotification, boolean isGroupchat, boolean isServer, boolean isBlocked) {

        super(accountColorIndicator, accountColorIndicatorBack, name, status,
                statusId, statusLevel, avatar,
                mucIndicatorLevel, contactJid, accountJid, unreadCount, mute, notificationMode, messageText,
                isOutgoing, time, messageStatus, messageOwner, archived, lastActivity, listener, forwardedCount,
                isCustomNotification, isGroupchat, isServer, isBlocked);
    }

    public static ExtContactVO convert(AbstractContact contact, ContactClickListener listener) {
        ContactVO contactVO = ContactVO.convert(contact, listener);
        return new ExtContactVO(
                contactVO.getAccountColorIndicator(), contactVO.getAccountColorIndicatorBack(),
                contactVO.getName(), contactVO.getStatus(), contactVO.getStatusId(),
                contactVO.getStatusLevel(), contactVO.getAvatar(), contactVO.getMucIndicatorLevel(),
                contactVO.getContactJid(), contactVO.getAccountJid(), contactVO.getUnreadCount(),
                contactVO.isMute(), contactVO.getNotificationMode(), contactVO.getMessageText(),
                contactVO.isOutgoing(), contactVO.getTime(), contactVO.getMessageStatus(),
                contactVO.getMessageOwner(), contactVO.isArchived(), contactVO.getLastActivity(),
                contactVO.listener, contactVO.forwardedCount, contactVO.isCustomNotification(),
                contactVO.isGroupchat(), contactVO.isServer(), contactVO.isBlocked());
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_ext_contact_in_contact_list;
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, ViewHolder viewHolder, int position, List<Object> payloads) {
        super.bindViewHolder(adapter, viewHolder, position, payloads);
        Context context = viewHolder.itemView.getContext();

        /** set up TIME of last message */
        viewHolder.tvTime.setText(StringUtils
                .getSmartTimeTextForRoster(context, getTime()));
        viewHolder.tvTime.setVisibility(View.VISIBLE);

        /** set up SENDER NAME */
        viewHolder.tvOutgoingMessage.setVisibility(View.GONE);
        if (isOutgoing()) {
            //viewHolder.tvOutgoingMessage.setText(context.getString(R.string.sender_is_you) + ": ");
            viewHolder.tvOutgoingMessage.setText("");
            viewHolder.tvOutgoingMessage.setVisibility(View.VISIBLE);
            viewHolder.tvOutgoingMessage.setTextColor(getAccountColorIndicator());
        }

        if (getMessageOwner() != null && !getMessageOwner().trim().isEmpty()) {
            viewHolder.tvOutgoingMessage.setText(getMessageOwner() + ": ");
            viewHolder.tvOutgoingMessage.setVisibility(View.VISIBLE);
            viewHolder.tvOutgoingMessage.setTextColor(getAccountColorIndicator());
        }

        /** set up MESSAGE TEXT */
        String text = getMessageText();
        if (text.isEmpty()) {
            if (forwardedCount > 0)
                viewHolder.tvMessageText.setText(String.format(context.getResources()
                        .getString(R.string.forwarded_messages_count), forwardedCount));
            else viewHolder.tvMessageText.setText(R.string.no_messages);
            viewHolder.tvMessageText.
                    setTypeface(viewHolder.tvMessageText.getTypeface(), Typeface.ITALIC);
        } else {
            viewHolder.tvMessageText.setTypeface(Typeface.DEFAULT);
            viewHolder.tvMessageText.setVisibility(View.VISIBLE);
            if (OTRManager.getInstance().isEncrypted(text)) {
                viewHolder.tvMessageText.setText(R.string.otr_not_decrypted_message);
                viewHolder.tvMessageText.
                        setTypeface(viewHolder.tvMessageText.getTypeface(), Typeface.ITALIC);
            } else {
                try{
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
                        try {
                            viewHolder.tvMessageText.setText(Html.fromHtml(Utils.getDecodedSpannable(text).toString()));
                        } catch (Exception e){
                            viewHolder.tvMessageText.setText(Html.fromHtml(text));
                        }
                    } else viewHolder.tvMessageText.setText(text);
                } catch (Exception e) {
                    LogManager.exception(this, e);
                    viewHolder.tvMessageText.setText(text);
                }
                viewHolder.tvMessageText.setTypeface(Typeface.DEFAULT);
            }
        }

        /** set up MESSAGE STATUS */
        viewHolder.ivMessageStatus.setVisibility(text.isEmpty() ? View.INVISIBLE : View.VISIBLE);

        switch (getMessageStatus()) {

            case 1:
                viewHolder.ivMessageStatus.setImageResource(R.drawable.ic_message_displayed);
                break;
            case 2:
                viewHolder.ivMessageStatus.setImageResource(R.drawable.ic_message_delivered_14dp);
                break;
            case 3:
                viewHolder.ivMessageStatus.setImageResource(R.drawable.ic_message_acknowledged_14dp);
                break;
            case 4:
                viewHolder.ivMessageStatus.setImageResource(R.drawable.ic_message_has_error_14dp);
                break;
            case 5:
                viewHolder.ivMessageStatus.setImageResource(R.drawable.ic_message_not_sent_14dp);
                break;
            default:
                viewHolder.ivMessageStatus.setVisibility(View.GONE);
                break;
        }
    }
}
