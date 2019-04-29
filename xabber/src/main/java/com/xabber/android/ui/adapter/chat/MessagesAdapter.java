package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.Utils;

import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class MessagesAdapter extends RealmRecyclerViewAdapter<MessageItem, BasicMessageVH>
        implements MessageVH.MessageClickListener, MessageVH.MessageLongClickListener,
        FileMessageVH.FileListener {

    private static final String LOG_TAG = MessagesAdapter.class.getSimpleName();

    public static final int VIEW_TYPE_INCOMING_MESSAGE = 2;
    public static final int VIEW_TYPE_INCOMING_MESSAGE_NOFLEX = 5;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE = 3;
    private static final int VIEW_TYPE_ACTION_MESSAGE = 4;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX = 6;

    private final Context context;
    private final MessageVH.MessageClickListener messageListener;
    private final FileMessageVH.FileListener fileListener;
    private final ForwardedAdapter.ForwardListener fwdListener;
    private final Listener listener;
    private final AnchorHolder anchorHolder;
    private final IncomingMessageVH.BindListener bindListener;

    // message font style
    private final int appearanceStyle = SettingsManager.chatsAppearanceStyle();
    private int accountMainColor;
    private ColorStateList colorStateList;
    private boolean isMUC;
    private Resourcepart mucNickname;
    private String userName;
    private AccountJid account;
    private UserJid user;
    private int prevItemCount;
    private String firstUnreadMessageID;
    private boolean isCheckMode;

    private List<String> itemsNeedOriginalText = new ArrayList<>();
    private List<String> checkedItemIds = new ArrayList<>();

    public interface Listener {
        void onMessageNumberChanged(int prevItemCount);
        void onMessagesUpdated();
        void onChangeCheckedItems(int checkedItems);
    }

    public interface AnchorHolder {
        View getAnchor();
    }

    public MessagesAdapter(
            Context context, RealmResults<MessageItem> messageItems,
            AbstractChat chat, MessageVH.MessageClickListener messageListener,
            FileMessageVH.FileListener fileListener, ForwardedAdapter.ForwardListener fwdListener,
            Listener listener, IncomingMessageVH.BindListener bindListener, AnchorHolder anchorHolder) {
        super(context, messageItems, true);

        this.context = context;
        this.messageListener = messageListener;
        this.fileListener = fileListener;
        this.fwdListener = fwdListener;
        this.listener = listener;
        this.anchorHolder = anchorHolder;
        this.bindListener = bindListener;

        account = chat.getAccount();
        user = chat.getUser();
        userName = RosterManager.getInstance().getName(account, user);
        prevItemCount = getItemCount();
        accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
        colorStateList = ColorManager.getInstance().getChatIncomingBalloonColorsStateList(account);

        isMUC = MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible());
        if (isMUC) mucNickname = MUCManager.getInstance().getNickname(account, user.getJid().asEntityBareJidIfPossible());
    }

    @Override
    public int getItemCount() {
        if (realmResults.isValid() && realmResults.isLoaded())
            return realmResults.size();
        else return 0;
    }

    @Override
    public int getItemViewType(int position) {
        MessageItem messageItem = getMessageItem(position);
        if (messageItem == null) return 0;

        if (messageItem.getAction() != null)
            return VIEW_TYPE_ACTION_MESSAGE;

        // if noFlex is true, should use special layout without flexbox-style text
        boolean noFlex = messageItem.haveForwardedMessages() || messageItem.haveAttachments() || messageItem.isImage();

        if (messageItem.isIncoming()) {
            if (isMUC && messageItem.getResource().equals(mucNickname)) {
                return noFlex ? VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX : VIEW_TYPE_OUTGOING_MESSAGE;
            }
            return noFlex ? VIEW_TYPE_INCOMING_MESSAGE_NOFLEX : VIEW_TYPE_INCOMING_MESSAGE;
        } else return noFlex ? VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX : VIEW_TYPE_OUTGOING_MESSAGE;
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

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final BasicMessageVH holder, int position) {

        final int viewType = getItemViewType(position);
        MessageItem messageItem = getMessageItem(position);

        if (messageItem == null) {
            LogManager.w(LOG_TAG, "onBindViewHolder Null message item. Position: " + position);
            return;
        }

        // setup message uniqueId
        if (holder instanceof MessageVH)
            ((MessageVH)holder).messageId = messageItem.getUniqueId();

        // setup message as unread
        boolean unread = messageItem.getUniqueId().equals(firstUnreadMessageID);

        // setup message as checked
        boolean checked = checkedItemIds.contains(messageItem.getUniqueId());

        // need show original OTR message
        boolean showOriginalOTR = itemsNeedOriginalText.contains(messageItem.getUniqueId());

        // need tail
        boolean needTail = false;
        if (isMUC) {
            MessageItem nextMessage = getMessageItem(position + 1);
            if (nextMessage != null)
                needTail = !messageItem.getResource().equals(nextMessage.getResource());
            else needTail = true;
        } else if (viewType != VIEW_TYPE_ACTION_MESSAGE) {
            needTail = getSimpleType(viewType) != getSimpleType(getItemViewType(position + 1));
        }

        // need date
        boolean needDate;
        MessageItem previousMessage = getMessageItem(position - 1);
        if (previousMessage != null) {
            needDate = !Utils.isSameDay(messageItem.getTimestamp(), previousMessage.getTimestamp());
        } else needDate = true;

        MessageExtraData extraData = new MessageExtraData(fileListener, fwdListener, anchorHolder,
                context, userName, colorStateList, accountMainColor, isMUC, showOriginalOTR, unread,
                checked, needTail, needDate);

        switch (viewType) {
            case VIEW_TYPE_ACTION_MESSAGE:
                ((ActionMessageVH)holder).bind(messageItem, context, account, isMUC);
                break;

            case VIEW_TYPE_INCOMING_MESSAGE:
                ((IncomingMessageVH)holder).bind(messageItem, extraData);
                break;

            case VIEW_TYPE_INCOMING_MESSAGE_NOFLEX:
                ((NoFlexIncomingMsgVH)holder).bind(messageItem, extraData);
                break;

            case VIEW_TYPE_OUTGOING_MESSAGE:
                ((OutgoingMessageVH)holder).bind(messageItem, extraData);
                break;

            case VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX:
                ((NoFlexOutgoingMsgVH)holder).bind(messageItem, extraData);
                break;
        }
    }

    @Override
    public void onChange() {
        notifyDataSetChanged();
        listener.onMessagesUpdated();

        int itemCount = getItemCount();
        if (prevItemCount != itemCount) {
            listener.onMessageNumberChanged(prevItemCount);
            prevItemCount = itemCount;
        }
    }

    @Nullable
    public MessageItem getMessageItem(int position) {
        if (position == RecyclerView.NO_POSITION) return null;

        if (position < realmResults.size())
            return realmResults.get(position);
        else return null;
    }

    @Override
    public void onMessageClick(View caller, int position) {
        if (isCheckMode) addOrRemoveCheckedItem(position);
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
    public void onFileLongClick(Attachment attachment, View caller) {
        fileListener.onFileLongClick(attachment, caller);
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
        String uniqueId = getItem(position).getUniqueId();

        if (checkedItemIds.contains(uniqueId))
            checkedItemIds.remove(uniqueId);
        else checkedItemIds.add(uniqueId);

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

    public int getCheckedItemsCount() {
        return checkedItemIds.size();
    }

    public void resetCheckedItems() {
        if (checkedItemIds.size() > 0) {
            checkedItemIds.clear();
            isCheckMode = false;
            notifyDataSetChanged();
            listener.onChangeCheckedItems(checkedItemIds.size());
        }
    }

    /** Message Extra Data */

    public static class MessageExtraData {

        private  Context context;
        private FileMessageVH.FileListener listener;
        private ForwardedAdapter.ForwardListener fwdListener;
        private AnchorHolder anchorHolder;
        private String username;
        private ColorStateList colorStateList;
        private int accountMainColor;

        private boolean isMuc;
        private boolean showOriginalOTR;
        private boolean unread;
        private boolean checked;
        private boolean needTail;
        private boolean needDate;

        public MessageExtraData(FileMessageVH.FileListener listener,
                                ForwardedAdapter.ForwardListener fwdListener,
                                AnchorHolder anchorHolder,
                                Context context, String username, ColorStateList colorStateList,
                                int accountMainColor, boolean isMuc, boolean showOriginalOTR,
                                boolean unread, boolean checked, boolean needTail, boolean needDate) {
            this.listener = listener;
            this.fwdListener = fwdListener;
            this.anchorHolder = anchorHolder;
            this.context = context;
            this.username = username;
            this.colorStateList = colorStateList;
            this.accountMainColor = accountMainColor;
            this.isMuc = isMuc;
            this.showOriginalOTR = showOriginalOTR;
            this.unread = unread;
            this.checked = checked;
            this.needTail = needTail;
            this.needDate = needDate;
        }

        public FileMessageVH.FileListener getListener() {
            return listener;
        }

        public ForwardedAdapter.ForwardListener getFwdListener() {
            return fwdListener;
        }

        public AnchorHolder getAnchorHolder() {
            return anchorHolder;
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

        public boolean isMuc() {
            return isMuc;
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
                return 1;
            case VIEW_TYPE_INCOMING_MESSAGE_NOFLEX:
                return 1;
            case VIEW_TYPE_OUTGOING_MESSAGE:
                return 2;
            case VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX:
                return 2;
            case VIEW_TYPE_ACTION_MESSAGE:
                return 3;
            default:
                return 0;
        }
    }
}
