package com.xabber.android.data.extension.groupchat.settings

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupSettingsDataFormResultIQ: GroupchatAbstractQueryIQ(NAMESPACE) {

    var dataFrom: DataForm? = null

    init {
        type = Type.result
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataFrom?.toXML().toString())
    }

}