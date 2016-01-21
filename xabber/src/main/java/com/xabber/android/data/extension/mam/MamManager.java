package com.xabber.android.data.extension.mam;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageItem;
import com.xabber.xmpp.address.Jid;

import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.io.messages.PlainTextMessage;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MamManager {
    private final static MamManager instance;
    public static final int SYNC_INTERVAL_MINUTES = 5;

    public static int PAGE_SIZE = AbstractChat.PRELOADED_MESSAGES;

    private NestedMap<SyncInfo> syncInfoByChat;

    private static class SyncInfo {
        private Date dateLastSynced;
        private String firstMessageId;
        private String lastMessageId;

        public Date getDateLastSynced() {
            return dateLastSynced;
        }

        public void setDateLastSynced(Date dateLastSynced) {
            this.dateLastSynced = dateLastSynced;
        }

        public String getFirstMessageId() {
            return firstMessageId;
        }

        public void setFirstMessageId(String firstMessageId) {
            this.firstMessageId = firstMessageId;
        }

        public String getLastMessageId() {
            return lastMessageId;
        }

        public void setLastMessageId(String lastMessageId) {
            this.lastMessageId = lastMessageId;
        }
    }

    static {
        instance = new MamManager();
        Application.getInstance().addManager(instance);
    }

    public static MamManager getInstance() {
        return instance;
    }

    public MamManager() {
        this.syncInfoByChat = new NestedMap<>();
    }

    public void requestAll(final AbstractChat chat) {
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

                org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResultFirst;

                int counter = 0;

                try {
                    mamQueryResultFirst = mamManager.queryArchiveLast(PAGE_SIZE, chat.getUser());
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
                    e.printStackTrace();
                    return;
                }

                for (Forwarded forwarded : mamQueryResultFirst.messages) {
                    LogManager.i("MAM", ((Message)forwarded.getForwardedPacket()).getBody() + " time " + forwarded.getDelayInformation().getStamp().getTime());
                }

                counter += mamQueryResultFirst.messages.size();

                chat.onMessageDownloaded(getMessageItems(mamQueryResultFirst, chat));


                while (counter < mamQueryResultFirst.mamFin.getRsmSet().getCount()) {
                    try {
                        mamQueryResultFirst = mamManager.pageBefore(mamQueryResultFirst, PAGE_SIZE);
                    } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }

                    for (Forwarded forwarded : mamQueryResultFirst.messages) {
                        LogManager.i("MAM", ((Message)forwarded.getForwardedPacket()).getBody());
                    }

                    counter += mamQueryResultFirst.messages.size();
                    chat.onMessageDownloaded(getMessageItems(mamQueryResultFirst, chat));

                    LogManager.i("MAM", "Got " + counter + " / " + mamQueryResultFirst.mamFin.getRsmSet().getCount());
                }
            }
        };
        thread.start();
    }

    public void requestLastHistory(final AbstractChat chat) {
        final SyncInfo syncInfo = getSyncInfo(chat);

        if (syncInfo.getDateLastSynced() != null && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - syncInfo.getDateLastSynced().getTime()) < SYNC_INTERVAL_MINUTES) {
            return;
        }

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

                    if (syncInfo.getLastMessageId() == null) {
                        mamQueryResult = mamManager.queryPage(chat.getUser(), PAGE_SIZE, null, "");
                    } else {
                        mamQueryResult = mamManager.queryPage(chat.getUser(), PAGE_SIZE, syncInfo.getLastMessageId(), null);
                    }


                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return;
                }

                LogManager.i("MAM", "queryArchive finished. fin count expected: " + mamQueryResult.mamFin.getRsmSet().getCount() + " real: " + mamQueryResult.messages.size());

                List<MessageItem> messageItems = getMessageItems(mamQueryResult, chat);

                chat.onMessageDownloaded(messageItems);

                syncInfo.setDateLastSynced(new Date(System.currentTimeMillis()));
                if (mamQueryResult.mamFin.getRsmSet() != null) {
                    if (syncInfo.getFirstMessageId() == null) {
                        syncInfo.setFirstMessageId(mamQueryResult.mamFin.getRsmSet().getFirst());
                    }
                    if (mamQueryResult.mamFin.getRsmSet().getLast() != null) {
                        syncInfo.setLastMessageId(mamQueryResult.mamFin.getRsmSet().getLast());
                    }
                }
            }
        };
        thread.start();
    }

    @NonNull
    private SyncInfo getSyncInfo(AbstractChat chat) {
        SyncInfo syncInfo = syncInfoByChat.get(chat.getAccount(), chat.getUser());
        if (syncInfo == null) {
            syncInfo = new SyncInfo();
            syncInfoByChat.put(chat.getAccount(), chat.getUser(), syncInfo);
        }
        return syncInfo;
    }

    @Nullable
    private List<MessageItem> getMessageItems(org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult, AbstractChat chat) {
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
                return null;
            }
            if (otrMessage != null) {
                if (otrMessage.messageType != net.java.otr4j.io.messages.AbstractMessage.MESSAGE_PLAINTEXT)
                    return null;
                body = ((PlainTextMessage) otrMessage).cleanText;
            }

            boolean incoming = Jid.getBareAddress(message.getFrom()).equalsIgnoreCase(Jid.getBareAddress(chat.getUser()));

            MessageItem messageItem = new MessageItem(chat, null,
                    Jid.getResource(chat.getUser()), body, null,
                    delayInformation.getStamp(), null, incoming, true,
                    true, false, true, false, false);
            messageItem.setStanzaId(message.getStanzaId());

            messageItems.add(messageItem);
        }
        return messageItems;
    }
}
