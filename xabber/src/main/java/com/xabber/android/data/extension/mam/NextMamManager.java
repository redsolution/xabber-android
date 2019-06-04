package com.xabber.android.data.extension.mam;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.ForwardId;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.messagerealm.SyncInfo;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ForwardManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.push.SyncManager;
import com.xabber.android.data.roster.OnRosterReceivedListener;
import com.xabber.android.data.roster.RosterCacheManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;

import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.io.messages.PlainTextMessage;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;

public class NextMamManager implements OnRosterReceivedListener, OnPacketListener {

    private static final String LOG_TAG = NextMamManager.class.getSimpleName();

    private static NextMamManager instance;

    private Map<AccountJid, Boolean> supportedByAccount = new ConcurrentHashMap<>();
    private boolean isRequested = false;
    private final Object lock = new Object();
    private Set<String> waitingRequests = new HashSet<>();

    public static NextMamManager getInstance() {
        if (instance == null)
            instance = new NextMamManager();
        return instance;
    }

    @Override
    public void onRosterReceived(AccountItem accountItem) {
        onAccountConnected(accountItem);
    }

    public void onAccountConnected(AccountItem accountItem) {
        updateIsSupported(accountItem);
        updatePreferencesFromServer(accountItem);
        Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
        accountItem.setStartHistoryTimestamp(getLastMessageTimestamp(accountItem, realm));
        if (accountItem.getStartHistoryTimestamp() == 0) {
            initializeStartTimestamp(accountItem);
            loadLastMessagesAsync(accountItem);
        } else {
            if (isNeedMigration(accountItem, realm)) {
                runMigrationToNewArchive(accountItem, realm);
            }
            String lastArchivedId = getLastMessageArchivedId(accountItem, realm);
            if (lastArchivedId != null) {
                boolean historyCompleted = loadAllNewMessages(realm, accountItem, lastArchivedId);
                if (!historyCompleted) loadLastMessagesAsync(accountItem);
            } else loadLastMessagesAsync(accountItem);
        }
        realm.close();
     }

    public void onChatOpen(final AbstractChat chat) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
        if (accountItem == null || accountItem.getLoadHistorySettings() == LoadHistorySettings.none
                || !isSupported(accountItem.getAccount())) return;

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();

                // if history is empty - load last message
                MessageItem firstMessage = getFirstMessage(chat, realm);
                if (firstMessage == null) loadLastMessage(realm, accountItem, chat);

                synchronized (lock) {
                    if (isRequested) return;
                    else isRequested = true;
                }

                // load prev page if history is not enough
                if (historyIsNotEnough(realm, chat) && !chat.historyIsFull()) {
                    EventBus.getDefault().post(new LastHistoryLoadStartedEvent(chat));
                    loadNextHistory(realm, accountItem, chat);
                    EventBus.getDefault().post(new LastHistoryLoadFinishedEvent(chat));
                }

                // load missed messages if need
                List<MessageItem> messages = findMissedMessages(realm, chat);
                if (messages != null && !messages.isEmpty() && accountItem != null) {
                    for (MessageItem message : messages) {
                        loadMissedMessages(realm, accountItem, chat, message);
                    }
                }

