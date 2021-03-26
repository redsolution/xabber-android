package com.xabber.android.ui.fragment.contactListFragment.viewObjects;

/**
 * Created by valery.miller on 02.02.18.
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageStatus;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatAction;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.activity.SearchActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.chatListFragment.ChatListFragment;
import com.xabber.android.utils.StringUtils;
import com.xabber.android.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

public class ContactVO extends AbstractFlexibleItem<ContactVO.ViewHolder> {

    private final String id;

    private final int accountColorIndicator;
    private final int accountColorIndicatorBack;

    private final String name;
    private final String status;
    private final int statusId;
    private final int statusLevel;
    private final Drawable avatar;
    private final int mucIndicatorLevel;
    private final ContactJid contactJid;
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
    private final boolean isGroupchat;
    private final boolean isServer;
    private final boolean isBlocked;

    protected final ContactClickListener listener;

    public interface ContactClickListener {
        void onContactAvatarClick(int adapterPosition);
        void onContactCreateContextMenu(int adapterPosition, ContextMenu menu);
        void onContactButtonClick(int adapterPosition);
    }

    protected ContactVO(int accountColorIndicator, int accountColorIndicatorBack,
                        String name,
                        String status, int statusId, int statusLevel, Drawable avatar,
                        int mucIndicatorLevel, ContactJid contactJid, AccountJid accountJid, int unreadCount,
                        boolean mute, NotificationState.NotificationMode notificationMode, String messageText,
                        boolean isOutgoing, Date time, int messageStatus, String messageOwner,
                        boolean archived, String lastActivity, ContactClickListener listener,
                        int forwardedCount, boolean isCustomNotification, boolean isGroupchat, boolean isServer, boolean isBlocked) {
        this.id = UUID.randomUUID().toString();
        this.accountColorIndicator = accountColorIndicator;
        this.accountColorIndicatorBack = accountColorIndicatorBack;
        this.name = name;
        this.status = status;
        this.statusId = statusId;
        this.statusLevel = statusLevel;
        this.avatar = avatar;
        this.mucIndicatorLevel = mucIndicatorLevel;
        this.contactJid = contactJid;
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
        this.isGroupchat = isGroupchat;
        this.isServer = isServer;
        this.isBlocked = isBlocked;
    }

    public static ContactVO convert(AbstractContact contact, ContactClickListener listener) {
        int accountColorIndicator;
        int accountColorIndicatorLight;
        int accountColorIndicatorBack;
        Drawable avatar;
        int statusLevel;
        boolean isOutgoing = false;
        Date time = null;
        int messageStatus = 0;
        int unreadCount = 0;
        int forwardedCount = 0;
        String messageOwner = null;

        accountColorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(contact.getAccount());
        accountColorIndicatorLight = ColorManager.getInstance().getAccountPainter()
                .getAccountColorWithTint(contact.getAccount(), 300);
        accountColorIndicatorBack = ColorManager.getInstance().getAccountPainter()
                .getAccountIndicatorBackColor(contact.getAccount());
        avatar = contact.getAvatar();

        String name;
        AbstractChat chat = ChatManager.getInstance().getChat(contact.getAccount(), contact.getContactJid());
        if (chat instanceof GroupChat && !"".equals(((GroupChat)chat).getName()))
            name = ((GroupChat)chat).getName();
        else name = contact.getName();


        statusLevel = contact.getStatusMode().getStatusLevel();
        String messageText;
        String statusText = contact.getStatusText().trim();
        int statusId = contact.getStatusMode().getStringID();

        String lastActivity = "";

        if (chat == null)
            chat = ChatManager.getInstance().createRegularChat(contact.getAccount(), contact.getContactJid());
        MessageRealmObject lastMessage = chat.getLastMessage();

        if (lastMessage == null || lastMessage.getText() == null) {
            messageText = statusText;
            if (chat.getLastActionTimestamp() != null) time = new Date(chat.getLastActionTimestamp());
        } else {
            if (ChatStateManager.getInstance().getFullChatStateString(contact.getAccount(), contact.getContactJid()) != null) {
                String chatState = ChatStateManager.getInstance().getFullChatStateString(contact.getAccount(), contact.getContactJid());
                messageText = StringUtils.getColoredText(chatState, accountColorIndicatorLight);
            } else if (lastMessage.haveAttachments() && lastMessage.getAttachmentRealmObjects().size() > 0) {
                AttachmentRealmObject attachmentRealmObject = lastMessage.getAttachmentRealmObjects().get(0);
                if (attachmentRealmObject.isVoice()) {
                    StringBuilder voiceText = new StringBuilder();
                    voiceText.append(Application.getInstance().getResources().getString(R.string.voice_message));
                    if (attachmentRealmObject.getDuration() != null && attachmentRealmObject.getDuration() != 0) {
                        voiceText.append(String.format(Locale.getDefault(), ", %s", StringUtils.getDurationStringForVoiceMessage(null, attachmentRealmObject.getDuration())));
                    }
                    messageText = StringUtils.getColoredText(voiceText.toString(), accountColorIndicator);
                } else messageText = StringUtils.getColoredText(attachmentRealmObject.getTitle().trim(), accountColorIndicator);
            } else if (lastMessage.getAttachmentRealmObjects() != null
                    && lastMessage.getAttachmentRealmObjects().size() !=0
                    && lastMessage.getAttachmentRealmObjects().get(0).getFilePath() != null) {
                messageText = new File(lastMessage.getAttachmentRealmObjects().get(0).getFilePath()).getName();
            } else if (ChatAction.available.toString().equals(lastMessage.getAction())) {
                messageText = StringUtils.getColoredText(lastMessage.getText().trim(), accountColorIndicator);
            } else {
                messageText = lastMessage.getText().trim();
            }

            time = new Date(lastMessage.getTimestamp());

            isOutgoing = !lastMessage.isIncoming();

            // message status
            if (isOutgoing) {
                if ( !lastMessage.getMessageStatus().equals(MessageStatus.UPLOADING)
                        && !lastMessage.getMessageStatus().equals(MessageStatus.SENT)
                        && System.currentTimeMillis() - lastMessage.getTimestamp() > 1000) {
                    messageStatus = 5;
                } else if (lastMessage.getMessageStatus().equals(MessageStatus.DISPLAYED)) {
                    messageStatus = 1;
                } else if (lastMessage.getMessageStatus().equals(MessageStatus.RECEIVED)) {
                    messageStatus = 2;
                } else if (lastMessage.getMessageStatus().equals(MessageStatus.ERROR)) {
                    messageStatus = 4;
                } else if (lastMessage.getMessageStatus().equals(MessageStatus.DELIVERED) || lastMessage.isForwarded()) {
                    messageStatus = 3;
                } else messageStatus = 5;
            }

            // forwarded
            if (lastMessage.haveForwardedMessages()) {
                forwardedCount = lastMessage.getForwardedIds().size();
                if (messageText.isEmpty()) {
                    String forwardText = lastMessage.getFirstForwardedMessageText(accountColorIndicator);
                    if (forwardText != null) messageText = forwardText;
                }
            }
        }

        if (!isOutgoing) unreadCount = chat.getUnreadMessageCount();

        // notification icon
        NotificationState.NotificationMode mode =
                chat.getNotificationState().determineModeByGlobalSettings();

        // custom notification
        boolean isCustomNotification = CustomNotifyPrefsManager.getInstance().
                isPrefsExist(Key.createKey(contact.getAccount(), contact.getContactJid()));

        boolean isBlocked = BlockingManager.getInstance().contactIsBlockedLocally(contact.getAccount(), contact.getContactJid());

        return new ContactVO(accountColorIndicator, accountColorIndicatorBack,
                name, statusText, statusId,
                statusLevel, avatar, 0, contact.getContactJid(), contact.getAccount(),
                unreadCount, !chat.notifyAboutMessage(), mode, messageText, isOutgoing, time,
                messageStatus, messageOwner, chat.isArchived(), lastActivity, listener, forwardedCount,
                isCustomNotification, chat instanceof GroupChat,
                contact.getContactJid().getJid().isDomainBareJid(), isBlocked);
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
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, ViewHolder viewHolder, int position, List<Object> payloads) {
        Context context = viewHolder.itemView.getContext();

        /** set up ACCOUNT COLOR indicator */
        if (AccountManager.getInstance().getEnabledAccounts().size() > 1){
            viewHolder.accountColorIndicator.setBackgroundColor(getAccountColorIndicator());
            viewHolder.accountColorIndicatorBack.setBackgroundColor(getAccountColorIndicatorBack());
        } else {
            viewHolder.accountColorIndicator.setBackgroundColor(context.getResources().getColor(R.color.transparent));
            viewHolder.accountColorIndicatorBack.setBackgroundColor(context.getResources().getColor(R.color.transparent));
        }

        if (viewHolder.itemView.getBackground() != null){
            if (adapter.isSelected(position)) {
                LogManager.d("ListSelection", "item at pos = " + position + " is selected");
                viewHolder.itemView.getBackground().setColorFilter(new PorterDuffColorFilter(Color.parseColor("#75757575"), PorterDuff.Mode.SRC_IN));
            } else {
                viewHolder.itemView.getBackground().setColorFilter(null);
            }
        }

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

        int displayedStatus = getStatusLevel();
        if (isBlocked) displayedStatus = 11;
        else if (isServer) displayedStatus = 10;
        else if (isGroupchat) displayedStatus += StatusMode.PUBLIC_GROUP_OFFSET;

        viewHolder.ivStatus.setImageLevel(displayedStatus);
        viewHolder.ivOnlyStatus.setImageLevel(displayedStatus);

        if (viewHolder.tvStatus != null) {
            if (displayedStatus == 6) {
                viewHolder.tvStatus.setTextColor(ColorManager.getInstance().getColorContactSecondLine());
                viewHolder.ivStatus.setVisibility(View.INVISIBLE);
            } else {
                if (showAvatars) viewHolder.ivStatus.setVisibility(View.VISIBLE);
                viewHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_color_in_contact_list_online));
            }

            viewHolder.tvStatus.setText(getStatus().isEmpty() || displayedStatus == 6 ?
                    context.getString(getStatusId()) : getStatus());

            switch (displayedStatus) {
                case 6:
                    if (!getLastActivity().isEmpty()) {
                        viewHolder.tvStatus.setText(getLastActivity());
                    }
                    break;
                case 10:
                    viewHolder.tvStatus.setText("Server");
                    break;
                case 11:
                    viewHolder.tvStatus.setText(R.string.blocked_contact_status);
                    viewHolder.tvStatus.setTextColor(Color.RED);
                    if (viewHolder.ivAvatar.getVisibility() == View.VISIBLE) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            viewHolder.ivAvatar.setImageAlpha(128);
                        } else {
                            viewHolder.ivAvatar.setAlpha(0.5f);
                        }
                    }
                    viewHolder.tvContactName.setTextColor(Utils.getAttrColor(viewHolder.tvContactName.getContext(), R.attr.contact_list_contact_second_line_text_color));
                    break;
            }
            if (displayedStatus != 11) {
                viewHolder.tvContactName.setTextColor(Utils.getAttrColor(viewHolder.tvContactName.getContext(), R.attr.contact_list_contact_name_text_color));
                if (viewHolder.ivAvatar.getVisibility() == View.VISIBLE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        viewHolder.ivAvatar.setImageAlpha(128);
                    } else {
                        viewHolder.ivAvatar.setAlpha(0.5f);
                    }
                }
            }
        }

        /* Show grey jid instead of status in SearchActivity */
        if (listener instanceof ChatListFragment
                && ((ChatListFragment) listener).getActivity() instanceof SearchActivity
                && viewHolder.tvStatus != null){
            viewHolder.tvStatus.setTextColor(ColorManager.getInstance().getColorContactSecondLine());
            viewHolder.tvStatus.setText(contactJid.toString());
        }

        /** set up CONTACT/MUC NAME */
        viewHolder.tvContactName.setText(getName());

        /** set up NOTIFICATION MUTE */
        Resources resources = context.getResources();
        int resID = 0;
        NotificationState.NotificationMode mode = getNotificationMode();
        if (mode == NotificationState.NotificationMode.enabled) resID = R.drawable.ic_unmute;
        else if (mode == NotificationState.NotificationMode.disabled) resID = R.drawable.ic_mute;
        else if (mode != NotificationState.NotificationMode.byDefault) resID = R.drawable.ic_snooze_mini;
        viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(null, null,
                resID != 0 ? resources.getDrawable(resID) : null, null);

        /** set up CUSTOM NOTIFICATION */
        if (isCustomNotification() && (mode == NotificationState.NotificationMode.enabled
                || mode == NotificationState.NotificationMode.byDefault))
            viewHolder.tvContactName.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    resources.getDrawable(R.drawable.ic_notif_custom), null);

        //if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
        viewHolder.tvUnreadCount.getBackground().mutate().clearColorFilter();
        viewHolder.tvUnreadCount.setTextColor(context.getResources().getColor(R.color.white));
