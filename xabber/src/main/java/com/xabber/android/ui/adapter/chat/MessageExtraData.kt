package com.xabber.android.ui.adapter.chat

import android.content.Context
import android.content.res.ColorStateList
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject

data class MessageExtraData(
    val listener: MessageVH.FileListener?,
    val fwdListener: ForwardedAdapter.ForwardListener?,
    val context: Context,
    val username: String,
    val colorStateList: ColorStateList,
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