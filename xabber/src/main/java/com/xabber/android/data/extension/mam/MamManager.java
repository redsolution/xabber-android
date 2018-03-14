package com.xabber.android.data.extension.mam;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.messagerealm.SyncInfo;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.OnRosterReceivedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;

import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.io.messages.PlainTextMessage;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmResults;

public class MamManager implements OnRosterReceivedListener {
    static final String LOG_TAG = MamManager.class.getSimpleName();
    private static MamManager instance;
    public static final int SYNC_INTERVAL_MINUTES = 5;

    public static int PAGE_SIZE = AbstractChat.PRELOADED_MESSAGES;

    private Map<AccountJid, Boolean> supportedByAccount;

    public static MamManager getInstance() {
        if (instance == null) {
            instance = new MamManager();
        }

        return instance;
    }

    public MamManager() {
        supportedByAccount = new ConcurrentHashMap<>();
    }

    public void onAuthorized(ConnectionItem connectionItem) {
        updateIsSupported((AccountItem) connectionItem);
    }


    @Override
    public void onRosterReceived(final AccountItem accountItem) {
        LogManager.i(this, "onRosterReceived " + accountItem.getAccount());
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (accountItem.getLoadHistorySettings() != LoadHistorySettings.all) {
                    return;
                }

                Collection<RosterContact> contacts = RosterManager.getInstance()
                        .getAccountRosterContacts(accountItem.getAccount());
                for (RosterContact contact : contacts) {
                    requestLastHistory(MessageManager.getInstance()
                            .getOrCreateChat(contact.getAccount(), contact.getUser()));
                }
            }
        });
    }

    @Nullable
    public Boolean isSupported(AccountJid accountJid) {
        return supportedByAccount.get(accountJid);
    }

    private boolean checkSupport(AccountItem accountItem) {
        Boolean isSupported = supportedByAccount.get(accountItem.getAccount());

        if (isSupported != null) {
            return isSupported;
        }

        return updateIsSupported(accountItem);
    }

    private boolean updateIsSupported(AccountItem accountItem) {
        org.jivesoftware.smackx.mam.MamManager mamManager = org.jivesoftware.smackx.mam.MamManager
                .getInstanceFor(accountItem.getConnection());

        boolean isSupported;
        try {
            isSupported = mamManager.isSupportedByServer();

            if (isSupported) {
                org.jivesoftware.smackx.mam.MamManager.MamPrefsResult archivingPreferences = mamManager.retrieveArchivingPreferences();
                LogManager.i(this, "archivingPreferences default behaviour " + archivingPreferences.mamPrefs.getDefault());
                org.jivesoftware.smackx.mam.MamManager.MamPrefsResult result
                        = mamManager.updateArchivingPreferences(null, null, accountItem.getMamDefaultBehaviour());
                LogManager.i(this, "updateArchivingPreferences result " + result.toString());
            }

        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | InterruptedException | SmackException.NotConnectedException | SmackException.NotLoggedInException e) {
            LogManager.exception(this, e);
            return false;
        }

        LogManager.i(this, "MAM support for account " + accountItem.getAccount() + " " + isSupported);
        supportedByAccount.put(accountItem.getAccount(), isSupported);

        AccountManager.getInstance().onAccountChanged(accountItem.getAccount());
        return isSupported;
    }

    public void requestUpdatePreferences(final AccountJid accountJid) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                if (accountItem == null) {
                    return;
                }
                org.jivesoftware.smackx.mam.MamManager mamManager = org.jivesoftware.smackx.mam.MamManager
                        .getInstanceFor(accountItem.getConnection());

                try {
                    org.jivesoftware.smackx.mam.MamManager.MamPrefsResult result
                            = mamManager.updateArchivingPreferences(null, null, accountItem.getMamDefaultBehaviour());
                    LogManager.i(LOG_TAG, "MAM default behavior updated to " + result.mamPrefs.getDefault());
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                        | InterruptedException | SmackException.NotConnectedException
                        | SmackException.NotLoggedInException e) {
                    LogManager.exception(LOG_TAG, e);
                }

            }
        });
    }

    public void requestLastHistoryByUser(final AbstractChat chat) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                getLastHistory(chat, false);
            }
        });
    }

    private void requestLastHistory(final AbstractChat chat) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                getLastHistory(chat, true);
            }
        });
    }

    private boolean isTimeToRefreshHistory(AbstractChat chat) {
        return chat.getLastSyncedTime() != null
                && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - chat.getLastSyncedTime().getTime())
                < SYNC_INTERVAL_MINUTES;
    }

    @SuppressWarnings("WeakerAccess")
    void getLastHistory(AbstractChat chat, boolean ignoreTime) {
        if (chat == null) {
            return;
        }

        if (!ignoreTime) {
            if (isTimeToRefreshHistory(chat)) {
                return;
            }
        }

        final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
        if (accountItem == null) {
            return;
        }

        XMPPTCPConnection connection = accountItem.getConnection();
        if (!connection.isAuthenticated()) {
            return;
        }

        if (!checkSupport(accountItem)) {
            return;
        }

        EventBus.getDefault().post(new LastHistoryLoadStartedEvent(chat));

        org.jivesoftware.smackx.mam.MamManager mamManager
                = org.jivesoftware.smackx.mam.MamManager.getInstanceFor(connection);

        String lastMessageMamId;
        int receivedMessagesCount;
        do {
            Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
            lastMessageMamId = getSyncInfo(realm, chat.getAccount(), chat.getUser()).getLastMessageMamId();
            realm.close();

            receivedMessagesCount = requestLastHistoryPage(mamManager, chat, lastMessageMamId);

            // if it was NOT the first time, and we got exactly one page,
            // it means that there should be more unloaded recent history
        } while (lastMessageMamId != null && receivedMessagesCount == PAGE_SIZE);

        // if it was first time receiving history, and we got less than a page
        // it mean that all previous history loaded
        if (lastMessageMamId == null
                && receivedMessagesCount >= 0 && receivedMessagesCount < PAGE_SIZE) {
            setRemoteHistoryCompletelyLoaded(chat);
        }

        EventBus.getDefault().post(new LastHistoryLoadFinishedEvent(chat));
    }

    public void setRemoteHistoryCompletelyLoaded(AbstractChat chat) {
        LogManager.i(this, "setRemoteHistoryCompletelyLoaded " + chat.getUser());

        Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
        SyncInfo syncInfo = getSyncInfo(realm, chat.getAccount(), chat.getUser());
        realm.beginTransaction();
        syncInfo.setRemoteHistoryCompletelyLoaded(true);
        realm.commitTransaction();
        realm.close();
    }

    private int requestLastHistoryPage(org.jivesoftware.smackx.mam.MamManager mamManager,
                                       AbstractChat chat, String lastMessageMamId) {
        final org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult;
        try {
            if (lastMessageMamId == null) {
                mamQueryResult = mamManager.pageBefore(chat.getUser().getJid(), "", PAGE_SIZE);
            } else {
                mamQueryResult = mamManager.pageAfter(chat.getUser().getJid(), lastMessageMamId, PAGE_SIZE);
            }
        } catch (SmackException.NotLoggedInException | InterruptedException
                | SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            LogManager.exception(this, e);
            return -1;
        }

        int receivedMessagesCount = mamQueryResult.forwardedMessages.size();

        LogManager.i(this, "receivedMessagesCount " + receivedMessagesCount);

        chat.setLastSyncedTime(new Date(System.currentTimeMillis()));

        Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
        updateLastHistorySyncInfo(realm, chat, mamQueryResult);
        syncMessages(realm, chat, getMessageItems(mamQueryResult, chat));
        realm.close();

        return receivedMessagesCount;
    }

    private void syncMessages(Realm realm, AbstractChat chat, final Collection<MessageItem> messagesFromServer) {

        if (messagesFromServer == null || messagesFromServer.isEmpty()) {
            return;
        }

        LogManager.i(this, "syncMessages: " + messagesFromServer.size());

        RealmResults<MessageItem> localMessages = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .findAll();

        Iterator<MessageItem> iterator = messagesFromServer.iterator();
        while (iterator.hasNext()) {
            MessageItem remoteMessage = iterator.next();

            // assume that Stanza ID could be not unique
            if (localMessages.where()
                    .equalTo(MessageItem.Fields.STANZA_ID, remoteMessage.getStanzaId())
                    .equalTo(MessageItem.Fields.TEXT, remoteMessage.getText())
                    .count() > 0) {
                LogManager.i(this, "Sync. Removing message with same Stanza ID and text. Remote message:"
                        + " Text: " + remoteMessage.getText()
                        + " Timestamp: " + remoteMessage.getTimestamp()
                        + " Delay Timestamp: " + remoteMessage.getDelayTimestamp()
                        + " StanzaId: " + remoteMessage.getStanzaId());
                iterator.remove();
                continue;
            }

            Long remoteMessageDelayTimestamp = remoteMessage.getDelayTimestamp();
            Long remoteMessageTimestamp = remoteMessage.getTimestamp();

            RealmResults<MessageItem> sameTextMessages = localMessages.where()
                    .equalTo(MessageItem.Fields.TEXT, remoteMessage.getText()).findAll();

            if (isTimeStampSimilar(sameTextMessages, remoteMessageTimestamp)) {
                LogManager.i(this, "Sync. Found messages with same text and similar remote timestamp. Removing. Remote message:"
                        + " Text: " + remoteMessage.getText()
                        + " Timestamp: " + remoteMessage.getTimestamp()
                        + " Delay Timestamp: " + remoteMessage.getDelayTimestamp()
                        + " StanzaId: " + remoteMessage.getStanzaId());
                iterator.remove();
                continue;
            }

            if (remoteMessageDelayTimestamp != null
                    && isTimeStampSimilar(sameTextMessages, remoteMessageDelayTimestamp)) {
                LogManager.i(this, "Sync. Found messages with same text and similar remote delay timestamp. Removing. Remote message:"
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
    private SyncInfo getSyncInfo(Realm realm, AccountJid account, UserJid user) {
        SyncInfo syncInfo = realm.where(SyncInfo.class)
                .equalTo(SyncInfo.FIELD_ACCOUNT, account.toString())
                .equalTo(SyncInfo.FIELD_USER, user.toString()).findFirst();

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

        if (mamQueryResult.mamFin.getRSMSet() != null) {

            if (syncInfo.getFirstMamMessageMamId() == null) {
                syncInfo.setFirstMamMessageMamId(mamQueryResult.mamFin.getRSMSet().getFirst());
                if (!mamQueryResult.forwardedMessages.isEmpty()) {
                    syncInfo.setFirstMamMessageStanzaId(mamQueryResult.forwardedMessages.get(0).getForwardedStanza().getStanzaId());
                }
            }
            if (mamQueryResult.mamFin.getRSMSet().getLast() != null) {
                syncInfo.setLastMessageMamId(mamQueryResult.mamFin.getRSMSet().getLast());
            }

        }

        realm.commitTransaction();
    }

    public void requestPreviousHistory(final AbstractChat chat) {
        if (chat == null || chat.isRemotePreviousHistoryCompletelyLoaded()) {
            return;
        }

        final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
        if (accountItem == null || !accountItem.getFactualStatusMode().isOnline()) {
            return;
        }

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                if (!checkSupport(accountItem)) {
                    return;
                }

                String firstMamMessageMamId;
                boolean remoteHistoryCompletelyLoaded;
                {
                    Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
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

                org.jivesoftware.smackx.mam.MamManager mamManager = org.jivesoftware.smackx.mam.MamManager.getInstanceFor(accountItem.getConnection());

                final org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult;
                try {
                    EventBus.getDefault().post(new PreviousHistoryLoadStartedEvent(chat));
                    LogManager.i("MAM", "Loading previous history");
                    mamQueryResult = mamManager.pageBefore(chat.getUser().getJid(), firstMamMessageMamId, PAGE_SIZE);
                } catch (SmackException.NotLoggedInException | SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                    LogManager.exception(this, e);
                    EventBus.getDefault().post(new PreviousHistoryLoadFinishedEvent(chat));
                    return;
                }

                EventBus.getDefault().post(new PreviousHistoryLoadFinishedEvent(chat));

                LogManager.i("MAM", "queryArchive finished. fin count expected: " + mamQueryResult.mamFin.getRSMSet().getCount() + " real: " + mamQueryResult.forwardedMessages.size());

                Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                List<MessageItem> messageItems = getMessageItems(mamQueryResult, chat);
                syncMessages(realm, chat, messageItems);
                updatePreviousHistorySyncInfo(realm, chat, mamQueryResult, messageItems);
                realm.close();
            }

        });

    }

    private void updatePreviousHistorySyncInfo(Realm realm, BaseEntity chat, org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult, List<MessageItem> messageItems) {
        SyncInfo syncInfo = getSyncInfo(realm, chat.getAccount(), chat.getUser());

        realm.beginTransaction();
        if (mamQueryResult.forwardedMessages.size() < PAGE_SIZE) {
            syncInfo.setRemoteHistoryCompletelyLoaded(true);
        }

        syncInfo.setFirstMamMessageMamId(mamQueryResult.mamFin.getRSMSet().getFirst());
        if (!mamQueryResult.forwardedMessages.isEmpty()) {
            syncInfo.setFirstMamMessageStanzaId(mamQueryResult.forwardedMessages.get(0).getForwardedStanza().getStanzaId());
        }
        realm.commitTransaction();
    }

    private List<MessageItem> getMessageItems(org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult, AbstractChat chat) {
        List<MessageItem> messageItems = new ArrayList<>();

        for (Forwarded forwarded : mamQueryResult.forwardedMessages) {
            if (!(forwarded.getForwardedStanza() instanceof Message)) {
                continue;
            }

            Message message = (Message) forwarded.getForwardedStanza();

            DelayInformation delayInformation = forwarded.getDelayInformation();

            DelayInformation messageDelay = DelayInformation.from(message);

            String body = message.getBody();
            net.java.otr4j.io.messages.AbstractMessage otrMessage;
            try {
                otrMessage = SerializationUtils.toMessage(body);
            } catch (IOException e) {
                continue;
            }
            boolean encrypted = false;
            if (otrMessage != null) {
                if (otrMessage.messageType != net.java.otr4j.io.messages.AbstractMessage.MESSAGE_PLAINTEXT) {
                    encrypted = true;
                    try {
                        // this transforming just decrypt message if have keys. No action as injectMessage or something else
                        body = OTRManager.getInstance().transformReceivingIfSessionExist(chat.getAccount(), chat.getUser(), body);
                        if (OTRManager.getInstance().isEncrypted(body)) {
                            continue;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
                else body = ((PlainTextMessage) otrMessage).cleanText;
            }

            boolean incoming = message.getFrom().asBareJid().equals(chat.getUser().getJid().asBareJid());

            MessageItem messageItem = new MessageItem();

            messageItem.setAccount(chat.getAccount());
            messageItem.setUser(chat.getUser());
            messageItem.setResource(chat.getUser().getJid().getResourceOrNull());
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
            messageItem.setEncrypted(encrypted);

            FileManager.processFileMessage(messageItem);

            messageItems.add(messageItem);
        }
        return messageItems;
    }

    /**
     * Only for debugging
     * Call only from background thread
     * @param chat
     */
    public void requestFullChatHistory(final AbstractChat chat) {
        if (chat == null || chat.isRemotePreviousHistoryCompletelyLoaded()) {
            return;
        }

        final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
        if (accountItem == null || !accountItem.getFactualStatusMode().isOnline()) {
            return;
        }

        if (!checkSupport(accountItem)) {
            return;
        }

        String firstMamMessageMamId;
        boolean remoteHistoryCompletelyLoaded;
        {
            Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
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

        org.jivesoftware.smackx.mam.MamManager mamManager =
                org.jivesoftware.smackx.mam.MamManager.getInstanceFor(accountItem.getConnection());

        final org.jivesoftware.smackx.mam.MamManager.MamQueryResult mamQueryResult;
        try {
            LogManager.i("MAM", "Loading previous history");
            mamQueryResult = mamManager.queryArchive(chat.getUser().getJid());
        } catch (SmackException.NotLoggedInException | SmackException.NoResponseException
                | XMPPException.XMPPErrorException | InterruptedException
                | SmackException.NotConnectedException e) {
            LogManager.exception(this, e);
            return;
        }
        LogManager.i("MAM", "queryArchive finished. fin count expected: "
                + mamQueryResult.mamFin.getRSMSet().getCount() + " real: "
                + mamQueryResult.forwardedMessages.size());

        Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
        List<MessageItem> messageItems = getMessageItems(mamQueryResult, chat);
        syncMessages(realm, chat, messageItems);
        updatePreviousHistorySyncInfo(realm, chat, mamQueryResult, messageItems);
        realm.close();
    }
}
