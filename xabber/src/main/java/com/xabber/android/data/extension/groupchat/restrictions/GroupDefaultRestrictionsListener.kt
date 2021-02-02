package com.xabber.android.data.extension.groupchat.restrictions

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.message.chat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

interface GroupDefaultRestrictionsListener: BaseUIListener {
    fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onError(groupchat: GroupChat)
    fun onSuccessful(groupchat: GroupChat)
}