package com.xabber.android.data.extension.carbons

import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.OnAuthenticatedListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.log.LogManager
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.carbons.packet.CarbonExtension
import org.jivesoftware.smackx.hints.element.NoStoreHint
import java.util.concurrent.ConcurrentHashMap
import org.jivesoftware.smackx.carbons.CarbonManager as SmackCarbonManager

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

    private var carbonCopyListeners: MutableMap<AccountJid, CarbonCopyListener> =
        ConcurrentHashMap()

    override fun onAuthenticated(connectionItem: ConnectionItem) {
        val carbonManager = SmackCarbonManager.getInstanceFor(connectionItem.connection)
        try {
            if (connectionItem.connection.user != null && carbonManager.isSupportedByServer) {
                LogManager.d(
                    this,
                    "Smack reports that carbons are " + if (carbonManager.carbonsEnabled) "enabled" else "disabled"
                )
                changeCarbonsStateAsync(carbonManager, connectionItem.account)
            }
        } catch (e: NoResponseException) {
            LogManager.exception(this, e)
            addListener(carbonManager, connectionItem.account)
        } catch (e: Exception) {
            LogManager.exception(this, e)
        }
    }

    // Async method sends carbons IQ without checking the current state, which is useful when the order of
    // authorized listeners becomes messed up and Smack's Carbons state flag doesn't reflect the real state since it didn't update yet.
    @Throws(InterruptedException::class)
    private fun changeCarbonsStateAsync(carbonManager: SmackCarbonManager, account: AccountJid) {
        carbonManager.enableCarbonsAsync(null)
        addListener(carbonManager, account)
        LogManager.d(this, "Forcefully sent <enable> carbons")
    }

    // we need to remove old listener not to cause memory leak
    private fun addListener(carbonManager: SmackCarbonManager, account: AccountJid) {
        removeListener(carbonManager, account)
        CarbonCopyListener(account).also {
            carbonCopyListeners[account] = it
            carbonManager.addCarbonCopyReceivedListener(it)
        }
    }

    private fun removeListener(carbonManager: SmackCarbonManager, account: AccountJid) {
        carbonCopyListeners.remove(account)?.let {
            carbonManager.removeCarbonCopyReceivedListener(it)
        }
    }

    fun isCarbonsEnabledForConnection(connection: ConnectionItem) =
        SmackCarbonManager.getInstanceFor(connection.connection).carbonsEnabled

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