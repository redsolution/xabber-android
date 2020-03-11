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
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.groupchat.GroupchatUser;
import com.xabber.android.data.groupchat.GroupchatUserManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class MessagesAdapter extends RealmRecyclerViewAdapter<MessageRealmObject, BasicMessageVH>
        implements MessageVH.MessageClickListener, MessageVH.MessageLongClickListener,
        FileMessageVH.FileListener {

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

    private final Context context;
    private final MessageVH.MessageClickListener messageListener;
    private final FileMessageVH.FileListener fileListener;
    private final ForwardedAdapter.ForwardListener fwdListener;
    private final Listener listener;
    private final IncomingMessageVH.BindListener bindListener;

    // message font style
    private final int appearanceStyle = SettingsManager.chatsAppearanceStyle();
    private int accountMainColor;
    private ColorStateList colorStateList;
    private int mentionColor;
    private String userName;
    private AccountJid account;
    private UserJid user;
    private Long mainMessageTimestamp;
    private int prevItemCount;
    private String prevFirstItemId;
    private String firstUnreadMessageID;
    private boolean isCheckMode;

    private RecyclerView recyclerView;
    private List<String> itemsNeedOriginalText = new ArrayList<>();
    private List<String> checkedItemIds = new ArrayList<>();
    private List<MessageRealmObject> checkedMessageRealmObjects = new ArrayList<>();

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
            Listener listener, IncomingMessageVH.BindListener bindListener) {
        super(context, messageRealmObjects, true);

        this.context = context;
        this.messageListener = messageListener;
        this.fileListener = fileListener;
        this.fwdListener = fwdListener;
        this.listener = listener;
        this.bindListener = bindListener;

        account = chat.getAccount();
        user = chat.getUser();
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
            return realmResults.first().getUniqueId();
        else return null;
    }

    @Override
    public int getItemViewType(int position) {
        MessageRealmObject messageRealmObject = getMessageItem(position);
        if (messageRealmObject == null) return 0;

        if (messageRealmObject.getAction() != null)
            return VIEW_TYPE_ACTION_MESSAGE;

        // if noFlex is true, should use special layout without flexbox-style text
        boolean isUploadMessage = messageRealmObject.getText().equals(FileMessageVH.UPLOAD_TAG);
        boolean noFlex = messageRealmObject.haveForwardedMessages() || messageRealmObject.haveAttachments();
        boolean isImage = messageRealmObject.hasImage();
        boolean notJustImage = (!messageRealmObject.getText().trim().isEmpty() && !isUploadMessage) || (!messageRealmObject.isAttachmentImageOnly());

        if (messageRealmObject.isIncoming()) {
            if(isImage) {
                return notJustImage? VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT : VIEW_TYPE_INCOMING_MESSAGE_IMAGE;
            }else return noFlex ? VIEW_TYPE_INCOMING_MESSAGE_NOFLEX : VIEW_TYPE_INCOMING_MESSAGE;

        } else if(isImage) return notJustImage? VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT : VIEW_TYPE_OUTGOING_MESSAGE_IMAGE;
        else return noFlex ? VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX : VIEW_TYPE_OUTGOING_MESSAGE;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public BasicMessageVH onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_ACTION_MESSAGE:
                return new ActionMessageVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_action_message, parent, false));

            case VIEW_TYPE_INCOMING_MESSAGE:
                return new IncomingMessageVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_incoming, parent, false),
                        this, this, this, bindListener, appearanceStyle);

            case VIEW_TYPE_INCOMING_MESSAGE_NOFLEX:
                return new NoFlexIncomingMsgVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_incoming_noflex, parent, false),
                        this, this, this, bindListener, appearanceStyle);

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
                        this, this, this, bindListener, appearanceStyle);

            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT:
                return new NoFlexIncomingMsgVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_incoming_image_text, parent, false),
                        this, this, this, bindListener, appearanceStyle);

            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT:
                return new NoFlexOutgoingMsgVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_outgoing_image_text, parent, false),
                        this, this, this, appearanceStyle);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final BasicMessageVH holder, int position) {

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
        GroupchatUser groupchatUser = GroupchatUserManager.getInstance().getGroupchatUser(messageRealmObject.getGroupchatUserId());

        // need tail
        boolean needTail = false;
        if (groupchatUser != null) {
            MessageRealmObject nextMessage = getMessageItem(position + 1);
            if (nextMessage != null) {
                GroupchatUser user2 = GroupchatUserManager.getInstance().getGroupchatUser(nextMessage.getGroupchatUserId());
                if (user2 != null) needTail = !groupchatUser.getId().equals(user2.getId());
                else needTail = true;
            } else needTail = true;
        } else if (viewType != VIEW_TYPE_ACTION_MESSAGE) {
            needTail = getSimpleType(viewType) != getSimpleType(getItemViewType(position + 1));
        }

        // need date
        boolean needDate;
        MessageRealmObject previousMessage = getMessageItem(position - 1);
        if (previousMessage != null) {
            needDate = !Utils.isSameDay(messageRealmObject.getTimestamp(), previousMessage.getTimestamp());
        } else needDate = true;

        mainMessageTimestamp = messageRealmObject.getTimestamp();

        MessageExtraData extraData = new MessageExtraData(fileListener, fwdListener,
                context, userName, colorStateList, groupchatUser, accountMainColor, mentionColor, mainMessageTimestamp,
                showOriginalOTR, unread, checked, needTail, needDate);

        switch (viewType) {
            case VIEW_TYPE_ACTION_MESSAGE:
                ((ActionMessageVH)holder).bind(messageRealmObject, context, account, needDate);
                break;

            case VIEW_TYPE_INCOMING_MESSAGE:
                ((IncomingMessageVH)holder).bind(messageRealmObject, extraData);
                break;

            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT:
            case VIEW_TYPE_INCOMING_MESSAGE_IMAGE:
            case VIEW_TYPE_INCOMING_MESSAGE_NOFLEX:
                ((NoFlexIncomingMsgVH)holder).bind(messageRealmObject, extraData);
                break;

            case VIEW_TYPE_OUTGOING_MESSAGE:
                ((OutgoingMessageVH)holder).bind(messageRealmObject, extraData);
                break;

            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT:
            case VIEW_TYPE_OUTGOING_MESSAGE_IMAGE:
            case VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX:
                ((NoFlexOutgoingMsgVH)holder).bind(messageRealmObject, extraData);
                break;

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
            if (firstMessageId != null && !firstMessageId.equals(prevFirstItemId))
                listener.scrollTo(lastPosition + (itemCount - prevItemCount));
            else if (lastPosition == prevItemCount - 1)
                listener.scrollTo(itemCount - 1);

            prevItemCount = itemCount;
            prevFirstItemId = firstMessageId;
        }
    }

    @Nullable
    public MessageRealmObject getMessageItem(int position) {
        if (position == RecyclerView.NO_POSITION) return null;

        if (position < realmResults.size())
            return realmResults.get(position);
        else return null;
    }

    @Override
    public void onMessageClick(View caller, int position) {
        if (isCheckMode && !recyclerView.isComputingLayout()) addOrRemoveCheckedItem(position);
        else messageListener.onMessageClick(caller, position);
    }

    @Override
    public void onLongMessageClick(int position) {
        addOrRemoveCheckedItem(position);
    }

    public int findMessagePosition(String uniqueId) {
        for (int i = 0; i < realmResults.size(); i++) {
            if (realmResults.get(i).getUniqueId().equals(uniqueId)) return i;
        }
        return RecyclerView.NO_POSITION;
    }

    public void setFirstUnreadMessageId(String id) {
        firstUnreadMessageID = id;
    }

    public void addOrRemoveItemNeedOriginalText(String messageId) {
        if (itemsNeedOriginalText.contains(messageId))
            itemsNeedOriginalText.remove(messageId);
        else itemsNeedOriginalText.add(messageId);
    }

    /** File listener */

    @Override
    public void onImageClick(int messagePosition, int attachmentPosition, String messageUID) {
        if (isCheckMode) addOrRemoveCheckedItem(messagePosition);
        else fileListener.onImageClick(messagePosition, attachmentPosition, messageUID);
    }

    @Override
    public void onFileClick(int messagePosition, int attachmentPosition, String messageUID) {
        if (isCheckMode) addOrRemoveCheckedItem(messagePosition);
        else fileListener.onFileClick(messagePosition, attachmentPosition, messageUID);
    }

    @Override
    public void onVoiceClick(int messagePosition, int attachmentPosition, String attachmentId, String messageUID, Long timestamp) {
        if (isCheckMode) addOrRemoveCheckedItem(messagePosition);
        else fileListener.onVoiceClick(messagePosition, attachmentPosition, attachmentId, messageUID, timestamp);
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
        if (recyclerView.isComputingLayout() || recyclerView.isAnimating())
            return;
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

        if (isCheckMode != isCheckModePrevious)
            notifyDataSetChanged();
        else notifyItemChanged(position);

        listener.onChangeCheckedItems(checkedItemIds.size());
    }

    public List<String> getCheckedItemIds() {
        return checkedItemIds;
    }

    public List<MessageRealmObject> getCheckedMessageRealmObjects(){
        return checkedMessageRealmObjects;
    }

    public int getCheckedItemsCount() {
        return checkedItemIds.size();
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

        private Context context;
        private FileMessageVH.FileListener listener;
        private ForwardedAdapter.ForwardListener fwdListener;
        private String username;
        private ColorStateList colorStateList;
        private int accountMainColor;
        private int mentionColor;
        private Long mainTimestamp;
        private GroupchatUser groupchatUser;

        private boolean showOriginalOTR;
        private boolean unread;
        private boolean checked;
        private boolean needTail;
        private boolean needDate;

        public MessageExtraData(FileMessageVH.FileListener listener,
                                ForwardedAdapter.ForwardListener fwdListener,
                                Context context, String username, ColorStateList colorStateList,
                                GroupchatUser groupchatUser, int accountMainColor, int mentionColor, Long mainTimestamp,
                                boolean showOriginalOTR, boolean unread, boolean checked,
                                boolean needTail, boolean needDate) {
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
            this.groupchatUser = groupchatUser;
            this.mainTimestamp = mainTimestamp;
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

        public GroupchatUser getGroupchatUser() {
            return groupchatUser;
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
