package com.xabber.xmpp.groups.rights

import com.xabber.android.data.message.chat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupRequestMemberRightsChangeIQ(val groupchat: GroupChat, val dataForm: DataForm)
    : GroupchatAbstractRightsIQ() {

    init {
        to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }

}