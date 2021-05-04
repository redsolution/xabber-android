package com.xabber.android.data.extension.carbons

import com.xabber.android.data.Application
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.chat_markers.ChatMarkerManager
import com.xabber.android.data.extension.chat_state.ChatStateManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageManager
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.carbons.CarbonCopyReceivedListener
import org.jivesoftware.smackx.carbons.packet.CarbonExtension

internal class CarbonCopyListener(var account: AccountJid,
) : CarbonCopyReceivedListener {

    override fun onCarbonCopyReceived(direction: CarbonExtension.Direction,
                                      carbonCopy: Message,
                                      wrappingMessage: Message
    ) {
        Application.getInstance().runOnUiThread {
            try {
                MessageManager.getInstance().processCarbonsMessage(account, carbonCopy, direction)
            } catch (e: Exception) {
                LogManager.exception(LOG_TAG, e)
            }
            ChatMarkerManager.getInstance().processCarbonsMessage(account, carbonCopy, direction)
            ChatStateManager.getInstance().processCarbonsMessage(account, carbonCopy, direction)
        }
    }

    companion object {
        private val LOG_TAG = CarbonCopyListener::class.java.simpleName
    }

}