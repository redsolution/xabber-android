package com.xabber.android.data.extension.xtoken

import android.os.Build
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.OnPacketListener
import com.xabber.android.data.log.LogManager
import com.xabber.android.ui.OnXTokenSessionsUpdatedListener
import com.xabber.android.ui.notifySamUiListeners
import com.xabber.xmpp.smack.XMPPTCPConnection
import com.xabber.xmpp.xtoken.*
import com.xabber.xmpp.xtoken.XTokenRevokeExtensionElement.Companion.hasXTokenRevokeExtensionElement
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import java.lang.ref.WeakReference

object XTokenManager : OnPacketListener {

    const val NAMESPACE = "https://xabber.com/protocol/auth-tokens"

    override fun onStanza(connection: ConnectionItem?, packet: Stanza?) {
        if (packet is IncomingNewXTokenIQ) {
            AccountManager.getInstance().updateXToken(
                connection?.account, packet.getXToken()
            )
        } else if (packet is Message && packet.hasExtension(NAMESPACE)) {
            notifySamUiListeners(OnXTokenSessionsUpdatedListener::class.java)
            if (packet.hasXTokenRevokeExtensionElement()) {
                LogManager.d(this, "Got revoke XTOKEN!")
            }
        }
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

    fun requestSessions(
        currentTokenUID: String, connection: XMPPTCPConnection, listener: SessionsListener
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            sendSessionsRequestIQ(
                currentTokenUID, connection, WeakReference(listener)
            )
        }
    }

    private fun sendSessionsRequestIQ(
        currentTokenUID: String,
        connection: XMPPTCPConnection,
        wrListener: WeakReference<SessionsListener>
    ) {
        val requestIQ = RequestSessionsIQ(connection.xmppServiceDomain)
        try {
            connection.sendIqWithResponseCallback(
                requestIQ,
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

    interface SessionsListener {
        fun onResult(currentSession: SessionVO?, sessions: MutableList<SessionVO>)
        fun onError()
    }

}