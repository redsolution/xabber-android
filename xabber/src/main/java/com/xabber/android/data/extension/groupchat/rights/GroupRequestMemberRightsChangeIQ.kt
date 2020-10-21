package com.xabber.android.data.extension.groupchat.rights

import com.xabber.android.data.entity.ContactJid
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupRequestMemberRightsChangeIQ(val groupchatJid: ContactJid, private val memberId: String,
                                       formFields: Collection<FormField?>)
    : GroupchatAbstractRightsIQ() {

    var dataForm: DataForm

    init {

        to = groupchatJid.jid

        type = Type.set

        dataForm = DataForm(DataForm.Type.submit)

        dataForm.addField(FormField("user-id").apply {
            this.type = FormField.Type.hidden
            this.addValue(memberId)
        })

        dataForm.addField(FormField(FormField.FORM_TYPE).apply {
            this.type = FormField.Type.hidden
            this.addValue(NAMESPACE + HASH_BLOCK)
        })

        for (formField in formFields)
            dataForm.addField(formField)
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataForm.toXML())
    }

}