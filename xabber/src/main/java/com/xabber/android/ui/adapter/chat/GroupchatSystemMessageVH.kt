package com.xabber.android.ui.adapter.chat

import android.view.Gravity
import android.view.View
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.utils.StringUtils
import com.xabber.android.utils.Utils

class GroupchatSystemMessageVH(itemView: View): BasicMessageVH(itemView) {

    fun bind(messageRealmObject: MessageRealmObject) {
        messageText.text = messageRealmObject.text
        messageText.gravity = Gravity.CENTER_HORIZONTAL
        date = StringUtils.getDateStringForMessage(messageRealmObject.timestamp)

        itemView.setPadding(itemView.paddingLeft,
                Utils.dipToPx(4f, itemView.context),
                itemView.paddingRight,
                Utils.dipToPx(4f, itemView.context))
    }

}