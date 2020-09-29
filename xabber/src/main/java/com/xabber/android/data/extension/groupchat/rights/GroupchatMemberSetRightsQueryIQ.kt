package com.xabber.android.data.extension.groupchat.rights

import com.xabber.android.data.entity.ContactJid
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupchatMemberSetRightsQueryIQ(private val dataForm: DataForm, groupchatJid: ContactJid): GroupchatAbstractRightsIQ(){

    init {
        this.type = Type.set
        this.to = groupchatJid.bareJid
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }
}