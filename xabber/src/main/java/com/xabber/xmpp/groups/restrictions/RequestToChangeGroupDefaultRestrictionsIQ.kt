package com.xabber.xmpp.groups.restrictions

import com.xabber.android.data.message.chat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

class RequestToChangeGroupDefaultRestrictionsIQ(groupchat: GroupChat, private val dataForm: DataForm)
    : AbstractGroupDefaultRestrictionsIQ() {

    init {
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }

}