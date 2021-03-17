package com.xabber.xmpp.groups.rights

import com.xabber.android.data.message.chat.GroupChat
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupchatMemberSetRightsQueryIQ(private val dataForm: DataForm, groupchat: GroupChat):
        GroupchatAbstractRightsIQ(){

    init {
        this.type = Type.set
        this.to = groupchat.fullJidIfPossible ?: groupchat.contactJid.jid
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }
}