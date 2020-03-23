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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.account.listeners.OnAccountDisabledListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.captcha.Captcha;
import com.xabber.android.data.extension.captcha.CaptchaManager;
import com.xabber.android.data.extension.iqlast.LastActivityInteractor;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.EntityNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.xmpp.vcardupdate.VCardUpdate;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process contact's presence information.
 *
 * @author alexander.ivanov
 */
public class PresenceManager implements OnLoadListener, OnAccountDisabledListener,
        OnPacketListener {

    private static PresenceManager instance;

    private final EntityNotificationProvider<SubscriptionRequest> subscriptionRequestProvider;
    /**
     * List of account with requested subscriptions for auto accept incoming
     * subscription request.
     */
    private final HashMap<AccountJid, Set<ContactJid>> requestedSubscriptions;

    private final Map<BareJid, Map<Resourcepart, Presence>> accountsPresenceMap = new ConcurrentHashMap<>();
    private final Map<BareJid, Map<BareJid, Map<Resourcepart, Presence>>> presenceMap = new ConcurrentHashMap<>();


    public static PresenceManager getInstance() {
        if (instance == null) {
            instance = new PresenceManager();
        }

        return instance;
    }

    private PresenceManager() {
        subscriptionRequestProvider = new EntityNotificationProvider<>(R.drawable.ic_stat_add_circle);
        requestedSubscriptions = new HashMap<>();
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
     * @return <code>null</code> can be returned.
     */
    public SubscriptionRequest getSubscriptionRequest(AccountJid account, ContactJid user) {
        return subscriptionRequestProvider.get(account, user);
    }

    /**
     * Requests subscription to the contact.
     *
     * @throws NetworkException
     */
    public void requestSubscription(AccountJid account, ContactJid user) throws NetworkException {
        requestSubscription(account, user, true);
    }

    /**
     * Requests subscription to the contact.
     * Create chat with new contact if need.
     * @throws NetworkException
     */
    public void requestSubscription(AccountJid account, ContactJid user, boolean createChat) throws NetworkException {
        Presence packet = new Presence(Presence.Type.subscribe);
        packet.setTo(user.getJid());
        StanzaSender.sendStanza(account, packet);
        addRequestedSubscription(account, user);
        if (createChat) createChatForNewContact(account, user);
    }

    private void removeRequestedSubscription(AccountJid account, ContactJid user) {
        Set<ContactJid> set = requestedSubscriptions.get(account);
        if (set != null) {
            set.remove(user);
        }
    }

    private void addRequestedSubscription(AccountJid account, ContactJid user) {
        Set<ContactJid> set = requestedSubscriptions.get(account);
        if (set == null) {
            set = new HashSet<>();
            requestedSubscriptions.put(account, set);
        }
        set.add(user);
    }

    /**
     * Accepts subscription request from the entity (share own presence).
     *
     * @param notify whether User should be notified of the automatically accepted
     *               request with a new Action message in the chat.
     *               Mainly just to avoid Action message spam when adding new contacts.
     */
    public void acceptSubscription(AccountJid account, ContactJid user, boolean notify) throws NetworkException {
        if (notify) createChatForAcceptingIncomingRequest(account, user);
        Presence packet = new Presence(Presence.Type.subscribed);
        packet.setTo(user.getJid());
        StanzaSender.sendStanza(account, packet);
        subscriptionRequestProvider.remove(account, user);
        removeRequestedSubscription(account, user);
    }

    public void acceptSubscription(AccountJid account, ContactJid user) throws NetworkException {
        acceptSubscription(account, user, true);
    }

    /** Added available action to chat, to show chat in recent chats */
    private void createChatForNewContact(AccountJid account, ContactJid user) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        chat.newAction(null, Application.getInstance().getResources().getString(R.string.action_subscription_sent),
                ChatAction.subscription_sent, false);
    }

    private void createChatForIncomingRequest(AccountJid account, ContactJid user) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        chat.newAction(null, Application.getInstance().getResources().getString(R.string.action_subscription_received),
                ChatAction.subscription_received, false);
    }

    private void createChatForAcceptingIncomingRequest(AccountJid account, ContactJid user) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        String name = RosterManager.getInstance().getBestContact(account, user).getName();
        chat.newAction(null, Application.getInstance().getResources().getString(R.string.action_subscription_received_add, name),
                ChatAction.subscription_received_accepted, false);
    }

    private void createChatForAcceptingOutgoingRequest(AccountJid account, ContactJid user) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        String name = RosterManager.getInstance().getBestContact(account, user).getName();
        chat.newAction(null, Application.getInstance().getResources().getString(R.string.action_subscription_sent_add, name),
                ChatAction.subscription_sent_accepted, false);
    }

    /**
     * Discards subscription request from the entity (deny own presence
     * sharing).
     */
    public void discardSubscription(AccountJid account, ContactJid user) throws NetworkException {
        Presence packet = new Presence(Presence.Type.unsubscribed);
        packet.setTo(user.getJid());
        StanzaSender.sendStanza(account, packet);
        subscriptionRequestProvider.remove(account, user);
        removeRequestedSubscription(account, user);
    }

    /**
     * Subscribe for contact's presence (has no bearing on own presence sharing)
     */
    public void subscribeForPresence(AccountJid account, ContactJid user) throws NetworkException {
        Presence packet = new Presence(Presence.Type.subscribe);
        packet.setTo(user.getJid());
        StanzaSender.sendStanza(account, packet);
    }

    /**
     * Unsubscribe from contact's presence (has no bearing on own presence sharing)
     */
    public void unsubscribeFromPresence(AccountJid account, ContactJid user) throws NetworkException {
        Presence packet = new Presence(Presence.Type.unsubscribe);
        packet.setTo(user.getJid());
        StanzaSender.sendStanza(account, packet);
    }

    /**
     * Either accepts the current subscription request from the contact(if present), or adds
     * an automatic acceptance of the incoming request
     */
    public void addAutoAcceptSubscription(AccountJid account, ContactJid user) throws NetworkException {
        if(subscriptionRequestProvider.get(account, user) != null) {
            acceptSubscription(account, user);
        } else {
            addRequestedSubscription(account, user);
        }
    }

    public void removeAutoAcceptSubscription(AccountJid account, ContactJid user) {
        removeRequestedSubscription(account, user);
    }

    //public void addAutoAcceptSubscription(AccountJid account, UserJid user) {
    //    addRequestedSubscription(account, user);
    //}

    public boolean hasAutoAcceptSubscription(AccountJid account, ContactJid user) {
        Set<ContactJid> set = requestedSubscriptions.get(account);
        if (set == null) {
            return false;
        }
        return set.contains(user);
    }

    /**
     *  Check if we have an incoming subscription request
     */
    public boolean hasSubscriptionRequest(AccountJid account, ContactJid bareAddress) {
        return getSubscriptionRequest(account, bareAddress) != null;
    }

    public StatusMode getStatusMode(AccountJid account, ContactJid user) {
        return StatusMode.createStatusMode(RosterManager.getInstance().getPresence(account, user));
    }

    public String getStatusText(AccountJid account, ContactJid bareAddress) {
        final Presence presence = RosterManager.getInstance().getPresence(account, bareAddress);
        if (presence == null) {
            return null;
        } else {
            return presence.getStatus();
        }
    }

    public void onPresenceChanged(AccountJid account, Presence presence) {
        ContactJid from;
        try {
            from = ContactJid.from(presence.getFrom());
        } catch (ContactJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            return;
        }

        if (presence.isAvailable()) {
            CapabilitiesManager.getInstance().onPresence(account, presence);
        }

        if (presence.getType() == Presence.Type.unavailable)
            LastActivityInteractor.getInstance().setLastActivityTimeNow(account, from.getBareUserJid());

        for (OnStatusChangeListener listener : Application.getInstance().getManagers(OnStatusChangeListener.class)) {
                listener.onStatusChanged(account, from,
                        StatusMode.createStatusMode(presence), presence.getStatus());
        }

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, from.getBareJid());
        if (rosterContact != null) {
            ArrayList<RosterContact> rosterContacts = new ArrayList<>();
            rosterContacts.add(rosterContact);
            for (OnRosterChangedListener listener
                    : Application.getInstance().getManagers(OnRosterChangedListener.class)) {
                listener.onPresenceChanged(rosterContacts);
            }
        }
        RosterManager.onContactChanged(account, from);
    }

    @Override
    public void onAccountDisabled(AccountItem accountItem) {
        requestedSubscriptions.remove(accountItem.getAccount());
    }

    /**
     * Sends new presence information.
     *
     * @throws NetworkException
     */
    public void resendPresence(AccountJid account) throws NetworkException {
        sendVCardUpdatePresence(account, AvatarManager.getInstance().getHash(account.getFullJid().asBareJid()));
    }

    public void sendVCardUpdatePresence(AccountJid account, String hash) throws NetworkException {
        LogManager.i(this, "sendVCardUpdatePresence: " + account);

        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            return;
        }

        final Presence presence = accountItem.getPresence();

        final VCardUpdate vCardUpdate = new VCardUpdate();
        vCardUpdate.setPhotoHash(hash);
        presence.addExtension(vCardUpdate);
        StanzaSender.sendStanza(account, presence);
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (!(connection instanceof AccountItem)) {
            return;
        }

        if (!(stanza instanceof Presence)) {
            return;
        }

        Presence presence = (Presence) stanza;

        ContactJid from;
        Resourcepart fromResource;
        try {
            from = ContactJid.from(stanza.getFrom());
            fromResource = stanza.getFrom().getResourceOrEmpty();
        } catch (ContactJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            return;
        }
        boolean accountPresence = isAccountPresence(connection.getAccount(), from.getBareJid());
        Map<Resourcepart, Presence> userPresences;
        switch (presence.getType()) {
            case available:
                userPresences = accountPresence ? getSingleAccountPresences(from.getBareJid()) : getSingleContactPresences(connection.getAccount().getFullJid().asBareJid(), from.getBareJid());
                userPresences.remove(Resourcepart.EMPTY);
                userPresences.put(fromResource, presence);
                if (accountPresence) {
                    AccountManager.getInstance().onAccountChanged(connection.getAccount());
                } else {
                    RosterManager.onContactChanged(connection.getAccount(), from);
                }
                break;
            case unavailable:
                // If no resource, this is likely an offline presence as part of
                // a roster presence flood. In that case, we store it.
                userPresences = accountPresence ? getSingleAccountPresences(from.getBareJid()) : getSingleContactPresences(connection.getAccount().getFullJid().asBareJid(), from.getBareJid());
                userPresences.put(fromResource.equals(Resourcepart.EMPTY) ? Resourcepart.EMPTY : fromResource, presence);
                if (accountPresence) {
                    AccountManager.getInstance().onAccountChanged(connection.getAccount());
                } else {
                    RosterManager.onContactChanged(connection.getAccount(), from);
                }
                break;
            case error:
                // No need to act on error presences send without from, i.e.
                // directly send from the users XMPP service, or where the from
                // address is not a bare JID
                if (!fromResource.equals(Resourcepart.EMPTY)) {
                    break;
                }
                userPresences = accountPresence ? getSingleAccountPresences(from.getBareJid()) : getSingleContactPresences(connection.getAccount().getFullJid().asBareJid(), from.getBareJid());
                userPresences.clear();
                userPresences.put(Resourcepart.EMPTY, presence);
                if (accountPresence) {
                    AccountManager.getInstance().onAccountChanged(connection.getAccount());
                } else {
                    RosterManager.onContactChanged(connection.getAccount(), from);
                }
                break;
            case subscribe:

                AccountJid account = connection.getAccount();

                // check spam-filter settings

                // reject all subscribe-requests
                if (SettingsManager.spamFilterMode() == SettingsManager.SpamFilterMode.noAuth) {
                    // send a warning message to sender
                    MessageManager.getInstance().sendMessageWithoutChat(from.getJid(),
                            StringUtils.randomString(12), account,
                            Application.getInstance().getResources().getString(R.string.spam_filter_ban_subscription));
                    // and discard subscription
                    try {
                        discardSubscription(account, ContactJid.from(from.toString()));
                    } catch (NetworkException | ContactJid.UserJidCreateException e) {
                        e.printStackTrace();
                    }

                    return;
                }

                // require captcha for subscription
                if (SettingsManager.spamFilterMode() == SettingsManager.SpamFilterMode.authCaptcha) {

                    Captcha captcha = CaptchaManager.getInstance().getCaptcha(account, from);

                    // if captcha for this user already exist, check expires time and discard if need
                    if (captcha != null) {

                        if (captcha.getExpiresDate() < System.currentTimeMillis()) {
                            // discard subscription
                            try {
                                discardSubscription(account, ContactJid.from(from.toString()));
                            } catch (NetworkException | ContactJid.UserJidCreateException e) {
                                e.printStackTrace();
                            }
                            return;
                        }

                        // skip subscription, waiting for captcha in messageManager
                        return;

                    } else {
                        // generate captcha
                        String captchaQuestion = CaptchaManager.getInstance().generateAndSaveCaptcha(account, from);

                        // send captcha message to sender
                        MessageManager.getInstance().sendMessageWithoutChat(from.getJid(),
                                StringUtils.randomString(12), account,
                                Application.getInstance().getResources().getString(R.string.spam_filter_limit_subscription) + " " + captchaQuestion);

                        // and skip subscription, waiting for captcha in messageManager
                        return;
                    }
                }

                // subscription request
                handleSubscriptionRequest(account, from);
                break;
            case subscribed:
                handleSubscriptionAccept(connection.getAccount(), from);
                break;
        }
    }

    public void handleSubscriptionRequest(AccountJid account, ContactJid from) {
        Set<ContactJid> set = requestedSubscriptions.get(account);
        if (set != null && set.contains(from)) {
            try {
                acceptSubscription(account, from, false);
            } catch (NetworkException e) {
                LogManager.exception(this, e);
            }
            subscriptionRequestProvider.remove(account, from);
        } else {
            if (!RosterManager.getInstance().contactIsSubscribedTo(account, from)) {
                subscriptionRequestProvider.add(new SubscriptionRequest(account, from), null);
                createChatForIncomingRequest(account, from);
            }
        }
    }

    public void handleSubscriptionAccept(AccountJid account, ContactJid from) {
        createChatForAcceptingOutgoingRequest(account, from);
    }

    public void clearSubscriptionRequestNotification(AccountJid account, ContactJid from) {
        if (subscriptionRequestProvider.get(account, from) != null) {
            subscriptionRequestProvider.remove(account, from);
        }
    }

    public void onAuthorized(ConnectionItem connection) {
        try {
            resendPresence(connection.getAccount());
        } catch (NetworkException e) {
            LogManager.exception(this, e);
        }
    }

    public void onRosterEntriesUpdated(AccountJid account, Collection<Jid> entries) {
        for (Jid entry : entries) {
            try {
                ContactJid user = ContactJid.from(entry);
                if(subscriptionRequestProvider.get(account, user) != null) {
                    subscriptionRequestProvider.remove(account, user);
                    createChatForNewContact(account, user);
                }
            } catch (ContactJid.UserJidCreateException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<BareJid, Map<Resourcepart, Presence>> getPresencesTiedToAccount(BareJid account) {
        Map<BareJid, Map<Resourcepart, Presence>> listOfContactsWithPresences = presenceMap.get(account);
        if (listOfContactsWithPresences == null) {
            listOfContactsWithPresences = new ConcurrentHashMap<>();
            presenceMap.put(account, listOfContactsWithPresences);
        }
        return listOfContactsWithPresences;
    }

    private Map<Resourcepart, Presence> getSingleContactPresences(BareJid account, BareJid contact) {
        Map<BareJid, Map<Resourcepart, Presence>> listOfContactsWithPresences = getPresencesTiedToAccount(account);
        Map<Resourcepart, Presence> listOfContactPresences = listOfContactsWithPresences.get(contact);
        if (listOfContactPresences == null) {
            listOfContactPresences = new ConcurrentHashMap<>();
            listOfContactsWithPresences.put(contact, listOfContactPresences);
        }
        return listOfContactPresences;
    }

    private Map<Resourcepart, Presence> getSingleAccountPresences(BareJid bareJid) {
        Map<Resourcepart, Presence> accountPresences = accountsPresenceMap.get(bareJid);
        if (accountPresences == null) {
            accountPresences = new ConcurrentHashMap<>();
            accountsPresenceMap.put(bareJid, accountPresences);
        }
        return accountPresences;
    }

    public List<Presence> getAvailableAccountPresences(AccountJid account) {
        Map<Resourcepart, Presence> accountPresences = getSingleAccountPresences(account.getFullJid().asBareJid());
        ArrayList<Presence> allAccountAvailablePresences = new ArrayList<>(accountPresences.values().size());
        for (Presence presence : accountPresences.values()) {
            if (presence.isAvailable()) {
                allAccountAvailablePresences.add(presence.clone());
            }
        }
        return allAccountAvailablePresences;
    }

    public List<Presence> getAllPresences(AccountJid account, BareJid contact) {
        Map<Resourcepart, Presence> userPresences = isAccountPresence(account, contact) ? getSingleAccountPresences(contact) : getSingleContactPresences(account.getFullJid().asBareJid(), contact);
        List<Presence> res;
        if (userPresences.isEmpty()) {
            // Create an unavailable presence if none was found
            Presence unavailable = new Presence(Presence.Type.unavailable);
            unavailable.setFrom(contact);
            res = new ArrayList<>(Collections.singletonList(unavailable));
        } else {
            res = new ArrayList<>(userPresences.values().size());
            for (Presence presence : userPresences.values()) {
                res.add(presence.clone());
            }
        }
        return res;
    }

    public List<Presence> getAvailablePresences(AccountJid account, BareJid contact) {
        List<Presence> allPresences = getAllPresences(account, contact);
        List<Presence> res = new ArrayList<>(allPresences.size());
        for (Presence presence : allPresences) {
            if (presence.isAvailable()) {
                // No need to clone presence here, getAllPresences already returns clones
                res.add(presence);
            }
        }
        return res;
    }

    public Presence getPresence(AccountJid account, ContactJid user) {
        boolean isAccountPresence = isAccountPresence(account, user.getBareJid());
        Map<Resourcepart, Presence> userPresences = isAccountPresence ? getSingleAccountPresences(user.getBareJid()) : getSingleContactPresences(account.getFullJid().asBareJid(), user.getBareJid());
        if (userPresences.isEmpty()) {
            Presence presence = new Presence(Presence.Type.unavailable);
            presence.setFrom(user.getBareJid());
            return presence;
        }
        else {
            // Find the resource with the highest priority
            // Might be changed to use the resource with the highest availability instead.
            Presence presence = null;
            // This is used in case no available presence is found
            Presence unavailable = null;

            for (Resourcepart resource : userPresences.keySet()) {
                Presence p = userPresences.get(resource);
                if (p == null) {
                    continue;
                }
                if (!p.isAvailable()) {
                    unavailable = p;
                    continue;
                }
                // Chose presence with highest priority first.
                if (presence == null || p.getPriority() > presence.getPriority()) {
                    presence = p;
                }
                // If equal priority, choose "most available" by the mode value.
                else if (p.getPriority() == presence.getPriority()) {
                    Presence.Mode pMode = p.getMode();
                    // Default to presence mode of available.
                    if (pMode == null) {
                        pMode = Presence.Mode.available;
                    }
                    Presence.Mode presenceMode = presence.getMode();
                    // Default to presence mode of available.
                    if (presenceMode == null) {
                        presenceMode = Presence.Mode.available;
                    }
                    if (pMode.compareTo(presenceMode) < 0) {
                        presence = p;
                    }
                }
            }
            if (presence == null) {
                if (unavailable != null) {
                    return unavailable.clone();
                }
                else {
                    presence = new Presence(Presence.Type.unavailable);
                    presence.setFrom(user.getBareJid());
                    return presence;
                }
            }
            else {
                return presence.clone();
            }
        }
    }

    /**
     * clear internal presences of this account
     * @param account account
     */
    public void clearAccountPresences(AccountJid account) {
        getSingleAccountPresences(account.getFullJid().asBareJid()).clear();
    }

    /**
     * clear the presences of this contact
     * @param account account to which this contact is tied to
     * @param contact contact
     */
    public void clearSingleContactPresences(AccountJid account, BareJid contact) {
        getSingleContactPresences(account.getFullJid().asBareJid(), contact).clear();
    }

    /**
     * clear all contact presences tied to this account
     * @param account account
     */
    public void clearAllContactPresences(AccountJid account) {
        getPresencesTiedToAccount(account.getFullJid().asBareJid()).clear();
    }

    public void clearPresencesTiedToThisAccount(AccountJid account) {
        clearAccountPresences(account);
        clearAllContactPresences(account);
    }

    public static void sortPresencesByPriority(List<Presence> allPresences) {
        Collections.sort(allPresences, PresenceComparatorByPriority.INSTANCE);
    }

    public static boolean isAccountPresence(AccountJid account, BareJid from) {
        return AccountManager.getInstance().isAccountExist(from.toString())
                && account.getFullJid().asBareJid().equals(from);
    }
}