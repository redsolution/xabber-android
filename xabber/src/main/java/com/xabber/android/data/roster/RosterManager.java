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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountDisabledListener;
import com.xabber.android.data.account.OnAccountEnabledListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.MessageManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Manage contact list (roster).
 *
 * @author alexander.ivanov
 */
public class RosterManager implements OnDisconnectListener, OnAccountEnabledListener,
        OnAccountDisabledListener {

    private final static RosterManager instance;

    static {
        instance = new RosterManager();
        Application.getInstance().addManager(instance);
    }

    private Collection<RosterContact> allRosterContacts;

    private RosterManager() {
        allRosterContacts = new ArrayList<>();
    }

    public static RosterManager getInstance() {
        return instance;
    }

    @Nullable
    private Roster getRoster(String account) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(account);

        if (accountItem == null) {
            return null;
        }

        final ConnectionThread connectionThread = accountItem.getConnectionThread();

        if (connectionThread == null) {
            return null;
        }

        final AbstractXMPPConnection xmppConnection = connectionThread.getXMPPConnection();

        if (xmppConnection == null) {
            return null;
        }

        return Roster.getInstanceFor(xmppConnection);
    }

    @Nullable
    public Presence getPresence(String account, String user) {
        final Roster roster = getRoster(account);
        if (roster == null) {
            return null;
        } else {
            return roster.getPresence(user);
        }
    }

    public List<Presence> getPresences(String account, String user) {
        final Roster roster = getRoster(account);
        if (roster == null) {
            return new ArrayList<>();
        } else {
            return roster.getAvailablePresences(user);
        }
    }

    public Collection<RosterContact> getContacts() {
        requestRosterReloadIfNeeded();

        return Collections.unmodifiableCollection(allRosterContacts);
    }

    private void requestRosterReloadIfNeeded() {
        for (String account : AccountManager.getInstance().getAccounts()) {
            final Roster roster = RosterManager.getInstance().getRoster(account);
            if (roster != null && !roster.isLoaded()) {
                try {
                    roster.reload();
                } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void updateContacts() {
        Collection<RosterContact> newRosterContacts = new ArrayList<>();
        for (String account : AccountManager.getInstance().getAccounts()) {
            final Roster roster = RosterManager.getInstance().getRoster(account);
            if (roster == null) {
                continue;
            }

            final Set<RosterEntry> entries = roster.getEntries();

            for (RosterEntry rosterEntry : entries) {

                final RosterContact contact = convertRosterEntryToRosterContact(account, roster, rosterEntry);

                newRosterContacts.add(contact);

            }
        }
        allRosterContacts = newRosterContacts;

        LogManager.i(this, "updateContacts: " + allRosterContacts.size());
    }

    @NonNull
    private RosterContact convertRosterEntryToRosterContact(String account, Roster roster, RosterEntry rosterEntry) {
        final RosterContact contact = new RosterContact(account, rosterEntry);

        final Collection<org.jivesoftware.smack.roster.RosterGroup> groups = roster.getGroups();

        for (org.jivesoftware.smack.roster.RosterGroup group : groups) {
            if (group.contains(rosterEntry)) {
                contact.addGroupReference(new RosterGroupReference(new RosterGroup(account, group.getName())));
            }
        }

        final RosterPacket.ItemType type = rosterEntry.getType();
        contact.setSubscribed(type == RosterPacket.ItemType.both || type == RosterPacket.ItemType.to);
        return contact;
    }

    /**
     * @param account
     * @param user
     * @return <code>null</code> can be returned.
     */
    public RosterContact getRosterContact(String account, String user) {

        final Roster roster = getRoster(account);

        if (roster == null) {
            return null;
        }

        final RosterEntry entry = roster.getEntry(user);

        if (entry == null) {
            return null;
        } else {
            return convertRosterEntryToRosterContact(account, roster, entry);
        }
    }

    /**
     * Gets {@link RoomContact}, {@link RosterContact}, {@link ChatContact} or
     * creates new {@link ChatContact}.
     *
     * @param account
     * @param user
     * @return
     */
    public AbstractContact getBestContact(String account, String user) {
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);
        if (abstractChat != null && abstractChat instanceof RoomChat) {
            return new RoomContact((RoomChat) abstractChat);
        }
        RosterContact rosterContact = getRosterContact(account, user);
        if (rosterContact != null) {
            return rosterContact;
        }
        if (abstractChat != null) {
            return new ChatContact(abstractChat);
        }
        return new ChatContact(account, user);
    }

    /**
     * @param account
     * @return List of groups in specified account.
     */
    public Collection<String> getGroups(String account) {
        final Roster roster = getRoster(account);

        Collection<String> returnGroups = new ArrayList<>();

        if (roster == null) {
            return returnGroups;
        }

        final Collection<org.jivesoftware.smack.roster.RosterGroup> groups = roster.getGroups();

        for (org.jivesoftware.smack.roster.RosterGroup rosterGroup : groups) {
            returnGroups.add(rosterGroup.getName());
        }

        return returnGroups;
    }

    /**
     * @param account
     * @param user
     * @return Contact's name.
     */
    public String getName(String account, String user) {
        RosterContact contact = getRosterContact(account, user);
        if (contact == null)
            return user;
        return contact.getName();
    }

    /**
     * @param account
     * @param user
     * @return Contact's groups.
     */
    public Collection<String> getGroups(String account, String user) {
        RosterContact contact = getRosterContact(account, user);
        if (contact == null)
            return Collections.emptyList();
        return contact.getGroupNames();
    }

    /**
     * Requests to create new contact.
     *
     * @param account
     * @param bareAddress
     * @param name
     * @param groups
     * @throws NetworkException
     */
    public void createContact(String account, String bareAddress, String name,
                              Collection<String> groups) throws SmackException.NotLoggedInException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {


        final Roster roster = getRoster(account);

        if (roster == null) {
            return;
        }

        roster.createEntry(bareAddress, name, groups.toArray(new String[groups.size()]));
    }

    /**
     * Requests contact removing.
     *
     */
    public void removeContact(String account, String bareAddress) {

        final Roster roster = getRoster(account);

        if (roster == null) {
            return;
        }

        final RosterEntry entry = roster.getEntry(bareAddress);

        if (entry == null) {
            return;
        }

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    roster.removeEntry(entry);
                } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException e) {
                    Application.getInstance().onError(R.string.NOT_CONNECTED);
                } catch (SmackException.NoResponseException e) {
                    Application.getInstance().onError(R.string.CONNECTION_FAILED);
                } catch (XMPPException.XMPPErrorException e) {
                    Application.getInstance().onError(R.string.XMPP_EXCEPTION);
                }
            }
        });
    }

    public void setGroups(String account, String bareAddress, Collection<String> groups) throws NetworkException {
        final Roster roster = getRoster(account);

        if (roster == null) {
            return;
        }

        final RosterEntry entry = roster.getEntry(bareAddress);

        if (entry == null) {
            return;
        }

        RosterPacket packet = new RosterPacket();
        packet.setType(IQ.Type.set);
        RosterPacket.Item item = new RosterPacket.Item(bareAddress, entry.getName());
        for (String group : groups) {
            item.addGroupName(group);
        }
        packet.addRosterItem(item);

        ConnectionManager.getInstance().sendStanza(account, packet);
    }

    public void setName(String account, String bareAddress, final String name) {
        final Roster roster = getRoster(account);

        if (roster == null) {
            return;
        }

        final RosterEntry entry = roster.getEntry(bareAddress);

        if (entry == null) {
            return;
        }

        try {
            entry.setName(name.trim());
        } catch (SmackException.NotConnectedException e) {
            Application.getInstance().onError(R.string.NOT_CONNECTED);
        } catch (SmackException.NoResponseException e) {
            Application.getInstance().onError(R.string.CONNECTION_FAILED);
        } catch (XMPPException.XMPPErrorException e) {
            Application.getInstance().onError(R.string.XMPP_EXCEPTION);
        }
    }

    /**
     * Requests to remove group from all contacts in account.
     */
    public void removeGroup(String account, String groupName)
            throws NetworkException {
        final Roster roster = getRoster(account);
        if (roster == null) {
            return;
        }

        final org.jivesoftware.smack.roster.RosterGroup group = roster.getGroup(groupName);
        if (group == null) {
            return;
        }


        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                for (RosterEntry entry : group.getEntries()) {
                    try {
                        group.removeEntry(entry);
                    } catch (SmackException.NoResponseException e) {
                        Application.getInstance().onError(R.string.CONNECTION_FAILED);
                    } catch (SmackException.NotConnectedException e) {
                        Application.getInstance().onError(R.string.NOT_CONNECTED);
                    } catch (XMPPException.XMPPErrorException e) {
                        Application.getInstance().onError(R.string.XMPP_EXCEPTION);
                    }
                }

            }
        });
    }

    /**
     * Requests to remove group from all contacts in all accounts.
     *
     */
    public void removeGroup(String group) throws NetworkException {
        for (String account : AccountManager.getInstance().getAccounts()) {
            removeGroup(account, group);
        }
    }

    /**
     * Requests to rename group.
     *
     * @param account
     * @param oldGroup can be <code>null</code> for "no group".
     */
    public void renameGroup(String account, String oldGroup, final String newGroup) {
        if (newGroup.equals(oldGroup)) {
            return;
        }

        final Roster roster = getRoster(account);
        if (roster == null) {
            return;
        }

        if (TextUtils.isEmpty(oldGroup)) {
            Application.getInstance().runInBackground(new Runnable() {
                @Override
                public void run() {
                    createGroupForUnfiledEntries(newGroup, roster);
                }
            });
            return;
        }

        final org.jivesoftware.smack.roster.RosterGroup group = roster.getGroup(oldGroup);
        if (group == null) {
            return;
        }

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    group.setName(newGroup);
                } catch (SmackException.NoResponseException e) {
                    Application.getInstance().onError(R.string.CONNECTION_FAILED);
                } catch (SmackException.NotConnectedException e) {
                    Application.getInstance().onError(R.string.NOT_CONNECTED);
                } catch (XMPPException.XMPPErrorException e) {
                    Application.getInstance().onError(R.string.XMPP_EXCEPTION);
                }
            }
        });

    }

    private void createGroupForUnfiledEntries(String newGroup, Roster roster) {
        final Set<RosterEntry> unfiledEntries = roster.getUnfiledEntries();

        final org.jivesoftware.smack.roster.RosterGroup group = roster.createGroup(newGroup);

        try {
            for (RosterEntry entry : unfiledEntries) {
                group.addEntry(entry);
            }
        } catch (SmackException.NoResponseException e) {
            Application.getInstance().onError(R.string.CONNECTION_FAILED);
        } catch (SmackException.NotConnectedException e) {
            Application.getInstance().onError(R.string.NOT_CONNECTED);
        } catch (XMPPException.XMPPErrorException e) {
            Application.getInstance().onError(R.string.XMPP_EXCEPTION);
        }
    }

    /**
     * Requests to rename group from all accounts.
     *
     * @param oldGroup can be <code>null</code> for "no group".
     */
    public void renameGroup(String oldGroup, String newGroup) {
        for (String account : AccountManager.getInstance().getAccounts()) {
            renameGroup(account, oldGroup, newGroup);
        }
    }

    /**
     * @param account
     * @return Whether roster for specified account has been received.
     */
    public boolean isRosterReceived(String account) {
        final Roster roster = getRoster(account);
        return roster != null && roster.isLoaded();
    }

    @Override
    public void onDisconnect(ConnectionItem connection) {
        if (!(connection instanceof AccountItem))
            return;
        String account = ((AccountItem) connection).getAccount();
        for (RosterContact contact : allRosterContacts) {
            if (contact.getAccount().equals(account)) {
                contact.setConnected(false);
            }
        }
    }

    @Override
    public void onAccountEnabled(AccountItem accountItem) {
        setEnabled(accountItem.getAccount(), true);
    }

    @Override
    public void onAccountDisabled(AccountItem accountItem) {
        setEnabled(accountItem.getAccount(), false);
    }

    /**
     * Sets whether contacts in accounts are enabled.
     */
    private void setEnabled(String account, boolean enabled) {
        for (RosterContact contact : allRosterContacts) {
            if (contact.getAccount().equals(account)) {
                contact.setEnabled(enabled);
            }
        }
    }

    /**
     * Notifies registered {@link OnContactChangedListener}.
     *
     * @param entities
     */
    public static void onContactsChanged(final Collection<BaseEntity> entities) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnContactChangedListener onContactChangedListener : Application
                        .getInstance().getUIListeners(
                                OnContactChangedListener.class))
                    onContactChangedListener.onContactsChanged(entities);
            }
        });
    }

    /**
     * Notifies registered {@link OnContactChangedListener}.
     */
    public static void onContactChanged(String account, String bareAddress) {
        final ArrayList<BaseEntity> entities = new ArrayList<>();
        entities.add(new BaseEntity(account, bareAddress));
        onContactsChanged(entities);
    }
}
