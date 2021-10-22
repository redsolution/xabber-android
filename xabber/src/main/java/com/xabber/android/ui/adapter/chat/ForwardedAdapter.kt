package com.xabber.android.ui.adapter.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.extension.groups.GroupMemberManager.getGroupMemberById
import com.xabber.android.data.log.LogManager
import com.xabber.android.ui.adapter.chat.MessageVH.MessageClickListener
import com.xabber.android.ui.adapter.chat.MessageVH.MessageLongClickListener
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults

class ForwardedAdapter(
    private val realmResults: RealmResults<MessageRealmObject?>,
    private val extraData: MessageExtraData
) : RealmRecyclerViewAdapter<MessageRealmObject?, BasicMessageVH?>(realmResults, true, true),
    MessageClickListener,
    MessageLongClickListener {

    private val appearanceStyle = SettingsManager.chatsAppearanceStyle()
    private val listener: MessageVH.FileListener? = extraData.listener
    private val fwdListener: ForwardListener? = extraData.fwdListener

    interface ForwardListener {
        fun onForwardClick(messageId: String?)
    }

    override fun getItemViewType(position: Int): Int {
        val messageRealmObject = getMessageItem(position) ?: return 0

        // if have forwarded-messages or attachments should use special layout without flexbox-style text
        return if (messageRealmObject.hasForwardedMessages()
            || messageRealmObject.hasAttachments()
            || messageRealmObject.hasImage()
        ) {
            VIEW_TYPE_IMAGE
        } else {
            VIEW_TYPE_MESSAGE
        }
    }

    override fun getItemCount() =
        if (realmResults.isValid && realmResults.isLoaded) realmResults.size else 0

    fun getMessageItem(position: Int): MessageRealmObject? =
        when {
            position == RecyclerView.NO_POSITION -> null
            position < realmResults.size -> realmResults[position]
            else -> null
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicMessageVH {
        return ForwardedVH(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_message_forwarded, parent, false
            ),
            this, this, listener, appearanceStyle
        )
    }

    override fun onBindViewHolder(holder: BasicMessageVH, position: Int) {
        val messageRealmObject = getMessageItem(position)
        if (messageRealmObject == null) {
            LogManager.w(this, "onBindViewHolder Null message item. Position: $position")
            return
        }

        // setup message uniqueId
        if (holder is MessageVH) {
            holder.messageId = messageRealmObject.primaryKey
        }

        val extraData = MessageExtraData(
            null,
            null,
            extraData.context,
            messageRealmObject.originalFrom ?: messageRealmObject.user.toString(),
            colorStateList = extraData.colorStateList,
            groupMember = messageRealmObject.groupchatUserId?.let {
                getGroupMemberById(messageRealmObject.account, messageRealmObject.user, it)
            },
            accountMainColor = extraData.accountMainColor,
            mentionColor = extraData.mentionColor,
            mainMessageTimestamp = extraData.mainMessageTimestamp,
            isUnread = false,
            isChecked = false,
            isNeedTail = false,
            isNeedDate = true,
            isNeedName = true
        )
        when (getItemViewType(position)) {
            VIEW_TYPE_IMAGE -> {
                (holder as MessageVH).bind(
                    messageRealmObject,
                    extraData,
                )
            }
            else -> {
                (holder as MessageVH).bind(
                    messageRealmObject,
                    extraData,
                )
            }
        }
    }

    override fun onMessageClick(caller: View, position: Int) {
        getItem(position)?.let { message ->
            if (message.hasForwardedMessages()) {
                fwdListener?.onForwardClick(message.primaryKey)
            }
        }
    }

    override fun onLongMessageClick(position: Int) {}

    companion object {
        private const val VIEW_TYPE_MESSAGE = 1
        private const val VIEW_TYPE_IMAGE = 3
    }

}