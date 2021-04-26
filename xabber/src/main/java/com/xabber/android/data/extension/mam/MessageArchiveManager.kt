package com.xabber.android.data.extension.mam

import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountItem
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.listeners.OnPacketListener
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageHandler
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.roster.OnRosterReceivedListener
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.OnLastHistoryLoadErrorListener
import com.xabber.android.ui.OnLastHistoryLoadFinishedListener
import com.xabber.android.ui.OnLastHistoryLoadStartedListener
import com.xabber.android.ui.forEachOnUi
import com.xabber.xmpp.mam.MamQueryIQ
import com.xabber.xmpp.mam.MamResultExtensionElement
import io.realm.Realm
import io.realm.Sort
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import org.jivesoftware.smackx.mam.element.MamElements

object MessageArchiveManager: OnRosterReceivedListener, OnPacketListener {

    const val NAMESPACE = "urn:xmpp:mam:2"

    override fun onRosterReceived(accountItem: AccountItem) {
        RosterManager.getInstance().getAccountRosterContacts(accountItem.account).forEach { rosterContact ->
            loadLastMessageInChat(ChatManager.getInstance().getChat(rosterContact.account, rosterContact.contactJid)
                    ?: ChatManager.getInstance().createRegularChat(rosterContact.account, rosterContact.contactJid))
        }
    }

    override fun onStanza(connection: ConnectionItem, packet: Stanza) {
        val accountJid = connection.account
        if (packet is Message && packet.hasExtension(MamResultExtensionElement.ELEMENT, NAMESPACE)){
            packet.extensions.filterIsInstance<MamResultExtensionElement>().forEach { mamResultElement ->
                val forwardedElement = mamResultElement.forwarded.forwardedStanza
                val contactJid =
                        if (forwardedElement.from.asBareJid() ==accountJid.fullJid.asBareJid()) {
                            ContactJid.from(forwardedElement.to.asBareJid().toString())
                        } else ContactJid.from(forwardedElement.from.asBareJid().toString())
                val delayInformation = mamResultElement.forwarded.delayInformation
                if (forwardedElement != null && forwardedElement is Message){
                    MessageHandler.parseMessage(accountJid, contactJid, forwardedElement, delayInformation)
                }
            }
        }
    }

    fun isSupported(accountItem: AccountItem) = try {
        ServiceDiscoveryManager.getInstanceFor(accountItem.connection)
                .supportsFeature(accountItem.connection.user.asBareJid(), MamElements.NAMESPACE)
    } catch (e: Exception) {
        LogManager.exception(this::class.java.simpleName, e)
        false
    }

    fun loadMessageByStanzaId(chat: AbstractChat, stanzaId: String){
        Application.getInstance().runInBackgroundNetwork {
            AccountManager.getInstance().getAccount(chat.account)?.connection?.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqMessageWithStanzaId(chat, stanzaId),
                    { packet -> if (packet is IQ && packet.type == IQ.Type.result) {
                        LogManager.i(MessageArchiveManager.javaClass,
                                "Message with stanza id $stanzaId successfully fetched")
                    } },
                    { exception -> LogManager.exception(MessageArchiveManager.javaClass, exception) }
            )
        }
    }

    fun loadLastMessageInChat(chat: AbstractChat){
        Application.getInstance().runInBackgroundNetwork {
            AccountManager.getInstance().getAccount(chat.account)?.connection?.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqLastMessageInChat(chat),
                    { },
                    { exception -> LogManager.exception(MessageArchiveManager.javaClass, exception) }
            )
        }
    }

    fun loadAllMessagesInChat(chat: AbstractChat){
        Application.getInstance().runInBackgroundNetwork {
            AccountManager.getInstance().getAccount(chat.account)?.connection?.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqAllMessagesInChat(chat),
                    { packet ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            LogManager.i(MessageArchiveManager.javaClass,
                                "All messages with in chat ${chat.account} and ${chat.contactJid} successfully fetched")
                    } },
                    { exception -> LogManager.exception(MessageArchiveManager.javaClass, exception) }
            )
        }
    }

    fun onChatOpen(chat: AbstractChat){
        //todo this
        LogManager.i(this, "Not implemented")
    }

    private fun getFirstChatMessageMessageInRealmStanzaId(chat: AbstractChat,
    ): String? {
        var result: String? = ""
        var realm: Realm? = null
        try {
            realm = DatabaseManager.getInstance().defaultRealmInstance
            result = realm.where(MessageRealmObject::class.java)
                    .equalTo(MessageRealmObject.Fields.ACCOUNT, chat.account.toString())
                    .equalTo(MessageRealmObject.Fields.USER, chat.contactJid.toString())
                    .isNull(MessageRealmObject.Fields.PARENT_MESSAGE_ID)
                    .isNotNull(MessageRealmObject.Fields.STANZA_ID)
                    .findAll()
                    .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
                    .first()
                    ?.stanzaId
        } catch (e: Exception){
            LogManager.exception(MessageArchiveManager::class.java, e)
        } finally {
            if (Looper.getMainLooper() != Looper.myLooper() && realm != null) realm.close()
        }
        return result
    }


    fun loadNextMessagesPortionInChat(chat: AbstractChat){
        LogManager.d(MessageArchiveManager::class.java, "Invoked loadNextMessagesPortionInChat")

        Application.getInstance().runInBackgroundNetworkUserRequest {

            val accountItem = AccountManager.getInstance().getAccount(chat.account)
            if (accountItem == null
                    || accountItem.loadHistorySettings == LoadHistorySettings.none
                    || !isSupported(accountItem)) {
                return@runInBackgroundNetworkUserRequest
            }

            Application.getInstance().getUIListeners(OnLastHistoryLoadStartedListener::class.java).forEachOnUi {
                listener -> listener.onLastHistoryLoadStarted(chat.account, chat.contactJid)
            }

            accountItem.connection.sendIqWithResponseCallback(
                    MamQueryIQ.createMamRequestIqMessagesAfterInChat(
                            chat, getFirstChatMessageMessageInRealmStanzaId(chat) ?: "" ),
                    { packet: Stanza? ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            Application.getInstance().getUIListeners(OnLastHistoryLoadFinishedListener::class.java)
                                    .forEachOnUi { listener -> listener.onLastHistoryLoadFinished(chat.account, chat.contactJid)
                                    }
                        } },
                    { exception: Exception? ->
                        Application.getInstance().getUIListeners(OnLastHistoryLoadErrorListener::class.java)
                                .forEachOnUi { listener ->
                                    if (exception is XMPPException.XMPPErrorException) {
                                        listener.onLastHistoryLoadingError(
                                                chat.account, chat.contactJid, exception.xmppError.conditionText)
                                    } else listener.onLastHistoryLoadingError(chat.account, chat.contactJid)
                                }
                    },
                    60000
            )
        }
    }

}