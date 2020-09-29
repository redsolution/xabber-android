package com.xabber.android.data.extension.groupchat.rights

import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupchatMemberRightsReplyIQ : GroupchatAbstractRightsIQ() {

    var dataFrom: DataForm? = null

    companion object {

        const val FIELD_USER_ID = "user-id"

        const val FIELD_PERMISSIONS = "permission"

        const val FIELD_OWNER = "owner"
        const val FIELD_RESTRICT_PARTICIPANTS = "restrict-participants"
        const val FIELD_BLOCK_PARTICIPANTS = "block-participants"
        const val FIELD_ADMINISTRATOR = "administrator"
        const val FIELD_CHANGE_BADGES = "change-badges"
        const val FIELD_CHANGE_NICKNAMES = "change-nicknames"
        const val FIELD_DELETE_MESSAGES = "delete-messages"

        const val FIELD_RESTRICTIONS = "restrictions"
        const val FIELD_SEND_MESSAGES = "send-messages"
        const val FIELD_READ_MESSAGES = "read-messages"
        const val FIELD_SEND_INVITATIONS = "send-invitations"
        const val FIELD_SEND_AUDIO = "send-audio"
        const val FIELD_SEND_IMAGES = "send-images"
    }

    init {
        this.type = Type.result
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
        append(dataFrom?.toXML())
    }

}