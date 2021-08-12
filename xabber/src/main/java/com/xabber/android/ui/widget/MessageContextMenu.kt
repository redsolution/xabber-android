package com.xabber.android.ui.widget

import android.app.Activity
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
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.widget.CustomMessageMenuAdapter.Companion.KEY_ID_COPY
import com.xabber.android.ui.widget.CustomMessageMenuAdapter.Companion.KEY_ID_EDIT
import com.xabber.android.ui.widget.CustomMessageMenuAdapter.Companion.KEY_ID_OTR
import com.xabber.android.ui.widget.CustomMessageMenuAdapter.Companion.KEY_ID_PIN
import com.xabber.android.ui.widget.CustomMessageMenuAdapter.Companion.KEY_ID_QUOTE
import com.xabber.android.ui.widget.CustomMessageMenuAdapter.Companion.KEY_ID_REMOVE
import com.xabber.android.ui.widget.CustomMessageMenuAdapter.Companion.KEY_ID_REPEAT
import com.xabber.android.ui.widget.CustomMessageMenuAdapter.Companion.KEY_ID_STATUS
import com.xabber.android.utils.StringUtils
import java.util.*

fun buildAndShowMessageContextMenu(
    activity: Activity,
    anchorView: View,
    message: MessageRealmObject,
    chat: AbstractChat,
    onMessageRepeatClick: () -> Unit,
    onMessageCopyClick: () -> Unit,
    onMessageQuoteClick: () -> Unit,
    onMessageRemoveClick: () -> Unit,
    onShowOriginalOtrClick: () -> Unit,
    onMessageStatusClick: () -> Unit,
    onMessageEditClick: () -> Unit,
    onPinClick: () -> Unit,
) {
    fun MutableList<HashMap<String, String>>.addMenuItem(id: String, title: String) {
        val map = HashMap<String, String>()
        map[CustomMessageMenuAdapter.KEY_ID] = id
        map[CustomMessageMenuAdapter.KEY_TITLE] = title
        this.add(map)
    }

    val menuItems: MutableList<HashMap<String, String>> = mutableListOf()

    if (message.messageStatus == MessageStatus.ERROR) {
        menuItems.addMenuItem(KEY_ID_REPEAT, activity.getString(R.string.message_repeat))
    }

    if (message.messageStatus != MessageStatus.UPLOADING) {
        menuItems.addMenuItem(KEY_ID_QUOTE, activity.getString(R.string.message_quote))
        menuItems.addMenuItem(KEY_ID_COPY, activity.getString(R.string.message_copy))
        menuItems.addMenuItem(KEY_ID_REMOVE, activity.getString(R.string.message_remove))
    }

    if (!message.isIncoming && !message.haveAttachments()
        && (message.messageStatus == MessageStatus.DELIVERED
                || message.messageStatus == MessageStatus.DISPLAYED
                || message.messageStatus == MessageStatus.RECEIVED)
    ) {
        menuItems.addMenuItem(KEY_ID_EDIT, activity.getString(R.string.message_edit))
    }

    if (OTRManager.getInstance().isEncrypted(message.text)) {
        menuItems.addMenuItem(KEY_ID_OTR, activity.getString(R.string.message_otr_show_original))
    }

    if (chat is GroupChat) {
        menuItems.addMenuItem(KEY_ID_PIN, activity.getString(R.string.message_pin))
    }

    if (chat.account.bareJid.toString().contains(chat.contactJid.bareJid.toString())
        && message.isIncoming
        && message.hasForwardedMessages()
    ) {
        menuItems.addMenuItem(
            CustomMessageMenuAdapter.KEY_ID_TIMESTAMP,
            StringUtils.getDateTimeText(
                Date(MessageRepository.getForwardedMessages(message).first().timestamp)
            )
        )
    }

    if (message.messageStatus != MessageStatus.UPLOADING
        && message.messageStatus != MessageStatus.NONE
        && message.messageStatus != MessageStatus.NOT_SENT
    ) {
        menuItems.addMenuItem(KEY_ID_STATUS, message.messageStatus.toString())
    }


    val listener =
        AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            if (menuItems.size > position) {
                val menuItem = menuItems[position]
                when (menuItem[CustomMessageMenuAdapter.KEY_ID]) {
                    KEY_ID_REPEAT -> onMessageRepeatClick()
                    KEY_ID_COPY -> onMessageCopyClick()
                    KEY_ID_QUOTE -> onMessageQuoteClick()
                    KEY_ID_REMOVE -> onMessageRemoveClick()
                    KEY_ID_OTR -> onShowOriginalOtrClick()
                    KEY_ID_STATUS -> onMessageStatusClick()
                    KEY_ID_EDIT -> onMessageEditClick()
                    KEY_ID_PIN -> onPinClick()
                    else -> { /* ignore */
                    }
                }
            }
        }

    showMenu(activity, anchorView, menuItems, listener)
}

private fun showMenu(
    context: Context, anchor: View, items: List<HashMap<String, String>>,
    itemClickListener: AdapterView.OnItemClickListener
) {
    ListPopupWindow(context).apply {
        val adapter = CustomMessageMenuAdapter(context, items)
        setAdapter(adapter)
        anchorView = anchor
        isModal = true
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        setOnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            itemClickListener.onItemClick(parent, view, position, id)
            dismiss()
        }

        // measure content dimens
        var mMeasureParent: ViewGroup? = null
        var height = 0
        var maxWidth = 0
        var itemView: View? = null
        var itemType = 0
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
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
    context: Context, var items: List<HashMap<String, String>>
) : BaseAdapter() {

    var lInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount() = items.size

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = items[position]
        val view: View
        val textView: TextView
        val ivStatus: ImageView
        when {
            item[KEY_ID] == KEY_ID_TIMESTAMP -> {
                view = lInflater.inflate(R.layout.item_menu_message_timestamp, parent, false)
                textView = view.findViewById<View>(R.id.tvStatus) as TextView
                textView.text = item[KEY_TITLE]
            }
            item[KEY_ID] == KEY_ID_STATUS -> {
                view = lInflater.inflate(R.layout.item_menu_message_status, parent, false)
                textView = view.findViewById<View>(R.id.tvStatus) as TextView
                ivStatus = view.findViewById<View>(R.id.ivStatus) as ImageView
                when (MessageStatus.valueOf(item[KEY_TITLE]!!)) {
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
                textView.text = item[KEY_TITLE]
            }
        }
        return view
    }

    companion object {
        const val KEY_ID_REPEAT = "action_message_repeat"
        const val KEY_ID_COPY = "action_message_copy"
        const val KEY_ID_QUOTE = "action_message_quote"
        const val KEY_ID_REMOVE = "action_message_remove"
        const val KEY_ID_OTR = "action_message_show_original_otr"
        const val KEY_ID_EDIT = "action_message_edit"
        const val KEY_ID_PIN = "action_message_pin"
        const val KEY_ID_STATUS = "action_message_status"
        const val KEY_ID_TIMESTAMP = "action_message_timestamp"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
    }

}