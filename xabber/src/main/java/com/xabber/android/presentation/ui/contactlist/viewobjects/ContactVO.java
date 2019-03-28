package com.xabber.android.presentation.ui.contactlist.viewobjects;

/**
 * Created by valery.miller on 02.02.18.
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.filedownload.FileCategory;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
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
    private int accountColorIndicatorBack;
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
    private final String lastActivity;
    private final boolean isCustomNotification;
    protected boolean archived;
    protected int forwardedCount;

    protected final ContactClickListener listener;

    public interface ContactClickListener {
        void onContactAvatarClick(int adapterPosition);
        void onContactCreateContextMenu(int adapterPosition, ContextMenu menu);
        void onContactButtonClick(int adapterPosition);
    }

    protected ContactVO(int accountColorIndicator, int accountColorIndicatorBack,
                        boolean showOfflineShadow, String name,
                        String status, int statusId, int statusLevel, Drawable avatar,
                        int mucIndicatorLevel, UserJid userJid, AccountJid accountJid, int unreadCount,
                        boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                        boolean isOutgoing, Date time, int messageStatus, String messageOwner,
                        boolean archived, String lastActivity, ContactClickListener listener,
                        int forwardedCount, boolean isCustomNotification) {
        this.id = UUID.randomUUID().toString();
        this.accountColorIndicator = accountColorIndicator;
        this.accountColorIndicatorBack = accountColorIndicatorBack;
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
        this.lastActivity = lastActivity;
        this.listener = listener;
        this.forwardedCount = forwardedCount;
        this.isCustomNotification = isCustomNotification;
    }

    public static ContactVO convert(AbstractContact contact, ContactClickListener listener) {
        boolean showOfflineShadow;
        int accountColorIndicator;
        int accountColorIndicatorBack;
        Drawable avatar;
        int statusLevel;
        int mucIndicatorLevel;
        boolean isOutgoing = false;
        Date time = null;
        int messageStatus = 0;
        int unreadCount = 0;
        int forwardedCount = 0;
        String messageOwner = null;

        AccountItem accountItem = AccountManager.getInstance().getAccount(contact.getAccount());
        if (accountItem != null && accountItem.getState() == ConnectionState.connected) {
            showOfflineShadow = false;
        } else {
            showOfflineShadow = true;
        }

        accountColorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(contact.getAccount());
        accountColorIndicatorBack = ColorManager.getInstance().getAccountPainter()
                .getAccountIndicatorBackColor(contact.getAccount());
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

        String lastActivity = "";
        if (contact instanceof RosterContact)
             lastActivity = ((RosterContact) contact).getLastActivity();

        MessageManager messageManager = MessageManager.getInstance();
        AbstractChat chat = messageManager.getOrCreateChat(contact.getAccount(), contact.getUser());
        MessageItem lastMessage = chat.getLastMessage();

        if (lastMessage == null) {
            messageText = statusText;
        } else {
            if (lastMessage.haveAttachments() && lastMessage.getAttachments().size() > 0) {
                Attachment attachment = lastMessage.getAttachments().get(0);
                FileCategory category = FileCategory.determineFileCategory(attachment.getMimeType());
                messageText = FileCategory.getCategoryName(category, true) + attachment.getTitle();
            } else if (lastMessage.getFilePath() != null) {
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

            // forwarded
            if (lastMessage.haveForwardedMessages()) forwardedCount = lastMessage.getForwardedIds().size();
        }

        if (!isOutgoing) unreadCount = chat.getUnreadMessageCount();

        // notification icon
        NotificationState.NotificationMode mode =
                chat.getNotificationState().determineModeByGlobalSettings(chat instanceof RoomChat);

        // custom notification
        boolean isCustomNotification = CustomNotifyPrefsManager.getInstance().
                isPrefsExist(Key.createKey(contact.getAccount(), contact.getUser()));

        return new ContactVO(accountColorIndicator, accountColorIndicatorBack,
                showOfflineShadow, name, statusText, statusId,
                statusLevel, avatar, mucIndicatorLevel, contact.getUser(), contact.getAccount(),
                unreadCount, !chat.notifyAboutMessage(), mode, messageText, isOutgoing, time,
                messageStatus, messageOwner, chat.isArchived(), lastActivity, listener, forwardedCount,
                isCustomNotification);
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
        return R.layout.item_contact_in_contact_list;
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
        viewHolder.accountColorIndicatorBack.setBackgroundColor(getAccountColorIndicatorBack());

        /** set up AVATAR */
        boolean showAvatars = SettingsManager.contactsShowAvatars();
        if (showAvatars) {
            viewHolder.ivAvatar.setVisibility(View.VISIBLE);
            viewHolder.ivAvatar.setImageDrawable(getAvatar());
            viewHolder.ivOnlyStatus.setVisibility(View.GONE);
        } else {
            viewHolder.ivAvatar.setVisibility(View.GONE);
            viewHolder.ivStatus.setVisibility(View.GONE);
            viewHolder.ivOnlyStatus.setVisibility(View.VISIBLE);
        }

        /** set up ROSTER STATUS */
        if (getStatusLevel() == 6 ||
                (getMucIndicatorLevel() != 0 && getStatusLevel() != 1)) {
            if (viewHolder.tvStatus != null)
                viewHolder.tvStatus.setTextColor(ColorManager.getInstance().getColorContactSecondLine());
            viewHolder.ivStatus.setVisibility(View.INVISIBLE);
        } else {
            if (showAvatars) viewHolder.ivStatus.setVisibility(View.VISIBLE);
            if (viewHolder.tvStatus != null)
                viewHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_color_in_contact_list_online));
        }
        viewHolder.ivStatus.setImageLevel(getStatusLevel());
        viewHolder.ivOnlyStatus.setImageLevel(getStatusLevel());
        if (viewHolder.tvStatus != null) viewHolder.tvStatus.setText(getStatus().isEmpty()
                ? context.getString(getStatusId()) : getStatus());

        if ((getStatusLevel() == 6 || (getMucIndicatorLevel() != 0 && getStatusLevel() != 1))
                && !getLastActivity().isEmpty())
            if (viewHolder.tvStatus != null) viewHolder.tvStatus.setText(getLastActivity());

        /** set up CONTACT/MUC NAME */
        viewHolder.tvContactName.setText(getName());

        /** set up MUC indicator */
        Drawable mucIndicator;
        if (getMucIndicatorLevel() == 0)
            mucIndicator = null;
        else {
            mucIndicator = context.getResources().getDrawable(R.drawable.muc_indicator_view);
            mucIndicator.setLevel(getMucIndicatorLevel());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mucIndicator.setTint(context.getResources().getColor(getThemeResource(context,
                        R.attr.contact_list_contact_name_text_color)));
            }
        }

        /** set up NOTIFICATION MUTE */
        Resources resources = context.getResources();
        int resID = 0;
        NotificationState.NotificationMode mode = getNotificationMode();
        if (mode == NotificationState.NotificationMode.enabled) resID = R.drawable.ic_unmute;
        else if (mode == NotificationState.NotificationMode.disabled) resID = R.drawable.ic_mute;
        else if (mode != NotificationState.NotificationMode.bydefault) resID = R.drawable.ic_snooze_mini;
        viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(mucIndicator, null,
                resID != 0 ? resources.getDrawable(resID) : null, null);

        /** set up CUSTOM NOTIFICATION */
        if (isCustomNotification() && (mode == NotificationState.NotificationMode.enabled
                || mode == NotificationState.NotificationMode.bydefault))
            viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(mucIndicator, null,
                    resources.getDrawable(R.drawable.ic_notif_custom), null);

        /** set up UNREAD COUNT */
        if (getUnreadCount() > 0) {
            viewHolder.tvUnreadCount.setText(String.valueOf(getUnreadCount()));
            viewHolder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else viewHolder.tvUnreadCount.setVisibility(View.GONE);

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

    public int getAccountColorIndicatorBack() {
        return accountColorIndicatorBack;
    }

    public boolean isShowOfflineShadow() {
        return showOfflineShadow;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public boolean isCustomNotification() {
        return isCustomNotification;
    }

    private int getThemeResource(Context context, int themeResourceId) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }

    public class ViewHolder extends FlexibleViewHolder implements View.OnCreateContextMenuListener {

        private final ContactClickListener listener;

        final View accountColorIndicator;
        final View accountColorIndicatorBack;
        final ImageView ivAvatar;
        final ImageView ivStatus;
        final ImageView ivOnlyStatus;
        final TextView tvStatus;
        final TextView tvContactName;
        final TextView tvOutgoingMessage;
        final TextView tvMessageText;
        final TextView tvTime;
        final ImageView ivMessageStatus;
        final ImageView offlineShadow;
        final TextView tvUnreadCount;
        public final TextView tvAction;
        public final TextView tvActionLeft;
        public final LinearLayout foregroundView;
        final Button btnListAction;

        public ViewHolder(View view, FlexibleAdapter adapter, ContactClickListener listener) {
            super(view, adapter);

            this.listener = listener;
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            accountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
            ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
            ivAvatar.setOnClickListener(this);
            ivStatus = (ImageView) view.findViewById(R.id.ivStatus);
            ivOnlyStatus = (ImageView) view.findViewById(R.id.ivOnlyStatus);
            tvStatus = (TextView) view.findViewById(R.id.tvStatus);
            tvContactName = (TextView) view.findViewById(R.id.tvContactName);
            tvOutgoingMessage = (TextView) view.findViewById(R.id.tvOutgoingMessage);
            tvMessageText = (TextView) view.findViewById(R.id.tvMessageText);
            tvTime = (TextView) view.findViewById(R.id.tvTime);
            ivMessageStatus = (ImageView) view.findViewById(R.id.ivMessageStatus);
            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
            tvUnreadCount = (TextView) view.findViewById(R.id.tvUnreadCount);
            foregroundView = (LinearLayout) view.findViewById(R.id.foregroundView);
            tvAction = (TextView) view.findViewById(R.id.tvAction);
            tvActionLeft = (TextView) view.findViewById(R.id.tvActionLeft);
            btnListAction = (Button) view.findViewById(R.id.btnListAction);
            if (btnListAction != null) btnListAction.setOnClickListener(this);
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
            } else if (view.getId() == R.id.btnListAction) {
                listener.onContactButtonClick(getAdapterPosition());
            } else {
                super.onClick(view);
            }
        }
    }

    /** Use only in RecentChatFragment for dynamic update item */
    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
