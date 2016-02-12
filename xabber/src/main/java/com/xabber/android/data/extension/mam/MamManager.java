package com.xabber.android.data.extension.mam;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.database.realm.MessageItem;
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

import de.greenrobot.event.EventBus;
import io.realm.Realm;

public class MamManager {
    private final static MamManager instance;
    public static final int SYNC_INTERVAL_MINUTES = 5;

    public static int PAGE_SIZE = AbstractChat.PRELOADED_MESSAGES;

    static {
        instance = new MamManager();
        Application.getInstance().addManager(instance);
    }

    public static MamManager getInstance() {
        return instance;
    }

    public void requestLastHistory(final AbstractChat chat) {
        if (chat == null) {
            return;
        }

        final SyncInfo syncInfo = chat.getSyncInfo();
        final SyncCache syncCache = chat.getSyncCache();

        if (syncCache.getLastSyncedTime() != null && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - syncCache.getLastSyncedTime().getTime()) < SYNC_INTERVAL_MINUTES) {
            return;
        }

        final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
        if (accountItem == null || !accountItem.getFactualStatusMode().isOnline()) {
            return;
        }
        ConnectionThread connectionThread = accountItem.getConnectionThread();

        if (connectionThread == null) {
            return;
        }

        final String lastMessageMamId = syncInfo.getLastMessageMamId();

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

                final org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult;
                try {

                    EventBus.getDefault().post(new LastHistoryLoadStartedEvent(chat));
                    if (lastMessageMamId == null) {
                        mamQueryResult = mamManager.queryPage(chat.getUser(), PAGE_SIZE, null, "");
                    } else {
                        mamQueryResult = mamManager.queryPage(chat.getUser(), PAGE_SIZE, lastMessageMamId, null);
                    }


                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    EventBus.getDefault().post(new LastHistoryLoadFinishedEvent(chat));
                }

                LogManager.i("MAM", "queryArchive finished. fin count expected: " + mamQueryResult.mamFin.getRsmSet().getCount() + " real: " + mamQueryResult.messages.size());

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chat.onMessageDownloaded(getMessageItems(mamQueryResult, chat));
                        syncCache.setLastSyncedTime(new Date(System.currentTimeMillis()));
                        updateSyncInfo(mamQueryResult, syncInfo);
                    }
                });
            }
        };
        thread.start();
    }

    private void updateSyncInfo(org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult, SyncInfo syncInfo) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        if (mamQueryResult.messages.size() < PAGE_SIZE) {
            syncInfo.setRemoteHistoryCompletelyLoaded(true);
        }


        if (mamQueryResult.mamFin.getRsmSet() != null) {

            if (syncInfo.getFirstMamMessageMamId() == null) {
                syncInfo.setFirstMamMessageMamId(mamQueryResult.mamFin.getRsmSet().getFirst());
                if (!mamQueryResult.messages.isEmpty()) {
                    syncInfo.setFirstMamMessageStanzaId(mamQueryResult.mamFin.getRsmSet().getFirst());
                }
            }
            if (mamQueryResult.mamFin.getRsmSet().getLast() != null) {
                syncInfo.setLastMessageMamId(mamQueryResult.mamFin.getRsmSet().getLast());
            }

        }

        realm.commitTransaction();
    }

    private void updateSyncInfo(org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult, SyncInfo syncInfo, List<MessageItem> messageItems) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        if (mamQueryResult.messages.size() < PAGE_SIZE) {
            syncInfo.setRemoteHistoryCompletelyLoaded(true);
        }

        syncInfo.setFirstMamMessageMamId(mamQueryResult.mamFin.getRsmSet().getFirst());
        if (messageItems != null && !messageItems.isEmpty()) {
            syncInfo.setFirstMamMessageStanzaId(messageItems.get(0).getStanzaId());
        }
        realm.commitTransaction();
    }



    public void requestPreviousHistory(final AbstractChat chat) {
        if (chat == null) {
            LogManager.i(this, "requestPreviousHistory chat == null");
            return;
        }

        final SyncInfo syncInfo = chat.getSyncInfo();
        final String firstMamMessageMamId = syncInfo.getFirstMamMessageMamId();

        LogManager.i(this, "requestPreviousHistory firstMamMessageMamId: " + firstMamMessageMamId
                + " syncInfo.isRemoteHistoryCompletelyLoaded() " + syncInfo.isRemoteHistoryCompletelyLoaded());

        if (firstMamMessageMamId == null || syncInfo.isRemoteHistoryCompletelyLoaded()) {
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

                final org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult;
                try {
                    EventBus.getDefault().post(new PreviousHistoryLoadStartedEvent(chat));
                    LogManager.i("MAM", "Loading previous history");
                    mamQueryResult = mamManager.queryPage(chat.getUser(), PAGE_SIZE, null, firstMamMessageMamId);
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    EventBus.getDefault().post(new PreviousHistoryLoadFinishedEvent(chat));
                    return;
                }

                EventBus.getDefault().post(new PreviousHistoryLoadFinishedEvent(chat));

                LogManager.i("MAM", "queryArchive finished. fin count expected: " + mamQueryResult.mamFin.getRsmSet().getCount() + " real: " + mamQueryResult.messages.size());

                final List<MessageItem> messageItems = getMessageItems(mamQueryResult, chat);

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chat.onMessageDownloaded(messageItems);
                        updateSyncInfo(mamQueryResult, syncInfo, messageItems);
                    }
                });

            }
        };
        thread.start();
    }

    private List<MessageItem> getMessageItems(org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult, AbstractChat chat) {
        List<MessageItem> messageItems = new ArrayList<>();

        for (Forwarded forwarded : mamQueryResult.messages) {
            if (!(forwarded.getForwardedPacket() instanceof Message)) {
                continue;
            }

            Message message = (Message) forwarded.getForwardedPacket();

            DelayInformation delayInformation = forwarded.getDelayInformation();

            DelayInformation messageDelay = DelayInformation.from(message);

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

            MessageItem messageItem = new MessageItem();

            messageItem.setAccount(chat.getAccount());
            messageItem.setUser(chat.getUser());
            messageItem.setResource(Jid.getResource(chat.getUser()));
            messageItem.setText(body);
            messageItem.setTimestamp(delayInformation.getStamp().getTime());
            if (messageDelay != null) {
                messageItem.setDelayTimestamp(messageDelay.getStamp().getTime());
            }
            messageItem.setIncoming(incoming);
            messageItem.setStanzaId(message.getStanzaId());
            messageItem.setReceivedFromMessageArchive(true);
            messageItem.setRead(true);
            messageItem.setSent(true);

            FileManager.processFileMessage(messageItem, false);

            messageItems.add(messageItem);
        }
        return messageItems;
    }
}
