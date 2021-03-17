package com.xabber.xmpp.groups.status

import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupStatusDataFormIQ: AbstractGroupStatusIQ() {

    var dataForm:DataForm? = null

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm?.toXML())
    }

}