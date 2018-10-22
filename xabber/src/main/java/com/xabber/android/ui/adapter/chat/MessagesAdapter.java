package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;

import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class MessagesAdapter extends RealmRecyclerViewAdapter<MessageItem, BasicMessageVH> {

    private static final String LOG_TAG = MessagesAdapter.class.getSimpleName();

    public static final int VIEW_TYPE_INCOMING_MESSAGE = 2;
    public static final int VIEW_TYPE_OUTGOING_MESSAGE = 3;
    private static final int VIEW_TYPE_ACTION_MESSAGE = 4;

    private final Context context;
    private final MessageVH.MessageClickListener messageClickListener;
    private final Listener listener;

    // message font style
    private final int appearanceStyle = SettingsManager.chatsAppearanceStyle();
    private ColorStateList incomingBackgroundColors;
    private boolean isMUC;
    private Resourcepart mucNickname;
    private String userName;
    private AccountJid account;
    private UserJid user;
    private int prevItemCount;
    private int unreadCount = 0;

    private List<String> itemsNeedOriginalText = new ArrayList<>();

    public interface Listener {
        void onMessageNumberChanged(int prevItemCount);
        void onMessagesUpdated();
    }

    public MessagesAdapter(
            Context context, RealmResults<MessageItem> messageItems,
            AbstractChat chat, MessageVH.MessageClickListener messageClickListener, Listener listener) {
        super(context, messageItems, true);

        this.context = context;
        this.messageClickListener = messageClickListener;
        this.listener = listener;

        account = chat.getAccount();
        user = chat.getUser();
        userName = RosterManager.getInstance().getName(account, user);
        prevItemCount = getItemCount();

        isMUC = MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible());
        if (isMUC) mucNickname = MUCManager.getInstance().getNickname(account, user.getJid().asEntityBareJidIfPossible());

        incomingBackgroundColors = ColorManager.getInstance().getChatIncomingBalloonColorsStateList(account);
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

        if (messageItem.isIncoming()) {
            if (isMUC && messageItem.getResource().equals(mucNickname)) {
                return VIEW_TYPE_OUTGOING_MESSAGE;
            }
            return VIEW_TYPE_INCOMING_MESSAGE;
        } else return VIEW_TYPE_OUTGOING_MESSAGE;
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
                        messageClickListener, appearanceStyle);

            case VIEW_TYPE_OUTGOING_MESSAGE:
                return new OutgoingMessageVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_outgoing, parent, false),
                        messageClickListener, appearanceStyle);
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
        boolean unread = position >= getItemCount() - unreadCount;

        // need show original OTR message
        boolean showOriginalOTR = itemsNeedOriginalText.contains(messageItem.getUniqueId());

        switch (viewType) {
            case VIEW_TYPE_ACTION_MESSAGE:
                ((ActionMessageVH)holder).bind(messageItem, context, account, isMUC);
                break;

            case VIEW_TYPE_INCOMING_MESSAGE:
                ((IncomingMessageVH)holder).bind(messageItem, isMUC, showOriginalOTR, context,
                        userName, unread, incomingBackgroundColors);

                break;
            case VIEW_TYPE_OUTGOING_MESSAGE:
                ((OutgoingMessageVH)holder).bind(messageItem, isMUC, showOriginalOTR, context,
                        unread, account);
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

    public int findMessagePosition(String uniqueId) {
        for (int i = 0; i < realmResults.size(); i++) {
            if (realmResults.get(i).getUniqueId().equals(uniqueId)) return i;
        }
        return RecyclerView.NO_POSITION;
    }

    public boolean setUnreadCount(int unreadCount) {
        if (this.unreadCount != unreadCount) {
            this.unreadCount = unreadCount;
            return true;
        } else return false;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void addOrRemoveItemNeedOriginalText(String messageId) {
        if (itemsNeedOriginalText.contains(messageId))
            itemsNeedOriginalText.remove(messageId);
        else itemsNeedOriginalText.add(messageId);
    }
}
