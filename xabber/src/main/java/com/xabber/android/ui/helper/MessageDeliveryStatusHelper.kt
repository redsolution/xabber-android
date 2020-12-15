package com.xabber.android.ui.helper

import android.view.View
import android.widget.ImageView
import com.xabber.android.R
import com.xabber.android.data.database.realmobjects.MessageRealmObject

object MessageDeliveryStatusHelper {

    fun setupStatusImageView(messageRealmObject: MessageRealmObject, imageView: ImageView) {
        imageView.visibility = if (messageRealmObject.text == null || messageRealmObject.isIncoming)
            View.INVISIBLE else View.VISIBLE
        imageView.setImageResource(getMessageStatusIconResource(messageRealmObject))
    }

    fun getMessageStatusIconResource(messageRealmObject: MessageRealmObject): Int {
        return if (!messageRealmObject.isIncoming) {
            if (MessageRealmObject.isUploadFileMessage(messageRealmObject)
                    && !messageRealmObject.isSent
                    && System.currentTimeMillis() - messageRealmObject.timestamp > 1000)
                R.drawable.ic_message_not_sent_14dp
            else if (messageRealmObject.isDisplayed || messageRealmObject.isReceivedFromMessageArchive)
                R.drawable.ic_message_displayed
            else if (messageRealmObject.isDelivered)
                R.drawable.ic_message_delivered_14dp
            else if (messageRealmObject.isError)
                R.drawable.ic_message_has_error_14dp
            else if (messageRealmObject.isAcknowledged || messageRealmObject.isForwarded)
                R.drawable.ic_message_acknowledged_14dp
            else R.drawable.ic_message_not_sent_14dp
        } else R.drawable.ic_message_not_sent_14dp
    }

}