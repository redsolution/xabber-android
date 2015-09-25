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
package com.xabber.android.ui.adapter;

import android.app.Activity;
import android.os.Handler;
import android.widget.Filter;
import android.widget.Filterable;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Adapter for contact list in the main activity.
 *
 * @author alexander.ivanov
 */
public class ContactListAdapter extends GroupedContactAdapter implements Runnable, Filterable {

    /**
     * Number of milliseconds between lazy refreshes.
     */
    private static final long REFRESH_INTERVAL = 1000;

    /**
     * Handler for deferred refresh.
     */
    private final Handler handler;

    /**
     * Lock for refresh requests.
     */
    private final Object refreshLock;

    /**
     * Whether refresh was requested.
     */
    private boolean refreshRequested;

    /**
     * Whether refresh is in progress.
     */
    private boolean refreshInProgress;

    /**
     * Minimal time when next refresh can be executed.
     */
    private Date nextRefresh;

    /**
     * Contact filter.
     */
    ContactFilter contactFilter;

    /**
     * Filter string. Can be <code>null</code> if filter is disabled.
     */
    String filterString;

    private final OnContactListChangedListener listener;
    private boolean hasActiveChats = false;

    public ContactListAdapter(Activity activity, OnContactListChangedListener listener,
                              OnClickListener onClickListener) {
        super(activity, onClickListener);
        this.listener = listener;
        handler = new Handler();
        refreshLock = new Object();
        refreshRequested = false;
        refreshInProgress = false;
        nextRefresh = new Date();
    }

    /**
     * Requests refresh in some time in future.
     */
    public void refreshRequest() {
        synchronized (refreshLock) {
            if (refreshRequested) {
                return;
            }
            if (refreshInProgress) {
                refreshRequested = true;
            } else {
                long delay = nextRefresh.getTime() - new Date().getTime();
                handler.postDelayed(this, delay > 0 ? delay : 0);
            }
        }
    }

    /**
     * Remove refresh requests.
     */
    public void removeRefreshRequests() {
        synchronized (refreshLock) {
            refreshRequested = false;
            refreshInProgress = false;
            handler.removeCallbacks(this);
        }
    }

