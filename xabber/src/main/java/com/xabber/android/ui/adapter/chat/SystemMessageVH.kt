package com.xabber.android.ui.adapter.chat

import android.content.Context
import android.view.Gravity
import android.view.View
import androidx.annotation.StyleRes
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.helper.dipToPx
import com.xabber.android.ui.text.getDateStringForMessage

class SystemMessageVH(
    itemView: View, @StyleRes appearance: Int = 0
) : BasicMessageVH(itemView, appearance) {

    fun bind(messageRealmObject: MessageRealmObject, needDate: Boolean, context: Context) {

        when {
            messageRealmObject.chatAction != null -> {
                messageTextTv.text = messageRealmObject.chatAction.getText(
                    context,
                    RosterManager.getInstance().getBestContact(
                        messageRealmObject.account, messageRealmObject.user
                    ).name,
                    messageRealmObject.spannable.toString()
                )
            }
            messageRealmObject.isGroupchatSystem -> {
                messageTextTv.text = messageRealmObject.text
            }
            else -> {
                LogManager.w(this, "Tried to bind system messages, but there are no implemented binds for current message")
            }
        }

        messageTextTv.gravity = Gravity.CENTER_HORIZONTAL

        date = getDateStringForMessage(messageRealmObject.timestamp)
        this.needDate = needDate

        itemView.setPadding(
            itemView.paddingLeft,
            dipToPx(4f, itemView.context),
            itemView.paddingRight,
            dipToPx(4f, itemView.context)
        )
    }

}