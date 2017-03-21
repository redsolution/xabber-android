package com.xabber.android.data.extension.blocking;

import android.support.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockingManager implements OnAuthorizedListener {

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
    }



    @Override
    public void onAuthorized(final ConnectionItem connection) {
        final AccountJid account = connection.getAccount();

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {

                BlockingCommandManager blockingCommandManager = BlockingCommandManager.getInstanceFor(connection.getConnection());

                try {
                    boolean supportedByServer = blockingCommandManager.isSupportedByServer();

                    if (supportedByServer) {
                        // cache block list inside
                        blockingCommandManager.getBlockList();
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
        });
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
        return supportForAccounts.get(account);
    }

    public List<UserJid> getBlockedContacts(AccountJid account) {
        List<UserJid> blockedContacts = new ArrayList<>();

        Boolean supported = isSupported(account);

        if (supported != null && supported) {
            try {
                List<Jid> blockedJids = getBlockingCommandManager(account).getBlockList();
                for (Jid jid : blockedJids) {
                    blockedContacts.add(UserJid.from(jid));
                }

            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                    | InterruptedException | SmackException.NotConnectedException | UserJid.UserJidCreateException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }

        return blockedContacts;
    }

    public interface BlockContactListener {
        void onSuccess();
        void onError();
    }

    public void blockContact(final AccountJid account, final UserJid contactJid, final BlockContactListener listener) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {

                BlockingCommandManager blockingCommandManager = getBlockingCommandManager(account);

                List<Jid> contactsToBlock = new ArrayList<>();
                contactsToBlock.add(contactJid.getJid());
                boolean success = false;

                try {
                    blockingCommandManager.blockContacts(contactsToBlock);
                    success = true;
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                        | InterruptedException | SmackException.NotConnectedException e) {
                    LogManager.exception(LOG_TAG, e);
                }

                final boolean finalSuccess = success;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            listener.onSuccess();
                        } else {
                            listener.onError();
                        }
                    }
                });
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    BlockingCommandManager getBlockingCommandManager(AccountJid account) {
        return BlockingCommandManager.getInstanceFor(AccountManager.getInstance().getAccount(account).getConnection());
    }

    static void blockContactLocally(AccountJid account, UserJid contactJid) {
        MessageManager.getInstance().closeChat(account, contactJid);
        NotificationManager.getInstance().removeMessageNotification(account, contactJid);
    }

    public interface UnblockContactListener {
        void onSuccess();
        void onError();
    }

    public void unblockContacts(final AccountJid account, final List<UserJid> contacts, final UnblockContactListener listener) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                BlockingCommandManager blockingCommandManager = getBlockingCommandManager(account);
                List<Jid> jidsToUnblock = new ArrayList<>(contacts.size());
                for (UserJid userJid : contacts) {
                    jidsToUnblock.add(userJid.getBareJid());
                }

                boolean success = false;
                try {
                    blockingCommandManager.unblockContacts(jidsToUnblock);
                    success = true;
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                        | SmackException.NotConnectedException | InterruptedException e) {
                    LogManager.exception(LOG_TAG, e);
                }

                final boolean finalSuccess = success;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            listener.onSuccess();
                        } else {
                            listener.onError();
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
                BlockingCommandManager blockingCommandManager = getBlockingCommandManager(account);

                boolean success = false;

                try {
                    blockingCommandManager.unblockAll();
                    success = true;
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                        | SmackException.NotConnectedException | InterruptedException e) {
                    LogManager.exception(LOG_TAG, e);
                }

                final boolean finalSuccess = success;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            listener.onSuccess();
                        } else {
                            listener.onError();
                        }
                    }
                });

            }
        });
    }

}
