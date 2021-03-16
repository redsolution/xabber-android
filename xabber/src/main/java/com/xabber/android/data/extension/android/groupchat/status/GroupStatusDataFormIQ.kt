package com.xabber.android.data.extension.groupchat.status

import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupStatusDataFormIQ: AbstractGroupStatusIQ() {

    var dataForm:DataForm? = null

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm?.toXML())
    }

}