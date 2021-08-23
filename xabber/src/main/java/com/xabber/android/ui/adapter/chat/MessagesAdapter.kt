package com.xabber.android.ui.adapter.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.adapter.chat.FileMessageVH.FileListener
import com.xabber.android.ui.adapter.chat.ForwardedAdapter.ForwardListener
import com.xabber.android.ui.adapter.chat.IncomingMessageVH.BindListener
import com.xabber.android.ui.adapter.chat.IncomingMessageVH.OnMessageAvatarClickListener
import com.xabber.android.ui.adapter.chat.MessageVH.MessageClickListener
import com.xabber.android.ui.adapter.chat.MessageVH.MessageLongClickListener
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.utils.Utils
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults
import java.util.*

class MessagesAdapter(
    private val context: Context,
    private val messageRealmObjects: RealmResults<MessageRealmObject?>,
    private val chat: AbstractChat,
    private val messageListener: MessageClickListener? = null,
    private val fileListener: FileListener? = null,
    private val fwdListener: ForwardListener? = null,
    private val listener: Listener? = null,
    private val bindListener: BindListener? = null,
    private val avatarClickListener: OnMessageAvatarClickListener? = null,
) : RealmRecyclerViewAdapter<MessageRealmObject?, BasicMessageVH?>(messageRealmObjects, true, true),
    MessageClickListener,
    MessageLongClickListener,
    FileListener,
    OnMessageAvatarClickListener {

    private var firstUnreadMessageID: String? = null
    private var isCheckMode = false
    private val isSavedMessagesMode: Boolean =
        chat.account.bareJid.toString() == chat.contactJid.bareJid.toString()
    private var recyclerView: RecyclerView? = null
    private val itemsNeedOriginalText: MutableList<String> = ArrayList()

    val checkedItemIds: MutableList<String> = ArrayList()
    val checkedMessageRealmObjects: MutableList<MessageRealmObject?> = ArrayList()

    interface Listener {
        fun onMessagesUpdated()
        fun onChangeCheckedItems(checkedItems: Int)
        fun scrollTo(position: Int)
    }

    override fun getItemCount(): Int =
        if (messageRealmObjects.isValid && messageRealmObjects.isLoaded) {
            messageRealmObjects.size
        } else {
            0
        }

    override fun getItemViewType(position: Int): Int {
        val messageRealmObject = getMessageItem(position) ?: return 0

        if (messageRealmObject.action != null) {
            return VIEW_TYPE_ACTION_MESSAGE
        }

        if (messageRealmObject.isGroupchatSystem) {
            return VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE
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

            val isUploadMessage = innerSingleSavedMessage.text == FileMessageVH.UPLOAD_TAG

            val noFlex = innerSingleSavedMessage.hasForwardedMessages()
                    || messageRealmObject.haveAttachments()

            val isImage = innerSingleSavedMessage.hasImage()
            val notJustImage =
                (innerSingleSavedMessage.text.trim { it <= ' ' }.isNotEmpty() && !isUploadMessage
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
                        noFlex -> VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_NOFLEX
                        else -> VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE
                    }
                }
                isImage && notJustImage -> VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT
                isImage -> VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE
                noFlex -> VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_NOFLEX
                else -> VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE
            }
        } else {

            // if noFlex is true, should use special layout without flexbox-style text
            val isUploadMessage = messageRealmObject.text == FileMessageVH.UPLOAD_TAG
            val noFlex =
                messageRealmObject.hasForwardedMessages() || messageRealmObject.haveAttachments()

            val isImage = messageRealmObject.hasImage()

            val notJustImage =
                (messageRealmObject.text.trim { it <= ' ' }.isNotEmpty() && !isUploadMessage
                        || !messageRealmObject.isAttachmentImageOnly)

            when {
                messageRealmObject.isIncoming && !isSavedMessagesMode && !isMeInGroup -> {
                    when {
                        isImage && notJustImage -> VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT
                        isImage -> VIEW_TYPE_INCOMING_MESSAGE_IMAGE
                        noFlex -> VIEW_TYPE_INCOMING_MESSAGE_NOFLEX
                        else -> VIEW_TYPE_INCOMING_MESSAGE
                    }
                }
                isImage && notJustImage -> VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT
                isImage -> VIEW_TYPE_OUTGOING_MESSAGE_IMAGE
                noFlex -> VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX
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
            VIEW_TYPE_ACTION_MESSAGE -> ActionMessageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_system_message, parent, false)
            )
            VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE -> GroupchatSystemMessageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_system_message, parent, false)
            )
            VIEW_TYPE_INCOMING_MESSAGE -> IncomingMessageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_incoming, parent, false),
                this, this, this, bindListener,
                this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_INCOMING_MESSAGE_NOFLEX -> NoFlexIncomingMsgVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_incoming_noflex, parent, false),
                this, this, this, bindListener,
                this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE -> NoFlexIncomingMsgVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_incoming_image, parent, false),
                this, this, this, bindListener,
                this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT -> NoFlexIncomingMsgVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_incoming_image_text, parent, false),
                this, this, this, bindListener,
                this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE -> SavedCompanionMessageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_incoming, parent, false),
                this, this, this, bindListener,
                this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_NOFLEX -> SavedCompanionMessageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_incoming_noflex, parent, false),
                this, this, this, bindListener,
                this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE -> SavedCompanionMessageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_incoming_image, parent, false),
                this, this, this, bindListener,
                this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT -> SavedCompanionMessageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_incoming_image_text, parent, false),
                this, this, this, bindListener,
                this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_OUTGOING_MESSAGE -> OutgoingMessageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_outgoing, parent, false),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX -> NoFlexOutgoingMsgVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_outgoing_noflex, parent, false),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE -> NoFlexOutgoingMsgVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_outgoing_image, parent, false),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT -> NoFlexOutgoingMsgVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_outgoing_image_text, parent, false),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE -> SavedOwnMessageVh(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_outgoing, parent, false),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_NOFLEX -> SavedOwnMessageVh(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_outgoing_noflex, parent, false),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE -> SavedOwnMessageVh(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_outgoing_image, parent, false),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT -> SavedOwnMessageVh(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_outgoing_image_text, parent, false),
                this, this, this, SettingsManager.chatsAppearanceStyle()
            )
            else -> throw IllegalStateException("Unsupported view type!")
        }
    }

    override fun onBindViewHolder(holder: BasicMessageVH, position: Int) {
        val viewType = getItemViewType(position)
        val messageRealmObject = getMessageItem(position)

        if (messageRealmObject == null) {
            LogManager.w(
                MessagesAdapter::class.java.simpleName,
                "onBindViewHolder Null message item. Position: $position"
            )
            return
        }

        // setup message uniqueId
        if (holder is MessageVH) {
            holder.messageId = messageRealmObject.primaryKey
        }

        val groupMember =
            if (messageRealmObject.groupchatUserId != null
                && messageRealmObject.groupchatUserId.isNotEmpty()
            ) {
                getGroupMemberById(
                    chat.account, chat.contactJid, messageRealmObject.groupchatUserId
                )
            } else {
                null
            }

        // need tail
        var needTail = true
        val nextMessage = getMessageItem(position + 1)
        if (nextMessage != null) {
            if (isSavedMessagesMode) {
                val actualCurrentMessage: MessageRealmObject =
                    if (messageRealmObject.account.bareJid.toString()
                            .contains(messageRealmObject.user.bareJid.toString())
                        && messageRealmObject.hasForwardedMessages()
                    ) {
                        MessageRepository.getForwardedMessages(messageRealmObject)[0]
                    } else {
                        messageRealmObject
                    }

                val actualNextMessage: MessageRealmObject =
                    if (nextMessage.account.bareJid.toString()
                            .contains(nextMessage.user.bareJid.toString())
                        && nextMessage.hasForwardedMessages()
                    ) MessageRepository.getForwardedMessages(nextMessage)[0] else nextMessage

                if (actualNextMessage.groupchatUserId != null && actualCurrentMessage.groupchatUserId != null) {
                    needTail =
                        actualCurrentMessage.groupchatUserId != actualNextMessage.groupchatUserId
                } else if (actualNextMessage.groupchatUserId == null && actualCurrentMessage.groupchatUserId == null) {
                    try {
                        val currentMessageSender =
                            ContactJid.from(actualCurrentMessage.originalFrom)
                        val previousMessageSender = ContactJid.from(actualNextMessage.originalFrom)
                        needTail =
                            !currentMessageSender.bareJid.toString()
                                .contains(previousMessageSender.bareJid.toString())
                    } catch (e: Exception) {
                        LogManager.exception(this, e)
                    }
                }

            } else {
                if (groupMember != null) {
                    val user2 =
                        if (nextMessage.groupchatUserId == null) {
                            null
                        } else {
                            getGroupMemberById(
                                chat.account,
                                chat.contactJid,
                                nextMessage.groupchatUserId
                            )
                        }
                    needTail = if (user2 != null) groupMember.memberId != user2.memberId else true

                } else if (viewType != VIEW_TYPE_ACTION_MESSAGE) {
                    needTail =
                        getSimpleType(viewType) != getSimpleType(getItemViewType(position + 1))
                }
            }
        }

        // need date, need name
        var needDate = false
        var needName = false
        val previousMessage = getMessageItem(position - 1)
        if (previousMessage != null) {
            if (isSavedMessagesMode) {
                val currentMessage: MessageRealmObject =
                    if (messageRealmObject.account.bareJid.toString()
                            .contains(messageRealmObject.user.bareJid.toString())
                        && messageRealmObject.hasForwardedMessages()
                    ) {
                        MessageRepository.getForwardedMessages(messageRealmObject)[0]
                    } else {
                        messageRealmObject
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
                    needName = currentMessage.groupchatUserId != actualPrevious.groupchatUserId
                } else if (actualPrevious.groupchatUserId == null && currentMessage.groupchatUserId == null) {
                    try {
                        val currentMessageSender = ContactJid.from(currentMessage.originalFrom)
                        val previousMessageSender = ContactJid.from(actualPrevious.originalFrom)
                        needName = !currentMessageSender.bareJid.toString().contains(
                            previousMessageSender.bareJid.toString()
                        )
                    } catch (e: Exception) {
                        LogManager.exception(this, e)
                    }
                } else needName = true
            } else {
                needDate = !Utils.isSameDay(
                    getMessageItem(position)!!.timestamp,
                    previousMessage.timestamp
                )
                needName =
                    if (messageRealmObject.groupchatUserId != null && messageRealmObject.groupchatUserId.isNotEmpty()
                        && previousMessage.groupchatUserId != null && previousMessage.groupchatUserId.isNotEmpty()
                    ) {
                        messageRealmObject.groupchatUserId != previousMessage.groupchatUserId
                    } else {
                        true
                    }
            }
        } else {
            needDate = true
            needName = true
        }

        val extraData = MessageExtraData(
            fileListener,
            fwdListener,
            context,
            RosterManager.getInstance().getName(chat.account, chat.contactJid),
            ColorManager.getInstance().getChatIncomingBalloonColorsStateList(chat.account),
            groupMember,
            ColorManager.getInstance().accountPainter.getAccountMainColor(chat.account),
            ColorManager.getInstance().accountPainter.getAccountIndicatorBackColor(chat.account),
            messageRealmObject.timestamp,
            itemsNeedOriginalText.contains(messageRealmObject.primaryKey),
            messageRealmObject.primaryKey == firstUnreadMessageID,
            checkedItemIds.contains(messageRealmObject.primaryKey),
            needTail,
            needDate,
            needName
        )

        when (viewType) {
            VIEW_TYPE_ACTION_MESSAGE -> {
                if (holder is ActionMessageVH) {
                    holder.bind(messageRealmObject, context, chat.account, needDate)
                }
            }

            VIEW_TYPE_INCOMING_MESSAGE -> if (holder is IncomingMessageVH) {
                holder.bind(messageRealmObject, extraData)
            }

            VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT, VIEW_TYPE_INCOMING_MESSAGE_IMAGE,
            VIEW_TYPE_INCOMING_MESSAGE_NOFLEX -> {
                if (holder is NoFlexIncomingMsgVH) {
                    holder.bind(messageRealmObject, extraData)
                }
            }

            VIEW_TYPE_OUTGOING_MESSAGE -> if (holder is OutgoingMessageVH) {
                holder.bind(messageRealmObject, extraData)
            }

            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT, VIEW_TYPE_OUTGOING_MESSAGE_IMAGE,
            VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX -> {
                if (holder is NoFlexOutgoingMsgVH) {
                    holder.bind(messageRealmObject, extraData)
                }
            }

            VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE -> {
                if (holder is GroupchatSystemMessageVH) {
                    holder.bind(messageRealmObject)
                }
                if (holder is SavedOwnMessageVh) {
                    holder.bind(messageRealmObject, extraData)
                }
                if (holder is SavedCompanionMessageVH) {
                    holder.bind(messageRealmObject, extraData)
                }
            }

            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE, VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_NOFLEX,
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE, VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT -> {
                if (holder is SavedOwnMessageVh) {
                    holder.bind(messageRealmObject, extraData)
                }
                if (holder is SavedCompanionMessageVH) {
                    holder.bind(messageRealmObject, extraData)
                }
            }

            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE, VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE,
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_NOFLEX, VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT -> {
                if (holder is SavedCompanionMessageVH) {
                    holder.bind(messageRealmObject, extraData)
                }
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

    fun addOrRemoveItemNeedOriginalText(messageId: String) {
        if (itemsNeedOriginalText.contains(messageId)) {
            itemsNeedOriginalText.remove(messageId)
        } else {
            itemsNeedOriginalText.add(messageId)
        }
    }

    /** File listener  */
    override fun onImageClick(messagePosition: Int, attachmentPosition: Int, messageUID: String) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition)
        } else {
            fileListener?.onImageClick(messagePosition, attachmentPosition, messageUID)
        }
    }

    override fun onFileClick(messagePosition: Int, attachmentPosition: Int, messageUID: String) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition)
        } else {
            fileListener?.onFileClick(messagePosition, attachmentPosition, messageUID)
        }
    }

    override fun onVoiceClick(
        messagePosition: Int, attachmentPosition: Int, attachmentId: String, messageUID: String,
        timestamp: Long
    ) {
        if (isCheckMode) {
            addOrRemoveCheckedItem(messagePosition)
        } else {
            fileListener?.onVoiceClick(
                messagePosition,
                attachmentPosition,
                attachmentId,
                messageUID,
                timestamp
            )
        }
    }

    override fun onFileLongClick(attachmentRealmObject: AttachmentRealmObject, caller: View) {
        fileListener?.onFileLongClick(attachmentRealmObject, caller)
    }

    override fun onDownloadCancel() {
        fileListener?.onDownloadCancel()
    }

    override fun onUploadCancel() {
        fileListener?.onUploadCancel()
    }

    override fun onDownloadError(error: String) {
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
        listener?.onChangeCheckedItems(checkedItemIds.size)
    }

    fun resetCheckedItems() {
        if (checkedItemIds.size > 0) {
            checkedItemIds.clear()
            checkedMessageRealmObjects.clear()
            isCheckMode = false
            notifyDataSetChanged()
            listener?.onChangeCheckedItems(checkedItemIds.size)
        }
    }

    private fun getSimpleType(type: Int): Int {
        return when (type) {

            VIEW_TYPE_INCOMING_MESSAGE, VIEW_TYPE_INCOMING_MESSAGE_NOFLEX, VIEW_TYPE_INCOMING_MESSAGE_IMAGE,
            VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT, VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE,
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE, VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT,
            VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_NOFLEX -> 1

            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE, VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE,
            VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT, VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_NOFLEX,
            VIEW_TYPE_OUTGOING_MESSAGE, VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX, VIEW_TYPE_OUTGOING_MESSAGE_IMAGE,
            VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT -> 2

            VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE, VIEW_TYPE_ACTION_MESSAGE -> 3

            else -> 0
        }
    }

    companion object {
        const val VIEW_TYPE_INCOMING_MESSAGE = 1
        const val VIEW_TYPE_OUTGOING_MESSAGE = 2
        const val VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE = 3
        const val VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE = 4
        const val VIEW_TYPE_INCOMING_MESSAGE_NOFLEX = 5
        const val VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX = 6
        const val VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_NOFLEX = 7
        const val VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_NOFLEX = 8
        const val VIEW_TYPE_OUTGOING_MESSAGE_IMAGE = 9
        const val VIEW_TYPE_INCOMING_MESSAGE_IMAGE = 10
        const val VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE = 11
        const val VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE = 12
        const val VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT = 13
        const val VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT = 14
        const val VIEW_TYPE_SAVED_SINGLE_OWN_MESSAGE_IMAGE_TEXT = 15
        const val VIEW_TYPE_SAVED_SINGLE_COMPANION_MESSAGE_IMAGE_TEXT = 16
        const val VIEW_TYPE_ACTION_MESSAGE = 17
        const val VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE = 18
    }

}