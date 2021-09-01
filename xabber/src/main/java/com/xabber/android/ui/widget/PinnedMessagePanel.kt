package com.xabber.android.ui.widget

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.fragment.ChatFragment
import com.xabber.android.ui.text.getColoredAttachmentDisplayName
import com.xabber.android.ui.text.getDecodedSpannable

class PinnedMessagePanel : Fragment() {

    private var message: MessageRealmObject? = null

    private var onCLoseListener: OnCloseClickListener? = null
    private var onClickListener: OnClickListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.pinned_message_layout, container, false)

        if (message == null) {
            return view.apply { visibility = View.GONE }
        }

        message?.let { message ->
            val groupMember = GroupMemberManager.getGroupMemberById(
                message.account, message.user, message.groupchatUserId
            ) ?: throw NullPointerException("Tried to pin message without group member!")

            onClickListener?.let { listener ->
                view.setOnClickListener { listener.onClick() }
            }

            view.findViewById<TextView>(R.id.pinned_message_text).apply {
                setupMessageText(this, message)
            }

            view.findViewById<ImageView>(R.id.pinned_message_close_iv).apply {
                onCLoseListener?.let { listener ->
                    setOnClickListener { listener.onCloseClick() }
                }
            }

            view.findViewById<TextView>(R.id.pinned_message_jid_tv).apply {
                text = groupMember.bestName
                setTextColor(
                    ColorManager.getInstance().accountPainter.getAccountColorWithTint(
                        message.account, 600
                    )
                )
            }

            view.findViewById<TextView>(R.id.pinned_message_badge_tv).apply {
                if (groupMember.badge.isNullOrEmpty()) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                    text = groupMember.badge
                }
            }

            view.findViewById<TextView>(R.id.pinned_message_role_tv).apply {
                if (groupMember.role != null) {
                    visibility = View.VISIBLE
                    text = groupMember.role.toString()
                    setBackgroundColor(
                        ColorManager.getInstance().accountPainter.getAccountColorWithTint(
                            message.account, 50
                        )
                    )
                } else {
                    visibility = View.GONE
                }
            }

            view.findViewById<ImageView>(R.id.pinned_message_icon).apply {
                setColorFilter(
                    ColorManager.getInstance().accountPainter.getAccountColorWithTint(
                        message.account, 600
                    )
                )
            }
        }

        return view
    }

    override fun onDestroy() {
        onCLoseListener = null
        onClickListener = null
        super.onDestroy()
    }

    private fun setupMessageText(textView: TextView, message: MessageRealmObject) {
        val text = message.text
        val forwardedCount = message.forwardedIds.size
        if (text == null || text.isEmpty()) {
            when {
                forwardedCount > 0 -> {
                    textView.text = resources.getQuantityString(
                        R.plurals.forwarded_messages_count,
                        forwardedCount, forwardedCount
                    )
                }
                message.haveAttachments() -> {
                    textView.text = getColoredAttachmentDisplayName(
                        requireContext(), message.attachmentRealmObjects, -1
                    )
                    textView.typeface = Typeface.DEFAULT
                    return
                }
                else -> textView.text = this.resources.getString(R.string.no_messages)
            }
            textView.setTypeface(textView.typeface, Typeface.ITALIC)
        } else {
            textView.typeface = Typeface.DEFAULT
            textView.visibility = View.VISIBLE
            try {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    try {
                        textView.text =
                            Html.fromHtml(getDecodedSpannable(text).toString())
                    } catch (e: Exception) {
                        textView.text = Html.fromHtml(text)
                    }
                } else textView.text = text
            } catch (e: Exception) {
                LogManager.exception(ChatFragment::class.java.simpleName, e)
                textView.text = text
            } finally {
                textView.alpha = 1f
            }
            textView.typeface = Typeface.DEFAULT
        }
    }

    fun interface OnCloseClickListener {
        fun onCloseClick()
    }

    fun interface OnClickListener {
        fun onClick()
    }

    companion object {

        // This is incorrect instancing
        fun newInstance(
            message: MessageRealmObject,
            onClickListener: OnClickListener? = null,
            onCloseClickListener: OnCloseClickListener? = null
        ) =
            PinnedMessagePanel().apply {
                this.message = message
                this.onClickListener = onClickListener
                this.onCLoseListener = onCloseClickListener
            }

    }

}