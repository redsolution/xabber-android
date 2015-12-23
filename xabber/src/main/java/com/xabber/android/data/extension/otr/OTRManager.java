/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.extension.otr;

import android.database.Cursor;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountAddedListener;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.NestedMap.Entry;
import com.xabber.android.data.entity.NestedNestedMaps;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.ssn.SSNManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.EntityNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.xmpp.archive.OtrMode;
import com.xabber.xmpp.archive.SaveMode;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.crypto.OtrCryptoEngine;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.session.InstanceTag;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Manage off-the-record encryption.
 * <p/>
 * http://www.cypherpunks.ca/otr/
 *
 * @author alexander.ivanov
 */
public class OTRManager implements OtrEngineHost, OtrEngineListener,
        OnLoadListener, OnAccountAddedListener, OnAccountRemovedListener, OnCloseListener {

    private final static OTRManager instance;
    private static Map<SecurityOtrMode, OtrPolicy> POLICIES;

    static {
        POLICIES = new HashMap<>();
        POLICIES.put(SecurityOtrMode.disabled, new OtrPolicy(OtrPolicy.NEVER));
        POLICIES.put(SecurityOtrMode.manual, new OtrPolicy(OtrPolicy.OTRL_POLICY_MANUAL & ~OtrPolicy.ALLOW_V1));
        POLICIES.put(SecurityOtrMode.auto, new OtrPolicy(OtrPolicy.OPPORTUNISTIC & ~OtrPolicy.ALLOW_V1));
        POLICIES.put(SecurityOtrMode.required, new OtrPolicy(OtrPolicy.OTRL_POLICY_ALWAYS & ~OtrPolicy.ALLOW_V1));
    }

    static {
        instance = new OTRManager();
        Application.getInstance().addManager(instance);
    }

    private final EntityNotificationProvider<SMRequest> smRequestProvider;
    private final EntityNotificationProvider<SMProgress> smProgressProvider;
    /**
     * Accepted fingerprints for user in account.
     */
    private final NestedNestedMaps<String, Boolean> fingerprints;
    /**
     * Fingerprint of encrypted or encrypted and verified session for user in account.
     */
    private final NestedMap<String> actives;
    /**
     * Finished entity's sessions for users in accounts.
     */
    private final NestedMap<Boolean> finished;
    /**
     * Used OTR sessions for users in accounts.
     */
    private final NestedMap<Session> sessions;
    /**
     * Service for keypair generation.
     */
    private final ExecutorService keyPairGenerator;

    private OTRManager() {
        smRequestProvider = new EntityNotificationProvider<>(R.drawable.ic_stat_help);
        smProgressProvider = new EntityNotificationProvider<>(R.drawable.ic_stat_play_circle_fill);
        smProgressProvider.setCanClearNotifications(false);
        fingerprints = new NestedNestedMaps<>();
        actives = new NestedMap<>();
        finished = new NestedMap<>();
        sessions = new NestedMap<>();
        keyPairGenerator = Executors.newSingleThreadExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "Key pair generator service");
                        thread.setPriority(Thread.MIN_PRIORITY);
                        thread.setDaemon(true);
                        return thread;
                    }
                });
    }

    public static OTRManager getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        final NestedNestedMaps<String, Boolean> fingerprints = new NestedNestedMaps<>();
        Cursor cursor = OTRTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    String account = OTRTable.getAccount(cursor);
                    String user = OTRTable.getUser(cursor);
                    fingerprints.put(account, user,
                            OTRTable.getFingerprint(cursor),
                            OTRTable.isVerified(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(fingerprints);
            }
        });
    }

    private void onLoaded(NestedNestedMaps<String, Boolean> fingerprints) {
        this.fingerprints.addAll(fingerprints);
        NotificationManager.getInstance().registerNotificationProvider(smRequestProvider);
        NotificationManager.getInstance().registerNotificationProvider(smProgressProvider);
    }

    public void startSession(String account, String user) throws NetworkException {
        LogManager.i(this, "Starting session for " + user);
        try {
            getOrCreateSession(account, user).startSession();
        } catch (OtrException e) {
            throw new NetworkException(R.string.OTR_ERROR, e);
        }
        LogManager.i(this, "Started session for " + user);
    }

    public void refreshSession(String account, String user) throws NetworkException {
        LogManager.i(this, "Refreshing session for " + user);
        try {
            getOrCreateSession(account, user).refreshSession();
        } catch (OtrException e) {
            throw new NetworkException(R.string.OTR_ERROR, e);
        }
        LogManager.i(this, "Refreshed session for " + user);
    }

    public void endSession(String account, String user) throws NetworkException {
        LogManager.i(this, "Ending session for " + user);
        try {
            getOrCreateSession(account, user).endSession();
        } catch (OtrException e) {
            throw new NetworkException(R.string.OTR_ERROR, e);
        }
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);
        MessageArchiveManager.getInstance().setSaveMode(account, user, abstractChat.getThreadId(), SaveMode.body);
        SSNManager.getInstance().setSessionOtrMode(account, user, abstractChat.getThreadId(), OtrMode.concede);
        LogManager.i(this, "Ended session for " + user);
    }

    private Session getOrCreateSession(String account, String user) {
        Session session = sessions.get(account, user);
        if (session != null) {
            LogManager.i(this, "Found session with id " + session.getSessionID() + " with status " + session.getSessionStatus() + " for user " + user);
            return session;
        }

        LogManager.i(this, "Creating new session for " + user);

        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        session = new Session(new SessionID(account, user,
                accountItem == null ? "" : accountItem.getConnectionSettings().getProtocol().toString()), this);
        session.addOtrEngineListener(this);
        sessions.put(account, user, session);
        return session;
    }

    @Override
    public void injectMessage(SessionID sessionID, String msg) throws OtrException {
        injectMessage(sessionID.getAccountID(), sessionID.getUserID(), msg);
    }

    private void injectMessage(String account, String user, String msg) throws OtrException {
        LogManager.i(this, "injectMessage. user: " + user + " message: " + msg);
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);
        try {
            MessageArchiveManager.getInstance().setSaveMode(account, user, abstractChat.getThreadId(), SaveMode.fls);
        } catch (NetworkException e) {
            throw new OtrException(e);
        }
        SSNManager.getInstance().setSessionOtrMode(account, user, abstractChat.getThreadId(), OtrMode.prefer);
        try {
            ConnectionManager.getInstance()
                    .sendStanza(abstractChat.getAccount(), abstractChat.createMessagePacket(msg));
        } catch (NetworkException e) {
            throw new OtrException(e);
        }
    }

    @Override
    public void unreadableMessageReceived(SessionID sessionID) throws OtrException {
        LogManager.i(this, "unreadableMessageReceived");
        newAction(sessionID.getAccountID(), sessionID.getUserID(), null, ChatAction.otr_unreadable);
    }

    /**
     * Creates new action in specified chat.
     */
    private void newAction(String account, String user, String text, ChatAction action) {
        LogManager.i(this, "newAction. text: " + text + " action " + action);
        MessageManager.getInstance().getChat(account, user).newAction(null, text, action);
    }

    @Override
    public String getReplyForUnreadableMessage(SessionID sessionID) {
        return Application.getInstance().getString(R.string.otr_unreadable_message);
    }

    @Override
    public void unencryptedMessageReceived(SessionID sessionID, String msg) throws OtrException {
        LogManager.i(this, "unencrypted Message Received. " + msg);
        throw new OtrException(new OTRUnencryptedException(msg));
    }

    @Override
    public void showError(SessionID sessionID, String error) throws OtrException {
        LogManager.i(this, "ShowError: " + error);
        newAction(sessionID.getAccountID(), sessionID.getUserID(), error, ChatAction.otr_error);
    }

    @Override
    public void smpError(SessionID sessionID, int tlvType, boolean cheated) throws OtrException {
        newAction(sessionID.getAccountID(), sessionID.getUserID(), null,
                cheated ? ChatAction.otr_smp_cheated : ChatAction.otr_smp_failed);
        if (cheated) {
            removeSMProgress(sessionID.getAccountID(), sessionID.getUserID());
        }
    }

    @Override
    public void smpAborted(SessionID sessionID) throws OtrException {
        removeSMRequest(sessionID.getAccountID(), sessionID.getUserID());
        removeSMProgress(sessionID.getAccountID(), sessionID.getUserID());
    }

    @Override
    public void finishedSessionMessage(SessionID sessionID, String msgText) throws OtrException {
        newAction(sessionID.getAccountID(), sessionID.getUserID(), null, ChatAction.otr_finished_session);
        throw new OtrException(new IllegalStateException(
                        "Prevent from null to be returned. Just process it as regular exception."));
    }

    @Override
    public void requireEncryptedMessage(SessionID sessionID, String msgText) throws OtrException {
        throw new OtrException(new IllegalStateException(
                        "Prevent from null to be returned. Just process it as regular exception."));
    }

    @Override
    public OtrPolicy getSessionPolicy(SessionID sessionID) {
        return POLICIES.get(SettingsManager.securityOtrMode());
    }

    private KeyPair getLocalKeyPair(String account) throws OtrException {
        KeyPair keyPair = AccountManager.getInstance().getAccount(account).getKeyPair();
        if (keyPair == null) {
            throw new OtrException(new IllegalStateException("KeyPair is not ready, yet."));
        }
        return keyPair;
    }

    @Override
    public KeyPair getLocalKeyPair(SessionID sessionID) throws OtrException {
        return getLocalKeyPair(sessionID.getAccountID());
    }

    @Override
    public void sessionStatusChanged(SessionID sessionID) {
        removeSMRequest(sessionID.getAccountID(), sessionID.getUserID());
        removeSMProgress(sessionID.getAccountID(), sessionID.getUserID());
        Session session = sessions.get(sessionID.getAccountID(), sessionID.getUserID());
        SessionStatus sStatus = session.getSessionStatus();

        LogManager.i(this, "session status changed " + sessionID.getUserID() + " status: " + sStatus);

        if (sStatus == SessionStatus.ENCRYPTED) {
            finished.remove(sessionID.getAccountID(), sessionID.getUserID());
            PublicKey remotePublicKey = session.getRemotePublicKey();
            String value;
            try {
                value = OtrCryptoEngine.getFingerprint(remotePublicKey);
            } catch (OtrCryptoException e) {
                LogManager.exception(this, e);
                value = null;
            }
            if (value != null) {
                actives.put(sessionID.getAccountID(), sessionID.getUserID(), value);
                if (fingerprints.get(sessionID.getAccountID(), sessionID.getUserID(), value) == null) {
                    fingerprints.put(sessionID.getAccountID(), sessionID.getUserID(), value, false);
                    requestToWrite(sessionID.getAccountID(), sessionID.getUserID(), value, false);
                }
            }
            newAction(sessionID.getAccountID(), sessionID.getUserID(), null, isVerified(sessionID.getAccountID(),
                    sessionID.getUserID()) ? ChatAction.otr_verified : ChatAction.otr_encryption);
            MessageManager.getInstance()
                    .getChat(sessionID.getAccountID(), sessionID.getUserID()).sendMessages();
        } else if (sStatus == SessionStatus.PLAINTEXT) {
            actives.remove(sessionID.getAccountID(), sessionID.getUserID());
            sessions.remove(sessionID.getAccountID(), sessionID.getUserID());
            finished.remove(sessionID.getAccountID(), sessionID.getUserID());
            try {
                session.endSession();
            } catch (OtrException e) {
                LogManager.exception(this, e);
            }
            newAction(sessionID.getAccountID(), sessionID.getUserID(), null, ChatAction.otr_plain);
        } else if (sStatus == SessionStatus.FINISHED) {
            actives.remove(sessionID.getAccountID(), sessionID.getUserID());
            sessions.remove(sessionID.getAccountID(), sessionID.getUserID());
            finished.put(sessionID.getAccountID(), sessionID.getUserID(), true);
            newAction(sessionID.getAccountID(), sessionID.getUserID(), null, ChatAction.otr_finish);
        } else {
            throw new IllegalStateException();
        }
        RosterManager.getInstance().onContactChanged(sessionID.getAccountID(), sessionID.getUserID());
    }

    @Override
    public void askForSecret(SessionID sessionID, InstanceTag receiverTag, String question) {
        smRequestProvider.add(new SMRequest(sessionID.getAccountID(), sessionID.getUserID(), question), true);
    }

    /**
     * Transform outgoing message before sending.
     */
    public String transformSending(String account, String user, String content) throws OtrException {
        LogManager.i(this, "transform outgoing message... " + user);
        String parts[] = getOrCreateSession(account, user).transformSending(content, null);
        if (BuildConfig.DEBUG && parts.length != 1) {
            throw new RuntimeException(
            "We do not use fragmentation, so there must be only one otr fragment.");
        }
        return parts[0];
    }

    /**
     * Transform incoming message after receiving.
     */
    public String transformReceiving(String account, String user, String content) throws OtrException {
        LogManager.i(this, "transform incoming message... " + content);
        Session session = getOrCreateSession(account, user);
        try {
            String s = session.transformReceiving(content);
            LogManager.i(this, "transformed incoming message: " + s + " session status: " + session.getSessionStatus());
            return s;
        } catch (UnsupportedOperationException e) {
            throw new OtrException(e);
        }
    }

    public SecurityLevel getSecurityLevel(String account, String user) {
        if (actives.get(account, user) == null) {
            if (finished.get(account, user) == null) {
                return SecurityLevel.plain;
            } else {
                return SecurityLevel.finished;
            }
        } else {
            if (isVerified(account, user)) {
                return SecurityLevel.verified;
            } else {
                return SecurityLevel.encrypted;
            }
        }
    }

    public boolean isVerified(String account, String user) {
        String active = actives.get(account, user);
        if (active == null) {
            return false;
        }
        Boolean value = fingerprints.get(account, user, active);
        return value != null && value;
    }

    private void setVerifyWithoutNotification(String account, String user, String fingerprint, boolean value) {
        fingerprints.put(account, user, fingerprint, value);
        requestToWrite(account, user, fingerprint, value);
    }

    /**
     * Set whether fingerprint was verified. Add action to the chat history.
     */
    public void setVerify(String account, String user, String fingerprint, boolean value) {
        setVerifyWithoutNotification(account, user, fingerprint, value);
        if (value) {
            newAction(account, user, null, ChatAction.otr_smp_verified);
        } else if (actives.get(account, user) != null) {
            newAction(account, user, null, ChatAction.otr_encryption);
        }
    }

    private void setVerify(SessionID sessionID, boolean value) {
        String active = actives.get(sessionID.getAccountID(), sessionID.getUserID());
        if (active == null) {
            LogManager.exception(this, new IllegalStateException("There is no active fingerprint"));
            return;
        }
        setVerifyWithoutNotification(sessionID.getAccountID(), sessionID.getUserID(), active, value);
        newAction(sessionID.getAccountID(), sessionID.getUserID(), null,
                value ? ChatAction.otr_smp_verified : ChatAction.otr_smp_unverified);
        RosterManager.getInstance().onContactChanged(sessionID.getAccountID(), sessionID.getUserID());
    }

    @Override
    public void verify(SessionID sessionID, String fingerprint, boolean approved) {
        if (approved) {
            setVerify(sessionID, true);
        } else if (isVerified(sessionID.getAccountID(), sessionID.getUserID())) {
            newAction(sessionID.getAccountID(), sessionID.getUserID(), null, ChatAction.otr_smp_not_approved);
        }
        removeSMProgress(sessionID.getAccountID(), sessionID.getUserID());
    }

    @Override
    public void unverify(SessionID sessionID, String fingerprint) {
        setVerify(sessionID, false);
        removeSMProgress(sessionID.getAccountID(), sessionID.getUserID());
    }

    public String getRemoteFingerprint(String account, String user) {
        return actives.get(account, user);
    }

    public String getLocalFingerprint(String account) {
        try {
            return OtrCryptoEngine.getFingerprint(getLocalKeyPair(account).getPublic());
        } catch (OtrException e) {
            LogManager.exception(this, e);
        }
        return null;
    }

    @Override
    public byte[] getLocalFingerprintRaw(SessionID sessionID) {
        return SerializationUtils.hexStringToByteArray(getLocalFingerprint(sessionID.getAccountID()));
    }

    @Override
    public String getFallbackMessage(SessionID sessionID) {
        return Application.getInstance().getString(R.string.otr_request);
    }

    /**
     * Respond using SM protocol.
     */
    public void respondSmp(String account, String user, String question, String secret) throws NetworkException {
        LogManager.i(this, "responding smp... " + user);
        removeSMRequest(account, user);
        addSMProgress(account, user);
        try {
            getOrCreateSession(account, user).respondSmp(question, secret);
        } catch (OtrException e) {
            throw new NetworkException(R.string.OTR_ERROR, e);
        }
    }

    /**
     * Initiate request using SM protocol.
     */
    public void initSmp(String account, String user, String question, String secret) throws NetworkException {
        LogManager.i(this, "initializing smp... " + user);
        removeSMRequest(account, user);
        addSMProgress(account, user);
        try {
            getOrCreateSession(account, user).initSmp(question, secret);
        } catch (OtrException e) {
            throw new NetworkException(R.string.OTR_ERROR, e);
        }
    }

    /**
     * Abort SM negotiation.
     */
    public void abortSmp(String account, String user) throws NetworkException {
        LogManager.i(this, "aborting smp... " + user);
        removeSMRequest(account, user);
        removeSMProgress(account, user);
        try {
            getOrCreateSession(account, user).abortSmp();
        } catch (OtrException e) {
            throw new NetworkException(R.string.OTR_ERROR, e);
        }
    }

    private void removeSMRequest(String account, String user) {
        smRequestProvider.remove(account, user);
    }

    private void addSMProgress(String account, String user) {
        smProgressProvider.add(new SMProgress(account, user), false);
    }

    private void removeSMProgress(String account, String user) {
        smProgressProvider.remove(account, user);
    }

    @Override
    public void onAccountAdded(final AccountItem accountItem) {
        if (accountItem.getKeyPair() != null) {
            return;
        }
        keyPairGenerator.execute(new Runnable() {
            @Override
            public void run() {
                LogManager.i(this, "KeyPair generation started for " + accountItem.getAccount());
                final KeyPair keyPair;
                try {
                    keyPair = KeyPairGenerator.getInstance("DSA").genKeyPair();
                } catch (final NoSuchAlgorithmException e) {
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            throw new RuntimeException(e);
                        }
                    });
                    return;
                }
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LogManager.i(this, "KeyPair generation finished for " + accountItem.getAccount());
                        if (AccountManager.getInstance().getAccount(accountItem.getAccount()) != null) {
                            AccountManager.getInstance().setKeyPair(accountItem.getAccount(), keyPair);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        fingerprints.clear(accountItem.getAccount());
        actives.clear(accountItem.getAccount());
        finished.clear(accountItem.getAccount());
        sessions.clear(accountItem.getAccount());
    }

    /**
     * Save chat specific otr settings.
     */
    private void requestToWrite(final String account, final String user,
                                final String fingerprint, final boolean verified) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                OTRTable.getInstance().write(account, user, fingerprint, verified);
            }
        });
    }

    private void endAllSessions() {
        LogManager.i(this, "End all sessions");
        NestedMap<String> entities = new NestedMap<>();
        entities.addAll(actives);
        for (Entry<String> entry : entities) {
            try {
                endSession(entry.getFirst(), entry.getSecond());
            } catch (NetworkException e) {
                LogManager.exception(this, e);
            }
        }
    }

    @Override
    public void onClose() {
        endAllSessions();
    }

    public void onSettingsChanged() {
        if (SettingsManager.securityOtrMode() == SecurityOtrMode.disabled) {
            endAllSessions();
        }
    }

    @Override
    public int getMaxFragmentSize(SessionID sessionID) {
        // we do not want fragmentation
        return Integer.MAX_VALUE;
    }

    @Override
    public void outgoingSessionChanged(SessionID sessionID) {
        LogManager.i(this, "Outgoing session change with SessionID " + sessionID);
        // TODO what to in this situation?
    }

    @Override
    public void messageFromAnotherInstanceReceived(SessionID sessionID) {
        LogManager.i(this, "Message from another instance received on SessionID "
                + sessionID + ". Restarting OTR session for this user.");
        newAction(sessionID.getAccountID(), sessionID.getUserID(), null, ChatAction.otr_unreadable);
    }

    @Override
    public void multipleInstancesDetected(SessionID sessionID) {
        LogManager.i(this, "Multiple instances detected on SessionID " + sessionID);
        // since this is not supported, we don't need to do anything
    }

    public void onContactUnAvailable(String account, String user) {
        Session session = sessions.get(account, user);

        if (session == null) {
            return;
        }

        if (session.getSessionStatus() == SessionStatus.ENCRYPTED) {
            try {
                LogManager.i(this, "onContactUnAvailable. Refresh session for " + user);
                session.refreshSession();
            } catch (OtrException e) {
                LogManager.exception(this, e);
            }
        }
    }
}
