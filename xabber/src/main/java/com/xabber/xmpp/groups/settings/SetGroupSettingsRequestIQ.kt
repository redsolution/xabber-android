package com.xabber.xmpp.groups.settings

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ
import com.xabber.android.data.message.chat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

class SetGroupSettingsRequestIQ(val groupchat: GroupChat, val dataForm: DataForm):
        GroupchatAbstractQueryIQ(NAMESPACE)  {

    init {
        type = Type.set
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }

}