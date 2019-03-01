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
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.captcha.Captcha;
import com.xabber.android.data.extension.captcha.CaptchaManager;
import com.xabber.android.data.extension.iqlast.LastActivityInteractor;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.Occupant;
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
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final HashMap<AccountJid, Set<UserJid>> requestedSubscriptions;

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
    public SubscriptionRequest getSubscriptionRequest(AccountJid account, UserJid user) {
        return subscriptionRequestProvider.get(account, user);
    }

    /**
     * Requests subscription to the contact.
     *
     * @throws NetworkException
     */
    public void requestSubscription(AccountJid account, UserJid user) throws NetworkException {
        Presence packet = new Presence(Presence.Type.subscribe);
        packet.setTo(user.getJid());
        StanzaSender.sendStanza(account, packet);
        Set<UserJid> set = requestedSubscriptions.get(account);
        if (set == null) {
            set = new HashSet<>();
            requestedSubscriptions.put(account, set);
        }
        set.add(user);
        createChatForNewContact(account, user);
    }

    private void removeRequestedSubscription(AccountJid account, UserJid user) {
        Set<UserJid> set = requestedSubscriptions.get(account);
        if (set != null) {
            set.remove(user);
        }
    }

    /**
     * Accepts subscription request from the entity (share own presence).
     */
    public void acceptSubscription(AccountJid account, UserJid user) throws NetworkException {
        Presence packet = new Presence(Presence.Type.subscribed);
        packet.setTo(user.getJid());
        StanzaSender.sendStanza(account, packet);
        subscriptionRequestProvider.remove(account, user);
        removeRequestedSubscription(account, user);
    }

    /** Added available action to chat, to show chat in recent chats */
    private void createChatForNewContact(AccountJid account, UserJid user) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        chat.newAction(null, Application.getInstance().getResources().getString(R.string.action_new_contact),
                ChatAction.available, false);
    }

    /**
     * Discards subscription request from the entity (deny own presence
     * sharing).
     */
    public void discardSubscription(AccountJid account, UserJid user) throws NetworkException {
        Presence packet = new Presence(Presence.Type.unsubscribed);
        packet.setTo(user.getJid());
        StanzaSender.sendStanza(account, packet);
        subscriptionRequestProvider.remove(account, user);
        removeRequestedSubscription(account, user);
    }

    public boolean hasSubscriptionRequest(AccountJid account, UserJid bareAddress) {
        return getSubscriptionRequest(account, bareAddress) != null;
    }

    public StatusMode getStatusMode(AccountJid account, UserJid user) {
        final Occupant occupant = getOccupant(account, user);
        if (occupant != null) {
            return occupant.getStatusMode();
        }

        return StatusMode.createStatusMode(RosterManager.getInstance().getPresence(account, user));
    }

    /**
     * if contact is private MUC chat
     */
    @Nullable
    private Occupant getOccupant(AccountJid account, UserJid user) {
        EntityBareJid userEntityBareJid = user.getJid().asEntityBareJidIfPossible();
        if (userEntityBareJid == null) {
            return null;
        }

        Resourcepart resourcepart = user.getJid().getResourceOrNull();
        if (resourcepart == null) {
            return null;
        }

        if (MUCManager.getInstance().hasRoom(account, userEntityBareJid)) {
            final Collection<Occupant> occupants = MUCManager.getInstance().getOccupants(account,
                    userEntityBareJid);
            for (Occupant occupant : occupants) {
                if (occupant.getNickname().equals(resourcepart)) {
                    return occupant;
                }
            }
        }
        return null;
    }

    public String getStatusText(AccountJid account, UserJid bareAddress) {
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

    public void onPresenceChanged(AccountJid account, Presence presence) {
        UserJid from;
        try {
            from = UserJid.from(presence.getFrom());
        } catch (UserJid.UserJidCreateException e) {
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

        UserJid from;
        try {
            from = UserJid.from(stanza.getFrom());
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            return;
        }

        if (presence.getType() == Presence.Type.subscribe) {
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
                    discardSubscription(account, UserJid.from(from.toString()));
                } catch (NetworkException | UserJid.UserJidCreateException e) {
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
                            discardSubscription(account, UserJid.from(from.toString()));
                        } catch (NetworkException | UserJid.UserJidCreateException e) {
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
        }
    }

    public void handleSubscriptionRequest(AccountJid account, UserJid from) {
        Set<UserJid> set = requestedSubscriptions.get(account);
        if (set != null && set.contains(from)) {
            try {
                acceptSubscription(account, from);
            } catch (NetworkException e) {
                LogManager.exception(this, e);
            }
            subscriptionRequestProvider.remove(account, from);
        } else {
            subscriptionRequestProvider.add(new SubscriptionRequest(account, from), null);
        }
    }

    public void onAuthorized(ConnectionItem connection) {
        try {
            resendPresence(connection.getAccount());
        } catch (NetworkException e) {
            LogManager.exception(this, e);
        }
    }

    public static void sortPresencesByPriority(List<Presence> allPresences) {
        Collections.sort(allPresences, PresenceComparatorByPriority.INSTANCE);
    }
}