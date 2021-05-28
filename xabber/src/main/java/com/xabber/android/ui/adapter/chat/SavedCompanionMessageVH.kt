package com.xabber.android.ui.adapter.chat

import android.view.View
import com.xabber.android.data.database.realmobjects.MessageRealmObject

/**
 * Represents saved message, contained only one message from simple contact or group member
 */
class SavedCompanionMessageVH(
    val itemView: View,
    val messageListener: MessageClickListener,
    val longClickListener: MessageLongClickListener,
    val fileListener: FileListener,
    val bindListener: BindListener,
    val avatarListener: OnMessageAvatarClickListener,
    val appearance: Int,
    val innerMessage: MessageRealmObject,
) : NoFlexIncomingMsgVH(
    itemView,
    messageListener,
    longClickListener,
    fileListener,
    bindListener,
    avatarListener,
    appearance
) {

    override fun bind(messageRealmObject: MessageRealmObject?, extraData: MessagesAdapter.MessageExtraData?) {
        super.bind(messageRealmObject, extraData)

    }
}