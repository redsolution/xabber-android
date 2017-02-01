package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.xmpp.blocking.Block;
import com.xabber.xmpp.blocking.BlockList;
import com.xabber.xmpp.blocking.Unblock;
import com.xabber.xmpp.blocking.XmlConstants;

import org.jivesoftware.smack.ExceptionCallback;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockingManager implements OnAuthorizedListener, OnPacketListener {


    private static BlockingManager instance;

    private Map<AccountJid, Boolean> supportForAccounts;
    @SuppressWarnings("WeakerAccess")
    Map<AccountJid, List<UserJid>> blockListsForAccounts;

    public static BlockingManager getInstance() {
        if (instance == null) {
            instance = new BlockingManager();
        }

        return instance;
    }

    private BlockingManager() {
        supportForAccounts = new ConcurrentHashMap<>();
        blockListsForAccounts = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("WeakerAccess")
    void discoverSupport(AccountJid account, XMPPConnection xmppConnection)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);
        final boolean isSupported = discoManager.serverSupportsFeature(XmlConstants.NAMESPACE);

        if (isSupported) {
            LogManager.i(this, "Blocking is supported for account" + account);
        }

        supportForAccounts.put(account, isSupported);
    }

    public boolean isSupported(AccountJid account) {
        final Boolean isSupported = supportForAccounts.get(account);
        if (isSupported == null) {
            return false;
        }
        return isSupported;
    }

    public Map<AccountJid, List<UserJid>> getBlockedContacts() {
        return Collections.unmodifiableMap(blockListsForAccounts);
    }

    public Collection<UserJid> getBlockedContacts(AccountJid account) {

        return Collections.unmodifiableCollection(getBlockedListForAccount(account));
    }

    private List<UserJid> getBlockedListForAccount(AccountJid account) {
        if (!blockListsForAccounts.containsKey(account)) {
            blockListsForAccounts.put(account, new ArrayList<UserJid>());
        }
        return blockListsForAccounts.get(account);
    }

    @SuppressWarnings("WeakerAccess")
    void requestBlockList(final AccountJid account) {
        if (!isSupported(account)) {
            return;
        }

        final BlockList blockListRequest  = new BlockList();
        blockListRequest.setType(IQ.Type.get);

        try {
            AccountManager.getInstance().getAccount(account).getConnection().sendIqWithResponseCallback(blockListRequest, new StanzaListener() {
                @Override
                public void processStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
                    if (!(stanza instanceof BlockList)) {
                        return;
                    }

                    BlockList blockList = (BlockList) stanza;

                    if (blockList.getType() == IQ.Type.result) {
                        List<Jid> items = blockList.getItems();
                        final List<UserJid> userJids = new ArrayList<>();
                        for (Jid jid : items) {
                            try {
                                userJids.add(UserJid.from(jid));
                            } catch (UserJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }
                        }

                        Application.getInstance().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                blockListsForAccounts.put(account, userJids);
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
                }
            }, new ExceptionCallback() {
                @Override
                public void processException(Exception exception) {
                    LogManager.exception(this, exception);
                }
            });
        } catch (InterruptedException | SmackException.NotConnectedException e) {
            LogManager.exception(this, e);
        }

    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        AccountJid account = connection.getAccount();

        if (packet instanceof Block && ((Block) packet).getType() == IQ.Type.set) {
            LogManager.i(this, "Block push received");
            Block block = (Block) packet;
            for (Jid contact : block.getItems()) {

                try {
                    blockContactLocally(account, UserJid.from(contact));
                } catch (UserJid.UserJidCreateException e) {
                    LogManager.exception(this, e);
                }
            }
            requestBlockList(account);
        }

        if (packet instanceof Unblock && ((Unblock) packet).getType() == IQ.Type.set) {
            LogManager.i(this, "Unblock push received");
            requestBlockList(account);
        }
    }

    public interface BlockContactListener {
        void onSuccess();
        void onError();
    }

    public void blockContact(final AccountJid account, final UserJid contactJid, final BlockContactListener listener) {
        final Block blockRequest  = new Block();
        blockRequest.setType(IQ.Type.set);
        blockRequest.addItem(contactJid.getJid());

        try {
            AccountManager.getInstance().getAccount(account).getConnection()
                    .sendIqWithResponseCallback(blockRequest, new StanzaListener() {
                        @Override
                        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                            if (!(packet instanceof IQ)) {
                                return;
                            }

                            if (((IQ)packet).getType() == IQ.Type.result) {
                                requestBlockList(account);
                                Application.getInstance().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onSuccess();
                                    }
                                });
                            } else {
                                Application.getInstance().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onError();
                                    }
                                });

                            }
                        }
                    }, new ExceptionCallback() {
                        @Override
                        public void processException(Exception exception) {
                            LogManager.exception(this, exception);
                            Application.getInstance().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onError();
                                }
                            });
                        }
                    });
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LogManager.exception(this, e);
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onError();
                }
            });
        }
    }

    private void blockContactLocally(AccountJid account, UserJid contactJid) {
        MessageManager.getInstance().closeChat(account, contactJid);
        NotificationManager.getInstance().removeMessageNotification(account, contactJid);
    }

    public interface UnblockContactListener {
        void onSuccess();
        void onError();
    }

    public void unblockContacts(AccountJid account, final List<UserJid> contacts, final UnblockContactListener listener) {
        if (!isSupported(account)) {
            return;
        }

        final Unblock unblockRequest  = new Unblock();
        unblockRequest.setType(IQ.Type.set);
        for (UserJid contact : contacts) {
            unblockRequest.addItem(contact.getJid());
        }

        sendUnblock(account, listener, unblockRequest);
    }

    public void unblockAll(AccountJid account, final UnblockContactListener listener) {
        unblockContacts(account, blockListsForAccounts.get(account), listener);
    }

    private void sendUnblock(final AccountJid account, final UnblockContactListener listener, final Unblock unblockRequest) {
        try {
            AccountManager.getInstance().getAccount(account).getConnection()
                    .sendIqWithResponseCallback(unblockRequest, new StanzaListener() {
                        @Override
                        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                            if (!(packet instanceof IQ)) {
                                return;
                            }

                            if (((IQ)packet).getType() == IQ.Type.result) {
                                requestBlockList(account);
                                Application.getInstance().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onSuccess();
                                    }
                                });

                            } else {
                                Application.getInstance().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onError();
                                    }
                                });

                            }
                        }
                    }, new ExceptionCallback() {
                        @Override
                        public void processException(Exception exception) {
                            LogManager.exception(this, exception);
                            Application.getInstance().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onError();
                                }
                            });

                        }
                    });
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LogManager.exception(this, e);
            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onError();
                }
            });

        }
    }

    @Override
    public void onAuthorized(final ConnectionItem connection) {
        final AccountJid account = connection.getAccount();

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    discoverSupport(account, connection.getConnection());
                    requestBlockList(account);
                } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException
                        | SmackException.NoResponseException | InterruptedException e) {
                    LogManager.exception(this, e);
                }
            }
        });
    }
}
