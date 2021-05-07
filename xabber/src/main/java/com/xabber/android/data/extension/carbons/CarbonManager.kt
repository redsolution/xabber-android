package com.xabber.android.data.extension.carbons

import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.SettingsManager.SecurityOtrMode
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.listeners.OnAuthenticatedListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.otr.SecurityLevel
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.AbstractChat
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.carbons.packet.CarbonExtension
import org.jivesoftware.smackx.hints.element.NoStoreHint
import java.util.concurrent.ConcurrentHashMap

/**
 * Packet extension for XEP-0280: Message Carbons. This class implements
 * the manager for registering Carbon support, enabling and disabling
 * message carbons.
 *
 *
 * You should call enableCarbons() before sending your first undirected
 * presence.
 *
 * @author Georg Lukas, Semyon Baranov
 */
object CarbonManager : OnAuthenticatedListener {

    private val LOG_TAG = CarbonManager::class.java.simpleName

    private var carbonCopyListeners: MutableMap<AccountJid, CarbonCopyListener> = ConcurrentHashMap()

    override fun onAuthenticated(connectionItem: ConnectionItem) = updateIsSupported(connectionItem)

    private fun updateIsSupported(connectionItem: ConnectionItem) {
        val carbonManager = org.jivesoftware.smackx.carbons.CarbonManager.getInstanceFor(connectionItem.connection)
        try {
            if (connectionItem.connection.user != null && carbonManager.isSupportedByServer) {
                LogManager.d(
                    LOG_TAG,
                    "Smack reports that carbons are " + if (carbonManager.carbonsEnabled) "enabled" else "disabled"
                )
                if (carbonManager.carbonsEnabled) {
                    // Sometimes Smack's CarbonManager still thinks that carbons are enabled during a
                    // period of time between disconnecting on error and completing a new authorization.
                    // Since our onAuthorized listener could be called earlier than the listener in Smack's CarbonManager,
                    // it can introduce an incorrect behavior of .getCarbonsEnabled().
                    // To avoid it we can use .enableCarbonsAsync(), and its' counterpart, to skip the Carbons's
                    // state check and "forcefully" send the correct carbons state IQ
                    changeCarbonsStateAsync(
                        carbonManager,
                        connectionItem.account,
                        SettingsManager.connectionUseCarbons()
                    )
                } else {
                    changeCarbonsStateAsync(
                        carbonManager,
                        connectionItem.account,
                        SettingsManager.connectionUseCarbons()
                    )
                }
            }
        } catch (e: NoResponseException) {
            LogManager.exception(LOG_TAG, e)
            if (SettingsManager.connectionUseCarbons()) {
                addListener(carbonManager, connectionItem.account)
            } else removeListener(carbonManager, connectionItem.account)
        } catch (e: Exception) {
            LogManager.exception(LOG_TAG, e)
        }
    }

    // Async method sends carbons IQ without checking the current state, which is useful when the order of
    // authorized listeners becomes messed up and Smack's Carbons state flag doesn't reflect the real state since it didn't update yet.
    @Throws(InterruptedException::class)
    private fun changeCarbonsStateAsync(
        carbonManager: org.jivesoftware.smackx.carbons.CarbonManager,
        account: AccountJid,
        enable: Boolean,
    ) {
        if (enable) {
            carbonManager.enableCarbonsAsync(null)
            addListener(carbonManager, account)
            LogManager.d(LOG_TAG, "Forcefully sent <enable> carbons")
        } else {
            carbonManager.disableCarbonsAsync(null)
            removeListener(carbonManager, account)
            LogManager.d(LOG_TAG, "Forcefully sent <disable> carbons")
        }
    }

    // we need to remove old listener not to cause memory leak
    private fun addListener(
        carbonManager: org.jivesoftware.smackx.carbons.CarbonManager,
        account: AccountJid,
    ) {
        removeListener(carbonManager, account)
        val carbonCopyListener = CarbonCopyListener(account)
        carbonCopyListeners[account] = carbonCopyListener
        carbonManager.addCarbonCopyReceivedListener(carbonCopyListener)
    }

    private fun removeListener(
        carbonManager: org.jivesoftware.smackx.carbons.CarbonManager,
        account: AccountJid,
    ) {
        val carbonCopyListener = carbonCopyListeners.remove(account)
        if (carbonCopyListener != null) carbonManager.removeCarbonCopyReceivedListener(carbonCopyListener)
    }

    fun isCarbonsEnabledForConnection(connection: ConnectionItem): Boolean {
        return org.jivesoftware.smackx.carbons.CarbonManager
            .getInstanceFor(connection.connection)
            .carbonsEnabled
    }

    /**
     * Sends the new state of message carbons to the server
     * when this setting has been changed
     */
    fun onUseCarbonsSettingsChanged() {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            AccountManager.getInstance().enabledAccounts.forEach { account ->
                if (AccountManager.getInstance().getAccount(account) != null) {
                    updateIsSupported(AccountManager.getInstance().getAccount(account)!!)
                }
            }
        }
    }

    /**
     * Update outgoing message before sending.
     * Marks the message as non-carbon-copied in the following cases:
     * - Message Carbons is enabled and OTR mode is enabled.
     * - Message Carbons is enabled and OTR security level != plain.
     *
     * @param message      the <tt>Message</tt> to be sent
     */
    fun updateOutgoingMessage(
        abstractChat: AbstractChat,
        message: Message?,
    ) {
        if (!SettingsManager.connectionUseCarbons()) return

        if (SettingsManager.securityOtrMode() == SecurityOtrMode.disabled) return

        val securityLevel = OTRManager.getInstance().getSecurityLevel(abstractChat.account, abstractChat.contactJid)

        if (securityLevel == SecurityLevel.plain || securityLevel == SecurityLevel.finished) return

        CarbonExtension.Private.addTo(message)
    }

    /**
     * Marks the message as non-carbon-copied
     * Should used for establishing OTR-session
     * @param message      the <tt>Message</tt> to be sent
     */
    fun setMessageToIgnoreCarbons(message: Message?) {
        CarbonExtension.Private.addTo(message)
        NoStoreHint.set(message)
    }

}