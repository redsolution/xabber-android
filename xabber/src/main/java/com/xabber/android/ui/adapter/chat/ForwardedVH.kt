package com.xabber.android.ui.adapter.chat

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.helper.dipToPx

open class ForwardedVH(
    itemView: View,
    messageListener: MessageClickListener?,
    longClickListener: MessageLongClickListener?,
    listener: FileListener?,
    appearance: Int,
) : FileMessageVH(itemView, messageListener, longClickListener, listener, appearance) {

    private val tvForwardedCount: TextView = itemView.findViewById(R.id.tvForwardedCount)

    @SuppressLint("UseCompatLoadingForDrawables")
    open fun bind(messageRealmObject: MessageRealmObject, extraData: MessageExtraData, accountJid: String) {
        super.bind(messageRealmObject, extraData)

        // hide STATUS ICONS
        statusIcon.visibility = View.GONE

        // setup MESSAGE AUTHOR
        val jid =
            try {
                ContactJid.from(messageRealmObject.originalFrom)
            } catch (e: ContactJidCreateException) {
                LogManager.exception(javaClass.simpleName, e)
                null
            }

        val author =
            if (extraData.groupMember != null && !extraData.groupMember.isMe) {
                extraData.groupMember.nickname
            } else {
                RosterManager.getDisplayAuthorName(messageRealmObject)
            }

        if (author != null && author.isNotEmpty()) {
            messageHeader.apply {
                text = author
                setTextColor(ColorManager.changeColor(ColorGenerator.MATERIAL.getColor(author), 0.8f))
                visibility = View.VISIBLE
            }
        } else {
            messageHeader.visibility = View.GONE
        }

        // setup FORWARDED
        val context = extraData.context
        val haveForwarded = messageRealmObject.hasForwardedMessages()
        if (haveForwarded) {
            forwardLayout.visibility = View.VISIBLE
            val forwardedCount = messageRealmObject.forwardedIds.size
            tvForwardedCount.text = extraData.context.resources.getQuantityString(
                R.plurals.forwarded_messages_count, forwardedCount, forwardedCount
            )
            tvForwardedCount.paintFlags = tvForwardedCount.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            forwardLayout.setBackgroundColor(ColorManager.getColorWithAlpha(R.color.forwarded_background_color, 0.2f))
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                forwardLeftBorder.setBackgroundColor(extraData.accountMainColor)
                forwardLeftBorder.alpha = 1f
                tvForwardedCount.alpha = 1f
            } else {
                forwardLeftBorder.setBackgroundColor(
                    ColorManager.getInstance().accountPainter
                        .getAccountColorWithTint(messageRealmObject.account, 900)
                )
                forwardLeftBorder.alpha = 0.6f
                tvForwardedCount.alpha = 0.6f
            }
        } else {
            forwardLayout.visibility = View.GONE
        }

        LogManager.d(this, messageRealmObject.forwardedIds.joinToString { it.forwardMessageId })

        // setup BACKGROUND
        val balloonDrawable = context.resources.getDrawable(
            if (haveForwarded) R.drawable.fwd else R.drawable.msg
        )

        val shadowDrawable = context.resources.getDrawable(
            if (haveForwarded) R.drawable.fwd_shadow else R.drawable.msg_shadow
        )

        shadowDrawable.setColorFilter(context.resources.getColor(R.color.black), PorterDuff.Mode.MULTIPLY)
        messageBalloon.background = balloonDrawable
        messageShadow.background = shadowDrawable
        val border = 3.5f
        if (messageRealmObject.haveAttachments()) {
            if (messageRealmObject.isAttachmentImageOnly) {
                messageBalloon.setPadding(
                    dipToPx(border, context),
                    dipToPx(border, context),
                    dipToPx(border, context),
                    dipToPx(border, context)
                )
            }
        }

        // setup BACKGROUND COLOR
        if (jid != null && accountJid != jid.bareJid.toString()) {
            setUpMessageBalloonBackground(messageBalloon, extraData.colorStateList)
        } else {
            setUpMessageBalloonBackground(
                messageBalloon, AppCompatResources.getColorStateList(
                    context,
                    TypedValue().apply {
                        context.theme.resolveAttribute(R.attr.message_background, this, true)
                    }.resourceId
                )
            )
        }
    }

}