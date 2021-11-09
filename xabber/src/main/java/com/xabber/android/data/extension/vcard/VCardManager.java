/*
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
package com.xabber.android.data.extension.vcard;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnConnectedListener;
import com.xabber.android.data.connection.OnDisconnectListener;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.database.realmobjects.VCardRealmObject;
import com.xabber.android.data.database.repositories.ContactRepository;
import com.xabber.android.data.database.repositories.VCardRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.groups.GroupsManager;
import com.xabber.android.data.extension.iqlast.LastActivityInteractor;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.data.roster.OnRosterChangedListener;
import com.xabber.android.data.roster.OnRosterReceivedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.StructuredName;
import com.xabber.android.ui.OnVCardListener;
import com.xabber.android.ui.OnVCardSaveListener;
import com.xabber.xmpp.vcard.VCard;
import com.xabber.xmpp.vcard.VCardProperty;
import com.xabber.xmpp.vcardupdate.VCardUpdate;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.id.StanzaIdUtil;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Manage vCards and there requests.
 *
 * @author alexander.ivanov
 */
public class VCardManager implements OnPacketListener, OnRosterReceivedListener,
        OnAccountRemovedListener, OnDisconnectListener {

    private static final String LOG_TAG = VCardManager.class.getSimpleName();

    private static final StructuredName EMPTY_STRUCTURED_NAME = new StructuredName(
            null, null, null, null, null);

    /**
     * Nick and formatted names for the users.
     */
    private final Map<Jid, StructuredName> names;

    /**
     * List of accounts which requests its avatar in order to avoid subsequence
     * requests.
     */
    private final ArrayList<AccountJid> accountRequested;

    private static VCardManager instance;

    @SuppressWarnings("WeakerAccess")
    Map<AccountJid, Set<Jid>> vCardRequests = new ConcurrentHashMap<>();
    @SuppressWarnings("WeakerAccess")
    Set<AccountJid> vCardSaveRequests = new ConcurrentSkipListSet<>();

    private final Map<AccountItem, RosterAndHistoryLoadState> rosterOrHistoryIsLoaded = new ConcurrentHashMap<>();

    enum RosterAndHistoryLoadState {
        ROSTER, HISTORY, BOTH
    }

    public static VCardManager getInstance() {
        if (instance == null) {
            instance = new VCardManager();
        }

        return instance;
    }

    private VCardManager() {
        names = new HashMap<>();
        accountRequested = new ArrayList<>();
        for (VCardRealmObject vCardRealmObject : VCardRepository.getAllVCardsFromRealm()){
            names.put(vCardRealmObject.getContactJid().getJid(), vCardRealmObject.getStructuredName());
        }
    }

    private void requestRosterVCards(AccountItem accountItem) {
        AccountJid account = accountItem.getAccount();
        if (!accountRequested.contains(account) && SettingsManager.connectionLoadVCard()) {
            BareJid bareAddress = accountItem.getRealJid().asBareJid();
            if (bareAddress != null && !names.containsKey(bareAddress)) {
                request(account, bareAddress);
                accountRequested.add(account);
            }
        }

        Collection<RosterContact> accountRosterContacts = RosterManager.getInstance().getAccountRosterContacts(account);

        // Request vCards for new contacts.
        for (RosterContact contact : accountRosterContacts) {
            if (!names.containsKey(contact.getContactJid().getJid())) {
                request(account, contact.getContactJid().getJid());
            }
        }
    }

    private void requestRosterLastActivity(AccountItem accountItem) {
        AccountJid account = accountItem.getAccount();
        Collection<RosterContact> accountRosterContacts = RosterManager.getInstance().getAccountRosterContacts(account);
        LastActivityInteractor.getInstance().requestRosterLastActivity(account, accountRosterContacts);
    }

    @Override
    public void onRosterReceived(AccountItem accountItem) {
        LogManager.d("VCardManager", "roster received");
        RosterAndHistoryLoadState loaded = rosterOrHistoryIsLoaded.get(accountItem);
        if (loaded == RosterAndHistoryLoadState.HISTORY) {
            rosterOrHistoryIsLoaded.put(accountItem, RosterAndHistoryLoadState.BOTH);
            requestRosterVCards(accountItem);
        } else {
            rosterOrHistoryIsLoaded.put(accountItem, RosterAndHistoryLoadState.ROSTER);
            requestRosterLastActivity(accountItem);
        }
    }

    @Override
    public void onDisconnect(ConnectionItem connection) { resetLoadedState(connection.getAccount()); }

    public boolean isRosterOrHistoryLoaded(AccountJid accountJid) {
        AccountItem account = AccountManager.INSTANCE.getAccount(accountJid);
        if (account == null) return false;
        RosterAndHistoryLoadState loaded = rosterOrHistoryIsLoaded.get(account);
        return loaded != null;
    }

    // TODO
    //  make sure this will not be called after reconnecting and getting full roster.
    //  (if the connection listener is calling this method too late)
    public void resetLoadedState(AccountJid accountJid) {
        AccountItem accountItem = AccountManager.INSTANCE.getAccount(accountJid);
        if (accountItem != null) {
            rosterOrHistoryIsLoaded.remove(accountItem);
            vCardRequests.remove(accountJid);
            LastActivityInteractor.getInstance().interruptLastActivityRequest(accountJid);
        }
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        accountRequested.remove(accountItem.getAccount());
    }

    public void requestByUser(final AccountJid account, final Jid jid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> getVCard(account, jid));
    }

    /**
     * Requests vCard.
     */
    public void request(final AccountJid account, final Jid jid) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> getVCard(account, jid));
    }

    /**
     * Get uses's nick name.
     *
     * @return first specified value:
     * <ul>
     * <li>nick name</li>
     * <li>formatted name</li>
     * <li>empty string</li>
     * </ul>
     */
    public String getName(Jid jid) {
        StructuredName name = names.get(jid);
        if (name == null)
            if (ContactRepository.getBestNameFromRealm(jid) == null)
                return "";
            else
                return ContactRepository.getBestNameFromRealm(jid);
        return name.getBestName();
    }

    @SuppressWarnings("WeakerAccess")
    void onVCardReceived(final AccountJid account, final Jid bareAddress, final VCard vCard) {
        final StructuredName name;
        if (vCard.getType() == Type.error) {
            onVCardFailed(account, bareAddress);
            removeVCardRequest(account, bareAddress.asBareJid());
            if (names.containsKey(bareAddress)) {
                return;
            }
            name = EMPTY_STRUCTURED_NAME;
        } else {
            try {
                String hash = vCard.getAvatarHash();
                byte[] avatar = vCard.getAvatar();
                AvatarManager.getInstance().onAvatarReceived(bareAddress, hash, avatar, "vcard");
                // "bad base-64" error happen sometimes
            } catch (IllegalArgumentException e) {
                LogManager.exception(this, e);
            }

            name = new StructuredName(vCard.getNickName(), vCard.getField(VCardProperty.FN.name()),
                    vCard.getFirstName(), vCard.getMiddleName(), vCard.getLastName());

        }

        names.put(bareAddress, name);
        removeVCardRequest(account, bareAddress.asBareJid());

        try {
            VCardRepository.saveOrUpdateVCardToRealm(ContactJid.from(bareAddress), vCard);
        } catch (Exception e) { LogManager.exception(LOG_TAG, e); }


        RosterContact rosterContact = RosterManager.getInstance()
                .getRosterContact(account, bareAddress.asBareJid());

        for (OnRosterChangedListener listener : Application.getInstance()
                .getManagers(OnRosterChangedListener.class)) {
            listener.onContactStructuredInfoChanged(rosterContact, name);
        }

        if (vCard.getFrom() == null) { // account it self
            AccountManager.INSTANCE.onAccountChanged(account);
        } else {
            try {
                RosterManager.onContactChanged(account, ContactJid.from(bareAddress));
            } catch (ContactJid.ContactJidCreateException e) {
                LogManager.exception(this, e);
            }
        }

        for (OnVCardListener listener : Application.getInstance().getUIListeners(OnVCardListener.class)) {
            listener.onVCardReceived(account, bareAddress, vCard);
        }

        try {
            if(ChatManager.getInstance().getChat(account, ContactJid.from(bareAddress)) instanceof GroupChat){
                GroupsManager.INSTANCE.processVcard(account, ContactJid.from(bareAddress), vCard);
            }
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }

    }

    @SuppressWarnings("WeakerAccess")
    void onVCardFailed(final AccountJid account, final Jid bareAddress) {
        for (OnVCardListener listener : Application.getInstance().getUIListeners(OnVCardListener.class)) {
            listener.onVCardFailed(account, bareAddress);
        }
    }

    @SuppressWarnings("WeakerAccess")
    void onVCardSaveSuccess(AccountJid account) {
        for (OnVCardSaveListener listener : Application.getInstance().getUIListeners(OnVCardSaveListener.class)) {
            listener.onVCardSaveSuccess(account);
        }
    }

    @SuppressWarnings("WeakerAccess")
    void onVCardSaveFailed(AccountJid account) {
        for (OnVCardSaveListener listener : Application.getInstance().getUIListeners(OnVCardSaveListener.class)) {
            listener.onVCardSaveFailed(account);
        }
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (!(connection instanceof AccountItem)) {
            return;
        }
        AccountJid account = connection.getAccount();
        if (stanza instanceof Presence) {
            Presence presence = (Presence) stanza;
            if (presence.getType() == Presence.Type.error) return;
            RosterAndHistoryLoadState loaded = rosterOrHistoryIsLoaded.get(connection);
            if (presence.isAvailable() || presence.getType().equals(Presence.Type.subscribe) || loaded == RosterAndHistoryLoadState.BOTH) {
                Jid from = stanza.getFrom();

                if (from == null) {
                    return;
                }

                // Request vCard for new users
                if (!names.containsKey(from)) {
                    if (SettingsManager.connectionLoadVCard()) {
                        request(account, from);
                    }
                }
            }
        }

        if (stanza instanceof VCard) {
            Jid from = stanza.getFrom();
            if (from == null) return;
            onVCardReceived(account, from, (VCard) stanza);
        }
    }

    @SuppressWarnings("WeakerAccess")
    void getVCard(final AccountJid account, final Jid srcUser) {
        final AccountItem accountItem = AccountManager.INSTANCE.getAccount(account);
        if (accountItem == null) {
            onVCardFailed(account, srcUser);
            return;
        }

        if (!accountItem.getConnection().isAuthenticated()) {
            onVCardFailed(account, srcUser);
            return;
        }

        final BareJid bareJid = srcUser.asBareJid();

        if (bareJid != null) {
            if (isVCardRequested(account, bareJid)) {
                return;
            } else {
                addVCardRequest(account, bareJid);
            }
            try {
                sendVCardRequest(accountItem.getConnection(), bareJid);
            } catch (SmackException.NotConnectedException e) {
                LogManager.exception(this, e);
                LogManager.w(this, "Error getting vCard: " + e.getMessage());
                onVCardFailed(account, srcUser);
                removeVCardRequest(account, bareJid);
            } catch (ClassCastException e) {
                LogManager.exception(this, e);
                // http://stackoverflow.com/questions/31498721/error-loading-vcard-information-using-smack-emptyresultiq-cannot-be-cast-to-or
                LogManager.w(this, "ClassCastException: " + e.getMessage());
                //vCard = new VCard();
                onVCardFailed(account, srcUser);
                removeVCardRequest(account, bareJid);
            } catch (InterruptedException e) {
                LogManager.exception(this, e);
                onVCardFailed(account, srcUser);
                removeVCardRequest(account, bareJid);
            }
        }
    }

    private void sendVCardRequest(XMPPConnection connection, Jid jid) throws SmackException.NotConnectedException, InterruptedException {
        VCard vcardRequest = new VCard();
        vcardRequest.setTo(jid);
        connection.sendStanza(vcardRequest);
    }

    public void saveVCard(final AccountJid account, final VCard vCard) {
        AccountItem accountItem = AccountManager.INSTANCE.getAccount(account);
        if (accountItem == null) {
            onVCardSaveFailed(account);
            return;
        }

        final AbstractXMPPConnection xmppConnection = accountItem.getConnection();

        Application.getInstance().runInBackgroundNetworkUserRequest(new Runnable() {
            @Override
            public void run() {

                boolean isSuccess = true;

                xmppConnection.setPacketReplyTimeout(120000);

                vCardSaveRequests.add(account);
                try {
                    sendUpdateAccountVcardStanza(xmppConnection, vCard);
                    String avatarHash = null;

                    try {
                        avatarHash = vCard.getAvatarHash();
                        // "bad base-64" error happen sometimes
                    } catch (IllegalArgumentException e) {
                        LogManager.exception(this, e);
                    }
                    if (avatarHash == null) {
                        avatarHash = AvatarManager.EMPTY_HASH;
                    }
                    PresenceManager.INSTANCE.sendVCardUpdatePresence(account, avatarHash);
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                        | SmackException.NotConnectedException | NetworkException | InterruptedException e) {
                    LogManager.w(this, "Error saving vCard: " + e.getMessage());
                    isSuccess = false;
                }
                vCardSaveRequests.remove(account);

                xmppConnection.setPacketReplyTimeout(ConnectionManager.PACKET_REPLY_TIMEOUT);

                final boolean finalIsSuccess = isSuccess;
                Application.getInstance().runOnUiThread(() -> {
                    if (finalIsSuccess) {
                        onVCardSaveSuccess(account);
                    } else {
                        onVCardSaveFailed(account);
                    }
                });
            }
        });
    }

    public void addVCardUpdateToPresence(Presence presence, String hash) {
        final VCardUpdate vCardUpdate = new VCardUpdate();
        vCardUpdate.setPhotoHash(hash);
        presence.addExtension(vCardUpdate);
    }

    /**
     * Save this vCard for the user connected by 'connection'. XMPPConnection should be authenticated
     * and not anonymous.
     *
     * @throws XMPPException.XMPPErrorException thrown if there was an issue setting the VCard in the server.
     * @throws SmackException.NoResponseException if there was no response from the server.
     */
    private void sendUpdateAccountVcardStanza(XMPPConnection connection, VCard vcard) throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException {
        // XEP-54 § 3.2 "A user may publish or update his or her vCard by sending an IQ of type "set" with no 'to' address…"
        vcard.setTo((Jid) null);
        vcard.setType(IQ.Type.set);
        // Also make sure to generate a new stanza id (the given vcard could be a vcard result), in which case we don't
        // want to use the same stanza id again (although it wouldn't break if we did)
        vcard.setStanzaId(StanzaIdUtil.newStanzaId());
        connection.createStanzaCollectorAndSend(vcard).nextResultOrThrow();
    }

    private void addVCardRequest(AccountJid accountJid, Jid user) {
        Set<Jid> requests = vCardRequests.get(accountJid);
        if (requests == null) {
            requests = new ConcurrentSkipListSet<>();
            vCardRequests.put(accountJid, requests);
        }
        requests.add(user);
    }

    private void removeVCardRequest(AccountJid accountJid, Jid user) {
        Set<Jid> requests = vCardRequests.get(accountJid);
        if (requests == null) {
            requests = new ConcurrentSkipListSet<>();
            vCardRequests.put(accountJid, requests);
        }
        requests.remove(user);
    }

    public boolean isVCardRequested(AccountJid accountJid, Jid user) {
        Set<Jid> requests = vCardRequests.get(accountJid);
        if (requests == null) {
            requests = new ConcurrentSkipListSet<>();
            vCardRequests.put(accountJid, requests);
        }
        return requests.contains(user.asBareJid());
    }

    public boolean isVCardSaveRequested(AccountJid account) {
        return vCardSaveRequests.contains(account);
    }

}
