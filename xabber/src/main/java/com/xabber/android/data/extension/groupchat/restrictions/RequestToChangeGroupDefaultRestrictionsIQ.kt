package com.xabber.android.data.extension.groupchat.restrictions

import com.xabber.android.data.entity.ContactJid
import org.jivesoftware.smackx.xdata.packet.DataForm

class RequestToChangeGroupDefaultRestrictionsIQ(groupJid: ContactJid, private val dataForm: DataForm)
    : AbstractGroupDefaultRestrictionsIQ() {

    init {
        to = groupJid.bareJid
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }

}