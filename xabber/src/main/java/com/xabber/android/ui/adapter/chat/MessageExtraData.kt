package com.xabber.android.ui.adapter.chat

import android.content.res.ColorStateList
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject

data class MessageExtraData(
    val listener: MessageVH.FileListener?,
    val fwdListener: ForwardedAdapter.ForwardListener?,
    val username: String,
    val colors: MessageBalloonColors,
    val groupMember: GroupMemberRealmObject?,
    val accountMainColor: Int,
    val mentionColor: Int,
    val mainMessageTimestamp: Long?,
    val isUnread: Boolean,
    val isChecked: Boolean,
    val isNeedTail: Boolean,
    val isNeedDate: Boolean,
    val isNeedName: Boolean,
)

data class MessageBalloonColors(
    val incomingRegularBalloonColors: ColorStateList,
    val incomingForwardedBalloonColors: ColorStateList,

    val outgoingRegularBalloonColors: ColorStateList,
    val outgoingForwardedBalloonColors: ColorStateList,
)