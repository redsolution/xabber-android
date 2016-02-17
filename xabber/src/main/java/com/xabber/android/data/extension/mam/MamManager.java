package com.xabber.android.data.extension.mam;

import android.support.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.xmpp.address.Jid;

import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.io.messages.PlainTextMessage;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmResults;

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

        final SyncCache syncCache = chat.getSyncCache();

        if (syncCache.getLastSyncedTime() != null
                && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - syncCache.getLastSyncedTime().getTime()) < SYNC_INTERVAL_MINUTES) {
            return;
        }

        final String account = chat.getAccount();
        final String user = chat.getUser();
        final AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null || !accountItem.getFactualStatusMode().isOnline()) {
            return;
        }
        ConnectionThread connectionThread = accountItem.getConnectionThread();

        if (connectionThread == null) {
            return;
        }

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                org.jivesoftware.smackx.mam.MamManager mamManager
                        = org.jivesoftware.smackx.mam.MamManager
                        .getInstanceFor(accountItem.getConnectionThread().getXMPPConnection());
                try {
                    if (!mamManager.isSupportedByServer()) {
                        return;
                    }
                } catch (SmackException.NoResponseException | InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return;
                }

                String lastMessageMamId;
                {
                    Realm realm = Realm.getDefaultInstance();
                    lastMessageMamId = getSyncInfo(realm, account, user).getLastMessageMamId();
                    realm.close();
                }

                final org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult;
                try {
                    EventBus.getDefault().post(new LastHistoryLoadStartedEvent(chat));
                    if (lastMessageMamId == null) {
                        mamQueryResult = mamManager.queryPage(user, PAGE_SIZE, null, "");
                    } else {
                        mamQueryResult = mamManager.queryPage(user, PAGE_SIZE, lastMessageMamId, null);
                    }
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    EventBus.getDefault().post(new LastHistoryLoadFinishedEvent(chat));
                }

                LogManager.i("MAM", "queryArchive finished. fin count expected: " + mamQueryResult.mamFin.getRsmSet().getCount() + " real: " + mamQueryResult.messages.size());



                syncCache.setLastSyncedTime(new Date(System.currentTimeMillis()));

                Realm realm = Realm.getDefaultInstance();
                updateLastHistorySyncInfo(realm, chat, mamQueryResult);
                syncMessages(realm, chat, getMessageItems(mamQueryResult, chat));
                realm.close();
            }
        });
    }

    public void syncMessages(Realm realm, AbstractChat chat, final Collection<MessageItem> messagesFromServer) {

        if (messagesFromServer == null || messagesFromServer.isEmpty()) {
            return;
        }

        LogManager.i(this, "syncMessages: " + messagesFromServer.size());

        RealmResults<MessageItem> localMessages = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount())
                .equalTo(MessageItem.Fields.USER, chat.getUser())
                .findAll();

        Iterator<MessageItem> iterator = messagesFromServer.iterator();
        while (iterator.hasNext()) {
            MessageItem remoteMessage = iterator.next();

            if (localMessages.where()
                    .equalTo(MessageItem.Fields.STANZA_ID, remoteMessage.getStanzaId())
                    .count() > 0) {
                LogManager.i(this, "Sync. Found messages with same Stanza ID. removing. Remote message:"
                        + " Text: " + remoteMessage.getText()
                        + " Timestamp: " + remoteMessage.getTimestamp()
                        + " Delay Timestamp: " + remoteMessage.getDelayTimestamp()
                        + " StanzaId: " + remoteMessage.getStanzaId());
                iterator.remove();
                continue;
            }

            if (remoteMessage.getText() == null || remoteMessage.getTimestamp() == null) {
                continue;
            }

            Long remoteMessageDelayTimestamp = remoteMessage.getDelayTimestamp();
            Long remoteMessageTimestamp = remoteMessage.getTimestamp();

            RealmResults<MessageItem> sameTextMessages = localMessages.where()
                    .equalTo(MessageItem.Fields.TEXT, remoteMessage.getText()).findAll();

            if (isTimeStampSimilar(sameTextMessages, remoteMessageTimestamp)) {
                LogManager.i(this, "Sync. Found messages with similar remote timestamp. Removing. Remote message:"
                        + " Text: " + remoteMessage.getText()
                        + " Timestamp: " + remoteMessage.getTimestamp()
                        + " Delay Timestamp: " + remoteMessage.getDelayTimestamp()
                        + " StanzaId: " + remoteMessage.getStanzaId());
                iterator.remove();
                continue;
            }

            if (remoteMessageDelayTimestamp != null
                    && isTimeStampSimilar(sameTextMessages, remoteMessageDelayTimestamp)) {
                LogManager.i(this, "Sync. Found messages with similar remote delay timestamp. Removing. Remote message:"
                        + " Text: " + remoteMessage.getText()
                        + " Timestamp: " + remoteMessage.getTimestamp()
                        + " Delay Timestamp: " + remoteMessage.getDelayTimestamp()
                        + " StanzaId: " + remoteMessage.getStanzaId());
                iterator.remove();
                continue;
            }
        }

        realm.beginTransaction();
        realm.copyToRealm(messagesFromServer);
        realm.commitTransaction();
    }

    private static boolean isTimeStampSimilar(RealmResults<MessageItem> sameTextMessages, long remoteMessageTimestamp) {
        long start = remoteMessageTimestamp - (1000 * 5);
        long end = remoteMessageTimestamp + (1000 * 5);

        if (sameTextMessages.where()
                .between(MessageItem.Fields.TIMESTAMP, start, end)
                .count() > 0) {
            LogManager.i(MamManager.class.getSimpleName(), "Sync. Found messages with similar local timestamp");
            return true;
        }

        if (sameTextMessages.where()
                .between(MessageItem.Fields.DELAY_TIMESTAMP, start, end)
                .count() > 0) {
            LogManager.i(MamManager.class.getSimpleName(), "Sync. Found messages with similar local delay timestamp.");
            return true;
        }
        return false;
    }

    @NonNull
    private SyncInfo getSyncInfo(Realm realm, String account, String user) {
        SyncInfo syncInfo = realm.where(SyncInfo.class)
                .equalTo(SyncInfo.FIELD_ACCOUNT, account)
                .equalTo(SyncInfo.FIELD_USER, user).findFirst();

        if (syncInfo == null) {
            realm.beginTransaction();
            syncInfo = realm.createObject(SyncInfo.class);
            syncInfo.setAccount(account);
            syncInfo.setUser(user);
            realm.commitTransaction();
        }
        return syncInfo;
    }

    private void updateLastHistorySyncInfo(Realm realm, BaseEntity chat, org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult) {
        SyncInfo syncInfo = getSyncInfo(realm, chat.getAccount(), chat.getUser());

        realm.beginTransaction();

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

    private void updatePreviousHistorySyncInfo(Realm realm, BaseEntity chat, org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult, List<MessageItem> messageItems) {
        SyncInfo syncInfo = getSyncInfo(realm, chat.getAccount(), chat.getUser());

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
        if (chat == null || chat.isRemotePreviousHistoryCompletelyLoaded()) {
            return;
        }

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
                ConnectionThread connectionThread = accountItem.getConnectionThread();

                if (!accountItem.getFactualStatusMode().isOnline() || connectionThread == null) {
                    return;
                }

                String firstMamMessageMamId;
                boolean remoteHistoryCompletelyLoaded;
                {
                    Realm realm = Realm.getDefaultInstance();
                    SyncInfo syncInfo = getSyncInfo(realm, chat.getAccount(), chat.getUser());
                    firstMamMessageMamId = syncInfo.getFirstMamMessageMamId();
                    remoteHistoryCompletelyLoaded = syncInfo.isRemoteHistoryCompletelyLoaded();
                    realm.close();
                }

                if (remoteHistoryCompletelyLoaded) {
                    chat.setRemotePreviousHistoryCompletelyLoaded(true);
                }

                if (firstMamMessageMamId == null || remoteHistoryCompletelyLoaded) {
                    return;
                }

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

                Realm realm = Realm.getDefaultInstance();
                List<MessageItem> messageItems = getMessageItems(mamQueryResult, chat);
                syncMessages(realm, chat, messageItems);
                updatePreviousHistorySyncInfo(realm, chat, mamQueryResult, messageItems);
                realm.close();
            }

        });

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
