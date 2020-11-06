package com.xabber.android.ui.adapter.chat

import android.view.Gravity
import android.view.View
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.utils.StringUtils

class GroupchatSystemMessageVH(val itemView: View): BasicMessageVH(itemView) {

    fun bind(messageRealmObject: MessageRealmObject) {
        messageText.text = messageRealmObject.text
        messageText.gravity = Gravity.CENTER_HORIZONTAL
        date = StringUtils.getDateStringForMessage(messageRealmObject.timestamp)
    }
}