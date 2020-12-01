package com.xabber.android.data.extension.groupchat.status

import com.xabber.android.data.message.chat.groupchat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupSetStatusRequestIQ(groupchat: GroupChat, private val dataForm: DataForm)
    : AbstractGroupStatusIQ() {

    init {
        type = Type.set
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }
}