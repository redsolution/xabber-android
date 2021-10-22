package com.xabber.android.ui.widget

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import com.xabber.android.R
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.text.getDateTimeText
import java.util.*

class MessageContextMenu(
    private val context: Context,
    private val anchor: View,
    message: MessageRealmObject,
    chat: AbstractChat,
    private val onMessageRepeatClick: () -> Unit,
    private val onMessageCopyClick: () -> Unit,
    private val onMessageQuoteClick: () -> Unit,
    private val onMessageRemoveClick: () -> Unit,
    private val onMessageStatusClick: () -> Unit,
    private val onMessageEditClick: () -> Unit,
    private val onPinClick: () -> Unit,
    private val onMentionUserClick: () -> Unit,
) {

    private val listener =
        AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            if (menuItems.size > position) {
                when (menuItems[position]) {
                    is MessageContextMenuItem.Repeat -> onMessageRepeatClick()
                    is MessageContextMenuItem.Copy -> onMessageCopyClick()
                    is MessageContextMenuItem.Quote -> onMessageQuoteClick()
                    is MessageContextMenuItem.Remove -> onMessageRemoveClick()
                    is MessageContextMenuItem.Status -> onMessageStatusClick()
                    is MessageContextMenuItem.Edit -> onMessageEditClick()
                    is MessageContextMenuItem.Pin -> onPinClick()
                    is MessageContextMenuItem.Timestamp -> onMentionUserClick()
                }
            }
        }

    private val menuItems: MutableList<MessageContextMenuItem> = mutableListOf()

    init {
        if (message.messageStatus == MessageStatus.ERROR) {
            menuItems += MessageContextMenuItem.Repeat(context)
        }

        if (message.messageStatus != MessageStatus.UPLOADING) {
            menuItems += MessageContextMenuItem.Quote(context)
            menuItems += MessageContextMenuItem.Copy(context)
            menuItems += MessageContextMenuItem.Remove(context)
        }

        if (!message.isIncoming && !message.hasAttachments()
            && (message.messageStatus == MessageStatus.DELIVERED
                    || message.messageStatus == MessageStatus.DISPLAYED
                    || message.messageStatus == MessageStatus.RECEIVED)
        ) {
            menuItems += MessageContextMenuItem.Edit(context)
        }

        if (chat is GroupChat) {
            menuItems += MessageContextMenuItem.Pin(context)
        }

        if (chat.account.bareJid.toString().contains(chat.contactJid.bareJid.toString())
            && message.isIncoming
            && message.hasForwardedMessages()
        ) {
            menuItems += MessageContextMenuItem.Timestamp(
                Date(MessageRepository.getForwardedMessages(message).first().timestamp)
            )
        }

        if (message.messageStatus != MessageStatus.UPLOADING
            && message.messageStatus != MessageStatus.NONE
            && message.messageStatus != MessageStatus.NOT_SENT
        ) {
            menuItems += MessageContextMenuItem.Status(message.messageStatus)
        }
    }

    fun show() {
        ListPopupWindow(context).apply {
            val adapter = CustomMessageMenuAdapter(context, menuItems)
            setAdapter(adapter)
            anchorView = anchor
            isModal = true
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            setOnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                listener.onItemClick(parent, view, position, id)
                dismiss()
            }

            // measure content dimens
            var mMeasureParent: ViewGroup? = null
            var height = 0
            var maxWidth = 0
            var itemView: View? = null
            var itemType = 0
            val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val heightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val count = adapter.count
            for (i in 0 until count) {
                val positionType = adapter.getItemViewType(i)
                if (positionType != itemType) {
                    itemType = positionType
                    itemView = null
                }
                if (mMeasureParent == null) {
                    mMeasureParent = FrameLayout(context)
                }
                itemView = adapter.getView(i, itemView, mMeasureParent)
                itemView.measure(widthMeasureSpec, heightMeasureSpec)
                val itemHeight = itemView.measuredHeight
                val itemWidth = itemView.measuredWidth
                if (itemWidth > maxWidth) maxWidth = itemWidth
                height += itemHeight
            }

            // set dimens and show
            width = maxWidth
            this.height = height
        }.show()
    }

    private class CustomMessageMenuAdapter(
        context: Context, val items: List<MessageContextMenuItem>
    ) : BaseAdapter() {

        var lInflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val currentItem = items[position]
            val view: View
            val textView: TextView
            val ivStatus: ImageView
            when (currentItem) {
                is MessageContextMenuItem.Timestamp -> {
                    view = lInflater.inflate(R.layout.item_menu_message_timestamp, parent, false)
                    textView = view.findViewById<View>(R.id.tvStatus) as TextView
                    textView.text = currentItem.valueString
                }
                is MessageContextMenuItem.Status -> {
                    view = lInflater.inflate(R.layout.item_menu_message_status, parent, false)
                    textView = view.findViewById(R.id.tvStatus)
                    ivStatus = view.findViewById(R.id.ivStatus)
                    when (MessageStatus.valueOf(currentItem.valueString)) {
                        MessageStatus.ERROR -> {
                            textView.setText(R.string.message_status_error)
                            ivStatus.setImageResource(R.drawable.ic_message_has_error_14dp)
                            textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                        }
                        MessageStatus.SENT -> {
                            textView.setText(R.string.message_status_not_sent)
                            ivStatus.setImageResource(R.drawable.ic_message_not_sent_14dp)
                        }
                        MessageStatus.DISPLAYED -> {
                            textView.setText(R.string.message_status_displayed)
                            ivStatus.setImageResource(R.drawable.ic_message_displayed)
                        }
                        MessageStatus.RECEIVED -> {
                            textView.setText(R.string.message_status_delivered)
                            ivStatus.setImageResource(R.drawable.ic_message_delivered_14dp)
                        }
                        MessageStatus.DELIVERED -> {
                            textView.setText(R.string.message_status_sent)
                            ivStatus.setImageResource(R.drawable.ic_message_acknowledged_14dp)
                        }
                        else -> {
                        }
                    }
                }
                else -> {
                    view = lInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                    textView = view.findViewById<View>(android.R.id.text1) as TextView
                    textView.text = currentItem.valueString
                }
            }
            return view
        }

    }

    private sealed class MessageContextMenuItem(val valueString: String) {
        class Repeat(context: Context) :
            MessageContextMenuItem(context.getString(R.string.message_repeat))

        class Copy(context: Context) :
            MessageContextMenuItem(context.getString(R.string.message_copy))

        class Quote(context: Context) :
            MessageContextMenuItem(context.getString(R.string.message_quote))

        class Remove(context: Context) :
            MessageContextMenuItem(context.getString(R.string.message_remove))

        class Edit(context: Context) :
            MessageContextMenuItem(context.getString(R.string.message_edit))

        class Pin(context: Context) :
            MessageContextMenuItem(context.getString(R.string.message_pin))

        class Status(messageStatus: MessageStatus) :
            MessageContextMenuItem(messageStatus.toString())

        class Timestamp(date: Date) :
            MessageContextMenuItem(date.getDateTimeText())
    }

}