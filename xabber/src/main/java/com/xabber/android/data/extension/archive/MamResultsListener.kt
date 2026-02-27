package com.xabber.android.data.extension.archive

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.MessageHandler
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.xmpp.mam.MamResultExtensionElement
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza

class RegularMamResultsHandler(
    private val accountJid: AccountJid,
    private val chatManager: ChatManager,
    private val messagesHandler: MessageHandler,
    private val listOfIgnoredGroups: MutableSet<GroupChat>? = null,
    private val isRegularReceivedMessage: Boolean = true,
) : StanzaListener {
    override fun processStanza(packet: Stanza) {
        packet.extensions.filterIsInstance<MamResultExtensionElement>().forEach { element ->
            val forwardedElement = element.forwarded.forwardedStanza
            val contactJid =
                if (forwardedElement.from.asBareJid() == accountJid.fullJid.asBareJid()) {
                    ContactJid.from(forwardedElement.to.asBareJid().toString())
                } else {
                    ContactJid.from(forwardedElement.from.asBareJid().toString())
                }

            val delayInformation = element.forwarded.delayInformation

            val chat = chatManager.getChat(accountJid, contactJid)
            if (chat is GroupChat
                && packet.from.asBareJid().toString() == accountJid.bareJid.toString()
                && listOfIgnoredGroups != null
            ) {
                // If we received group message from local archive
                // Don't save this message and request it from remote archive
                listOfIgnoredGroups.add(chat)
            } else if (forwardedElement != null && forwardedElement is Message) {
                messagesHandler.handleMessageStanza(
                    accountJid,
                    contactJid,
                    forwardedElement,
                    delayInformation,
                    isRegularReceivedMessage
                )
            }
        }
    }
}