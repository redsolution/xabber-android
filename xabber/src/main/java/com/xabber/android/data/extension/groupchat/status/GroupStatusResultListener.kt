package com.xabber.android.data.extension.groupchat.status

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.message.chat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

interface GroupStatusResultListener: BaseUIListener {
    fun onStatusDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onError(groupchat: GroupChat)
    fun onStatusSuccessfullyChanged(groupchat: GroupChat)
}