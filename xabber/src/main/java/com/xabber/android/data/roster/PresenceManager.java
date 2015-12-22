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
package com.xabber.android.data.roster;

import android.support.annotation.Nullable;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountDisabledListener;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnAuthorizedListener;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.extension.archive.OnArchiveModificationsReceivedListener;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.Occupant;
import com.xabber.android.data.notification.EntityNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.avatar.VCardUpdate;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Process contact's presence information.
 *
 * @author alexander.ivanov
 */
public class PresenceManager implements OnArchiveModificationsReceivedListener,
        OnLoadListener, OnAccountDisabledListener, OnDisconnectListener, OnPacketListener, OnAuthorizedListener {

    private final static PresenceManager instance;

    static {
        instance = new PresenceManager();
        Application.getInstance().addManager(instance);
    }

    private final EntityNotificationProvider<SubscriptionRequest> subscriptionRequestProvider;
    /**
     * List of account with requested subscriptions for auto accept incoming
     * subscription request.
     */
    private final HashMap<String, HashSet<String>> requestedSubscriptions;
    /**
     * Account ready to send / update its presence information.
     */
    private final ArrayList<String> readyAccounts;

    private PresenceManager() {
        subscriptionRequestProvider = new EntityNotificationProvider<>(R.drawable.ic_stat_add_circle);
        requestedSubscriptions = new HashMap<>();
        readyAccounts = new ArrayList<>();
    }

    public static PresenceManager getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded();
            }
        });
    }

    private void onLoaded() {
        NotificationManager.getInstance().registerNotificationProvider(subscriptionRequestProvider);
    }

    /**
     * @param account
     * @param user
     * @return <code>null</code> can be returned.
     */
    public SubscriptionRequest getSubscriptionRequest(String account, String user) {
        return subscriptionRequestProvider.get(account, user);
    }

    /**
     * Requests subscription to the contact.
     *
     * @param account
     * @param bareAddress
     * @throws NetworkException
     */
    public void requestSubscription(String account, String bareAddress)
            throws NetworkException {
        Presence packet = new Presence(Presence.Type.subscribe);
        packet.setTo(bareAddress);
        ConnectionManager.getInstance().sendStanza(account, packet);
        HashSet<String> set = requestedSubscriptions.get(account);
        if (set == null) {
            set = new HashSet<>();
            requestedSubscriptions.put(account, set);
        }
        set.add(bareAddress);
    }

    private void removeRequestedSubscription(String account, String bareAddress) {
        HashSet<String> set = requestedSubscriptions.get(account);
        if (set != null) {
            set.remove(bareAddress);
        }
    }

    /**
     * Accepts subscription request from the entity (share own presence).
     *
     * @param account
     * @param bareAddress
     * @throws NetworkException
     */
    public void acceptSubscription(String account, String bareAddress) throws NetworkException {
        Presence packet = new Presence(Presence.Type.subscribed);
        packet.setTo(bareAddress);
        ConnectionManager.getInstance().sendStanza(account, packet);
        subscriptionRequestProvider.remove(account, bareAddress);
        removeRequestedSubscription(account, bareAddress);
    }

    /**
     * Discards subscription request from the entity (deny own presence
     * sharing).
     *
     * @param account
     * @param bareAddress
     * @throws NetworkException
     */
    public void discardSubscription(String account, String bareAddress) throws NetworkException {
        Presence packet = new Presence(Presence.Type.unsubscribed);
        packet.setTo(bareAddress);
        ConnectionManager.getInstance().sendStanza(account, packet);
        subscriptionRequestProvider.remove(account, bareAddress);
        removeRequestedSubscription(account, bareAddress);
    }

    public boolean hasSubscriptionRequest(String account, String bareAddress) {
        return getSubscriptionRequest(account, bareAddress) != null;
    }

    public StatusMode getStatusMode(String account, String bareAddress) {
        final Occupant occupant = getOccupant(account, Jid.getBareAddress(bareAddress));
        if (occupant != null) {
            return occupant.getStatusMode();
        }

        return StatusMode.createStatusMode(RosterManager.getInstance().getPresence(account, bareAddress));
    }

    /**
     * if contact is private MUC chat
     */
    @Nullable
    private Occupant getOccupant(String account, String bareAddress) {
        if (MUCManager.getInstance().hasRoom(account, Jid.getBareAddress(bareAddress))) {
            final Collection<Occupant> occupants = MUCManager.getInstance().getOccupants(account, Jid.getBareAddress(bareAddress));
            for (Occupant occupant : occupants) {
                if (occupant.getNickname().equals(Jid.getResource(bareAddress))) {
                    return occupant;
                }
            }
        }
        return null;
    }

    public String getStatusText(String account, String bareAddress) {
        final Occupant occupant = getOccupant(account, bareAddress);
        if (occupant != null) {
            return occupant.getStatusText();
        }

        final Presence presence = RosterManager.getInstance().getPresence(account, bareAddress);
        if (presence == null) {
            return null;
        } else {
            return presence.getStatus();
        }
    }

    public void onPresenceChanged(String account, Presence presence) {
        final String bareAddress = Jid.getBareAddress(presence.getFrom());

        CapabilitiesManager.getInstance().onPresenceChanged(account, presence);
        for (OnStatusChangeListener listener
                : Application.getInstance().getManagers(OnStatusChangeListener.class)) {
                listener.onStatusChanged(account, bareAddress, Jid.getResource(presence.getFrom()), StatusMode.createStatusMode(presence), presence.getStatus());
        }

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, bareAddress);
        if (rosterContact != null) {
            ArrayList<RosterContact> rosterContacts = new ArrayList<>();
            rosterContacts.add(rosterContact);
            for (OnRosterChangedListener listener
                    : Application.getInstance().getManagers(OnRosterChangedListener.class)) {
                listener.onPresenceChanged(rosterContacts);
            }
        }
        RosterManager.onContactChanged(account, bareAddress);
    }

    @Override
    public void onArchiveModificationsReceived(ConnectionItem connection) {

    }

    @Override
    public void onDisconnect(ConnectionItem connection) {
        if (!(connection instanceof AccountItem))
            return;
        String account = ((AccountItem) connection).getAccount();
        readyAccounts.remove(account);
    }

    @Override
    public void onAccountDisabled(AccountItem accountItem) {
        requestedSubscriptions.remove(accountItem.getAccount());
    }

    /**
     * Sends new presence information.
     *
     * @param account
     * @throws NetworkException
     */
    public void resendPresence(String account) throws NetworkException {
        sendVCardUpdatePresence(account, AvatarManager.getInstance().getHash(Jid.getBareAddress(account)));
    }

    public void sendVCardUpdatePresence(String account, String hash) throws NetworkException {
        final Presence presence = AccountManager.getInstance().getAccount(account).getPresence();

        final VCardUpdate vCardUpdate = new VCardUpdate();
        vCardUpdate.setPhotoHash(hash);
        presence.addExtension(vCardUpdate);
        ConnectionManager.getInstance().sendStanza(account, presence);
    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress, Stanza stanza) {
        if (!(connection instanceof AccountItem)) {
            return;
        }

        if (!(stanza instanceof Presence)) {
            return;
        }

        Presence presence = (Presence) stanza;

        if (presence.getType() == Presence.Type.subscribe) {
            String account = ((AccountItem) connection).getAccount();

            // Subscription request
            HashSet<String> set = requestedSubscriptions.get(account);
            if (set != null && set.contains(bareAddress)) {
                try {
                    acceptSubscription(account, bareAddress);
                } catch (NetworkException e) {
                }
                subscriptionRequestProvider.remove(account, bareAddress);
            } else {
                subscriptionRequestProvider.add(new SubscriptionRequest(account, bareAddress), null);
            }
        }
    }

    @Override
    public void onAuthorized(ConnectionItem connection) {
        if (!(connection instanceof AccountItem)) {
            return;
        }

        try {
            resendPresence(((AccountItem) connection).getAccount());
        } catch (NetworkException e) {
            e.printStackTrace();
        }
    }
}
