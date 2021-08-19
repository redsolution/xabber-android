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
import com.xabber.android.ui.adapter.chat.FileMessageVH.FileListener
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
    private val listener: FileListener? = extraData.listener
    private val fwdListener: ForwardListener? = extraData.fwdListener

    interface ForwardListener {
        fun onForwardClick(messageId: String?)
    }

    override fun getItemViewType(position: Int): Int {
        val messageRealmObject = getMessageItem(position) ?: return 0

        // if have forwarded-messages or attachments should use special layout without flexbox-style text
        return if (messageRealmObject.hasForwardedMessages()
            || messageRealmObject.haveAttachments()
            || messageRealmObject.hasImage()
        ) {
            if (messageRealmObject.haveAttachments()
                && messageRealmObject.isAttachmentImageOnly
                && messageRealmObject.text.trim { it <= ' ' }.isEmpty()
            ) {
                VIEW_TYPE_IMAGE
            } else {
                VIEW_TYPE_MESSAGE_NOFLEX
            }
        } else {
            VIEW_TYPE_MESSAGE
        }
    }

    override fun getItemCount() = if (realmResults.isValid && realmResults.isLoaded) realmResults.size else 0

    fun getMessageItem(position: Int): MessageRealmObject? =
        when {
            position == RecyclerView.NO_POSITION -> null
            position < realmResults.size -> realmResults[position]
            else -> null
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicMessageVH {
        return when (viewType) {
            VIEW_TYPE_IMAGE -> NoFlexForwardedVH(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_forwarded_image, parent, false),
                this, this, listener, appearanceStyle
            )
            VIEW_TYPE_MESSAGE_NOFLEX -> NoFlexForwardedVH(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_forwarded_noflex, parent, false),
                this, this, listener, appearanceStyle
            )
            else -> ForwardedVH(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_forwarded, parent, false),
                this, this, listener, appearanceStyle
            )
        }
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
            messageRealmObject.originalFrom,
            colorStateList = extraData.colorStateList,
            groupMember = messageRealmObject.groupchatUserId?.let {
                getGroupMemberById(messageRealmObject.account, messageRealmObject.user, it)
            },
            accountMainColor = extraData.accountMainColor,
            mentionColor = extraData.mentionColor,
            mainMessageTimestamp = extraData.mainMessageTimestamp,
            isShowOriginalOTR = false,
            isUnread = false,
            isChecked = false,
            isNeedTail = false,
            isNeedDate = true,
            isNeedName = true
        )
        when (getItemViewType(position)) {
            VIEW_TYPE_IMAGE, VIEW_TYPE_MESSAGE_NOFLEX -> {
                (holder as NoFlexForwardedVH).bind(
                    messageRealmObject, extraData, messageRealmObject.account.fullJid.asBareJid().toString()
                )
            }
            else -> {
                (holder as ForwardedVH).bind(
                    messageRealmObject, extraData, messageRealmObject.account.fullJid.asBareJid().toString()
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
        private const val VIEW_TYPE_MESSAGE_NOFLEX = 2
        private const val VIEW_TYPE_IMAGE = 3
    }

}