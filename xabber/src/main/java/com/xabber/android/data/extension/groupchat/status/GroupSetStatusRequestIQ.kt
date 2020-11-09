package com.xabber.android.data.extension.groupchat.status

import com.xabber.android.data.entity.ContactJid
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupSetStatusRequestIQ(groupchatJid: ContactJid, private val dataForm: DataForm)
    : AbstractGroupStatusIQ() {

    init {
        type = Type.set
        to = groupchatJid.bareJid
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }
}