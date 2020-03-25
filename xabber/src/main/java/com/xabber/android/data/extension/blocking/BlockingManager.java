package com.xabber.android.data.extension.blocking;

import androidx.annotation.Nullable;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockingManager {

    static final String LOG_TAG = BlockingManager.class.getSimpleName();
    private static BlockingManager instance;

    @SuppressWarnings("WeakerAccess")
    Map<AccountJid, Boolean> supportForAccounts;

    @SuppressWarnings("WeakerAccess")
    Map<AccountJid, BlockedListener> blockedListeners;
    @SuppressWarnings("WeakerAccess")
    Map<AccountJid, UnblockedListener> unblockedListeners;
    @SuppressWarnings("WeakerAccess")
    Map<AccountJid, UnblockedAllListener> unblockedAllListeners;

    private Map<AccountJid, List<ContactJid>> cachedBlockedContacts;

    public static BlockingManager getInstance() {
        if (instance == null) {
            instance = new BlockingManager();
        }

        return instance;
    }

    private BlockingManager() {
        supportForAccounts = new ConcurrentHashMap<>();

        blockedListeners = new ConcurrentHashMap<>();
        unblockedListeners = new ConcurrentHashMap<>();
        unblockedAllListeners = new ConcurrentHashMap<>();
        cachedBlockedContacts = new HashMap<>();
    }

    public void onAuthorized(final ConnectionItem connection) {
        final AccountJid account = connection.getAccount();

        BlockingCommandManager blockingCommandManager = BlockingCommandManager.getInstanceFor(connection.getConnection());

        try {
            boolean supportedByServer = blockingCommandManager.isSupportedByServer();

            if (supportedByServer) {
                // cache block list inside
                List<ContactJid> blockedContacts = new ArrayList<>();
                try {
                    List<Jid> blockedJids = blockingCommandManager.getBlockList();
                    for (Jid jid : blockedJids) {
                        blockedContacts.add(ContactJid.from(jid));
                    }

                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                        | InterruptedException | SmackException.NotConnectedException | ContactJid.UserJidCreateException e) {
                    LogManager.exception(LOG_TAG, e);
                }
                // Cache block inside manager
                // For contact list building used only this list of blocked contacts
                updateCachedBlockedContacts(account, blockedContacts);
            }

            addBlockedListener(blockingCommandManager, account);
            addUnblockedListener(blockingCommandManager, account);
            addUnblockedAllListener(blockingCommandManager, account);

            // block list already cached successfully
            supportForAccounts.put(account, supportedByServer);

            BlockingManager.notify(account);

        } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException
                | SmackException.NoResponseException | InterruptedException e) {
            LogManager.exception(this, e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    void addBlockedListener(BlockingCommandManager blockingCommandManager, AccountJid account) {
        BlockedListener blockedListener = blockedListeners.remove(account);
        if (blockedListener != null) {
            blockingCommandManager.removeJidsBlockedListener(blockedListener);
        }

        blockedListener = new BlockedListener(account);
        blockedListeners.put(account, blockedListener);
        blockingCommandManager.addJidsBlockedListener(blockedListener);
    }

    @SuppressWarnings("WeakerAccess")
    void addUnblockedListener(BlockingCommandManager blockingCommandManager, AccountJid account) {
        UnblockedListener unblockedListener = unblockedListeners.remove(account);
        if (unblockedListener != null) {
            blockingCommandManager.removeJidsUnblockedListener(unblockedListener);
        }

        unblockedListener = new UnblockedListener(account);
        unblockedListeners.put(account, unblockedListener);
        blockingCommandManager.addJidsUnblockedListener(unblockedListener);
    }

    @SuppressWarnings("WeakerAccess")
    void addUnblockedAllListener(BlockingCommandManager blockingCommandManager, AccountJid account) {
        UnblockedAllListener unblockedAllListener = unblockedAllListeners.remove(account);
        if (unblockedAllListener != null) {
            blockingCommandManager.removeAllJidsUnblockedListener(unblockedAllListener);
        }

        unblockedAllListener = new UnblockedAllListener(account);
        unblockedAllListeners.put(account, unblockedAllListener);
        blockingCommandManager.addAllJidsUnblockedListener(unblockedAllListener);
    }

    static void notify(final AccountJid account) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnBlockedListChangedListener onBlockedListChangedListener
                        : Application.getInstance().getUIListeners(OnBlockedListChangedListener.class)) {
                    onBlockedListChangedListener.onBlockedListChanged(account);
                }

                for (OnContactChangedListener onContactChangedListener
                        : Application.getInstance().getUIListeners(OnContactChangedListener.class)) {
                    onContactChangedListener.onContactsChanged(new ArrayList<RosterContact>());
                }
            }
        });
    }

    /**
     *
     * @param account
     * @return true if supported, false if not supported and null if unknown yet
     */
    @Nullable
    public Boolean isSupported(AccountJid account) {
        if (getBlockingCommandManager(account) == null) {
            supportForAccounts.remove(account);
            return null;
        }

        return supportForAccounts.get(account);
    }

    public List<ContactJid> getCachedBlockedContacts(AccountJid account) {
        if (cachedBlockedContacts.get(account) == null)
            return new ArrayList<>();
        else return cachedBlockedContacts.get(account);
    }

    private void updateCachedBlockedContacts(AccountJid account, List<ContactJid> blockedContacts) {
        cachedBlockedContacts.remove(account);
        cachedBlockedContacts.put(account, blockedContacts);
    }

    public boolean contactIsBlockedLocally(AccountJid account, ContactJid contactJid) {
        Collection<ContactJid> cachedBlockedContacts = getCachedBlockedContacts(account);
        for (ContactJid blockedContact : cachedBlockedContacts) {
            if (blockedContact.getJid().equals(contactJid.getBareJid())) {
                return true;
            }
        }
        return false;
    }

    public boolean contactIsBlocked(AccountJid account, ContactJid user) {
        Collection<ContactJid> blockedContacts = getBlockedContacts(account);
        for (ContactJid blockedContact : blockedContacts) {
            // we specifically check for the blocked contact's full jid
            // to filter out jids blocked as a group-invite.
            if (blockedContact.getJid().equals(user.getBareJid())) {
                return true;
            }
        }
        return  false;
    }

    public List<ContactJid> getBlockedContacts(AccountJid account) {
        List<ContactJid> blockedContacts = new ArrayList<>();

        Boolean supported = isSupported(account);

        BlockingCommandManager blockingCommandManager = getBlockingCommandManager(account);

        if (blockingCommandManager != null && supported != null && supported) {
            try {
                List<Jid> blockedJids = blockingCommandManager.getBlockList();
                for (Jid jid : blockedJids) {
                    blockedContacts.add(ContactJid.from(jid));
                }

            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                    | InterruptedException | SmackException.NotConnectedException | ContactJid.UserJidCreateException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }

        updateCachedBlockedContacts(account, blockedContacts);
        return blockedContacts;
    }

    public interface BlockContactListener {
        void onSuccessBlock();
        void onErrorBlock();
    }

    public void blockContact(final AccountJid account, final ContactJid contactJid, final BlockContactListener listener) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                boolean success = false;

                BlockingCommandManager blockingCommandManager = getBlockingCommandManager(account);

                if (blockingCommandManager != null) {
                    List<Jid> contactsToBlock = new ArrayList<>();
                    contactsToBlock.add(contactJid.getJid());

                    try {
                        blockingCommandManager.blockContacts(contactsToBlock);
                        success = true;
                    } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                            | InterruptedException | SmackException.NotConnectedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }

                if (success) {
                    PresenceManager.getInstance().clearSingleContactPresences(account, contactJid.getBareJid());
                    AbstractChat chat = MessageManager.getInstance().getChat(account, contactJid);
                    if (chat != null) {
                        chat.newAction(null, Application.getInstance().getString(R.string.action_contact_blocked), ChatAction.contact_blocked, false);
                    }
                }

                final boolean finalSuccess = success;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            cachedBlockedContacts.get(account).add(contactJid);
                            listener.onSuccessBlock();
                        } else {
                            listener.onErrorBlock();
                        }
                    }
                });
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    BlockingCommandManager getBlockingCommandManager(AccountJid account) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            return null;
        }

        XMPPTCPConnection connection = accountItem.getConnection();

        // "Must have a local (user) JID set. Either you didn't configure one or you where not connected at least once"
        if (connection.getUser() == null) {
            return null;
        }

        return BlockingCommandManager.getInstanceFor(connection);
    }

    static void blockContactLocally(AccountJid account, ContactJid contactJid) {
        MessageManager.getInstance().closeChat(account, contactJid);
        NotificationManager.getInstance().removeMessageNotification(account, contactJid);
    }

    public interface UnblockContactListener {
        void onSuccessUnblock();
        void onErrorUnblock();
    }

    public void unblockContacts(final AccountJid account, final List<ContactJid> contacts, final UnblockContactListener listener) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                boolean success = false;

                BlockingCommandManager blockingCommandManager = getBlockingCommandManager(account);

                if (blockingCommandManager != null) {
                    List<Jid> jidsToUnblock = new ArrayList<>(contacts.size());
                    for (ContactJid contactJid : contacts) {
                        jidsToUnblock.add(contactJid.getJid());
                    }


                    try {
                        blockingCommandManager.unblockContacts(jidsToUnblock);
                        success = true;
                    } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                            | SmackException.NotConnectedException | InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }

                if (success) {
                    for (ContactJid contactJid : contacts) {
                        AbstractChat chat = MessageManager.getInstance().getChat(account, contactJid);
                        if (chat != null) {
                            chat.newAction(null, Application.getInstance().getString(R.string.action_contact_unblocked), ChatAction.contact_unblocked, false);
                        }
                    }
                }

                final boolean finalSuccess = success;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            if (cachedBlockedContacts.get(account) != null) {
                                cachedBlockedContacts.get(account).removeAll(contacts);
                            }
                            listener.onSuccessUnblock();
                        } else {
                            listener.onErrorUnblock();
                        }
                    }
                });

            }
        });
    }

    public void unblockAll(final AccountJid account, final UnblockContactListener listener) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                boolean success = false;

                BlockingCommandManager blockingCommandManager = getBlockingCommandManager(account);

                if (blockingCommandManager != null) {
                    try {
                        blockingCommandManager.unblockAll();
                        success = true;
                    } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                            | SmackException.NotConnectedException | InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
                    }
                }

                final boolean finalSuccess = success;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            if (cachedBlockedContacts.get(account) != null) {
                                cachedBlockedContacts.get(account).clear();
                            }
                            listener.onSuccessUnblock();
                        } else {
                            listener.onErrorUnblock();
                        }
                    }
                });

            }
        });
    }

}
