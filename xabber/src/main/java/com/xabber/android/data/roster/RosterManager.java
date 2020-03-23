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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountDisabledListener;
import com.xabber.android.data.account.listeners.OnAccountEnabledListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnDisconnectListener;
import com.xabber.android.data.database.realmobjects.CircleRealmObject;
import com.xabber.android.data.database.realmobjects.ContactRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.database.repositories.ContactRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.iqlast.LastActivityInteractor;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.MessageManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.stringprep.XmppStringprepException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Manage contact list (roster).
 *
 * @author alexander.ivanov
 */
public class RosterManager implements OnDisconnectListener, OnAccountEnabledListener,
        OnAccountDisabledListener {

    private static final String LOG_TAG = RosterManager.class.getSimpleName();

    private static RosterManager instance;

    private NestedMap<RosterContact> rosterContacts;

    private final NestedMap<WeakReference<AbstractContact>> contactsCache;

    private RosterManager() {
        rosterContacts = new NestedMap<>();
        contactsCache = new NestedMap<>();
    }

    public static RosterManager getInstance() {
        if (instance == null) {
            instance = new RosterManager();
        }

        return instance;
    }

    public void onPreInitialize() {
        List<ContactRealmObject> contacts = ContactRepository.getContactsFromRealm();
        for (ContactRealmObject contactRealmObject : contacts) {
            try {
                AccountJid account = AccountJid.from(contactRealmObject.getAccountJid()); //TODO REALM UPDATE possibly there we need there an account resource
                ContactJid contactJid = ContactJid.from(contactRealmObject.getContactJid());
                RosterContact contact = RosterContact.getRosterContact(account, contactJid, contactRealmObject.getBestName());

                for (CircleRealmObject group : contactRealmObject.getCircles()) {
                    contact.addGroupReference(new RosterCircleReference(new RosterCircle(account, group.getCircleName())));
                }

                rosterContacts.put(contact.getAccount().toString(),
                        contact.getUser().getBareJid().toString(), contact);

                MessageManager.getInstance().getOrCreateChat(contact.getAccount(), contact.getUser());

            } catch (ContactJid.UserJidCreateException e) {
                e.printStackTrace();
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
        }
        onContactsChanged(Collections.<RosterContact>emptyList());
    }

    @Nullable
    private Roster getRoster(AccountJid account) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(account);

        if (accountItem == null) {
            return null;
        }

        return Roster.getInstanceFor(accountItem.getConnection());
    }

    public static String getDisplayAuthorName(MessageRealmObject messageRealmObject) {
        ContactJid jid = null;
        try {
            jid = ContactJid.from(messageRealmObject.getOriginalFrom());
        } catch (ContactJid.UserJidCreateException e) {
            e.printStackTrace();
        }

        String author;

        if (!messageRealmObject.getAccount().getFullJid().asBareJid().equals(jid.getBareJid()))
            author = RosterManager.getInstance().getNameOrBareJid(messageRealmObject.getAccount(), jid);
        else author = AccountManager.getInstance().getNickName(messageRealmObject.getAccount());

        return author;
    }

    @Nullable
    public Presence getPresence(AccountJid account, ContactJid user) {
        return PresenceManager.getInstance().getPresence(account, user);
        //final Roster roster = getRoster(account);
        //if (roster == null) {
        //    return null;
        //} else {
        //    return roster.getPresence(user.getJid().asBareJid());
        //}
    }

    public List<Presence> getPresences(AccountJid account, Jid user) {
        return PresenceManager.getInstance().getAvailablePresences(account, user.asBareJid());
        //final Roster roster = getRoster(account);
        //if (roster == null) {
        //    return new ArrayList<>();
        //} else {
        //    return roster.getAvailablePresences(user.asBareJid());
        //}
    }

    public boolean accountIsSubscribedTo(AccountJid account, ContactJid user) {
        final Roster roster = getRoster(account);
        if (roster == null) {
            return false;
        } else {
            return roster.iAmSubscribedTo(user.getJid());
        }
    }

    public boolean contactIsSubscribedTo(AccountJid account, ContactJid user) {
        final Roster roster = getRoster(account);
        if (roster == null) {
            return false;
        } else {
            return roster.isSubscribedToMyPresence(user.getJid());
        }
    }

    public RosterPacket.ItemType getSubscriptionType(AccountJid account, ContactJid user) {
        RosterEntry entry = getRoster(account).getEntry(user.getJid().asBareJid());
        if (entry == null) {
            return null;
        }
        return entry.getType();
    }

    /**
     *  Check if we have an outgoing subscription request
     */
    public boolean hasSubscriptionPending(AccountJid account, ContactJid user) {
        RosterEntry entry = getRoster(account).getEntry(user.getJid().asBareJid());
        if (entry == null) {
            return false;
        }
        return entry.isSubscriptionPending();
    }

    public SubscriptionState getSubscriptionState(AccountJid account, ContactJid user) {
        boolean outgoingRequest = hasSubscriptionPending(account, user);
        boolean incomingRequest = PresenceManager.getInstance().hasSubscriptionRequest(account, user);
        RosterPacket.ItemType subscription = getSubscriptionType(account, user);

        SubscriptionState state = new SubscriptionState(subscription);
        state.setPendingSubscriptions(incomingRequest, outgoingRequest);
        return state;
    }

    public Collection<RosterContact> getAccountRosterContacts(final AccountJid accountJid) {
        List<RosterContact> contactsCopy = new ArrayList<>(rosterContacts.getNested(accountJid.toString()).values());
        return Collections.unmodifiableCollection(contactsCopy);
    }

    public Collection<RosterContact> getAllContacts() {
        List<RosterContact> contactsCopy = new ArrayList<>();
        for (Iterator<String> it = rosterContacts.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            contactsCopy.addAll(rosterContacts.getNested(key).values());
        }
        return Collections.unmodifiableCollection(contactsCopy);
    }

    void onContactsAdded(final AccountJid account, Collection<Jid> addresses) {
        final Roster roster = RosterManager.getInstance().getRoster(account);
        final Collection<RosterContact> newContacts = new ArrayList<>(addresses.size());
        for (Jid jid : addresses) {
            RosterEntry entry = roster.getEntry(jid.asBareJid());
            try {
                RosterContact contact = convertRosterEntryToRosterContact(account, roster, entry);
                rosterContacts.put(account.toString(),
                        contact.getUser().getBareJid().toString(), contact);
                newContacts.add(contact);

                LastActivityInteractor.getInstance().requestLastActivityAsync(account, ContactJid.from(jid));
            } catch (ContactJid.UserJidCreateException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }

        ContactRepository.saveContactToRealm(account, newContacts);

        onContactsChanged(newContacts);
    }

    void onContactsUpdated(AccountJid account, Collection<Jid> addresses) {
        onContactsAdded(account, addresses);
    }

    void onContactsDeleted(AccountJid account, Collection<Jid> addresses) {
        final Collection<RosterContact> removedContacts = new ArrayList<>(addresses.size());

        for (Jid jid : addresses) {
            RosterContact contact = rosterContacts.remove(account.toString(), jid.asBareJid().toString());
            if (contact != null) {
                removedContacts.add(contact);
            }
        }

        ContactRepository.removeContacts(removedContacts);

        onContactsChanged(removedContacts);
    }

    @NonNull
    private RosterContact convertRosterEntryToRosterContact(AccountJid account, Roster roster, RosterEntry rosterEntry) throws ContactJid.UserJidCreateException {
        final RosterContact contact = RosterContact
                .getRosterContact(account, ContactJid.from(rosterEntry.getJid()), rosterEntry.getName());

        final Collection<org.jivesoftware.smack.roster.RosterGroup> groups = roster.getGroups();

        contact.clearGroupReferences();
        for (org.jivesoftware.smack.roster.RosterGroup group : groups) {
            if (group.contains(rosterEntry)) {
                contact.addGroupReference(new RosterCircleReference(new RosterCircle(account, group.getName())));
            }
        }
        contact.setEnabled(true);
        contact.setConnected(true);


        return contact;
    }

    public AbstractContact getAbstractContact(@NonNull AccountJid accountJid, @NonNull ContactJid contactJid) {
        WeakReference<AbstractContact> contactWeakReference = contactsCache.get(accountJid.toString(), contactJid.toString());
        if (contactWeakReference != null && contactWeakReference.get() != null) {
            return contactWeakReference.get();
        }

        AbstractContact newContact = new AbstractContact(accountJid, contactJid);
        contactsCache.put(accountJid.toString(), contactJid.toString(), new WeakReference<>(newContact));
        return newContact;
    }

    @Nullable
    public RosterContact getRosterContact(AccountJid accountJid, BareJid bareJid) {
        return rosterContacts.get(accountJid.toString(), bareJid.toString());
    }

    @Nullable
    public RosterContact getRosterContact(AccountJid accountJid, ContactJid contactJid) {
        return getRosterContact(accountJid, contactJid.getBareJid());
    }

    /**
     *
     * @param account
     * @param user
     * @return
     */
    public AbstractContact getBestContact(AccountJid account, ContactJid user) {
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);

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
    public Collection<String> getGroups(AccountJid account) {
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
     * @return Contact's name.
     */
    public String getName(AccountJid account, ContactJid user) {
        RosterContact contact = getRosterContact(account, user);
        if (contact == null) {
            return user.toString();
        }
        return contact.getName();
    }

    /**
     * @return Roster contact's nickname if present.
     */
    public String getNickname(AccountJid account, ContactJid user) {
        RosterContact contact = getRosterContact(account, user);
        if (contact == null) {
            return "";
        }
        return contact.getNickname();
    }

    /**
     * @return Contact's name or BareJid if that contacts not exist.
     */
    public String getNameOrBareJid(AccountJid account, ContactJid user) {
        RosterContact contact = getRosterContact(account, user);
        if (contact == null) {
            return user.getBareJid().toString();
        }
        return contact.getName();
    }

    /**
     * @return Contact's groups.
     */
    public Collection<String> getGroups(AccountJid account, ContactJid user) {
        RosterContact contact = getRosterContact(account, user);
        if (contact == null) {
            return Collections.emptyList();
        }
        return contact.getGroupNames();
    }

    /**
     * Requests to create new contact.
     *
     * @param account
     * @param user
     * @param name
     * @param groups
     * @throws NetworkException
     */
    public void createContact(AccountJid account, ContactJid user, String name,
                              Collection<String> groups)
            throws SmackException.NotLoggedInException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        final Roster roster = getRoster(account);

        if (roster == null) {
            return;
        }

        roster.createEntry(user.getBareJid(), name, groups.toArray(new String[groups.size()]));

    }

    /**
     * Requests contact removing.
     *
     */
    public void removeContact(AccountJid account, ContactJid user) {

        final Roster roster = getRoster(account);

        if (roster == null) {
            return;
        }

        final RosterEntry entry = roster.getEntry(user.getJid().asBareJid());

        if (entry == null) {
            return;
        }

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                try {
                    roster.removeEntry(entry);
                    PresenceManager.getInstance().clearSingleContactPresences(account, user.getBareJid());
                } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException e) {
                    Application.getInstance().onError(R.string.NOT_CONNECTED);
                } catch (SmackException.NoResponseException e) {
                    Application.getInstance().onError(R.string.CONNECTION_FAILED);
                } catch (XMPPException.XMPPErrorException e) {
                    Application.getInstance().onError(R.string.XMPP_EXCEPTION);
                } catch (InterruptedException e) {
                    LogManager.exception(LOG_TAG, e);
                }
            }
        });
    }

    public void setGroups(AccountJid account, ContactJid user, Collection<String> groups) throws NetworkException {
        final Roster roster = getRoster(account);

        if (roster == null) {
            return;
        }

        final RosterEntry entry = roster.getEntry(user.getJid().asBareJid());

        if (entry == null) {
            return;
        }

        RosterPacket packet = new RosterPacket();
        packet.setType(IQ.Type.set);
        RosterPacket.Item item = new RosterPacket.Item(user.getBareJid(), entry.getName());
        for (String group : groups) {
            item.addGroupName(group);
        }
        packet.addRosterItem(item);

        StanzaSender.sendStanza(account, packet);
    }

    public void setName(AccountJid account, ContactJid user, final String name) {
        final Roster roster = getRoster(account);

        if (roster == null) {
            return;
        }

        final RosterEntry entry = roster.getEntry(user.getJid().asBareJid());

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
        } catch (InterruptedException e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    /**
     * Requests to remove group from all contacts in account.
     */
    public void removeGroup(AccountJid account, String groupName)
            throws NetworkException {
        final Roster roster = getRoster(account);
        if (roster == null) {
            return;
        }

        final org.jivesoftware.smack.roster.RosterGroup group = roster.getGroup(groupName);
        if (group == null) {
            return;
        }


        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
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
                    } catch (InterruptedException e) {
                        LogManager.exception(LOG_TAG, e);
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
        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            removeGroup(account, group);
        }
    }

    /**
     * Requests to rename group.
     *
     * @param account
     * @param oldGroup can be <code>null</code> for "no group".
     */
    public void renameGroup(AccountJid account, String oldGroup, final String newGroup) {
        if (newGroup.equals(oldGroup)) {
            return;
        }

        final Roster roster = getRoster(account);
        if (roster == null) {
            return;
        }

        if (TextUtils.isEmpty(oldGroup)) {
            Application.getInstance().runInBackgroundUserRequest(new Runnable() {
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

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
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
                } catch (InterruptedException e) {
                    LogManager.exception(LOG_TAG, e);
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
        } catch (InterruptedException e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    /**
     * Requests to rename group from all accounts.
     *
     * @param oldGroup can be <code>null</code> for "no group".
     */
    public void renameGroup(String oldGroup, String newGroup) {
        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            renameGroup(account, oldGroup, newGroup);
        }
    }

    /**
     * @param account
     * @return Whether roster for specified account has been received.
     */
    public boolean isRosterReceived(AccountJid account) {
        final Roster roster = getRoster(account);
        return roster != null && roster.isLoaded();
    }

    @Override
    public void onDisconnect(ConnectionItem connection) {
        if (!(connection instanceof AccountItem)) {
            return;
        }

        Collection<RosterContact> accountContacts
                = rosterContacts.getNested(connection.getAccount().toString()).values();

        for (RosterContact contact : accountContacts) {
            contact.setConnected(false);
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
    private void setEnabled(AccountJid account, boolean enabled) {
        Collection<RosterContact> accountContacts
                = rosterContacts.getNested(account.toString()).values();

        for (RosterContact contact : accountContacts) {
            contact.setEnabled(enabled);
        }
    }

    /**
     * Notifies registered {@link OnContactChangedListener}.
     *
     * @param entities
     */
    public static void onContactsChanged(final Collection<RosterContact> entities) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnContactChangedListener onContactChangedListener : Application
                        .getInstance().getUIListeners(OnContactChangedListener.class)) {
                    onContactChangedListener.onContactsChanged(entities);
                }
            }
        });
    }

    /**
     * Notifies registered {@link OnContactChangedListener}.
     */
    public static void onContactChanged(AccountJid account, ContactJid bareAddress) {
        final Collection<RosterContact> entities = new ArrayList<>();
        RosterContact rosterContact = getInstance().getRosterContact(account, bareAddress);
        if (rosterContact != null) {
            entities.add(rosterContact);
        }
        onContactsChanged(entities);
    }

    /**
     * Notifies registered {@link OnChatStateListener}.
     */
    public static void onChatStateChanged(AccountJid account, ContactJid bareAddress) {
        final Collection<RosterContact> entities = new ArrayList<>();
        RosterContact rosterContact = getInstance().getRosterContact(account, bareAddress);
        if (rosterContact != null) {
            entities.add(rosterContact);
        }

        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (OnChatStateListener onChatStateListener : Application
                        .getInstance().getUIListeners(OnChatStateListener.class)) {
                    onChatStateListener.onChatStateChanged(entities);
                }
            }
        });
    }

    //A wrapper for the current subscription type and any current
    //pending subscription requests between contact and user.
    public static class SubscriptionState {

        //Subscription types
        //no subscriptions
        public static final int NONE = 0;

        //subscription from us to contact
        public static final int TO = 4;

        //subscription from contact to us
        public static final int FROM = 6;

        //2-way subscription
        public static final int BOTH = 8;


        //Current state of pending subscriptions
        //no pending subscriptions
        public static final int PENDING_NONE = -1;

        //pending incoming subscription
        public static final int PENDING_IN = -2;

        //pending outgoing subscription
        public static final int PENDING_OUT = -3;

        //pending outgoing and incoming subscription
        public static final int PENDING_IN_OUT = -4;


        private int subscriptionType;
        private int pendingSubscription;

        public SubscriptionState() {}

        public SubscriptionState(RosterPacket.ItemType type) {
            if (type == null) {
                subscriptionType = NONE;
            } else
                switch (type) {
                    case both:
                        subscriptionType = BOTH;
                        break;
                    case to:
                        subscriptionType = TO;
                        break;
                    case from:
                        subscriptionType = FROM;
                        break;
                    default:
                        subscriptionType = NONE;
                        break;
                }
        }

        public int getSubscriptionType() {
            return subscriptionType;
        }

        public void setSubscriptionType(int subscriptionType) {
            this.subscriptionType = subscriptionType;
        }

        public int getPendingSubscription() {
            return pendingSubscription;
        }

        public void setPendingSubscription(int pendingSubscription) {
            this.pendingSubscription = pendingSubscription;
        }

        public void setPendingSubscriptions(boolean incoming, boolean outgoing) {
            if (incoming && outgoing) {
                pendingSubscription = PENDING_IN_OUT;
                return;
            }
            if (incoming) {
                pendingSubscription = PENDING_IN;
                return;
            }
            if (outgoing) {
                pendingSubscription = PENDING_OUT;
                return;
            }
            pendingSubscription = PENDING_NONE;
        }

        public boolean hasOutgoingSubscription() {
            return pendingSubscription == PENDING_OUT || pendingSubscription == PENDING_IN_OUT;
        }

        public boolean hasIncomingSubscription() {
            return pendingSubscription == PENDING_IN || pendingSubscription == PENDING_IN_OUT;
        }
    }
}