                synchronized (lock) {
                    isRequested = false;
                }
                realm.close();
            }
        });
    }

    public void onScrollInChat(final AbstractChat chat) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
        if (accountItem == null || accountItem.getLoadHistorySettings() == LoadHistorySettings.none
                || !isSupported(accountItem.getAccount())) return;

        if (chat.historyIsFull()) return;
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    if (isRequested) return;
                    else isRequested = true;
                }
                EventBus.getDefault().post(new LastHistoryLoadStartedEvent(chat));
                Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                loadNextHistory(realm, accountItem, chat);
                realm.close();
                EventBus.getDefault().post(new LastHistoryLoadFinishedEvent(chat));
                synchronized (lock) {
                    isRequested = false;
                }
            }
        });
    }

    public void loadFullChatHistory(AbstractChat chat) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(chat.getAccount());
        if (accountItem == null || !isSupported(accountItem.getAccount()) || chat.historyIsFull()) return;

        Realm realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();

        // if history is empty - load last message
        MessageItem firstMessage = getFirstMessage(chat, realm);
        if (firstMessage == null) loadLastMessage(realm, accountItem, chat);

        boolean complete = false;
        while (!complete) {
            complete = loadNextHistory(realm, accountItem, chat);
        }

        realm.close();
    }

    public void onRequestUpdatePreferences(AccountJid accountJid) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem == null || !isSupported(accountJid)) return;

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                requestUpdatePreferences(accountItem);
            }
        });
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof Message) {
            for (ExtensionElement packetExtension : packet.getExtensions()) {
                if (packetExtension instanceof MamElements.MamResultExtension) {
                    MamElements.MamResultExtension resultExtension =
                            (MamElements.MamResultExtension) packetExtension;
                    String resultID = resultExtension.getQueryId();
                    if (waitingRequests.contains(resultID)) {
                        parseAndSaveMessageFromMamResult(connection.getAccount(), resultExtension);
                        waitingRequests.remove(resultID);
                    }
                }
            }
        }
    }

    public boolean isSupported(AccountJid accountJid) {
        Boolean isSupported = supportedByAccount.get(accountJid);
        if (isSupported != null) return isSupported;
        else return false;
    }

    /** MAIN */

    private void loadLastMessagesAsync(AccountItem accountItem) {
        if (accountItem.getLoadHistorySettings() != LoadHistorySettings.all
                || !isSupported(accountItem.getAccount())) return;

        LogManager.d(LOG_TAG, "load last messages in each chat");
        Collection<RosterContact> contacts = RosterManager.getInstance()
                .getAccountRosterContacts(accountItem.getAccount());

        for (RosterContact contact : contacts) {
            AbstractChat chat = MessageManager.getInstance()
                    .getOrCreateChat(contact.getAccount(), contact.getUser());
            requestLastMessageAsync(accountItem, chat);
        }
    }

    private void loadLastMessage(Realm realm, AccountItem accountItem, AbstractChat chat) {
        LogManager.d(LOG_TAG, "load last messages in chat: " + chat.getUser());
        MamManager.MamQueryResult queryResult = requestLastMessage(accountItem, chat);
        if (queryResult != null) {
            List<Forwarded> messages = new ArrayList<>(queryResult.forwardedMessages);
            saveOrUpdateMessages(realm, parseMessage(accountItem, chat.getAccount(), chat.getUser(), messages, null));
        }
        updateLastMessageId(chat, realm);
    }

    private boolean loadAllNewMessages(Realm realm, AccountItem accountItem, String lastArchivedId) {
        if (accountItem.getLoadHistorySettings() != LoadHistorySettings.all
                || !isSupported(accountItem.getAccount())) return true;

        LogManager.d(LOG_TAG, "load new messages");
        List<Forwarded> messages = new ArrayList<>();
        boolean complete = false;
        String id = lastArchivedId;
        int pageLoaded = 0;
        // Request all new messages after last archived id
        while (!complete && id != null && pageLoaded < 2) {
            MamManager.MamQueryResult queryResult = requestMessagesFromId(accountItem, null, id);
            if (queryResult != null) {
                messages.addAll(queryResult.forwardedMessages);
                complete = queryResult.mamFin.isComplete();
                id = getNextId(queryResult);
                pageLoaded++;
            } else complete = true;
        }

        if (!messages.isEmpty()) {
            HashMap<String, ArrayList<Forwarded>> messagesByChat = new HashMap<>();
            List<MessageItem> parsedMessages = new ArrayList<>();
            List<AbstractChat> chatsNeedUpdateLastMessageId = new ArrayList<>();

            // Sort messages by chat to separate lists
            for (Forwarded forwarded : messages) {
                Stanza stanza = forwarded.getForwardedStanza();
                Jid user = stanza.getFrom().asBareJid();
                if (user.equals(accountItem.getAccount().getFullJid().asBareJid()))
                    user = stanza.getTo().asBareJid();

                if (!messagesByChat.containsKey(user.toString())) {
                    messagesByChat.put(user.toString(), new ArrayList<Forwarded>());
                }
                ArrayList<Forwarded> list = messagesByChat.get(user.toString());
                if (list != null) list.add(forwarded);
            }

            // parse message lists
            for (Map.Entry<String, ArrayList<Forwarded>> entry : messagesByChat.entrySet()) {
                ArrayList<Forwarded> list = entry.getValue();
                if (list != null) {
                    try {
                        AbstractChat chat = MessageManager.getInstance()
                                .getOrCreateChat(accountItem.getAccount(), UserJid.from(entry.getKey()));

                        // sort messages in list by timestamp
                        Collections.sort(list, new Comparator<Forwarded>() {
                            @Override
                            public int compare(Forwarded o1, Forwarded o2) {
                                DelayInformation delayInformation1 = o1.getDelayInformation();
                                long time1 = delayInformation1.getStamp().getTime();

                                DelayInformation delayInformation2 = o2.getDelayInformation();
                                long time2 = delayInformation2.getStamp().getTime();

                                return Long.valueOf(time1).compareTo(time2);
                            }
                        });

                        // parse messages and set previous id
                        parsedMessages.addAll(
                                parseMessage(accountItem, accountItem.getAccount(),
                                        chat.getUser(), list, chat.getLastMessageId()));
                        chatsNeedUpdateLastMessageId.add(chat);

                    } catch (UserJid.UserJidCreateException e) {
                        LogManager.d(LOG_TAG, e.toString());
                        continue;
                    }
                }
            }

            // save messages to Realm
            saveOrUpdateMessages(realm, parsedMessages);
            for (AbstractChat chat : chatsNeedUpdateLastMessageId) {
                updateLastMessageId(chat, realm);
            }
        }
        return complete;
    }

    private boolean loadNextHistory(Realm realm, AccountItem accountItem, AbstractChat chat) {
        LogManager.d(LOG_TAG, "load next history in chat: " + chat.getUser());
        MessageItem firstMessage = getFirstMessage(chat, realm);
        if (firstMessage != null) {
            if (firstMessage.getArchivedId().equals(firstMessage.getPreviousId())) {
                chat.setHistoryIsFull();
                return true;
            }

            MamManager.MamQueryResult queryResult = requestMessagesBeforeId(accountItem, chat, firstMessage.getArchivedId());
            if (queryResult != null) {
                List<Forwarded> messages = new ArrayList<>(queryResult.forwardedMessages);
                if (!messages.isEmpty()) {
                    List<MessageItem> savedMessages = saveOrUpdateMessages(realm,
                            parseMessage(accountItem, chat.getAccount(), chat.getUser(), messages, null));

                    if (savedMessages != null && !savedMessages.isEmpty()) {
                        realm.beginTransaction();
                        firstMessage.setPreviousId(savedMessages.get(savedMessages.size() - 1).getArchivedId());
                        realm.commitTransaction();
                        return false;
                    }
                } else if (queryResult.mamFin.isComplete()) {
                    realm.beginTransaction();
                    firstMessage.setPreviousId(firstMessage.getArchivedId());
                    realm.commitTransaction();
                }
            }
        }
        return true;
    }

    private void loadMissedMessages(Realm realm, AccountItem accountItem, AbstractChat chat, MessageItem m1) {
        LogManager.d(LOG_TAG, "load missed messages in chat: " + chat.getUser());
        MessageItem m2 = getMessageForCloseMissedMessages(realm, m1);
        if (m2 != null && !m2.getUniqueId().equals(m1.getUniqueId())) {
            Date startDate = new Date(m2.getTimestamp());
            Date endDate = new Date(m1.getTimestamp());

            List<Forwarded> messages = new ArrayList<>();
            boolean complete = false;

            while (!complete && startDate != null) {
                MamManager.MamQueryResult queryResult = requestMissedMessages(accountItem, chat, startDate, endDate);
                if (queryResult != null) {
                    messages.addAll(queryResult.forwardedMessages);
                    complete = queryResult.mamFin.isComplete();
                    startDate = getNextDate(queryResult);
                } else complete = true;
            }

            if (!messages.isEmpty()) {
                List<MessageItem> savedMessages = saveOrUpdateMessages(realm,
                        parseMessage(accountItem, chat.getAccount(), chat.getUser(), messages, m2.getArchivedId()));

                if (savedMessages != null && !savedMessages.isEmpty()) {
                    realm.beginTransaction();
                    m1.setPreviousId(savedMessages.get(savedMessages.size() - 1).getArchivedId());
                    realm.commitTransaction();
                }
            }
        }
    }

    /** Request most recent message from all history and save it timestamp to startHistoryTimestamp
     *  If message is null save current time to startHistoryTimestamp */
    private void initializeStartTimestamp(@Nonnull AccountItem accountItem) {
        long startHistoryTimestamp = System.currentTimeMillis();

        MamManager.MamQueryResult queryResult = requestLastMessage(accountItem, null);
        if (queryResult != null && !queryResult.forwardedMessages.isEmpty()) {
            Forwarded forwarded = queryResult.forwardedMessages.get(0);
            startHistoryTimestamp = forwarded.getDelayInformation().getStamp().getTime();
        }
        accountItem.setStartHistoryTimestamp(startHistoryTimestamp);
    }

    private void updateIsSupported(AccountItem accountItem) {
        MamManager mamManager = MamManager.getInstanceFor(accountItem.getConnection());
        boolean isSupported;
        try {
            isSupported = mamManager.isSupportedByServer();
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | InterruptedException | SmackException.NotConnectedException | ClassCastException e) {
            LogManager.exception(this, e);
            isSupported = false;
        }
        supportedByAccount.put(accountItem.getAccount(), isSupported);
        AccountManager.getInstance().onAccountChanged(accountItem.getAccount());
    }

    private void updatePreferencesFromServer(@Nonnull AccountItem accountItem) {
        MamManager.MamPrefsResult prefsResult = requestPreferencesFromServer(accountItem);
        if (prefsResult != null) {
            MamPrefsIQ.DefaultBehavior behavior = prefsResult.mamPrefs.getDefault();
            AccountManager.getInstance().setMamDefaultBehaviour(accountItem.getAccount(), behavior);
        }
    }

    /** REQUESTS */

    /** T extends MamManager.MamQueryResult or T extends MamManager.MamPrefsResult */
    abstract class MamRequest<T>  {
        abstract T execute(MamManager manager) throws Exception;
    }

    /** T extends MamManager.MamQueryResult or T extends MamManager.MamPrefsResult */
    private <T> T requestToMessageArchive(AccountItem accountItem, MamRequest<T> request) {
        T result = null;
        XMPPTCPConnection connection = accountItem.getConnection();

        if (connection.isAuthenticated()) {
            MamManager mamManager = MamManager.getInstanceFor(connection);
            try {
                result = request.execute(mamManager);
            } catch (Exception e) {
                LogManager.exception(this, e);
            }
        }
        return result;
    }

    /** Request recent message from chat history if chat not null
     *  Else request most recent message from all history*/
    private @Nullable MamManager.MamQueryResult requestLastMessage(
            @Nonnull AccountItem accountItem, @Nullable final AbstractChat chat) {

        return requestToMessageArchive(accountItem, new MamRequest<MamManager.MamQueryResult>() {
            @Override
            MamManager.MamQueryResult execute(MamManager manager) throws Exception {
                if (chat != null) return manager.mostRecentPage(chat.getUser().getJid(), 1);
                else return manager.mostRecentPage(null, 1);
            }
        });
    }

    /** Send async request for recent message from chat history */
    private void requestLastMessageAsync(@Nonnull final AccountItem accountItem, @Nonnull final AbstractChat chat) {
        requestToMessageArchive(accountItem, new MamRequest<MamManager.MamQueryResult>() {
            @Override
            MamManager.MamQueryResult execute(MamManager manager) throws Exception {
                // add request id to waiting list
                String queryID = UUID.randomUUID().toString();
                waitingRequests.add(queryID);

                // send request stanza
                RSMSet rsmSet = new RSMSet(null, "", -1, -1, null, 1, null, -1);
                DataForm dataForm = getNewMamForm();
                addWithJid(chat.getUser().getJid(), dataForm);
                MamQueryIQ mamQueryIQ = new MamQueryIQ(queryID, null, dataForm);
                mamQueryIQ.setType(IQ.Type.set);
                mamQueryIQ.setTo((Jid) null);
                mamQueryIQ.addExtension(rsmSet);
                accountItem.getConnection().sendStanza(mamQueryIQ);
                return null;
            }
        });
    }

    /** Request messages after archivedID from chat history
    *  Else request messages after archivedID from all history */
    private @Nullable MamManager.MamQueryResult requestMessagesFromId(
            @Nonnull AccountItem accountItem, @Nullable final AbstractChat chat, final String archivedId) {

        return requestToMessageArchive(accountItem, new MamRequest<MamManager.MamQueryResult>() {
            @Override
            MamManager.MamQueryResult execute(MamManager manager) throws Exception {
                if (chat != null) return manager.pageAfter(chat.getUser().getJid(), archivedId, 50);
                else return manager.pageAfter(null, archivedId, 50);
            }
        });
    }

    /** Request messages before archivedID from chat history */
    private @Nullable MamManager.MamQueryResult requestMessagesBeforeId(
            @Nonnull AccountItem accountItem, @Nonnull final AbstractChat chat, final String archivedId) {

        return requestToMessageArchive(accountItem, new MamRequest<MamManager.MamQueryResult>() {
            @Override
            MamManager.MamQueryResult execute(MamManager manager) throws Exception {
                return manager.pageBefore(chat.getUser().getJid(), archivedId, 50);
            }
        });
    }

    /** Request messages started with startID and ending with endID from chat history */
    private @Nullable MamManager.MamQueryResult requestMissedMessages(
            @Nonnull AccountItem accountItem, @Nonnull final AbstractChat chat,
            final Date startDate, final Date endDate) {

        return requestToMessageArchive(accountItem, new MamRequest<MamManager.MamQueryResult>() {
            @Override
            MamManager.MamQueryResult execute(MamManager manager) throws Exception {
                return manager.queryArchive(50, startDate, endDate, chat.getUser().getJid(), null);
            }
        });
    }

    /** Request update archiving preferences on server */
    private void requestUpdatePreferences(@Nonnull final AccountItem accountItem) {
        requestToMessageArchive(accountItem, new MamRequest<MamManager.MamPrefsResult>() {
            @Override
            MamManager.MamPrefsResult execute(MamManager manager) throws Exception {
                return manager.updateArchivingPreferences(null, null, accountItem.getMamDefaultBehaviour());
            }
        });
    }

    /** Request archiving preferences from server */
    private @Nullable MamManager.MamPrefsResult requestPreferencesFromServer(@Nonnull final AccountItem accountItem) {
        return requestToMessageArchive(accountItem, new MamRequest<MamManager.MamPrefsResult>() {
            @Override
            MamManager.MamPrefsResult execute(MamManager manager) throws Exception {
                return manager.retrieveArchivingPreferences();
            }
        });
    }

    /** PARSING */

    private void parseAndSaveMessageFromMamResult(AccountJid account, MamElements.MamResultExtension resultExtension) {
        Forwarded forwarded = resultExtension.getForwarded();
        Stanza stanza = forwarded.getForwardedStanza();
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        Jid user = stanza.getFrom().asBareJid();
        if (user.equals(account.getFullJid().asBareJid()))
            user = stanza.getTo().asBareJid();

        try {
            AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, UserJid.from(user));
            MessageItem messageItem = parseMessage(accountItem, account, chat.getUser(), forwarded, null);
            if (messageItem != null) {
                Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
                saveOrUpdateMessages(realm, Collections.singletonList(messageItem), true);
                updateLastMessageId(chat, realm);
            }
        } catch (UserJid.UserJidCreateException e) {
            LogManager.d(LOG_TAG, e.toString());
        }
    }

    private List<MessageItem> parseMessage(AccountItem accountItem, AccountJid account, UserJid user,
                                           List<Forwarded> forwardedMessages, String prevID) {
        List<MessageItem> messageItems = new ArrayList<>();
        String lastOutgoingId = null;
        for (Forwarded forwarded : forwardedMessages) {
            MessageItem message = parseMessage(accountItem, account, user, forwarded, prevID);
            if (message != null) {
                messageItems.add(message);
                prevID = message.getArchivedId();
                if (!message.isIncoming()) lastOutgoingId = message.getUniqueId();
            }
        }

        // mark messages before outgoing as read
        if (lastOutgoingId != null) {
            for (MessageItem message : messageItems) {
                if (lastOutgoingId.equals(message.getUniqueId())) {
                    break;
                }
                message.setRead(true);
            }
        }

        return messageItems;
    }

    private @Nullable MessageItem parseMessage(AccountItem accountItem, AccountJid account, UserJid user, Forwarded forwarded, String prevID) {
        if (!(forwarded.getForwardedStanza() instanceof Message)) {
            return null;
        }

        Message message = (Message) forwarded.getForwardedStanza();

        DelayInformation delayInformation = forwarded.getDelayInformation();

        DelayInformation messageDelay = DelayInformation.from(message);

        String body = message.getBody();
        net.java.otr4j.io.messages.AbstractMessage otrMessage;
        try {
            otrMessage = SerializationUtils.toMessage(body);
        } catch (IOException e) {
            return null;
        }
        boolean encrypted = false;
        if (otrMessage != null) {
            if (otrMessage.messageType != net.java.otr4j.io.messages.AbstractMessage.MESSAGE_PLAINTEXT) {
                encrypted = true;
                try {
                    // this transforming just decrypt message if have keys. No action as injectMessage or something else
                    body = OTRManager.getInstance().transformReceivingIfSessionExist(account, user, body);
                    if (OTRManager.getInstance().isEncrypted(body)) {
                        return null;
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            else body = ((PlainTextMessage) otrMessage).cleanText;
        }

        boolean incoming = message.getFrom().asBareJid().equals(user.getJid().asBareJid());

        String uid = UUID.randomUUID().toString();
        MessageItem messageItem = new MessageItem(uid);
        messageItem.setPreviousId(prevID);

        String archivedId = ArchivedHelper.getArchivedId(forwarded.getForwardedStanza());
        if (archivedId != null) messageItem.setArchivedId(archivedId);

        long timestamp = delayInformation.getStamp().getTime();

        messageItem.setAccount(account);
        messageItem.setUser(user);
        messageItem.setResource(user.getJid().getResourceOrNull());
        messageItem.setText(body);
        messageItem.setTimestamp(timestamp);
        if (messageDelay != null) {
            messageItem.setDelayTimestamp(messageDelay.getStamp().getTime());
        }
        messageItem.setIncoming(incoming);
        messageItem.setStanzaId(AbstractChat.getStanzaId(message));
        messageItem.setPacketId(message.getStanzaId());
        messageItem.setReceivedFromMessageArchive(true);
        messageItem.setRead(timestamp <= accountItem.getStartHistoryTimestamp());
        messageItem.setSent(true);
        messageItem.setEncrypted(encrypted);

        // attachments
        FileManager.processFileMessage(messageItem);

        RealmList<Attachment> attachments = HttpFileUploadManager.parseFileMessage(message);
        if (attachments.size() > 0)
            messageItem.setAttachments(attachments);

        // forwarded
        messageItem.setOriginalStanza(message.toXML().toString());
        messageItem.setOriginalFrom(message.getFrom().toString());

        return messageItem;
    }

    /** SAVING */

    private List<MessageItem> saveOrUpdateMessages(Realm realm, final Collection<MessageItem> messages) {
        return saveOrUpdateMessages(realm, messages, false);
    }

    private List<MessageItem> saveOrUpdateMessages(Realm realm, final Collection<MessageItem> messages, boolean ui) {
        List<MessageItem> messagesToSave = new ArrayList<>();
        if (messages != null && !messages.isEmpty()) {
            Iterator<MessageItem> iterator = messages.iterator();
            while (iterator.hasNext()) {
                MessageItem newMessage = determineSaveOrUpdate(realm, iterator.next(), ui);
                if (newMessage != null) messagesToSave.add(newMessage);
            }
        }
        realm.beginTransaction();
        realm.copyToRealm(messagesToSave);
        realm.commitTransaction();
        SyncManager.getInstance().onMessageSaved();
        EventBus.getDefault().post(new NewMessageEvent());
        return messagesToSave;
    }

    private MessageItem determineSaveOrUpdate(Realm realm, final MessageItem message, boolean ui) {
        // set text from comment to text in message for prevent doubling messages from MAM
        Message originalMessage = null;
        try {
            originalMessage = (Message) PacketParserUtils.parseStanza(message.getOriginalStanza());
            String comment = ForwardManager.parseForwardComment(originalMessage);
            if (comment != null) message.setText(comment);
        } catch (Exception e) {
            e.printStackTrace();
        }

        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(message.getAccount(), message.getUser());
        if (chat == null) return null;

        MessageItem localMessage = findSameLocalMessage(realm, chat, message);
        if (localMessage == null) {
            // forwarded
            if (originalMessage != null) {
                RealmList<ForwardId> forwardIds = chat.parseForwardedMessage(ui, originalMessage, message.getUniqueId());
                if (forwardIds != null && !forwardIds.isEmpty())
                    message.setForwardedIds(forwardIds);
            }

            // notify about new message
            chat.enableNotificationsIfNeed();
            boolean notify = !message.isRead() && (message.getText() != null && !message.getText().trim().isEmpty())
                    && message.isIncoming() && chat.notifyAboutMessage();
            boolean visible = MessageManager.getInstance().isVisibleChat(chat);
            if (notify && !visible)
                NotificationManager.getInstance().onMessageNotification(message);
            //

            return message;
        } else {
            realm.beginTransaction();
            localMessage.setArchivedId(message.getArchivedId());
            realm.commitTransaction();
            return null;
        }
    }

    /** UTILS */

    private static DataForm getNewMamForm() {
        FormField field = new FormField(FormField.FORM_TYPE);
        field.setType(FormField.Type.hidden);
        field.addValue(MamElements.NAMESPACE);
        DataForm form = new DataForm(DataForm.Type.submit);
        form.addField(field);
        return form;
    }

    private static void addWithJid(Jid withJid, DataForm dataForm) {
        if (withJid == null) return;
        FormField formField = new FormField("with");
        formField.addValue(withJid.toString());
        dataForm.addField(formField);
    }

    private String getNextId(MamManager.MamQueryResult queryResult) {
        String archivedId = null;
        if (queryResult.forwardedMessages != null && !queryResult.forwardedMessages.isEmpty()) {
            Forwarded forwarded = queryResult.forwardedMessages.get(queryResult.forwardedMessages.size() - 1);
            archivedId = ArchivedHelper.getArchivedId(forwarded.getForwardedStanza());
        }
        return archivedId;
    }

    private Date getNextDate(MamManager.MamQueryResult queryResult) {
        Date date = null;
        if (queryResult.forwardedMessages != null && !queryResult.forwardedMessages.isEmpty()) {
            Forwarded forwarded = queryResult.forwardedMessages.get(queryResult.forwardedMessages.size() - 1);
            DelayInformation delayInformation = forwarded.getDelayInformation();
            date = new Date(delayInformation.getStamp().getTime() + 1);
        }
        return date;
    }

    @Nullable private List<MessageItem> findMissedMessages(Realm realm, AbstractChat chat) {
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.ARCHIVED_ID)
                .isNull(MessageItem.Fields.PREVIOUS_ID)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.DESCENDING);

        if (results != null && !results.isEmpty()) {
            return new ArrayList<>(results);
        } else return null;
    }

    private MessageItem getMessageForCloseMissedMessages(Realm realm, MessageItem messageItem) {
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, messageItem.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, messageItem.getUser().toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.ARCHIVED_ID)
                .lessThan(MessageItem.Fields.TIMESTAMP, messageItem.getTimestamp())
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.DESCENDING);

        if (results != null && !results.isEmpty()) {
            return results.first();
        } else return null;
    }

    private boolean isNeedMigration(AccountItem account, Realm realm) {
        MessageItem result = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account.getAccount().toString())
                .notEqualTo(MessageItem.Fields.PREVIOUS_ID, "legacy")
                .findFirst();
        return result == null;
    }

    private boolean historyIsNotEnough(Realm realm, AbstractChat chat) {
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .findAll();
        return results.size() < 30;
    }

    private String getLastMessageArchivedId(AccountItem account, Realm realm) {
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account.getAccount().toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.ARCHIVED_ID)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);

        if (results != null && !results.isEmpty()) {
            MessageItem lastMessage = results.last();
            return lastMessage.getArchivedId();
        } else return null;
    }

    private MessageItem getFirstMessage(AbstractChat chat, Realm realm) {
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .isNotNull(MessageItem.Fields.ARCHIVED_ID)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);

        if (results != null && !results.isEmpty()) {
            return results.first();
        } else return null;
    }

    private MessageItem getFirstMessageForMigration(AbstractChat chat, Realm realm) {
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);

        if (results != null && !results.isEmpty()) {
            return results.first();
        } else return null;
    }

    private long getLastMessageTimestamp(AccountItem account, Realm realm) {
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, account.getAccount().toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);

        if (results != null && !results.isEmpty()) {
            MessageItem lastMessage = results.last();
            return lastMessage.getTimestamp();
        } else return 0;
    }

    private void updateLastMessageId(AbstractChat chat, Realm realm) {
        RealmResults<MessageItem> results = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);

        if (results != null && !results.isEmpty()) {
            MessageItem lastMessage = results.last();
            String id = lastMessage.getArchivedId();
            if (id == null) id = lastMessage.getStanzaId();
            chat.setLastMessageId(id);
        }
    }

    private MessageItem findSameLocalMessage(Realm realm, AbstractChat chat, MessageItem message) {
        return realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .equalTo(MessageItem.Fields.TEXT, message.getText())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .equalTo(MessageItem.Fields.STANZA_ID, message.getStanzaId())
                .or()
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .equalTo(MessageItem.Fields.TEXT, message.getText())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .equalTo(MessageItem.Fields.STANZA_ID, message.getPacketId())
                .or()
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .equalTo(MessageItem.Fields.TEXT, message.getText())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .equalTo(MessageItem.Fields.STANZA_ID, message.getArchivedId())
                .or()
                .equalTo(MessageItem.Fields.ACCOUNT, chat.getAccount().toString())
                .equalTo(MessageItem.Fields.USER, chat.getUser().toString())
                .equalTo(MessageItem.Fields.TEXT, message.getText())
                .isNull(MessageItem.Fields.PARENT_MESSAGE_ID)
                .equalTo(MessageItem.Fields.ARCHIVED_ID, message.getArchivedId())
                .findFirst();
    }

    private void runMigrationToNewArchive(AccountItem accountItem, Realm realm) {
        LogManager.d(LOG_TAG, "run migration for account: " + accountItem.getAccount().toString());
        Collection<RosterContact> contacts = RosterManager.getInstance()
                .getAccountRosterContacts(accountItem.getAccount());

        for (RosterContact contact : contacts) {
            AbstractChat chat = MessageManager.getInstance()
                    .getOrCreateChat(contact.getAccount(), contact.getUser());

            MessageItem firstMessage = getFirstMessageForMigration(chat, realm);
            SyncInfo syncInfo = realm.where(SyncInfo.class)
                    .equalTo(SyncInfo.FIELD_ACCOUNT, accountItem.getAccount().toString())
                    .equalTo(SyncInfo.FIELD_USER, chat.getUser().toString()).findFirst();

            if (firstMessage != null && syncInfo != null) {
                realm.beginTransaction();
                firstMessage.setArchivedId(syncInfo.getFirstMamMessageMamId());
                firstMessage.setPreviousId(null);
                realm.commitTransaction();
            }
        }
    }
}
