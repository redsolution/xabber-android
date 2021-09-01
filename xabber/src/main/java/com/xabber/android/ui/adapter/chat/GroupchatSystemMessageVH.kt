package com.xabber.android.ui.adapter.chat

import android.view.Gravity
import android.view.View
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.ui.helper.dipToPx
import com.xabber.android.ui.text.getDateStringForMessage

class GroupchatSystemMessageVH(itemView: View) : BasicMessageVH(itemView) {

    fun bind(messageRealmObject: MessageRealmObject) {
        messageText.text = messageRealmObject.text
        messageText.gravity = Gravity.CENTER_HORIZONTAL
        date = getDateStringForMessage(messageRealmObject.timestamp)

        itemView.setPadding(
            itemView.paddingLeft,
            dipToPx(4f, itemView.context),
            itemView.paddingRight,
            dipToPx(4f, itemView.context)
        )
    }

}