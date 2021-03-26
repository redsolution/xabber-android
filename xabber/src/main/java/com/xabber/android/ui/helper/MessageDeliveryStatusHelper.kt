package com.xabber.android.ui.helper

import android.view.View
import android.widget.ImageView
import com.xabber.android.R
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.message.MessageStatus

object MessageDeliveryStatusHelper {

    fun setupStatusImageView(messageRealmObject: MessageRealmObject, imageView: ImageView) {
        imageView.visibility =
                if (messageRealmObject.text == null || messageRealmObject.isIncoming) {
                    View.INVISIBLE
                } else View.VISIBLE
        imageView.setImageResource(getMessageStatusIconResource(messageRealmObject))
    }

    fun getMessageStatusIconResource(messageRealmObject: MessageRealmObject): Int =
            when (messageRealmObject.messageStatus){
                MessageStatus.DELIVERED -> R.drawable.ic_message_acknowledged_14dp
                MessageStatus.RECEIVED -> R.drawable.ic_message_delivered_14dp
                MessageStatus.DISPLAYED -> R.drawable.ic_message_displayed
                MessageStatus.ERROR -> R.drawable.ic_message_has_error_14dp
                else -> R.drawable.ic_message_not_sent_14dp
            }

}