    @Override
    public void onChange() {
        synchronized (refreshLock) {
            refreshRequested = false;
            refreshInProgress = true;
            handler.removeCallbacks(this);
        }

        final Collection<RosterContact> rosterContacts = RosterManager.getInstance().getContacts();
        final boolean showOffline = SettingsManager.contactsShowOffline();
        final boolean showGroups = SettingsManager.contactsShowGroups();
        final boolean showEmptyGroups = SettingsManager.contactsShowEmptyGroups();
        final boolean showActiveChats = SettingsManager.contactsShowActiveChats();
        final boolean stayActiveChats = SettingsManager.contactsStayActiveChats();
        final boolean showAccounts = SettingsManager.contactsShowAccounts();
        final Comparator<AbstractContact> comparator = SettingsManager.contactsOrder();
        final CommonState commonState = AccountManager.getInstance().getCommonState();
        final String selectedAccount = AccountManager.getInstance().getSelectedAccount();


        /**
         * Groups.
         */
        final Map<String, GroupConfiguration> groups;

        /**
         * Contacts.
         */
        final List<AbstractContact> contacts;

        /**
         * List of active chats.
         */
        final GroupConfiguration activeChats;

        /**
         * Whether there is at least one contact.
         */
        boolean hasContacts = false;

        /**
         * Whether there is at least one visible contact.
         */
        boolean hasVisibleContacts = false;

        final Map<String, AccountConfiguration> accounts = new TreeMap<>();

        for (String account : AccountManager.getInstance().getAccounts()) {
            accounts.put(account, null);
        }

        /**
         * List of rooms and active chats grouped by users inside accounts.
         */
        final Map<String, Map<String, AbstractChat>> abstractChats = new TreeMap<>();

        for (AbstractChat abstractChat : MessageManager.getInstance().getChats()) {
            if ((abstractChat instanceof RoomChat || abstractChat.isActive())
                    && accounts.containsKey(abstractChat.getAccount())) {
                final String account = abstractChat.getAccount();
                Map<String, AbstractChat> users = abstractChats.get(account);
                if (users == null) {
                    users = new TreeMap<>();
                    abstractChats.put(account, users);
                }
                users.put(abstractChat.getUser(), abstractChat);
            }
        }

        if (filterString == null) {
            // Create arrays.
            if (showAccounts) {
                groups = null;
                contacts = null;
                for (Entry<String, AccountConfiguration> entry : accounts.entrySet()) {
                    entry.setValue(new AccountConfiguration(entry.getKey(),
                            GroupManager.IS_ACCOUNT, GroupManager.getInstance()));
                }
            } else {
                if (showGroups) {
                    groups = new TreeMap<>();
                    contacts = null;
                } else {
                    groups = null;
                    contacts = new ArrayList<>();
                }
            }
            if (showActiveChats) {
                activeChats = new GroupConfiguration(GroupManager.NO_ACCOUNT,
                        GroupManager.ACTIVE_CHATS, GroupManager.getInstance());
            } else {
                activeChats = null;
            }

            // Build structure.
            for (RosterContact rosterContact : rosterContacts) {
                if (!rosterContact.isEnabled()) {
                    continue;
                }
                hasContacts = true;
                final boolean online = rosterContact.getStatusMode().isOnline();
                final String account = rosterContact.getAccount();
                final Map<String, AbstractChat> users = abstractChats.get(account);
                final AbstractChat abstractChat;
                if (users == null) {
                    abstractChat = null;
                } else {
                    abstractChat = users.remove(rosterContact.getUser());
                }
                if (showActiveChats && abstractChat != null && abstractChat.isActive()) {
                    activeChats.setNotEmpty();
                    hasVisibleContacts = true;
                    if (activeChats.isExpanded()) {
                        activeChats.addAbstractContact(rosterContact);
                    }
                    activeChats.increment(online);
                    if (!stayActiveChats || (!showAccounts && !showGroups)) {
                        continue;
                    }
                }
                if (selectedAccount != null && !selectedAccount.equals(account)) {
                    continue;
                }
                if (addContact(rosterContact, online, accounts, groups,
                        contacts, showAccounts, showGroups, showOffline)) {
                    hasVisibleContacts = true;
                }
            }
            for (Map<String, AbstractChat> users : abstractChats.values()) {
                for (AbstractChat abstractChat : users.values()) {
                    final AbstractContact abstractContact;
                    if (abstractChat instanceof RoomChat) {
                        abstractContact = new RoomContact((RoomChat) abstractChat);
                    } else {
                        abstractContact = new ChatContact(abstractChat);
                    }
                    if (showActiveChats && abstractChat.isActive()) {
                        activeChats.setNotEmpty();
                        hasVisibleContacts = true;
                        if (activeChats.isExpanded()) {
                            activeChats.addAbstractContact(abstractContact);
                        }
                        activeChats.increment(false);
                        if (!stayActiveChats || (!showAccounts && !showGroups)) {
                            continue;
                        }
                    }
                    if (selectedAccount != null && !selectedAccount.equals(abstractChat.getAccount())) {
                        continue;
                    }
                    final String group;
                    final boolean online;
                    if (abstractChat instanceof RoomChat) {
                        group = GroupManager.IS_ROOM;
                        online = abstractContact.getStatusMode().isOnline();
                    } else {
                        group = GroupManager.NO_GROUP;
                        online = false;
                    }
                    hasVisibleContacts = true;
                    addContact(abstractContact, group, online, accounts, groups, contacts,
                            showAccounts, showGroups);
                }
            }

            hasActiveChats = activeChats != null && activeChats.getTotal() > 0;

            // Remove empty groups, sort and apply structure.
            baseEntities.clear();
            if (hasVisibleContacts) {
                if (showActiveChats) {
                    if (!activeChats.isEmpty()) {
                        if (showAccounts || showGroups) {
                            baseEntities.add(activeChats);
                        }
                        activeChats.sortAbstractContacts(ComparatorByChat.COMPARATOR_BY_CHAT);
                        baseEntities.addAll(activeChats.getAbstractContacts());
                    }
                }
                if (showAccounts) {
                    boolean isFirst = baseEntities.isEmpty();
                    for (AccountConfiguration rosterAccount : accounts.values()) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            baseEntities.add(new AccountTopSeparator(null, null));
                        }

                        baseEntities.add(rosterAccount);

                        if (showGroups) {
                            if (rosterAccount.isExpanded()) {
                                for (GroupConfiguration rosterConfiguration : rosterAccount
                                        .getSortedGroupConfigurations()) {
                                    if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                                        baseEntities.add(rosterConfiguration);
                                        rosterConfiguration.sortAbstractContacts(comparator);
                                        baseEntities.addAll(rosterConfiguration.getAbstractContacts());
                                    }
                                }
                            }
                        } else {
                            rosterAccount.sortAbstractContacts(comparator);
                            baseEntities.addAll(rosterAccount.getAbstractContacts());
                        }

                        if (rosterAccount.getTotal() > 0 && !rosterAccount.isExpanded()) {
                            baseEntities.add(new AccountBottomSeparator(rosterAccount.getAccount(), null));
                        }
                    }
                } else {
                    if (showGroups) {
                        for (GroupConfiguration rosterConfiguration : groups.values()) {
                            if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                                baseEntities.add(rosterConfiguration);
                                rosterConfiguration.sortAbstractContacts(comparator);
                                baseEntities.addAll(rosterConfiguration.getAbstractContacts());
                            }
                        }
                    } else {
                        Collections.sort(contacts, comparator);
                        baseEntities.addAll(contacts);
                    }
                }
            }
        } else { // Search
            final ArrayList<AbstractContact> baseEntities = getSearchResults(rosterContacts, comparator, abstractChats);
            this.baseEntities.clear();
            this.baseEntities.addAll(baseEntities);
            hasVisibleContacts = baseEntities.size() > 0;
        }

        super.onChange();
        listener.onContactListChanged(commonState, hasContacts, hasVisibleContacts, filterString != null);

        synchronized (refreshLock) {
            nextRefresh = new Date(new Date().getTime() + REFRESH_INTERVAL);
            refreshInProgress = false;
            handler.removeCallbacks(this); // Just to be sure.
            if (refreshRequested) {
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    }

    private ArrayList<AbstractContact> getSearchResults(Collection<RosterContact> rosterContacts,
                                                        Comparator<AbstractContact> comparator,
                                                        Map<String, Map<String, AbstractChat>> abstractChats) {
        final ArrayList<AbstractContact> baseEntities = new ArrayList<>();

        // Build structure.
        for (RosterContact rosterContact : rosterContacts) {
            if (!rosterContact.isEnabled()) {
                continue;
            }
            final String account = rosterContact.getAccount();
            final Map<String, AbstractChat> users = abstractChats.get(account);
            if (users != null) {
                users.remove(rosterContact.getUser());
            }
            if (rosterContact.getName().toLowerCase(locale).contains(filterString)) {
                baseEntities.add(rosterContact);
            }
        }
        for (Map<String, AbstractChat> users : abstractChats.values()) {
            for (AbstractChat abstractChat : users.values()) {
                final AbstractContact abstractContact;
                if (abstractChat instanceof RoomChat) {
                    abstractContact = new RoomContact((RoomChat) abstractChat);
                } else {
                    abstractContact = new ChatContact(abstractChat);
                }
                if (abstractContact.getName().toLowerCase(locale).contains(filterString)) {
                    baseEntities.add(abstractContact);
                }
            }
        }
        Collections.sort(baseEntities, comparator);
        return baseEntities;
    }

    @Override
    public void run() {
        onChange();
    }

    /**
     * Listener for contact list appearance changes.
     *
     * @author alexander.ivanov
     */
    public interface OnContactListChangedListener {

        void onContactListChanged(CommonState commonState, boolean hasContacts,
                                  boolean hasVisibleContacts, boolean isFilterEnabled);

    }

    public static class AccountTopSeparator extends BaseEntity {
        public AccountTopSeparator(String account, String user) {
            super(account, user);
        }
    }

    public static class AccountBottomSeparator extends BaseEntity {
        public AccountBottomSeparator(String account, String user) {
            super(account, user);
        }
    }

    @Override
    public Filter getFilter() {
        if (contactFilter == null) {
            contactFilter = new ContactFilter();
        }
        return contactFilter;
    }

    private class ContactFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            if (constraint == null || constraint.length() == 0) {
                filterString = null;
            } else {
                filterString = constraint.toString().toLowerCase(locale);
            }
            onChange();
        }

    }

    public boolean isHasActiveChats() {
        return hasActiveChats;
    }
}
