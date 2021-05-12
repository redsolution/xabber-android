package com.xabber.android.ui.adapter.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.extension.groups.GroupMember;
import com.xabber.android.data.extension.groups.GroupMemberManager;

import org.jetbrains.annotations.NotNull;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class ForwardedAdapter extends RealmRecyclerViewAdapter<MessageRealmObject, BasicMessageVH>
    implements MessageVH.MessageClickListener, MessageVH.MessageLongClickListener {

    private static final String LOG_TAG = MessagesAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_MESSAGE = 1;
    private static final int VIEW_TYPE_MESSAGE_NOFLEX = 2;
    private static final int VIEW_TYPE_IMAGE = 3;

    private final int appearanceStyle = SettingsManager.chatsAppearanceStyle();
    private final MessagesAdapter.MessageExtraData extraData;
    private final FileMessageVH.FileListener listener;
    private final ForwardListener fwdListener;

    public interface ForwardListener {
        void onForwardClick(String messageId);
    }

    public ForwardedAdapter(RealmResults<MessageRealmObject> realmResults,
                            MessagesAdapter.MessageExtraData extraData) {
        super(extraData.getContext(), realmResults, true);
        this.extraData = extraData;
        this.listener = extraData.getListener();
        this.fwdListener = extraData.getFwdListener();
    }

    @Override
    public int getItemViewType(int position) {
        MessageRealmObject messageRealmObject = getMessageItem(position);
        if (messageRealmObject == null) return 0;

        // if have forwarded-messages or attachments should use special layout without flexbox-style text
        if (messageRealmObject.haveForwardedMessages()
                || messageRealmObject.haveAttachments()
                || messageRealmObject.hasImage()) {
            if(messageRealmObject.haveAttachments()
                    && messageRealmObject.isAttachmentImageOnly()
                    && messageRealmObject.getText().trim().isEmpty())
                return VIEW_TYPE_IMAGE;
            else return VIEW_TYPE_MESSAGE_NOFLEX;
        } else return VIEW_TYPE_MESSAGE;
    }

    @Override
    public int getItemCount() {
        if (realmResults.isValid() && realmResults.isLoaded())
            return realmResults.size();
        else return 0;
    }

    @Nullable
    public MessageRealmObject getMessageItem(int position) {
        if (position == RecyclerView.NO_POSITION) return null;

        if (position < realmResults.size())
            return realmResults.get(position);
        else return null;
    }

    @NotNull
    @Override
    public BasicMessageVH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_IMAGE:
                return new NoFlexForwardedVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_forwarded_image, parent, false),
                        this, this, listener, appearanceStyle);
            case VIEW_TYPE_MESSAGE_NOFLEX:
                return new NoFlexForwardedVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_forwarded_noflex, parent, false),
                        this, this, listener, appearanceStyle);
            default:
                return new ForwardedVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_forwarded, parent, false),
                        this, this, listener, appearanceStyle);
        }
    }

    @Override
    public void onBindViewHolder(@NotNull final BasicMessageVH holder, int position) {
        MessageRealmObject messageRealmObject = getMessageItem(position);

        if (messageRealmObject == null) {
            LogManager.w(LOG_TAG, "onBindViewHolder Null message item. Position: " + position);
            return;
        }

        // setup message uniqueId
        if (holder instanceof MessageVH)
            ((MessageVH)holder).messageId = messageRealmObject.getPrimaryKey();

        // groupchat user
        GroupMember groupMember = GroupMemberManager.INSTANCE
                .getGroupMemberById(messageRealmObject.getGroupchatUserId());

        MessagesAdapter.MessageExtraData extraData = new MessagesAdapter.MessageExtraData(
                null, null, this.extraData.getContext(), messageRealmObject.getOriginalFrom(),
                this.extraData.getColorStateList(), groupMember,
                this.extraData.getAccountMainColor(), this.extraData.getMentionColor(),
                this.extraData.getMainMessageTimestamp(), false, false, false, false, true, true);

        final int viewType = getItemViewType(position);
        switch (viewType) {
            case VIEW_TYPE_IMAGE:
            case VIEW_TYPE_MESSAGE_NOFLEX:
                ((NoFlexForwardedVH)holder).bind(messageRealmObject, extraData,
                        messageRealmObject.getAccount().getFullJid().asBareJid().toString());
                break;
            default:
                ((ForwardedVH)holder).bind(messageRealmObject, extraData,
                        messageRealmObject.getAccount().getFullJid().asBareJid().toString());
        }
    }

    @Override
    public void onMessageClick(View caller, int position) {
        MessageRealmObject message = getItem(position);
        if (message != null && message.haveForwardedMessages())
            fwdListener.onForwardClick(message.getPrimaryKey());
    }

    @Override
    public void onLongMessageClick(int position) { }

}
