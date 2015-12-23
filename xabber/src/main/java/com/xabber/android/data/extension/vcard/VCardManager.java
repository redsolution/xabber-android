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
package com.xabber.android.data.extension.vcard;

import android.database.Cursor;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.roster.OnRosterChangedListener;
import com.xabber.android.data.roster.OnRosterReceivedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.StructuredName;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.vcard.VCardProperty;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Manage vCards and there requests.
 *
 * @author alexander.ivanov
 */
public class VCardManager implements OnLoadListener, OnPacketListener,
        OnRosterReceivedListener, OnAccountRemovedListener {

    private static final StructuredName EMPTY_STRUCTURED_NAME = new StructuredName(
            null, null, null, null, null);

    /**
     * Nick and formatted names for the users.
     */
    private final Map<String, StructuredName> names;

    /**
     * List of accounts which requests its avatar in order to avoid subsequence
     * requests.
     */
    private final ArrayList<String> accountRequested;

    private final static VCardManager instance;

    private Set<String> vCardRequests = new ConcurrentSkipListSet<>();
    private Set<String> vCardSaveRequests = new ConcurrentSkipListSet<>();

    static {
        instance = new VCardManager();
        Application.getInstance().addManager(instance);
    }

    public static VCardManager getInstance() {
        return instance;
    }

    private VCardManager() {
        names = new HashMap<>();
        accountRequested = new ArrayList<>();
    }

    @Override
    public void onLoad() {
        final Map<String, StructuredName> names = new HashMap<>();
        Cursor cursor = VCardTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    names.put(
                            VCardTable.getUser(cursor),
                            new StructuredName(VCardTable.getNickName(cursor),
                                    VCardTable.getFormattedName(cursor),
                                    VCardTable.getFirstName(cursor), VCardTable
                                    .getMiddleName(cursor), VCardTable
                                    .getLastName(cursor)));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(names);
            }
        });
    }

    private void onLoaded(Map<String, StructuredName> names) {
        this.names.putAll(names);
    }

    @Override
    public void onRosterReceived(AccountItem accountItem) {
        String account = accountItem.getAccount();
        if (!accountRequested.contains(account) && SettingsManager.connectionLoadVCard()) {
            String bareAddress = Jid.getBareAddress(accountItem.getRealJid());
            if (bareAddress != null && !names.containsKey(bareAddress)) {
                request(account, bareAddress);
                accountRequested.add(account);
            }
        }

        // Request vCards for new contacts.
        for (RosterContact contact : RosterManager.getInstance().getContacts()) {
            if (account.equals(contact.getAccount()) && !names.containsKey(contact.getUser())) {
                request(account, contact.getUser());
            }
        }
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        accountRequested.remove(accountItem.getAccount());
    }

    /**
     * Requests vCard.
     */
    public void request(String account, String bareAddress) {
        requestVCard(account, bareAddress);
    }

    /**
     * Get uses's nick name.
     *
     * @param bareAddress
     * @return first specified value:
     * <ul>
     * <li>nick name</li>
     * <li>formatted name</li>
     * <li>empty string</li>
     * </ul>
     */
    public String getName(String bareAddress) {
        StructuredName name = names.get(bareAddress);
        if (name == null)
            return "";
        return name.getBestName();
    }

    /**
     * Get uses's name information.
     *
     * @param bareAddress
     * @return <code>null</code> if there is no info.
     */
    public StructuredName getStructuredName(String bareAddress) {
        return names.get(bareAddress);
    }

    private void onVCardReceived(final String account, final String bareAddress, final VCard vCard) {
        final StructuredName name;
        if (vCard.getType() == Type.error) {
            onVCardFailed(account, bareAddress);
            if (names.containsKey(bareAddress)) {
                return;
            }
            name = EMPTY_STRUCTURED_NAME;
        } else {
            for (OnVCardListener listener : Application.getInstance().getUIListeners(OnVCardListener.class)) {
                listener.onVCardReceived(account, bareAddress, vCard);
            }

            String hash = vCard.getAvatarHash();
            AvatarManager.getInstance().onAvatarReceived(bareAddress, hash, vCard.getAvatar());
            name = new StructuredName(vCard.getNickName(), vCard.getField(VCardProperty.FN.name()),
                    vCard.getFirstName(), vCard.getMiddleName(), vCard.getLastName());

            Application.getInstance().runInBackground(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Jid.getBareAddress(account).equals(bareAddress)) {
                            PresenceManager.getInstance().resendPresence(account);
                        }
                    } catch (NetworkException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
        names.put(bareAddress, name);
        for (RosterContact rosterContact : RosterManager.getInstance().getContacts()) {
            if (rosterContact.getUser().equals(bareAddress)) {
                for (OnRosterChangedListener listener : Application.getInstance()
                        .getManagers(OnRosterChangedListener.class)) {
                    listener.onContactStructuredInfoChanged(rosterContact, name);
                }
            }
        }
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                VCardTable.getInstance().write(bareAddress, name);
            }
        });
        if (vCard.getFrom() == null) { // account it self
            AccountManager.getInstance().onAccountChanged(account);
        } else {
            RosterManager.getInstance().onContactChanged(account, bareAddress);
        }
    }

    private void onVCardFailed(final String account, final String bareAddress) {
        for (OnVCardListener listener : Application.getInstance().getUIListeners(OnVCardListener.class)) {
            listener.onVCardFailed(account, bareAddress);
        }
    }

    private void onVCardSaveSuccess(String account) {
        for (OnVCardSaveListener listener : Application.getInstance().getUIListeners(OnVCardSaveListener.class)) {
            listener.onVCardSaveSuccess(account);
        }
    }

    private void onVCardSaveFailed(String account) {
        for (OnVCardSaveListener listener : Application.getInstance().getUIListeners(OnVCardSaveListener.class)) {
            listener.onVCardSaveFailed(account);
        }
    }

    @Override
    public void onPacket(ConnectionItem connection, final String bareAddress, Stanza packet) {
        if (!(connection instanceof AccountItem)) {
            return;
        }
        String account = ((AccountItem) connection).getAccount();
        if (packet instanceof Presence && ((Presence) packet).getType() != Presence.Type.error) {
            if (bareAddress == null) {
                return;
            }

            String addressForVcard = bareAddress;

            if (MUCManager.getInstance().hasRoom(account, bareAddress)) {
                addressForVcard = packet.getFrom();
            }

            // Request vCard for new users
            if (!names.containsKey(addressForVcard)) {
                if (SettingsManager.connectionLoadVCard()) {
                    request(account, addressForVcard);
                }
            }
        }
    }

    private void requestVCard(final String account, final String srcUser) {
        final String userBareJid = srcUser;

        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        ConnectionThread connectionThread = accountItem.getConnectionThread();

        if (!accountItem.getFactualStatusMode().isOnline() || connectionThread == null) {
            onVCardFailed(account, userBareJid);
            return;
        }

        final org.jivesoftware.smackx.vcardtemp.VCardManager vCardManager
                = org.jivesoftware.smackx.vcardtemp.VCardManager.getInstanceFor(connectionThread.getXMPPConnection());

        final Thread thread = new Thread("Get vCard user " + userBareJid + " for account " + account) {
            @Override
            public void run() {
                VCard vCard = null;

                vCardRequests.add(userBareJid);
                try {
                    vCard = vCardManager.loadVCard(userBareJid);
                } catch (SmackException.NoResponseException | SmackException.NotConnectedException e) {
                    LogManager.w(this, "Error getting vCard: " + e.getMessage());
                } catch (XMPPException.XMPPErrorException e ) {
                    LogManager.w(this, "XMPP error getting vCard: " + e.getMessage() + e.getXMPPError());

                    if (e.getXMPPError().getCondition() == XMPPError.Condition.item_not_found) {
                        vCard = new VCard();
                    }

                } catch (ClassCastException e) {
                    // http://stackoverflow.com/questions/31498721/error-loading-vcard-information-using-smack-emptyresultiq-cannot-be-cast-to-or
                    LogManager.w(this, "ClassCastException: " + e.getMessage());
                    vCard = new VCard();
                }
                vCardRequests.remove(userBareJid);

                final VCard finalVCard = vCard;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalVCard == null) {
                            onVCardFailed(account, userBareJid);
                        } else {
                            onVCardReceived(account, userBareJid, finalVCard);
                        }
                    }
                });
            }
        };
        thread.start();
    }

    public void saveVCard(final String account, final VCard vCard) {
        ConnectionThread connectionThread = AccountManager.getInstance().getAccount(account).getConnectionThread();
        if (connectionThread == null) {
            onVCardSaveFailed(account);
            return;
        }

        final AbstractXMPPConnection xmppConnection = connectionThread.getXMPPConnection();
        final org.jivesoftware.smackx.vcardtemp.VCardManager vCardManager = org.jivesoftware.smackx.vcardtemp.VCardManager.getInstanceFor(xmppConnection);

        final Thread thread = new Thread("Save vCard for account " + account) {
            @Override
            public void run() {

                boolean isSuccess = true;

                xmppConnection.setPacketReplyTimeout(120000);

                vCardSaveRequests.add(account);
                try {
                    vCardManager.saveVCard(vCard);
                    String avatarHash = vCard.getAvatarHash();
                    if (avatarHash == null) {
                        avatarHash = AvatarManager.EMPTY_HASH;
                    }
                    PresenceManager.getInstance().sendVCardUpdatePresence(account, avatarHash);
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                        | SmackException.NotConnectedException | NetworkException e) {
                    LogManager.w(this, "Error saving vCard: " + e.getMessage());
                    isSuccess = false;
                }
                vCardSaveRequests.remove(account);

                xmppConnection.setPacketReplyTimeout(ConnectionManager.PACKET_REPLY_TIMEOUT);

                final boolean finalIsSuccess = isSuccess;
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalIsSuccess) {
                            onVCardSaveSuccess(account);
                        } else {
                            onVCardSaveFailed(account);
                        }
                    }
                });

            }
        };
        thread.start();
    }

    public boolean isVCardRequested(String user) {
        return vCardRequests.contains(Jid.getBareAddress(user));
    }

    public boolean isVCardSaveRequested(String account) {
        return vCardSaveRequests.contains(account);
    }

}
