package com.xabber.android.data.extension.groupchat.rights

import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupchatMemberRightsReplyIQ : GroupchatAbstractRightsIQ() {

    var dataFrom: DataForm? = null

    companion object {
        const val FIELD_USER_ID = "user-id"
    }

    init {
        this.type = Type.result
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataFrom?.toXML())
    }

}