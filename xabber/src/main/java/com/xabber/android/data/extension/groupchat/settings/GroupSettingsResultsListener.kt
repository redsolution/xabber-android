package com.xabber.android.data.extension.groupchat.settings

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.message.chat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

interface GroupSettingsResultsListener: BaseUIListener {
    fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm)
    fun onErrorAtDataFormRequesting(groupchat: GroupChat)
    fun onErrorAtSettingsSetting(groupchat: GroupChat)
    fun onGroupSettingsSuccessfullyChanged(groupchat: GroupChat)
}