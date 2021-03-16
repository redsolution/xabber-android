package com.xabber.android.data.extension.groupchat.settings

import com.xabber.android.data.extension.groupchat.members.GroupchatMembersQueryIQ
import com.xabber.android.data.extension.httpfileupload.CustomDataProvider
import org.jivesoftware.smack.provider.IQProvider
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.xmlpull.v1.XmlPullParser

class GroupSettingsRequestDataFormResultProvider: IQProvider<GroupSettingsDataFormResultIQ>() {

    override fun parse(parser: XmlPullParser, initialDepth: Int): GroupSettingsDataFormResultIQ {
        val resultIQ = GroupSettingsDataFormResultIQ()
        var dataForm: DataForm? = DataForm(DataForm.Type.form)
        outerloop@ while (true) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.namespace) {
                    DataForm.NAMESPACE -> {
                        dataForm = CustomDataProvider.INSTANCE.parse(parser, initialDepth)
                        break@outerloop
                    }
                    else -> parser.next()
                }
                XmlPullParser.END_TAG -> if (GroupchatMembersQueryIQ.ELEMENT == parser.name) {
                    break@outerloop
                } else parser.next()
                else -> parser.next()
            }
        }
        resultIQ.dataFrom = dataForm
        return resultIQ
    }
}