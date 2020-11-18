package com.xabber.android.data.extension.groupchat.rights

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.message.chat.groupchat.GroupChat

interface GroupMemberRightsListener: BaseUIListener {
    fun onGroupchatMemberRightsFormReceived(groupchat: GroupChat, iq: GroupchatMemberRightsReplyIQ)
    fun onSuccessfullyChanges(groupchat: GroupChat)
    fun onError(groupchat: GroupChat)
}