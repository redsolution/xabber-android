package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.connection.listeners.OnResponseListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.xmpp.blocking.Block;
import com.xabber.xmpp.blocking.BlockList;
import com.xabber.xmpp.blocking.Unblock;
import com.xabber.xmpp.blocking.XmlConstants;

import org.jivesoftware.smack.SmackException;
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


    private final static BlockingManager instance;

    private Map<AccountJid, Boolean> supportForAccounts;
    private Map<AccountJid, List<UserJid>> blockListsForAccounts;

    static {
        instance = new BlockingManager();
        Application.getInstance().addManager(instance);
    }

    public static BlockingManager getInstance() {
        return instance;
    }

    private BlockingManager() {
        supportForAccounts = new ConcurrentHashMap<>();
        blockListsForAccounts = new ConcurrentHashMap<>();
    }

    private void discoverSupport(AccountJid account, XMPPConnection xmppConnection)
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

    public void requestBlockList(AccountJid account) {
        if (!isSupported(account)) {
            return;
        }

        final BlockList blockListRequest  = new BlockList();
        blockListRequest.setType(IQ.Type.get);

        try {
            ConnectionManager.getInstance().sendRequest(account, blockListRequest, new OnResponseListener() {

                @Override
                public void onReceived(AccountJid account, String packetId, IQ iq) {
                    if (!blockListRequest.getStanzaId().equals(packetId) || !(iq instanceof BlockList)) {
                        return;
                    }

                    if (iq.getType() == IQ.Type.result) {
                        List<Jid> items = ((BlockList) iq).getItems();
                        List<UserJid> userJids = new ArrayList<>();
                        for (Jid jid : items) {
                            try {
                                userJids.add(UserJid.from(jid));
                            } catch (UserJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }
                        }

                        blockListsForAccounts.put(account, userJids);
                        for (OnBlockedListChangedListener onBlockedListChangedListener
                                : Application.getInstance().getUIListeners(OnBlockedListChangedListener.class)) {
                            onBlockedListChangedListener.onBlockedListChanged(account);
                        }

                        for (OnContactChangedListener onContactChangedListener
                                : Application.getInstance().getUIListeners(OnContactChangedListener.class)) {
                            onContactChangedListener.onContactsChanged(new ArrayList<BaseEntity>());
                        }
                    }
                }

                @Override
                public void onError(AccountJid account, String packetId, IQ iq) {

                }

                @Override
                public void onTimeout(AccountJid account, String packetId) {

                }

                @Override
                public void onDisconnect(AccountJid account, String packetId) {

                }
            });
        } catch (NetworkException e) {
            LogManager.exception(this, e);
        }

    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        AccountJid account = ((AccountItem) connection).getAccount();

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

    public void blockContact(AccountJid account, final UserJid contactJid, final BlockContactListener listener) {
        final Block blockRequest  = new Block();
        blockRequest.setType(IQ.Type.set);
        blockRequest.addItem(contactJid.getJid());

        try {
            ConnectionManager.getInstance().sendRequest(account, blockRequest, new OnResponseListener() {

                @Override
                public void onReceived(AccountJid account, String packetId, IQ iq) {
                    if (!blockRequest.getStanzaId().equals(packetId)) {
                        return;
                    }

                    if (iq.getType() == IQ.Type.result) {
                        requestBlockList(account);
                        listener.onSuccess();
                    } else {
                        listener.onError();
                    }
                }

                @Override
                public void onError(AccountJid account, String packetId, IQ iq) {
                    listener.onError();
                }

                @Override
                public void onTimeout(AccountJid account, String packetId) {
                    listener.onError();
                }

                @Override
                public void onDisconnect(AccountJid account, String packetId) {
                    listener.onError();
                }
            });
        } catch (NetworkException e) {
            LogManager.exception(this, e);
            listener.onError();
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

    private void sendUnblock(AccountJid account, final UnblockContactListener listener, final Unblock unblockRequest) {
        try {
            ConnectionManager.getInstance().sendRequest(account, unblockRequest, new OnResponseListener() {

                @Override
                public void onReceived(AccountJid account, String packetId, IQ iq) {
                    if (!unblockRequest.getStanzaId().equals(packetId)) {
                        return;
                    }

                    if (iq.getType() == IQ.Type.result) {
                        requestBlockList(account);
                        listener.onSuccess();
                    } else {
                        listener.onError();
                    }
                }

                @Override
                public void onError(AccountJid account, String packetId, IQ iq) {
                    listener.onError();
                }

                @Override
                public void onTimeout(AccountJid account, String packetId) {
                    listener.onError();
                }

                @Override
                public void onDisconnect(AccountJid account, String packetId) {
                    listener.onError();
                }
            });
        } catch (NetworkException e) {
            LogManager.exception(this, e);
            listener.onError();
        }
    }

    @Override
    public void onAuthorized(final ConnectionItem connection) {
        final AccountJid account = connection.getAccount();

        new Thread("Thread to check " + connection.getRealJid() + " for blocking command support") {
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
        }.start();
    }
}
