package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnAuthorizedListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.connection.OnResponseListener;
import com.xabber.android.data.entity.BaseEntity;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockingManager implements OnAuthorizedListener, OnPacketListener {


    private final static BlockingManager instance;

    private Map<String, Boolean> supportForAccounts;
    private Map<String, List<String>> blockListsForAccounts;

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

    private void discoverSupport(XMPPConnection xmppConnection)
            throws SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NoResponseException {
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);
        final String account = xmppConnection.getUser();
        final boolean isSupported = discoManager.serverSupportsFeature(XmlConstants.NAMESPACE);

        if (isSupported) {
            LogManager.i(this, "Blocking is supported for account" + account);
        }

        supportForAccounts.put(account, isSupported);
    }

    public boolean isSupported(String account) {
        final Boolean isSupported = supportForAccounts.get(account);
        if (isSupported == null) {
            return false;
        }
        return isSupported;
    }

    public Map<String, List<String>> getBlockedContacts() {
        return Collections.unmodifiableMap(blockListsForAccounts);
    }

    public Collection<String> getBlockedContacts(String account) {

        return Collections.unmodifiableCollection(getBlockedListForAccount(account));
    }

    private List<String> getBlockedListForAccount(String account) {
        if (!blockListsForAccounts.containsKey(account)) {
            blockListsForAccounts.put(account, new ArrayList<String>());
        }
        return blockListsForAccounts.get(account);
    }

    public void requestBlockList(String account) {
        if (!isSupported(account)) {
            return;
        }

        final BlockList blockListRequest  = new BlockList();
        blockListRequest.setType(IQ.Type.get);

        try {
            ConnectionManager.getInstance().sendRequest(account, blockListRequest, new OnResponseListener() {

                @Override
                public void onReceived(String account, String packetId, IQ iq) {
                    if (!blockListRequest.getStanzaId().equals(packetId) || !(iq instanceof BlockList)) {
                        return;
                    }

                    if (iq.getType() == IQ.Type.result) {
                        blockListsForAccounts.put(account, ((BlockList) iq).getItems());
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
                public void onError(String account, String packetId, IQ iq) {

                }

                @Override
                public void onTimeout(String account, String packetId) {

                }

                @Override
                public void onDisconnect(String account, String packetId) {

                }
            });
        } catch (NetworkException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress, Stanza packet) {
        if (packet instanceof Block && ((Block) packet).getType() == IQ.Type.set) {
            LogManager.i(this, "Block push received");
            Block block = (Block) packet;
            for (String contact : block.getItems()) {
                blockContactLocally(packet.getTo(), contact);
            }
            requestBlockList(packet.getTo());
        }

        if (packet instanceof Unblock && ((Unblock) packet).getType() == IQ.Type.set) {
            LogManager.i(this, "Unblock push received");
            requestBlockList(packet.getTo());
        }
    }

    public interface BlockContactListener {
        void onSuccess();
        void onError();
    }

    public void blockContact(String account, final String contactJid, final BlockContactListener listener) {
        final Block blockRequest  = new Block();
        blockRequest.setType(IQ.Type.set);
        blockRequest.addItem(contactJid);

        try {
            ConnectionManager.getInstance().sendRequest(account, blockRequest, new OnResponseListener() {

                @Override
                public void onReceived(String account, String packetId, IQ iq) {
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
                public void onError(String account, String packetId, IQ iq) {
                    listener.onError();
                }

                @Override
                public void onTimeout(String account, String packetId) {
                    listener.onError();
                }

                @Override
                public void onDisconnect(String account, String packetId) {
                    listener.onError();
                }
            });
        } catch (NetworkException e) {
            e.printStackTrace();
            listener.onError();
        }

    }

    private void blockContactLocally(String account, String contactJid) {
        MessageManager.getInstance().closeChat(account, contactJid);
        NotificationManager.getInstance().removeMessageNotification(account, contactJid);
    }

    public interface UnblockContactListener {
        void onSuccess();
        void onError();
    }

    public void unblockContacts(String account, final List<String> contacts, final UnblockContactListener listener) {
        if (!isSupported(account)) {
            return;
        }

        final Unblock unblockRequest  = new Unblock();
        unblockRequest.setType(IQ.Type.set);
        for (String contact : contacts) {
            unblockRequest.addItem(contact);
        }

        sendUnblock(account, listener, unblockRequest);
    }

    public void unblockAll(String account, final UnblockContactListener listener) {
        unblockContacts(account, blockListsForAccounts.get(account), listener);
    }

    private void sendUnblock(String account, final UnblockContactListener listener, final Unblock unblockRequest) {
        try {
            ConnectionManager.getInstance().sendRequest(account, unblockRequest, new OnResponseListener() {

                @Override
                public void onReceived(String account, String packetId, IQ iq) {
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
                public void onError(String account, String packetId, IQ iq) {
                    listener.onError();
                }

                @Override
                public void onTimeout(String account, String packetId) {
                    listener.onError();
                }

                @Override
                public void onDisconnect(String account, String packetId) {
                    listener.onError();
                }
            });
        } catch (NetworkException e) {
            e.printStackTrace();
            listener.onError();
        }
    }

    @Override
    public void onAuthorized(final ConnectionItem connection) {
        new Thread("Thread to check " + connection.getRealJid() + " for blocking command support") {
            @Override
            public void run() {
                try {
                    discoverSupport(connection.getConnectionThread().getXMPPConnection());
                    requestBlockList(connection.getConnectionThread().getXMPPConnection().getUser());
                } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
