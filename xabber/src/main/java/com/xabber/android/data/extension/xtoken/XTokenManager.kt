package com.xabber.android.data.extension.xtoken

import android.os.Build
import com.xabber.android.data.Application
import com.xabber.android.data.NetworkException
import com.xabber.android.data.account.AccountErrorEvent
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.OnAuthenticatedListener
import com.xabber.android.data.connection.OnPacketListener
import com.xabber.android.data.database.repositories.AccountRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.log.LogManager
import com.xabber.android.ui.OnXTokenSessionsUpdatedListener
import com.xabber.android.ui.notifySamUiListeners
import com.xabber.xmpp.smack.XMPPTCPConnection
import com.xabber.xmpp.xtoken.*
import com.xabber.xmpp.xtoken.XTokenRevokeExtensionElement.Companion.getXTokenRevokeExtensionElement
import com.xabber.xmpp.xtoken.XTokenRevokeExtensionElement.Companion.hasXTokenRevokeExtensionElement
import org.greenrobot.eventbus.EventBus
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import java.lang.ref.WeakReference

object XTokenManager : OnPacketListener, OnAuthenticatedListener {

    const val NAMESPACE = "https://xabber.com/protocol/auth-tokens"

    override fun onStanza(connection: ConnectionItem?, packet: Stanza?) {
        if (packet is IncomingNewXTokenIQ) {
            AccountManager.updateXToken(connection?.account, packet.getXToken())
        } else if (packet is Message && packet.hasExtension(NAMESPACE)) {
            notifySamUiListeners(OnXTokenSessionsUpdatedListener::class.java)
            if (packet.hasXTokenRevokeExtensionElement()) {
                connection?.account?.let {
                    val myXtoken = AccountManager.getAccount(it)?.connectionSettings?.xToken?.uid
                    if (packet.getXTokenRevokeExtensionElement().uids.contains(myXtoken)) {
                        onAccountXTokenRevoked(it)
                    }
                }
            }
        }
    }

    override fun onAuthenticated(connectionItem: ConnectionItem) {
        val account = AccountManager.getAccount(connectionItem.account)
        account?.connectionSettings?.xToken?.counter = (account?.connectionSettings?.xToken?.counter ?: -1) + 1
        AccountRepository.saveAccountToRealm(account)
    }

    fun onAccountXTokenRevoked(accountJid: AccountJid) {
        LogManager.e(this, "${this::class.java.simpleName}.onAccountXTokenRevoked($accountJid)")
        AccountManager.removeAccount(accountJid)
    }

    fun onAccountXTokenCounterOutOfSync(accountJid: AccountJid) {
        LogManager.e(this, "${this::class.java.simpleName}.onAccountXTokenCounterOutOfSync($accountJid)")
        val accountItem = AccountManager.getAccount(accountJid)
        accountItem?.connectionSettings?.xToken = null
        accountItem?.recreateConnection()
        AccountManager.setEnabled(accountJid, false)
        AccountRepository.saveAccountToRealm(accountItem)
        val accountErrorEvent = AccountErrorEvent(
            accountJid,
            AccountErrorEvent.Type.PASS_REQUIRED,
            ""
        )
        EventBus.getDefault().postSticky(accountErrorEvent)
        //todo possible remove xtoken
    }

    fun sendXTokenRequest(connection: XMPPTCPConnection) {
        try {
            connection.sendStanza(
                XTokenRequestIQ(
                    server = connection.xmppServiceDomain,
                    client = Application.getInstance().versionName,
                    device = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
                )
            )
        } catch (e: Exception) {
            LogManager.d(this, "Error on request x-token: $e")
        }
    }

    fun sendChangeXTokenDescriptionRequest(
        connection: XMPPTCPConnection, tokenID: String, description: String
    ) {
        try {
            connection.sendStanza(
                ChangeXTokenDescriptionIQ(connection.xmppServiceDomain, tokenID, description)
            )
        } catch (e: NetworkException) {
            LogManager.exception(javaClass.simpleName, e)
        }
    }

    fun sendRevokeXTokenRequest(connection: XMPPTCPConnection, tokenID: String) {
        sendRevokeXTokenRequest(connection, mutableListOf(tokenID))
    }

    fun sendRevokeXTokenRequest(connection: XMPPTCPConnection, tokenIDs: List<String>) {
        try {
            connection.sendStanza(XTokenRevokeIQ(connection.xmppServiceDomain, tokenIDs))
        } catch (e: Exception) {
            LogManager.exception(javaClass.simpleName, e)
        }
    }

    fun sendRevokeAllRequest(connection: XMPPTCPConnection) {
        try {
            connection.sendStanza(RevokeAllXTokenRequestIQ(connection.xmppServiceDomain))
        } catch (e: Exception) {
            LogManager.exception(javaClass.simpleName, e)
        }
    }

    fun requestSessions(
        currentTokenUID: String, connection: XMPPTCPConnection, listener: SessionsListener
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val wrListener = WeakReference(listener)
            try {
                connection.sendIqWithResponseCallback(
                    RequestSessionsIQ(connection.xmppServiceDomain),
                    {
                        (it as? ResultSessionsIQ)?.getMainAndOtherSessions(currentTokenUID)?.let {
                            Application.getInstance().runOnUiThread {
                                wrListener.get()?.onResult(
                                    it.first, it.second.toMutableList()
                                )
                            }
                        }
                    },
                    {
                        wrListener.get()?.onError()
                    }
                )
            } catch (e: Exception) {
                wrListener.get()?.onError()
                LogManager.exception(this, e)
            }
        }
    }

    interface SessionsListener {
        fun onResult(currentSession: SessionVO?, sessions: MutableList<SessionVO>)
        fun onError()
    }

}