package com.xabber.android.data.extension.devices

import android.os.Build
import com.xabber.android.data.Application
import com.xabber.android.data.NetworkException
import com.xabber.android.data.account.AccountErrorEvent
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.OnPacketListener
import com.xabber.android.data.database.repositories.AccountRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.log.LogManager
import com.xabber.android.ui.OnDevicesSessionsUpdatedListener
import com.xabber.android.ui.notifySamUiListeners
import com.xabber.xmpp.smack.XMPPTCPConnection
import com.xabber.xmpp.devices.*
import com.xabber.xmpp.devices.DeviceRevokeExtensionElement.Companion.getDeviceRevokeExtensionElement
import com.xabber.xmpp.devices.DeviceRevokeExtensionElement.Companion.hasDeviceRevokeExtensionElement
import org.greenrobot.eventbus.EventBus
import org.jivesoftware.smack.ExceptionCallback
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza

object DevicesManager : OnPacketListener {

    const val NAMESPACE = "https://xabber.com/protocol/devices"

    override fun onStanza(connection: ConnectionItem?, packet: Stanza?) {
        if (packet is IncomingNewDeviceIQ) {
            AccountManager.updateDevice(connection?.account, packet.getDeviceElement())
        } else if (packet is Message && packet.hasExtension(NAMESPACE)) {
            notifySamUiListeners(OnDevicesSessionsUpdatedListener::class.java)
            if (packet.hasDeviceRevokeExtensionElement()) {
                connection?.account?.let {
                    val myDevice = AccountManager.getAccount(it)?.connectionSettings?.device?.uid
                    if (packet.getDeviceRevokeExtensionElement().ids.contains(myDevice)) {
                        onAccountDeviceRevoked(it)
                    }
                }
            }
        }
    }

    fun onLogin(connectionItem: ConnectionItem) {
        val account = AccountManager.getAccount(connectionItem.account)
        LogManager.d("XToken", "onAuthentikated; prev counter: ${connectionItem.connectionSettings.device.counter}")
        account?.connectionSettings?.device?.apply {
            counter++
        }?.also {
            LogManager.d("XToken", "onAuthentikated; new counter: ${connectionItem.connectionSettings.device.counter}")
            connectionItem.connectionSettings.device = it
            connectionItem.connectionSettings.password = it.getPasswordString()
        }
        AccountRepository.saveAccountToRealm(account)

    }

    fun onAccountDeviceRevoked(accountJid: AccountJid) {
        LogManager.e(this, "${this::class.java.simpleName}.onAccountXTokenRevoked($accountJid)")
        AccountManager.removeAccount(accountJid)
    }

    fun onSecretAndCounterOutOfSync(accountJid: AccountJid) {
        LogManager.e(this, "${this::class.java.simpleName}.onAccountXTokenCounterOutOfSync($accountJid)")
        AccountManager.getAccount(accountJid)?.apply {
            connectionSettings.device = null
            recreateConnection(false)
        }?.also {
            AccountManager.setEnabled(accountJid, false)
            AccountRepository.saveAccountToRealm(it)
        }

        val accountErrorEvent = AccountErrorEvent(
            accountJid,
            AccountErrorEvent.Type.PASS_REQUIRED,
            ""
        )
        EventBus.getDefault().postSticky(accountErrorEvent)
    }

    fun sendRegisterDeviceRequest(connection: XMPPTCPConnection) {
        try {
            connection.sendStanza(
                DeviceRegisterIQ.createRegisterDeviceRequest(
                    server = connection.xmppServiceDomain,
                    client = Application.getInstance().versionName,
                    info = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
                )
            )
        } catch (e: Exception) {
            LogManager.d(this, "Error on device register: $e")
        }
    }

    fun sendChangeDeviceDescriptionRequest(
        connection: XMPPTCPConnection,
        deviceId: String,
        description: String,
        listener: StanzaListener,
        exceptionCallback: ExceptionCallback,
    ) {
        try {
            connection.sendIqWithResponseCallback(
                ChangeDeviceDescriptionIQ(connection.xmppServiceDomain, deviceId, description),
                listener,
                exceptionCallback
            )
        } catch (e: NetworkException) {
            LogManager.exception(javaClass.simpleName, e)
        }
    }

    fun sendRevokeDeviceRequest(connection: XMPPTCPConnection, deviceId: String) {
        sendRevokeDeviceRequest(connection, mutableListOf(deviceId))
    }

    fun sendRevokeDeviceRequest(connection: XMPPTCPConnection, deviceIDs: List<String>) {
        try {
            connection.sendStanza(RevokeDeviceIq(connection.xmppServiceDomain, deviceIDs))
        } catch (e: Exception) {
            LogManager.exception(javaClass.simpleName, e)
        }
    }

    fun sendRevokeAllDevicesRequest(connection: XMPPTCPConnection) {
        try {
            connection.sendStanza(RevokeAllDevicesRequestIQ(connection.xmppServiceDomain))
        } catch (e: Exception) {
            LogManager.exception(javaClass.simpleName, e)
        }
    }

    fun requestSessions(
        currentDeviceUID: String, connection: XMPPTCPConnection, listener: SessionsListener
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                connection.sendIqWithResponseCallback(
                    RequestSessionsIQ(connection.xmppServiceDomain),
                    {
                        (it as? ResultSessionsIQ)?.getMainAndOtherSessions(currentDeviceUID)?.let {
                            Application.getInstance().runOnUiThread {
                                listener.onResult(it.first, it.second.toMutableList())
                            }
                        }
                    },
                    {
                        listener.onError()
                    }
                )
            } catch (e: Exception) {
                listener.onError()
                LogManager.exception(this, e)
            }
        }
    }

    interface SessionsListener {
        fun onResult(currentSession: SessionVO?, sessions: MutableList<SessionVO>)
        fun onError()
    }

}