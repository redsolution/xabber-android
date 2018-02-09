package com.xabber.android.presentation.ui.contactlist.viewobjects;

/**
 * Created by valery.miller on 02.02.18.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.R;
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
import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

public class ContactVO extends AbstractFlexibleItem<ContactVO.ViewHolder> {

    private final String id;

    private int accountColorIndicator;
    private boolean showOfflineShadow;

    private final String name;
    private final String status;
    private final int statusId;
    private final int statusLevel;
    private final Drawable avatar;
    private final int mucIndicatorLevel;
    private final UserJid userJid;
    private final AccountJid accountJid;
    private final int unreadCount;
    private final boolean mute;
    private final NotificationState.NotificationMode notificationMode;
    private final String messageText;
    private final boolean isOutgoing;
    private final Date time;
    private final int messageStatus;
    private final String messageOwner;
    private final boolean archived;

    protected final ContactClickListener listener;

    public interface ContactClickListener {
        void onContactAvatarClick(int adapterPosition);
        void onContactCreateContextMenu(int adapterPosition, ContextMenu menu);
    }

    protected ContactVO(int accountColorIndicator, boolean showOfflineShadow, String name,
                        String status, int statusId, int statusLevel, Drawable avatar,
                  int mucIndicatorLevel, UserJid userJid, AccountJid accountJid, int unreadCount,
                  boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                  boolean isOutgoing, Date time, int messageStatus, String messageOwner,
                        boolean archived, ContactClickListener listener) {
        this.id = UUID.randomUUID().toString();
        this.accountColorIndicator = accountColorIndicator;
        this.showOfflineShadow = showOfflineShadow;
        this.name = name;
        this.status = status;
        this.statusId = statusId;
        this.statusLevel = statusLevel;
        this.avatar = avatar;
        this.mucIndicatorLevel = mucIndicatorLevel;
        this.userJid = userJid;
        this.accountJid = accountJid;
        this.unreadCount = unreadCount;
        this.mute = mute;
        this.notificationMode = notificationMode;
        this.messageText = messageText;
        this.isOutgoing = isOutgoing;
        this.time = time;
        this.messageStatus = messageStatus;
        this.messageOwner = messageOwner;
        this.archived = archived;
        this.listener = listener;
    }

    public static ContactVO convert(AbstractContact contact, ContactClickListener listener) {
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

        return new ContactVO(accountColorIndicator, showOfflineShadow, name, statusText, statusId,
                statusLevel, avatar, mucIndicatorLevel, contact.getUser(), contact.getAccount(),
                unreadCount, !chat.notifyAboutMessage(), mode, messageText, isOutgoing, time,
                messageStatus, messageOwner, chat.isArchived(), listener);
    }

    public static ArrayList<IFlexible> convert(Collection<AbstractContact> contacts, ContactClickListener listener) {
        ArrayList<IFlexible> items = new ArrayList<>();
        for (AbstractContact contact : contacts) {
            items.add(convert(contact, listener));
        }
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ContactVO) {
            ContactVO inItem = (ContactVO) o;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_contact_in_contact_list_new;
    }

    @Override
    public ViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new ViewHolder(view, adapter, listener);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder viewHolder, int position, List<Object> payloads) {
        Context context = viewHolder.itemView.getContext();

        /** set up OFFLINE SHADOW */
        if (isShowOfflineShadow())
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        else viewHolder.offlineShadow.setVisibility(View.GONE);

        /** set up ACCOUNT COLOR indicator */
        viewHolder.accountColorIndicator.setBackgroundColor(getAccountColorIndicator());

        /** set up AVATAR */
        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.ivAvatar.setVisibility(View.VISIBLE);
            viewHolder.ivStatus.setVisibility(View.VISIBLE);
            viewHolder.ivAvatar.setImageDrawable(getAvatar());
            viewHolder.ivOnlyStatus.setVisibility(View.GONE);
        } else {
            viewHolder.ivAvatar.setVisibility(View.GONE);
            viewHolder.ivStatus.setVisibility(View.GONE);
            viewHolder.ivOnlyStatus.setVisibility(View.VISIBLE);
        }

        /** set up ROSTER STATUS */
        if (getStatusLevel() == 6 ||
                (getMucIndicatorLevel() != 0 && getStatusLevel() != 1))
            viewHolder.ivStatus.setVisibility(View.INVISIBLE);
        else viewHolder.ivStatus.setVisibility(View.VISIBLE);
        viewHolder.ivStatus.setImageLevel(getStatusLevel());
        viewHolder.ivOnlyStatus.setImageLevel(getStatusLevel());

        /** set up CONTACT/MUC NAME */
        viewHolder.tvContactName.setText(getName());

        /** set up MUC indicator */
        Drawable mucIndicator;
        if (getMucIndicatorLevel() == 0)
            mucIndicator = null;
        else {
            mucIndicator = context.getResources().getDrawable(R.drawable.muc_indicator_view);
            mucIndicator.setLevel(getMucIndicatorLevel());
        }

        /** set up NOTIFICATION MUTE */
        Resources resources = context.getResources();
        switch (getNotificationMode()) {
            case enabled:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(mucIndicator, null,
                        resources.getDrawable(R.drawable.ic_unmute), null);
                break;
            case disabled:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(mucIndicator, null,
                        resources.getDrawable(R.drawable.ic_mute), null);
                break;
            default:
                viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(
                        mucIndicator, null, null, null);
        }

        /** set up UNREAD COUNT */
        if (getUnreadCount() > 0) {
            viewHolder.tvUnreadCount.setText(String.valueOf(getUnreadCount()));
            viewHolder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else viewHolder.tvUnreadCount.setVisibility(View.INVISIBLE);

        if (isMute())
            viewHolder.tvUnreadCount.getBackground().mutate().setColorFilter(
                    resources.getColor(R.color.grey_500),
                    PorterDuff.Mode.SRC_IN);
        else viewHolder.tvUnreadCount.getBackground().mutate().clearColorFilter();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public int getStatusId() {
        return statusId;
    }

    public int getStatusLevel() {
        return statusLevel;
    }

    public Drawable getAvatar() {
        return avatar;
    }

    public int getMucIndicatorLevel() {
        return mucIndicatorLevel;
    }

    public UserJid getUserJid() {
        return userJid;
    }

    public AccountJid getAccountJid() {
        return accountJid;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public boolean isMute() {
        return mute;
    }

    public NotificationState.NotificationMode getNotificationMode() {
        return notificationMode;
    }

    public String getMessageText() {
        return messageText;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public Date getTime() {
        return time;
    }

    public int getMessageStatus() {
        return messageStatus;
    }

    public String getMessageOwner() {
        return messageOwner;
    }

    public boolean isArchived() {
        return archived;
    }

    public int getAccountColorIndicator() {
        return accountColorIndicator;
    }

    public boolean isShowOfflineShadow() {
        return showOfflineShadow;
    }

    public class ViewHolder extends FlexibleViewHolder implements View.OnCreateContextMenuListener {

        private final ContactClickListener listener;

        final View accountColorIndicator;
        final ImageView ivAvatar;
        final ImageView ivStatus;
        final ImageView ivOnlyStatus;
        final TextView tvContactName;
        final TextView tvOutgoingMessage;
        final TextView tvMessageText;
        final TextView tvTime;
        final ImageView ivMessageStatus;
        final ImageView offlineShadow;
        final TextView tvUnreadCount;
        public final TextView tvAction;
        public final TextView tvActionLeft;
        public final RelativeLayout foregroundView;

        public ViewHolder(View view, FlexibleAdapter adapter, ContactClickListener listener) {
            super(view, adapter);

            this.listener = listener;
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
            ivAvatar.setOnClickListener(this);
            ivStatus = (ImageView) view.findViewById(R.id.ivStatus);
            ivOnlyStatus = (ImageView) view.findViewById(R.id.ivOnlyStatus);
            tvContactName = (TextView) view.findViewById(R.id.tvContactName);
            tvOutgoingMessage = (TextView) view.findViewById(R.id.tvOutgoingMessage);
            tvMessageText = (TextView) view.findViewById(R.id.tvMessageText);
            tvTime = (TextView) view.findViewById(R.id.tvTime);
            ivMessageStatus = (ImageView) view.findViewById(R.id.ivMessageStatus);
            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
            tvUnreadCount = (TextView) view.findViewById(R.id.tvUnreadCount);
            foregroundView = (RelativeLayout) view.findViewById(R.id.foregroundView);
            tvAction = (TextView) view.findViewById(R.id.tvAction);
            tvActionLeft = (TextView) view.findViewById(R.id.tvActionLeft);
        }

        @Override
        public View getFrontView() {
            return foregroundView;
        }

        @Override
        public View getRearLeftView() {
            return tvActionLeft;
        }

        @Override
        public View getRearRightView() {
            return tvAction;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            listener.onContactCreateContextMenu(adapterPosition, menu);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.ivAvatar) {
                listener.onContactAvatarClick(getAdapterPosition());
            } else {
                super.onClick(view);
            }
        }
    }
}
