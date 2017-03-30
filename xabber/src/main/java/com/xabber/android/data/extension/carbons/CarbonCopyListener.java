package com.xabber.android.data.extension.carbons;


import com.xabber.android.data.Application;
import com.xabber.android.data.entity.AccountJid;
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
                                     final Message carbonCopy, Message wrappingMessage) {
        LogManager.i(LOG_TAG, "onCarbonCopyReceived " + direction + " " + carbonCopy.getBody());
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MessageManager.getInstance().processCarbonsMessage(account, carbonCopy, direction);
            }
        });
    }
}
