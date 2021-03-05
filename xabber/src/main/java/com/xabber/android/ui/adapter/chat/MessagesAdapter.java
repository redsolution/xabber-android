package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.groups.GroupMember;
import com.xabber.android.data.groups.GroupMemberManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class MessagesAdapter extends RealmRecyclerViewAdapter<MessageRealmObject, BasicMessageVH>
        implements MessageVH.MessageClickListener, MessageVH.MessageLongClickListener,
        FileMessageVH.FileListener, IncomingMessageVH.OnMessageAvatarClickListener {

    private static final String LOG_TAG = MessagesAdapter.class.getSimpleName();

    public static final int VIEW_TYPE_INCOMING_MESSAGE = 2;
    public static final int VIEW_TYPE_INCOMING_MESSAGE_NOFLEX = 5;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE = 3;
    private static final int VIEW_TYPE_ACTION_MESSAGE = 4;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX = 6;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE_IMAGE = 7;
    public static final int VIEW_TYPE_INCOMING_MESSAGE_IMAGE = 8;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT = 9;
    public static final int VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT = 10;
    public static final int VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE = 11;

    private final Context context;
    private final MessageVH.MessageClickListener messageListener;
    private final FileMessageVH.FileListener fileListener;
    private final ForwardedAdapter.ForwardListener fwdListener;
    private final Listener listener;
    private final IncomingMessageVH.BindListener bindListener;
    private final IncomingMessageVH.OnMessageAvatarClickListener onMessageAvatarClickListener;

    // message font style
    private final int appearanceStyle = SettingsManager.chatsAppearanceStyle();
    private final int accountMainColor;
    private final ColorStateList colorStateList;
    private final int mentionColor;
    private final String userName;
    private final AccountJid account;
    private int prevItemCount;
    private String prevFirstItemId;
    private String firstUnreadMessageID;
    private boolean isCheckMode;

    private RecyclerView recyclerView;
    private final List<String> itemsNeedOriginalText = new ArrayList<>();
    private final List<String> checkedItemIds = new ArrayList<>();
    private final List<MessageRealmObject> checkedMessageRealmObjects = new ArrayList<>();

    public interface Listener {
        void onMessagesUpdated();
        void onChangeCheckedItems(int checkedItems);
        int getLastVisiblePosition();
        void scrollTo(int position);
    }

    public MessagesAdapter(
            Context context, RealmResults<MessageRealmObject> messageRealmObjects,
            AbstractChat chat, MessageVH.MessageClickListener messageListener,
            FileMessageVH.FileListener fileListener, ForwardedAdapter.ForwardListener fwdListener,
            Listener listener, IncomingMessageVH.BindListener bindListener,
            IncomingMessageVH.OnMessageAvatarClickListener avatarClickListener) {
        super(context, messageRealmObjects, true);

        this.context = context;
        this.messageListener = messageListener;
        this.fileListener = fileListener;
        this.fwdListener = fwdListener;
        this.listener = listener;
        this.bindListener = bindListener;
        this.onMessageAvatarClickListener = avatarClickListener;

        account = chat.getAccount();
        ContactJid user = chat.getContactJid();
        userName = RosterManager.getInstance().getName(account, user);
        prevItemCount = getItemCount();
        prevFirstItemId = getFirstMessageId();
        accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
        colorStateList = ColorManager.getInstance().getChatIncomingBalloonColorsStateList(account);
        mentionColor = ColorManager.getInstance().getAccountPainter().getAccountIndicatorBackColor(account);
    }

    @Override
    public int getItemCount() {
        if (realmResults.isValid() && realmResults.isLoaded())
            return realmResults.size();
        else return 0;
    }

    private String getFirstMessageId() {
        if (realmResults.isValid() && realmResults.isLoaded() && realmResults.size() > 0)
            return Objects.requireNonNull(realmResults.first()).getUniqueId();
        else return null;
    }

    @Override
    public int getItemViewType(int position) {
        MessageRealmObject messageRealmObject = getMessageItem(position);
        if (messageRealmObject == null) return 0;

        if (messageRealmObject.getAction() != null)
            return VIEW_TYPE_ACTION_MESSAGE;

        if (messageRealmObject.isGroupchatSystem())
            return VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE;

        // if noFlex is true, should use special layout without flexbox-style text
        boolean isUploadMessage = messageRealmObject.getText().equals(FileMessageVH.UPLOAD_TAG);
        boolean noFlex = messageRealmObject.haveForwardedMessages() || messageRealmObject.haveAttachments();
        boolean isImage = messageRealmObject.hasImage();
        boolean notJustImage = (!messageRealmObject.getText().trim().isEmpty() && !isUploadMessage)
                || (!messageRealmObject.isAttachmentImageOnly());

        if (messageRealmObject.isIncoming()) {
            if(isImage) {
                return notJustImage? VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT : VIEW_TYPE_INCOMING_MESSAGE_IMAGE;
            } else return noFlex ? VIEW_TYPE_INCOMING_MESSAGE_NOFLEX : VIEW_TYPE_INCOMING_MESSAGE;

        } else if(isImage)
            return notJustImage? VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT : VIEW_TYPE_OUTGOING_MESSAGE_IMAGE;
        else return noFlex ? VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX : VIEW_TYPE_OUTGOING_MESSAGE;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NotNull
    @Override
    public BasicMessageVH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_ACTION_MESSAGE:
                return new ActionMessageVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_system_message, parent, false));

            case VIEW_TYPE_INCOMING_MESSAGE:
                return new IncomingMessageVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_incoming, parent, false),
                        this, this, this, bindListener,
                        this, appearanceStyle);

            case VIEW_TYPE_INCOMING_MESSAGE_NOFLEX:
                return new NoFlexIncomingMsgVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_incoming_noflex, parent, false),
                        this, this, this, bindListener,
                        this, appearanceStyle);

            case VIEW_TYPE_OUTGOING_MESSAGE:
                return new OutgoingMessageVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_outgoing, parent, false),
                        this, this, this, appearanceStyle);

            case VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX:
                return new NoFlexOutgoingMsgVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_outgoing_noflex, parent, false),
                        this, this, this, appearanceStyle);

            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE:
                return new NoFlexOutgoingMsgVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_outgoing_image, parent, false),
                        this, this, this, appearanceStyle);

            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE:
                return new NoFlexIncomingMsgVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_incoming_image, parent, false),
                        this, this, this, bindListener,
                        this, appearanceStyle);

            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT:
                return new NoFlexIncomingMsgVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_incoming_image_text, parent, false),
                        this, this, this, bindListener,
                        this, appearanceStyle);

            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT:
                return new NoFlexOutgoingMsgVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_outgoing_image_text, parent, false),
                        this, this, this, appearanceStyle);

            case VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE:
                return new GroupchatSystemMessageVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_system_message, parent, false));
            default: return null;
        }
    }

    @Override
    public void onBindViewHolder(@NotNull final BasicMessageVH holder, int position) {

        final int viewType = getItemViewType(position);
        MessageRealmObject messageRealmObject = getMessageItem(position);

        if (messageRealmObject == null) {
            LogManager.w(LOG_TAG, "onBindViewHolder Null message item. Position: " + position);
            return;
        }

        // setup message uniqueId
        if (holder instanceof MessageVH)
            ((MessageVH)holder).messageId = messageRealmObject.getUniqueId();

        // setup message as unread
        boolean unread = messageRealmObject.getUniqueId().equals(firstUnreadMessageID);

        // setup message as checked
        boolean checked = checkedItemIds.contains(messageRealmObject.getUniqueId());

        // need show original OTR message
        boolean showOriginalOTR = itemsNeedOriginalText.contains(messageRealmObject.getUniqueId());

        // groupchat user
        GroupMember groupMember =
                GroupMemberManager.getInstance().getGroupMemberById(messageRealmObject.getGroupchatUserId());

        // need tail
        boolean needTail = false;
        if (groupMember != null) {
            MessageRealmObject nextMessage = getMessageItem(position + 1);
            if (nextMessage != null) {
                GroupMember user2 =
                        GroupMemberManager.getInstance().getGroupMemberById(nextMessage.getGroupchatUserId());

                if (user2 != null) needTail = !groupMember.getId().equals(user2.getId());
                else needTail = true;
            } else needTail = true;
        } else if (viewType != VIEW_TYPE_ACTION_MESSAGE) {
            needTail = getSimpleType(viewType) != getSimpleType(getItemViewType(position + 1));
        }

        // need date, need name
        boolean needDate;
        boolean needName;
        MessageRealmObject previousMessage = getMessageItem(position - 1);
        if (previousMessage != null) {
            needDate = !Utils.isSameDay(messageRealmObject.getTimestamp(), previousMessage.getTimestamp());
            if (messageRealmObject.getGroupchatUserId() != null
                    && !messageRealmObject.getGroupchatUserId().isEmpty()
                    && previousMessage.getGroupchatUserId() != null
                    && !previousMessage.getGroupchatUserId().isEmpty()){
                needName = !messageRealmObject.getGroupchatUserId().equals(previousMessage.getGroupchatUserId());
            } else needName = true;
        } else {
            needDate = true;
            needName = true;
        }


        Long mainMessageTimestamp = messageRealmObject.getTimestamp();

        MessageExtraData extraData = new MessageExtraData(fileListener, fwdListener, context, userName, colorStateList,
                groupMember, accountMainColor, mentionColor, mainMessageTimestamp, showOriginalOTR, unread, checked,
                needTail, needDate, needName);

        switch (viewType) {
            case VIEW_TYPE_ACTION_MESSAGE:
                if (holder instanceof ActionMessageVH) {
                    ((ActionMessageVH)holder).bind(messageRealmObject, context, account, needDate);
                }
                break;

            case VIEW_TYPE_INCOMING_MESSAGE:
                if (holder instanceof IncomingMessageVH) {
                    ((IncomingMessageVH)holder).bind(messageRealmObject, extraData);
                }
                break;

            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT:
            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE:
            case VIEW_TYPE_INCOMING_MESSAGE_NOFLEX:
                if (holder instanceof NoFlexIncomingMsgVH) {
                    ((NoFlexIncomingMsgVH)holder).bind(messageRealmObject, extraData);
                }
                break;

            case VIEW_TYPE_OUTGOING_MESSAGE:
                if (holder instanceof OutgoingMessageVH) {
                    ((OutgoingMessageVH)holder).bind(messageRealmObject, extraData);
                }
                break;

            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT:
            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE:
            case VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX:
                if (holder instanceof NoFlexOutgoingMsgVH) {
                    ((NoFlexOutgoingMsgVH)holder).bind(messageRealmObject, extraData);
                }
                break;
            case VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE:
                if (holder instanceof GroupchatSystemMessageVH) {
                    ((GroupchatSystemMessageVH)holder).bind(messageRealmObject);
                }
        }
    }

    @Override
    public void onChange() {
        int lastPosition = listener.getLastVisiblePosition();
        String firstMessageId = getFirstMessageId();
        notifyDataSetChanged();
        listener.onMessagesUpdated();

        int itemCount = getItemCount();
        if (prevItemCount != itemCount) {
            if (firstMessageId != null && !firstMessageId.equals(prevFirstItemId)) {
                listener.scrollTo(lastPosition + (itemCount - prevItemCount));
            } else if (lastPosition == prevItemCount - 1) listener.scrollTo(itemCount - 1);
            prevItemCount = itemCount;
            prevFirstItemId = firstMessageId;
        }
    }

    @Nullable
    public MessageRealmObject getMessageItem(int position) {
        if (position == RecyclerView.NO_POSITION) return null;
        if (position < realmResults.size()) return realmResults.get(position);
        else return null;
    }

    @Override
    public void onMessageClick(View caller, int position) {
        if (isCheckMode && !recyclerView.isComputingLayout()) {
            addOrRemoveCheckedItem(position);
        } else messageListener.onMessageClick(caller, position);
    }

    @Override
    public void onLongMessageClick(int position) {
        addOrRemoveCheckedItem(position);
    }

    @Override
    public void onMessageAvatarClick(int position) {
        if (isCheckMode && !recyclerView.isComputingLayout()) {
            addOrRemoveCheckedItem(position);
        } else onMessageAvatarClickListener.onMessageAvatarClick(position);
    }

    public void setFirstUnreadMessageId(String id) {
        firstUnreadMessageID = id;
    }

    public void addOrRemoveItemNeedOriginalText(String messageId) {
        if (itemsNeedOriginalText.contains(messageId)) {
            itemsNeedOriginalText.remove(messageId);
        } else itemsNeedOriginalText.add(messageId);
    }

    /** File listener */

    @Override
    public void onImageClick(int messagePosition, int attachmentPosition, String messageUID) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition);
        } else fileListener.onImageClick(messagePosition, attachmentPosition, messageUID);
    }

    @Override
    public void onFileClick(int messagePosition, int attachmentPosition, String messageUID) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition);
        } else fileListener.onFileClick(messagePosition, attachmentPosition, messageUID);
    }

    @Override
    public void onVoiceClick(int messagePosition, int attachmentPosition, String attachmentId, String messageUID,
                             Long timestamp) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition);
        } else fileListener.onVoiceClick(messagePosition, attachmentPosition, attachmentId, messageUID, timestamp);
    }

    @Override
    public void onFileLongClick(AttachmentRealmObject attachmentRealmObject, View caller) {
        fileListener.onFileLongClick(attachmentRealmObject, caller);
    }

    @Override
    public void onDownloadCancel() {
        fileListener.onDownloadCancel();
    }

    @Override
    public void onUploadCancel() {
        fileListener.onUploadCancel();
    }

    @Override
    public void onDownloadError(String error) {
        fileListener.onDownloadError(error);
    }

    /** Checked items */
    private void addOrRemoveCheckedItem(int position) {
        if (recyclerView.isComputingLayout() || recyclerView.isAnimating()) return;

        recyclerView.stopScroll();
        MessageRealmObject messageRealmObject = getItem(position);
        String uniqueId = messageRealmObject.getUniqueId();

        if (checkedItemIds.contains(uniqueId)){
            checkedMessageRealmObjects.remove(messageRealmObject);
            checkedItemIds.remove(uniqueId);
        } else {
            checkedItemIds.add(uniqueId);
            checkedMessageRealmObjects.add(messageRealmObject);
        }

        boolean isCheckModePrevious = isCheckMode;
        isCheckMode = checkedItemIds.size() > 0;

        if (isCheckMode != isCheckModePrevious) {
            notifyDataSetChanged();
        } else notifyItemChanged(position);

        listener.onChangeCheckedItems(checkedItemIds.size());
    }

    public List<String> getCheckedItemIds() {
        return checkedItemIds;
    }

    public List<MessageRealmObject> getCheckedMessageRealmObjects(){
        return checkedMessageRealmObjects;
    }

    public void resetCheckedItems() {
        if (checkedItemIds.size() > 0) {
            checkedItemIds.clear();
            checkedMessageRealmObjects.clear();
            isCheckMode = false;
            notifyDataSetChanged();
            listener.onChangeCheckedItems(checkedItemIds.size());
        }
    }

    /** Message Extra Data */
    public static class MessageExtraData {

        private final Context context;
        private final FileMessageVH.FileListener listener;
        private final ForwardedAdapter.ForwardListener fwdListener;
        private final String username;
        private final ColorStateList colorStateList;
        private final int accountMainColor;
        private final int mentionColor;
        private final Long mainTimestamp;
        private final GroupMember groupMember;

        private final boolean showOriginalOTR;
        private final boolean unread;
        private final boolean checked;
        private final boolean needTail;
        private final boolean needDate;
        private final boolean needName;

        public MessageExtraData(FileMessageVH.FileListener listener,
                                ForwardedAdapter.ForwardListener fwdListener,
                                Context context, String username, ColorStateList colorStateList,
                                GroupMember groupMember, int accountMainColor, int mentionColor, Long mainTimestamp,
                                boolean showOriginalOTR, boolean unread, boolean checked,
                                boolean needTail, boolean needDate, boolean needName) {
            this.listener = listener;
            this.fwdListener = fwdListener;
            this.context = context;
            this.username = username;
            this.colorStateList = colorStateList;
            this.accountMainColor = accountMainColor;
            this.mentionColor = mentionColor;
            this.showOriginalOTR = showOriginalOTR;
            this.unread = unread;
            this.checked = checked;
            this.needTail = needTail;
            this.needDate = needDate;
            this.groupMember = groupMember;
            this.mainTimestamp = mainTimestamp;
            this.needName = needName;
        }

        public FileMessageVH.FileListener getListener() {
            return listener;
        }

        public ForwardedAdapter.ForwardListener getFwdListener() {
            return fwdListener;
        }

        public Context getContext() {
            return context;
        }

        public String getUsername() {
            return username;
        }

        public ColorStateList getColorStateList() {
            return colorStateList;
        }

        public int getAccountMainColor() {
            return accountMainColor;
        }

        public int getMentionColor() {
            return mentionColor;
        }

        public GroupMember getGroupMember() {
            return groupMember;
        }

        public Long getMainMessageTimestamp() {
            return mainTimestamp;
        }

        public boolean isShowOriginalOTR() {
            return showOriginalOTR;
        }

        public boolean isUnread() {
            return unread;
        }

        public boolean isChecked() {
            return checked;
        }

        public boolean isNeedTail() {
            return needTail;
        }

        public boolean isNeedDate() {
            return needDate;
        }

        public boolean isNeedName() { return needName; }
    }

    private int getSimpleType(int type) {
        switch (type) {
            case VIEW_TYPE_INCOMING_MESSAGE:
            case VIEW_TYPE_INCOMING_MESSAGE_NOFLEX:
            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE:
            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT:
                return 1;

            case VIEW_TYPE_OUTGOING_MESSAGE:
            case VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX:
            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE:
            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT:
                return 2;

            case VIEW_TYPE_ACTION_MESSAGE:
                return 3;

            default:
                return 0;
        }
    }

}
