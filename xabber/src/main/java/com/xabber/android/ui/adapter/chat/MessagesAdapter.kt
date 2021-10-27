package com.xabber.android.ui.adapter.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groups.GroupMemberManager.getGroupMemberById
import com.xabber.android.data.extension.groups.GroupMemberManager.getMe
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.adapter.chat.ForwardedAdapter.ForwardListener
import com.xabber.android.ui.adapter.chat.IncomingMessageVH.BindListener
import com.xabber.android.ui.adapter.chat.IncomingMessageVH.OnMessageAvatarClickListener
import com.xabber.android.ui.adapter.chat.MessageVH.MessageClickListener
import com.xabber.android.ui.adapter.chat.MessageVH.MessageLongClickListener
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.text.isSameDayWith
import io.realm.*
import java.util.*

class MessagesAdapter(
    private val context: Context,
    private val messageRealmObjects: RealmResults<MessageRealmObject?>,
    private val chat: AbstractChat,
    private val messageListener: MessageClickListener? = null,
    private val fileListener: MessageVH.FileListener? = null,
    private val fwdListener: ForwardListener? = null,
    private val adapterListener: AdapterListener? = null,
    private val bindListener: BindListener? = null,
    private val avatarClickListener: OnMessageAvatarClickListener? = null,
) : RecyclerView.Adapter<BasicMessageVH>(), MessageClickListener, MessageLongClickListener,
    MessageVH.FileListener, OnMessageAvatarClickListener {

    private val realmListener: OrderedRealmCollectionChangeListener<RealmResults<MessageRealmObject?>?> =
        OrderedRealmCollectionChangeListener<RealmResults<MessageRealmObject?>?> { _, changeSet ->
            if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL) {
                notifyDataSetChanged()
                return@OrderedRealmCollectionChangeListener
            }

            val llm = recyclerView?.layoutManager as? LinearLayoutManager
                ?: throw ClassCastException("Messages recyclerView's layoutManager must implement linearLayoutManager!")

            val isListAlreadyScrolledToBottom = llm.findLastVisibleItemPosition() >= llm.itemCount - 3
            val isLastMessageIsOutgoing = !(messageRealmObjects.last()?.isIncoming ?: true)

            changeSet.deletionRanges.reversed().map { range ->
                notifyItemRangeRemoved(range.startIndex, range.length)
            }

            changeSet.insertionRanges.map { range ->
                val isAddedToBottom = range.startIndex + range.length == messageRealmObjects.size
                notifyItemRangeInserted(range.startIndex, range.length)
                if (isAddedToBottom && (isListAlreadyScrolledToBottom || isLastMessageIsOutgoing)) {
                    llm.scrollToPosition(llm.itemCount - 1)
                }
            }

            changeSet.changeRanges?.map { range ->
                val isChangedInBottom = range.startIndex + range.length == messageRealmObjects.size
                notifyItemRangeChanged(range.startIndex, range.length)
                if (isChangedInBottom && (isListAlreadyScrolledToBottom || isLastMessageIsOutgoing)) {
                    llm.scrollToPosition(llm.itemCount - 1)
                }
            }

        }

    private var firstUnreadMessageID: String? = null
    private var isCheckMode = false
    private val isSavedMessagesMode: Boolean =
        chat.account.bareJid.toString() == chat.contactJid.bareJid.toString()
    private var recyclerView: RecyclerView? = null

    val checkedItemIds: MutableList<String> = ArrayList()
    val checkedMessageRealmObjects: MutableList<MessageRealmObject?> = ArrayList()

    init {
        messageRealmObjects.addChangeListener(realmListener)
    }

    private fun getItem(index: Int): MessageRealmObject? {
        require(index >= 0) { "Only indexes >= 0 are allowed. Input was: $index" }
        return when {
            index >= messageRealmObjects.size -> null
            messageRealmObjects.isValid && messageRealmObjects.isLoaded -> messageRealmObjects[index]
            else -> null
        }
    }

    override fun getItemCount(): Int =
        if (messageRealmObjects.isValid && messageRealmObjects.isLoaded) {
            messageRealmObjects.size
        } else {
            0
        }

    override fun getItemViewType(position: Int): Int {
        val messageRealmObject = getMessageItem(position) ?: return 0

        if (messageRealmObject.action != null || messageRealmObject.isGroupchatSystem) {
            return VIEW_TYPE_SYSTEM_MESSAGE
        }

        val isMeInGroup =
            messageRealmObject.groupchatUserId != null
                    && chat is GroupChat
                    && getMe(chat) != null
                    && getMe(chat)!!.memberId != null
                    && (getMe(chat)!!.memberId == messageRealmObject.groupchatUserId)

        val isNeedUnpackSingleMessageForSavedMessages = (isSavedMessagesMode
                && messageRealmObject.hasForwardedMessages()
                && MessageRepository.getForwardedMessages(messageRealmObject).size == 1)

        return if (isNeedUnpackSingleMessageForSavedMessages) {
            val innerSingleSavedMessage =
                MessageRepository.getForwardedMessages(messageRealmObject)[0]

            val isImage = innerSingleSavedMessage.hasImage()
            val notJustImage =
                (innerSingleSavedMessage.text.trim { it <= ' ' }.isNotEmpty()
                        && innerSingleSavedMessage.messageStatus != MessageStatus.UPLOADING
                        || !innerSingleSavedMessage.isAttachmentImageOnly)

            val contact =
                if (innerSingleSavedMessage.originalFrom != null) {
                    innerSingleSavedMessage.originalFrom
                } else {
                    innerSingleSavedMessage.user.toString()
                }

            when {
                !contact.contains(chat.account.bareJid.toString()) -> {
                    when {
                        isImage && notJustImage -> VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT
                        isImage -> VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE
                        else -> VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE
                    }
                }
                isImage && notJustImage -> VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT
                isImage -> VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE
                else -> VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE
            }
        } else {

            val isImage = messageRealmObject.hasImage()

            val notJustImage =
                (messageRealmObject.text.trim { it <= ' ' }.isNotEmpty()
                        && messageRealmObject.messageStatus != MessageStatus.UPLOADING
                        || !messageRealmObject.isAttachmentImageOnly)

            when {
                messageRealmObject.isIncoming && !isSavedMessagesMode && !isMeInGroup -> {
                    when {
                        isImage && notJustImage -> VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT
                        isImage -> VIEW_TYPE_INCOMING_MESSAGE_IMAGE
                        else -> VIEW_TYPE_INCOMING_MESSAGE
                    }
                }
                isImage && notJustImage -> VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT
                isImage -> VIEW_TYPE_OUTGOING_MESSAGE_IMAGE
                else -> VIEW_TYPE_OUTGOING_MESSAGE
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicMessageVH {
        return when (viewType) {
            VIEW_TYPE_SYSTEM_MESSAGE -> SystemMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_system_message, parent, false
                )
            )
            VIEW_TYPE_INCOMING_MESSAGE -> IncomingMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_incoming, parent, false
                ),
                this, this, this, bindListener, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE -> IncomingMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_incoming, parent, false
                ),
                this, this, this, bindListener, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT -> IncomingMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_incoming, parent, false
                ),
                this, this, this, bindListener, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE -> SavedCompanionMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_incoming, parent, false
                ),
                this, this, this, bindListener, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE -> SavedCompanionMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_incoming, parent, false
                ),
                this, this, this, bindListener, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT -> SavedCompanionMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_incoming, parent, false
                ),
                this, this, this, bindListener, this, SettingsManager.chatsAppearanceStyle()
            )


            VIEW_TYPE_OUTGOING_MESSAGE -> OutgoingMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_outgoing, parent, false
                ),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE -> OutgoingMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_outgoing, parent, false
                ),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT -> OutgoingMessageVH(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_outgoing, parent, false
                ),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE -> SavedOwnMessageVh(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_outgoing, parent, false
                ),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE -> SavedOwnMessageVh(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_outgoing, parent, false
                ),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT -> SavedOwnMessageVh(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_message_outgoing, parent, false
                ),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            else -> throw IllegalStateException("Unsupported view type!")
        }
    }

    private fun isMessageNeedTail(position: Int): Boolean {
        val viewType = getItemViewType(position)
        val message = getMessageItem(position) ?: return true
        val nextMessage = getMessageItem(position + 1) ?: return true

        if (isSavedMessagesMode) {
            val actualCurrentMessage: MessageRealmObject =
                if (message.account.bareJid.toString().contains(message.user.bareJid.toString())
                    && message.hasForwardedMessages()
                ) {
                    MessageRepository.getForwardedMessages(message)[0]
                } else {
                    message
                }

            val actualNextMessage: MessageRealmObject =
                if (nextMessage.account.bareJid.toString()
                        .contains(nextMessage.user.bareJid.toString())
                    && nextMessage.hasForwardedMessages()
                ) {
                    MessageRepository.getForwardedMessages(nextMessage)[0]
                } else {
                    nextMessage
                }

            return when {
                actualNextMessage.groupchatUserId != null && actualCurrentMessage.groupchatUserId != null -> {
                    actualCurrentMessage.groupchatUserId != actualNextMessage.groupchatUserId
                }
                actualNextMessage.groupchatUserId == null && actualCurrentMessage.groupchatUserId == null -> {
                    try{
                        val currentMessageSender = ContactJid.from(actualCurrentMessage.originalFrom)
                        val previousMessageSender = ContactJid.from(actualNextMessage.originalFrom)

                        !currentMessageSender.bareJid.toString()
                            .contains(previousMessageSender.bareJid.toString())
                    } catch (ex: ContactJid.ContactJidCreateException) {
                        true
                    }

                }
                else -> {
                    true
                }
            }

        } else {
            val groupMember =
                if (message.groupchatUserId != null && message.groupchatUserId.isNotEmpty()) {
                    getGroupMemberById(chat.account, chat.contactJid, message.groupchatUserId)
                } else {
                    null
                }

            when {
                groupMember != null -> {
                    val user2 =
                        if (nextMessage.groupchatUserId == null) {
                            null
                        } else {
                            getGroupMemberById(
                                chat.account, chat.contactJid, nextMessage.groupchatUserId
                            )
                        }
                    return if (user2 != null) groupMember.memberId != user2.memberId else true
                }
                viewType != VIEW_TYPE_SYSTEM_MESSAGE -> {
                    return getSimpleType(viewType) != getSimpleType(getItemViewType(position + 1))
                }
                else -> {
                    return true
                }
            }
        }
    }

    private fun isMessageNeedDate(position: Int): Boolean {
        val message = getMessageItem(position) ?: return true

        val previousMessage = getMessageItem(position - 1) ?: return true

        if (isSavedMessagesMode) {
            val currentMessage: MessageRealmObject =
                if (message.account.bareJid.toString()
                        .contains(message.user.bareJid.toString())
                    && message.hasForwardedMessages()
                ) {
                    MessageRepository.getForwardedMessages(message)[0]
                } else {
                    message
                }

            val actualPrevious: MessageRealmObject =
                if (previousMessage.account.bareJid.toString()
                        .contains(previousMessage.user.bareJid.toString())
                    && previousMessage.hasForwardedMessages()
                ) {
                    MessageRepository.getForwardedMessages(previousMessage)[0]
                } else {
                    previousMessage
                }

            return !(currentMessage.timestamp isSameDayWith actualPrevious.timestamp)
        } else {
            return !(message.timestamp isSameDayWith previousMessage.timestamp)
        }
    }

    private fun isMessageNeedName(position: Int): Boolean {
        val message = getMessageItem(position) ?: return true
        val previousMessage = getMessageItem(position - 1) ?: return true

        if (isSavedMessagesMode) {
            val currentMessage: MessageRealmObject =
                if (message.account.bareJid.toString()
                        .contains(message.user.bareJid.toString())
                    && message.hasForwardedMessages()
                ) {
                    MessageRepository.getForwardedMessages(message)[0]
                } else {
                    message
                }
            val actualPrevious: MessageRealmObject =
                if (previousMessage.account.bareJid.toString()
                        .contains(previousMessage.user.bareJid.toString())
                    && previousMessage.hasForwardedMessages()
                ) {
                    MessageRepository.getForwardedMessages(previousMessage)[0]
                } else {
                    previousMessage
                }

            if (actualPrevious.groupchatUserId != null && currentMessage.groupchatUserId != null) {
                return currentMessage.groupchatUserId != actualPrevious.groupchatUserId
            } else if (actualPrevious.groupchatUserId == null && currentMessage.groupchatUserId == null) {
                return try {
                    val currentMessageSender = ContactJid.from(currentMessage.originalFrom)
                    val previousMessageSender = ContactJid.from(actualPrevious.originalFrom)
                    !currentMessageSender.bareJid.toString().contains(
                        previousMessageSender.bareJid.toString()
                    )
                } catch (e: Exception) {
                    LogManager.exception(this, e)
                    true
                }
            } else {
                return true
            }
        } else {
            return if (message.groupchatUserId != null
                && message.groupchatUserId.isNotEmpty()
                && previousMessage.groupchatUserId != null
                && previousMessage.groupchatUserId.isNotEmpty()
            ) {
                message.groupchatUserId != previousMessage.groupchatUserId
            } else {
                true
            }
        }
    }

    override fun onBindViewHolder(holder: BasicMessageVH, position: Int) {
        val viewType = getItemViewType(position)
        val message = getMessageItem(position)

        if (message == null) {
            LogManager.w(this, "onBindViewHolder Null message item. Position: $position")
            return
        }

        // setup message uniqueId
        (holder as? MessageVH)?.messageId = message.primaryKey

        val groupMember =
            if (message.groupchatUserId != null && message.groupchatUserId.isNotEmpty()) {
                getGroupMemberById(chat.account, chat.contactJid, message.groupchatUserId)
            } else {
                null
            }

        val isNeedDate = isMessageNeedDate(position)

        val extraData = MessageExtraData(
            fileListener,
            fwdListener,
            RosterManager.getInstance().getName(chat.account, chat.contactJid),
            ColorManager.getInstance().getChatIncomingBalloonColorsStateList(chat.account),
            groupMember,
            ColorManager.getInstance().accountPainter.getAccountMainColor(chat.account),
            ColorManager.getInstance().accountPainter.getAccountIndicatorBackColor(chat.account),
            message.timestamp,
            message.primaryKey == firstUnreadMessageID,
            checkedItemIds.contains(message.primaryKey),
            isMessageNeedTail(position),
            isNeedDate,
            isMessageNeedName(position)
        )

        when (viewType) {
            VIEW_TYPE_SYSTEM_MESSAGE -> {
                (holder as? SystemMessageVH)?.bind(message, isNeedDate, context)
            }

            VIEW_TYPE_INCOMING_MESSAGE,
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT,
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE -> {
                (holder as? IncomingMessageVH)?.bind(message, extraData)
            }

            VIEW_TYPE_OUTGOING_MESSAGE,
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT,
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE -> {
                (holder as? OutgoingMessageVH)?.bind(message, extraData)
            }

            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE,
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE,
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT -> {
                (holder as? SavedOwnMessageVh)?.bind(message, extraData)
            }

            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE,
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE,
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT -> {
                (holder as? SavedCompanionMessageVH)?.bind(message, extraData)
            }
        }
    }

    fun getMessageItem(position: Int): MessageRealmObject? =
        when {
            position == RecyclerView.NO_POSITION -> null
            position < messageRealmObjects.size -> messageRealmObjects[position]
            else -> null
        }

    override fun onMessageClick(caller: View, position: Int) {
        if (isCheckMode && recyclerView?.isComputingLayout != true) {
            addOrRemoveCheckedItem(position)
        } else {
            messageListener?.onMessageClick(caller, position)
        }
    }

    override fun onLongMessageClick(position: Int) {
        addOrRemoveCheckedItem(position)
    }

    override fun onMessageAvatarClick(position: Int) {
        if (isCheckMode && recyclerView?.isComputingLayout != true) {
            addOrRemoveCheckedItem(position)
        } else {
            avatarClickListener?.onMessageAvatarClick(position)
        }
    }

    fun setFirstUnreadMessageId(id: String?) {
        firstUnreadMessageID = id
    }

    override fun onImageClick(messagePosition: Int, attachmentPosition: Int, messageUID: String?) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition)
        } else {
            fileListener?.onImageClick(messagePosition, attachmentPosition, messageUID)
        }
    }

    override fun onFileClick(messagePosition: Int, attachmentPosition: Int, messageUID: String?) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition)
        } else {
            fileListener?.onFileClick(messagePosition, attachmentPosition, messageUID)
        }
    }

    override fun onVoiceClick(
        messagePosition: Int,
        attachmentPosition: Int,
        attachmentId: String?,
        messageUID: String?,
        timestamp: Long?
    ) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition)
        } else {
            fileListener?.onVoiceClick(
                messagePosition, attachmentPosition, attachmentId, messageUID, timestamp
            )
        }
    }

    override fun onFileLongClick(attachmentRealmObject: AttachmentRealmObject?, caller: View?) {
        fileListener?.onFileLongClick(attachmentRealmObject, caller)
    }

    override fun onDownloadCancel() {
        fileListener?.onDownloadCancel()
    }

    override fun onUploadCancel() {
        fileListener?.onUploadCancel()
    }

    override fun onDownloadError(error: String?) {
        fileListener?.onDownloadError(error)
    }

    /** Checked items  */
    private fun addOrRemoveCheckedItem(position: Int) {
        if (recyclerView?.isComputingLayout == true || recyclerView?.isAnimating == true) {
            return
        }

        recyclerView?.stopScroll()
        val messageRealmObject = getItem(position)
        val uniqueId = messageRealmObject?.primaryKey

        if (checkedItemIds.contains(uniqueId)) {
            checkedMessageRealmObjects.remove(messageRealmObject)
            checkedItemIds.remove(uniqueId)
        } else {
            uniqueId?.let { checkedItemIds.add(it) }
            checkedMessageRealmObjects.add(messageRealmObject)
        }

        val isCheckModePrevious = isCheckMode

        isCheckMode = checkedItemIds.size > 0
        if (isCheckMode != isCheckModePrevious) {
            notifyDataSetChanged()
        } else {
            notifyItemChanged(position)
        }

        adapterListener?.onChangeCheckedItems(checkedItemIds.size)
    }

    fun resetCheckedItems() {
        if (checkedItemIds.size > 0) {
            checkedItemIds.clear()
            checkedMessageRealmObjects.clear()
            isCheckMode = false
            notifyDataSetChanged()
            adapterListener?.onChangeCheckedItems(checkedItemIds.size)
        }
    }

    private fun getSimpleType(type: Int): Int {
        return when (type) {

            VIEW_TYPE_INCOMING_MESSAGE,
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE,
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT,
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE,
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE,
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT -> 1

            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE,
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE,
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT,
            VIEW_TYPE_OUTGOING_MESSAGE,
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE,
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT -> 2

            VIEW_TYPE_SYSTEM_MESSAGE -> 3

            else -> 0
        }
    }

    fun release() {
        messageRealmObjects.removeChangeListener(realmListener)
    }

    companion object {
        const val VIEW_TYPE_INCOMING_MESSAGE = 1
        const val VIEW_TYPE_OUTGOING_MESSAGE = 2
        const val VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE = 3
        const val VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE = 4
        const val VIEW_TYPE_OUTGOING_MESSAGE_IMAGE = 9
        const val VIEW_TYPE_INCOMING_MESSAGE_IMAGE = 10
        const val VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE = 11
        const val VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE = 12
        const val VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT = 13
        const val VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT = 14
        const val VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT = 15
        const val VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT = 16
        const val VIEW_TYPE_SYSTEM_MESSAGE = 17
    }

    interface AdapterListener {
        fun onMessagesUpdated()
        fun onChangeCheckedItems(checkedItems: Int)
        fun scrollTo(position: Int)
    }

}