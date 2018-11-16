package com.xabber.android.ui.adapter.chat;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.log.LogManager;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class ForwardedAdapter extends RealmRecyclerViewAdapter<MessageItem, BasicMessageVH>
    implements MessageVH.MessageClickListener, MessageVH.MessageLongClickListener {

    private static final String LOG_TAG = MessagesAdapter.class.getSimpleName();

    private final int appearanceStyle = SettingsManager.chatsAppearanceStyle();
    private MessagesAdapter.MessageExtraData extraData;
    private FileMessageVH.FileListener listener;


    public ForwardedAdapter(RealmResults<MessageItem> realmResults,
                            MessagesAdapter.MessageExtraData extraData) {
        super(extraData.getContext(), realmResults, true);
        this.extraData = extraData;
        this.listener = extraData.getListener();
    }

    @Override
    public int getItemCount() {
        if (realmResults.isValid() && realmResults.isLoaded())
            return realmResults.size();
        else return 0;
    }

    @Nullable
    public MessageItem getMessageItem(int position) {
        if (position == RecyclerView.NO_POSITION) return null;

        if (position < realmResults.size())
            return realmResults.get(position);
        else return null;
    }

    @Override
    public BasicMessageVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ForwardedVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_forwarded, parent, false),
                this, this, listener, appearanceStyle);
    }

    @Override
    public void onBindViewHolder(final BasicMessageVH holder, int position) {
        MessageItem messageItem = getMessageItem(position);

        if (messageItem == null) {
            LogManager.w(LOG_TAG, "onBindViewHolder Null message item. Position: " + position);
            return;
        }

        // setup message uniqueId
        if (holder instanceof MessageVH)
            ((MessageVH)holder).messageId = messageItem.getUniqueId();

        MessagesAdapter.MessageExtraData extraData = new MessagesAdapter.MessageExtraData(
                null, this.extraData.getContext(), messageItem.getOriginalFrom(),
                this.extraData.getColorStateList(), this.extraData.getAccountMainColor(),
                false, false, false, false, false);

        ((ForwardedVH)holder).bind(messageItem, extraData,
                messageItem.getAccount().getFullJid().asBareJid().toString());
    }

    @Override
    public void onMessageClick(View caller, int position) {

    }

    @Override
    public void onLongMessageClick(int position) {

    }
}
