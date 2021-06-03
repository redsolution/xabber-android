package com.xabber.android.ui.adapter.chat

import android.view.View
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.MessageRepository

/**
 * Represents saved message, contained only one outgoing message
 */
class SavedOwnMessageVh(
    val itemView: View,
    val messageListener: MessageClickListener,
    val longClickListener: MessageLongClickListener,
    val fileListener: FileListener,
    val appearance: Int,
) : NoFlexOutgoingMsgVH(
    itemView,
    messageListener,
    longClickListener,
    fileListener,
    appearance,
) {

    override fun bind(messageRealmObject: MessageRealmObject, extraData: MessagesAdapter.MessageExtraData) {
        super.bind(
            MessageRepository.getForwardedMessages(messageRealmObject)?.first() ?: messageRealmObject,
            extraData
        )
    }

}