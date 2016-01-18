package com.xabber.android.data.extension.mam;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageItem;
import com.xabber.xmpp.address.Jid;

import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.io.messages.PlainTextMessage;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MamManager {
    private final static MamManager instance;

    static {
        instance = new MamManager();
        Application.getInstance().addManager(instance);
    }

    public static MamManager getInstance() {
        return instance;
    }

    public void request(final AbstractChat chat) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
        ConnectionThread connectionThread = accountItem.getConnectionThread();

        if (!accountItem.getFactualStatusMode().isOnline() || connectionThread == null) {
            return;
        }

        final Thread thread = new Thread("Request MAM chat " + chat) {
            @Override
            public void run() {
                org.jivesoftware.smackx.mam.MamManager mamManager = org.jivesoftware.smackx.mam.MamManager.getInstanceFor(accountItem.getConnectionThread().getXMPPConnection());
                try {
                    if (!mamManager.isSupportedByServer()) {
                        return;
                    }

                } catch (SmackException.NoResponseException | InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return;
                }

                org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult;
                try {
                    mamQueryResult = mamManager.queryArchiveLast(20, chat.getUser());
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return;
                }


                LogManager.i("MAM", "queryArchive finished. fin count expected: " + mamQueryResult.mamFin.getRsmSet().getCount() + " real: " + mamQueryResult.messages.size());

                List<MessageItem> messageItems = new ArrayList<>();

                for (Forwarded forwarded : mamQueryResult.messages) {
                    if (!(forwarded.getForwardedPacket() instanceof Message)) {
                        continue;
                    }

                    Message message = (Message) forwarded.getForwardedPacket();

                    DelayInformation delayInformation = forwarded.getDelayInformation();

                    String body = message.getBody();
                    net.java.otr4j.io.messages.AbstractMessage otrMessage;
                    try {
                        otrMessage = SerializationUtils.toMessage(body);
                    } catch (IOException e) {
                        return;
                    }
                    if (otrMessage != null) {
                        if (otrMessage.messageType != net.java.otr4j.io.messages.AbstractMessage.MESSAGE_PLAINTEXT)
                            return;
                        body = ((PlainTextMessage) otrMessage).cleanText;
                    }

                    boolean incoming = Jid.getBareAddress(message.getFrom()).equalsIgnoreCase(Jid.getBareAddress(chat.getUser()));

                    MessageItem messageItem = new MessageItem(chat,
                            null,
                            Jid.getResource(chat.getUser()), body, null,
                            delayInformation.getStamp(), null, incoming, true,
                            true, false, true, false, false);
                    messageItem.setPacketID(message.getStanzaId());

                    messageItems.add(messageItem);
                }

                chat.onMessageDownloaded(messageItems);

            }
        };
        thread.start();
    }
}
