package com.xabber.android.data.message

import com.xabber.android.data.TestApplication
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.xmpp.mam.ArchivedIdElement
import com.xabber.xmpp.sid.OriginIdElement
import com.xabber.xmpp.sid.StanzaIdElement
import junit.framework.TestCase
import org.jivesoftware.smack.packet.Message
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


/** Simple, without forward or attachment */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class SimpleMessageHandlerTest : TestCase() {

    private val jidCompanion = ContactJid.from("companion@server.domain/companionResource")
    private val jidAccount = AccountJid.from("account@other_server.domain/resource")
    private val typeChat = Message.Type.chat
    private val messageId = "messageId"

    private val bodyText = "bodyText"

    private val companionArchivedId = "companionArchivedId"
    private val companionStanzaId = companionArchivedId

    private val accountArchivedId = "accountArchivedId"
    private val accountStanzaId = accountArchivedId

    private val messageOriginId = messageId

    // Runtime
    @Test
    fun `test parseMessage for simple regular incoming runtime message`(){

        val sourceMessage = Message().apply {
            from = jidCompanion.jid
            to = jidAccount.fullJid
            type = typeChat
            stanzaId = messageId

            body = bodyText

            addExtension(ArchivedIdElement(companionArchivedId, jidCompanion.bareJid.toString()))
            addExtension(StanzaIdElement(jidCompanion.bareJid.toString(), stanzaId))

            addExtension(ArchivedIdElement(accountArchivedId, jidAccount.bareJid.toString()))
            addExtension(StanzaIdElement(jidAccount.bareJid.toString(), accountStanzaId))

            addExtension(OriginIdElement(messageOriginId))
        }

        val resultMessageRealmObject = MessageHandler.parseMessage(jidAccount, jidCompanion, sourceMessage)

        assertEquals("Error in creating primary key",
                "account@other_server.domain/resource#companion@server.domain/companionResource#$messageOriginId",
                    resultMessageRealmObject?.primaryKey)
        //assertEquals("Error while handling message id", messageId, resultMessageRealmObject.id)

    }

    fun `test parseMessage for simple group incoming runtime message`(){
    }


    // Archive
    fun `test parseMessage for simple regular incoming archive message`(){
    }

    fun `test parseMessage for simple regular outgoing archive message`(){
    }

    fun `test parseMessage for simple group incoming archive message`(){
    }

    fun `test parseMessage for simple group outgoing archive message`(){
    }


    // Carbons
    fun `test parseMessage for simple regular outgoing carbons message`(){

    }

    fun `test parseMessage for simple group outgoing carbons message`(){
    }

}