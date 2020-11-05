package com.xabber.android.data.extension.groupchat.settings

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ
import org.jivesoftware.smackx.xdata.packet.DataForm

class SetGroupSettingsRequestIQ(val groupchatJid: ContactJid, val dataForm: DataForm):
        GroupchatAbstractQueryIQ(NAMESPACE)  {

    init {
        type = Type.set
        to = groupchatJid.bareJid
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }

}