//        } else {
//            viewHolder.tvUnreadCount.getBackground().mutate().setColorFilter(
//                    resources.getColor(R.color.grey_700), PorterDuff.Mode.SRC_IN);
//            viewHolder.tvUnreadCount.setTextColor(context.getResources().getColor(R.color.black));
//        }

        /** set up UNREAD COUNT */
        if (getUnreadCount() > 0) {
            viewHolder.tvUnreadCount.setText(String.valueOf(getUnreadCount()));
            viewHolder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else viewHolder.tvUnreadCount.setVisibility(View.GONE);

        if (isMute())
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                viewHolder.tvUnreadCount.getBackground().mutate().setColorFilter(
                        resources.getColor(R.color.grey_500),
                        PorterDuff.Mode.SRC_IN);
                viewHolder.tvUnreadCount.setTextColor(context.getResources().getColor(R.color.grey_100));
            } else {
                viewHolder.tvUnreadCount.getBackground().mutate().setColorFilter(
                        resources.getColor(R.color.grey_700),
                        PorterDuff.Mode.SRC_IN);
                viewHolder.tvUnreadCount.setTextColor(context.getResources().getColor(R.color.black));
            }
        else if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            viewHolder.tvUnreadCount.getBackground().mutate().clearColorFilter();

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark)
            viewHolder.tvContactName.setTextColor(context.getResources().getColor(R.color.grey_200));
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

    public ContactJid getContactJid() {
        return contactJid;
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

    public String getLastActivity() {
        return lastActivity;
    }

    public boolean isCustomNotification() {
        return isCustomNotification;
    }

    public boolean isGroupchat() {
        return isGroupchat;
    }

    public boolean isServer() {
        return isServer;
    }

    public boolean isBlocked() {
        return isBlocked;
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
        //final ImageView ivStatusGroupchat;
        final TextView tvStatus;
        final TextView tvContactName;
        final TextView tvOutgoingMessage;
        final TextView tvMessageText;
        final TextView tvTime;
        final ImageView ivMessageStatus;
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
            //ivStatusGroupchat = (ImageView) view.findViewById(R.id.ivStatusGroupchat);
            tvStatus = (TextView) view.findViewById(R.id.tvStatus);
            tvContactName = (TextView) view.findViewById(R.id.tvContactName);
            tvOutgoingMessage = (TextView) view.findViewById(R.id.tvOutgoingMessage);
            tvMessageText = (TextView) view.findViewById(R.id.tvMessageText);
            tvTime = (TextView) view.findViewById(R.id.tvTime);
            ivMessageStatus = (ImageView) view.findViewById(R.id.ivMessageStatus);
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
