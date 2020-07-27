package com.xabber.android.data.extension.carbons;


import com.xabber.android.data.Application;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.chat_markers.ChatMarkerManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.carbons.CarbonCopyReceivedListener;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;

class CarbonCopyListener implements CarbonCopyReceivedListener {

    private static final String LOG_TAG = CarbonCopyListener.class.getSimpleName();

    @SuppressWarnings("WeakerAccess")
    AccountJid account;

    CarbonCopyListener(AccountJid account) {
        this.account = account;
    }

    @Override
    public void onCarbonCopyReceived(final CarbonExtension.Direction direction,
                                     final Message carbonCopy, final Message wrappingMessage) {
        LogManager.d(LOG_TAG, "onCarbonCopyReceive reached, just before runOnUiThread() call. CarbonExtension.Direction: "
                + direction.toString() + " Message: " + carbonCopy.toString() + " Message: "
                + wrappingMessage);
        Application.getInstance().runOnUiThread(() -> {
            try{
                MessageManager.getInstance().processCarbonsMessage(account, carbonCopy, direction);
            } catch (Exception e){ LogManager.exception(LOG_TAG, e); }

            ChatMarkerManager.getInstance().processCarbonsMessage(account, carbonCopy, direction);
            ChatStateManager.getInstance().processCarbonsMessage(account, carbonCopy, direction);
            LogManager.d(LOG_TAG, "invoked onCarbonCopyReceived on CarbonExtension.Direction: "
                    + direction.toString() + " Message: " + carbonCopy.toString() + " Message: "
                    + wrappingMessage);
        });
    }
}
