package com.xabber.android.data.extension.groupchat.invite.incoming

import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import org.jivesoftware.smack.packet.IQ

class DeclineGroupInviteIQ(groupJid: ContactJid): IQ(DECLINE_ELEMENT, GroupchatManager.NAMESPACE + HASH_BLOCK) {

    init {
        to = groupJid.jid
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder) = xml.apply {
        rightAngleBracket()
    }

    companion object{
        private const val HASH_BLOCK = "#invite"
        private const val DECLINE_ELEMENT = "decline"
    }

}