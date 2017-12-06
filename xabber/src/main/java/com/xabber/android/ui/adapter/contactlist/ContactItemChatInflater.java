package com.xabber.android.ui.adapter.contactlist;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ChatVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.StringUtils;

/**
 * Created by valery.miller on 10.10.17.
 */

public class ContactItemChatInflater {

    private final Context context;
    private String outgoingMessageIndicatorText;

    ContactItemChatInflater(Context context) {
        this.context = context;
        outgoingMessageIndicatorText = context.getString(R.string.sender_is_you) + ": ";
    }

    void bindViewHolder(ExtContactViewHolder viewHolder, final ChatVO viewObject) {

        if (viewObject.isShowOfflineShadow())
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        else viewHolder.offlineShadow.setVisibility(View.GONE);

        viewHolder.accountColorIndicator.setBackgroundColor(viewObject.getAccountColorIndicator());

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatarView.setVisibility(View.VISIBLE);
            viewHolder.ivAvatar.setImageDrawable(viewObject.getAvatar());
        } else viewHolder.avatarView.setVisibility(View.GONE);

        viewHolder.tvContactName.setText(viewObject.getName());

        if (viewObject.getMucIndicatorLevel() == 0)
            viewHolder.ivMucIndicator.setVisibility(View.GONE);
        else {
            viewHolder.ivMucIndicator.setVisibility(View.VISIBLE);
            viewHolder.ivMucIndicator.setImageLevel(viewObject.getMucIndicatorLevel());
        }

        if (viewObject.getStatusLevel() == 6 ||
                (viewObject.getMucIndicatorLevel() != 0 && viewObject.getStatusLevel() != 1))
            viewHolder.ivStatus.setVisibility(View.INVISIBLE);
        else viewHolder.ivStatus.setVisibility(View.VISIBLE);
        viewHolder.ivStatus.setImageLevel(viewObject.getStatusLevel());

        viewHolder.tvOutgoingMessage.setVisibility(View.GONE);

        viewHolder.tvTime.setText(StringUtils
                .getSmartTimeTextForRoster(context, viewObject.getTime()));
        viewHolder.tvTime.setVisibility(View.VISIBLE);

        if (viewObject.isOutgoing()) {
            viewHolder.tvOutgoingMessage.setText(outgoingMessageIndicatorText);
            viewHolder.tvOutgoingMessage.setVisibility(View.VISIBLE);
            viewHolder.tvOutgoingMessage.setTextColor(viewObject.getAccountColorIndicator());
        }

        if (viewObject.getMessageOwner() != null && !viewObject.getMessageOwner().trim().isEmpty()) {
            viewHolder.tvOutgoingMessage.setText(viewObject.getMessageOwner() + ": ");
            viewHolder.tvOutgoingMessage.setVisibility(View.VISIBLE);
            viewHolder.tvOutgoingMessage.setTextColor(viewObject.getAccountColorIndicator());
        }

        String text = viewObject.getMessageText();
        if (text.isEmpty()) {
            viewHolder.tvMessageText.setText(R.string.no_messages);
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
                viewHolder.tvMessageText.setText(text);
                viewHolder.tvMessageText.setTypeface(Typeface.DEFAULT);
            }
        }

        // message status
        viewHolder.ivMessageStatus.setVisibility(View.VISIBLE);

        switch (viewObject.getMessageStatus()) {
            case 0:
                viewHolder.ivMessageStatus.setImageResource(R.drawable.ic_message_delivered_14dp);
                break;
            case 3:
                viewHolder.ivMessageStatus.setImageResource(R.drawable.ic_message_has_error_14dp);
                break;
            case 4:
                viewHolder.ivMessageStatus.setImageResource(R.drawable.ic_message_acknowledged_14dp);
                break;
            default:
                viewHolder.ivMessageStatus.setVisibility(View.INVISIBLE);
                break;
        }

        if (viewObject.getUnreadCount() > 0) {
            viewHolder.tvUnreadCount.setText(String.valueOf(viewObject.getUnreadCount()));
            viewHolder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else viewHolder.tvUnreadCount.setVisibility(View.INVISIBLE);

        // notification mute
        Resources resources = context.getResources();
        switch (viewObject.getNotificationMode()) {
            case enabled:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        resources.getDrawable(R.drawable.ic_unmute), null);
                break;
            case disabled:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        resources.getDrawable(R.drawable.ic_mute), null);
                break;
            default:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, null, null);
        }

        if (viewObject.isMute())
            viewHolder.tvUnreadCount.getBackground().mutate().setColorFilter(
                    resources.getColor(R.color.grey_500),
                    PorterDuff.Mode.SRC_IN);
        else viewHolder.tvUnreadCount.getBackground().mutate().clearColorFilter();

        // swipe background
        boolean archived = viewObject.isArchived();
        viewHolder.tvAction.setText(archived ? R.string.unarchive_chat : R.string.archive_chat);
        viewHolder.tvActionLeft.setText(archived ? R.string.unarchive_chat : R.string.archive_chat);
        viewHolder.ivActionIcon.setImageResource(archived ? R.drawable.ic_unarchived : R.drawable.ic_arcived);
        viewHolder.ivActionIconLeft.setImageResource(archived ? R.drawable.ic_unarchived : R.drawable.ic_arcived);
        viewHolder.foregroundView.setBackgroundColor(archived ? ColorManager.getInstance().getArchivedContactBackgroundColor()
                : ColorManager.getInstance().getContactBackground());
    }

    void bindViewHolder(RosterChatViewHolder viewHolder, final ChatVO viewObject, boolean isCurrent) {
        bindViewHolder(viewHolder, viewObject);

        // current chat
        int[] accountGroupColors = context.getResources().getIntArray(
                getThemeResource(R.attr.current_chat_background));
        final int level = AccountManager.getInstance().getColorLevel(viewObject.getAccountJid());
        if (isCurrent) viewHolder.foregroundView.setBackgroundColor(accountGroupColors[level]);
    }

    void bindViewHolderWithButton(ExtContactViewHolder viewHolder, final ChatVO viewObject, boolean isButton) {
        bindViewHolder(viewHolder, viewObject);

        // button
        viewHolder.buttonView.setVisibility(isButton ? View.VISIBLE : View.GONE);
    }

    void onAvatarClick(ContactVO contact) {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(contact.getAccountJid(), contact.getUserJid())) {
            intent = ContactActivity.createIntent(context, contact.getAccountJid(), contact.getUserJid());
        } else {
            intent = ContactEditActivity.createIntent(context, contact.getAccountJid(), contact.getUserJid());
        }
        context.startActivity(intent);
    }

    private int getThemeResource(int themeResourceId) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }
}
