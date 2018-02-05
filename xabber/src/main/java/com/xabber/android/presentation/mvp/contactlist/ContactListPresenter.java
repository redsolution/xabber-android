package com.xabber.android.presentation.mvp.contactlist;

import android.content.Context;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.GroupVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ToolbarVO;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListAdapter;
import com.xabber.android.ui.adapter.contactlist.ContactListGroupUtils;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public class ContactListPresenter {

    private static ContactListPresenter instance;
    private ContactListView view;
    private Context context;

    private String filterString = null;
    private ContactListAdapter.ChatListState currentChatsState = ContactListAdapter.ChatListState.recent;

    public static ContactListPresenter getInstance(Context context) {
        if (instance == null) instance = new ContactListPresenter(context);
        return instance;
    }

    public ContactListPresenter(Context context) {
        this.context = context;
    }

    public void onLoadContactList(ContactListView view) {
        this.view = view;
        List<IFlexible> items = new ArrayList<>();
        buildStructure(items);

//        items.add(new ToolbarVO());
//
//        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts()) {
//            items.add(AccountVO.convert(new AccountConfiguration(accountJid, GroupManager.IS_ACCOUNT,
//                    GroupManager.getInstance())));
//            items.add(GroupVO.convert(new GroupConfiguration(accountJid, "No groups", GroupManager.getInstance())));
//        }
//
//        final Collection<RosterContact> allRosterContacts = RosterManager.getInstance().getAllContacts();
//        items.addAll(ContactVO.convert(allRosterContacts));

        this.view.updateItems(items);
    }

    private void buildStructure(List<IFlexible> items) {

//        synchronized (refreshLock) {
//            refreshRequested = false;
//            refreshInProgress = true;
//            handler.removeCallbacks(this);
//        }

//        listener.hidePlaceholder();

        final Collection<RosterContact> allRosterContacts = RosterManager.getInstance().getAllContacts();

        Map<AccountJid, Collection<UserJid>> blockedContacts = new TreeMap<>();
        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            blockedContacts.put(account, BlockingManager.getInstance().getBlockedContacts(account));
        }

        final Collection<RosterContact> rosterContacts = new ArrayList<>();
        for (RosterContact contact : allRosterContacts) {
            if (blockedContacts.containsKey(contact.getAccount())) {
                if (!blockedContacts.get(contact.getAccount()).contains(contact.getUser())) {
                    rosterContacts.add(contact);
                }
            }
        }

        final boolean showOffline = SettingsManager.contactsShowOffline();
        final boolean showGroups = SettingsManager.contactsShowGroups();
        final boolean showEmptyGroups = SettingsManager.contactsShowEmptyGroups();
        final boolean showActiveChats = false;
        final boolean stayActiveChats = true;
        final boolean showAccounts = SettingsManager.contactsShowAccounts();
        final Comparator<AbstractContact> comparator = SettingsManager.contactsOrder();
        final CommonState commonState = AccountManager.getInstance().getCommonState();
        final AccountJid selectedAccount = AccountManager.getInstance().getSelectedAccount();


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
        final GroupConfiguration chatsGroup;

        /**
         * Whether there is at least one contact.
         */
        boolean hasContacts = false;

        /**
         * Whether there is at least one visible contact.
         */
        boolean hasVisibleContacts = false;

        final Map<AccountJid, AccountConfiguration> accounts = new TreeMap<>();

        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            accounts.put(account, null);
        }

        /**
         * List of rooms and active chats grouped by users inside accounts.
         */
        final Map<AccountJid, Map<UserJid, AbstractChat>> abstractChats = new TreeMap<>();

        for (AbstractChat abstractChat : MessageManager.getInstance().getChats()) {
            if ((abstractChat instanceof RoomChat || abstractChat.isActive())
                    && accounts.containsKey(abstractChat.getAccount())) {
                final AccountJid account = abstractChat.getAccount();
                Map<UserJid, AbstractChat> users = abstractChats.get(account);
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
                for (Map.Entry<AccountJid, AccountConfiguration> entry : accounts.entrySet()) {
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

            // chats on top
//            Collection<AbstractChat> chats = MessageManager.getInstance().getChatsOfEnabledAccount();
//            chatsGroup = getChatsGroup(chats, currentChatsState);

            // Build structure.
            for (RosterContact rosterContact : rosterContacts) {
                if (!rosterContact.isEnabled()) {
                    continue;
                }
                hasContacts = true;
                final boolean online = rosterContact.getStatusMode().isOnline();
                final AccountJid account = rosterContact.getAccount();
                final Map<UserJid, AbstractChat> users = abstractChats.get(account);
                final AbstractChat abstractChat;
                if (users == null) {
                    abstractChat = null;
                } else {
                    abstractChat = users.remove(rosterContact.getUser());
                }

                if (selectedAccount != null && !selectedAccount.equals(account)) {
                    continue;
                }
                if (ContactListGroupUtils.addContact(rosterContact, online, accounts, groups,
                        contacts, showAccounts, showGroups, showOffline)) {
                    hasVisibleContacts = true;
                }
            }
            for (Map<UserJid, AbstractChat> users : abstractChats.values())
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
                    } else if (MUCManager.getInstance().isMucPrivateChat(abstractChat.getAccount(), abstractChat.getUser())) {
                        group = GroupManager.IS_ROOM;
                        online = abstractContact.getStatusMode().isOnline();
                    } else {
                        group = GroupManager.NO_GROUP;
                        online = false;
                    }
                    hasVisibleContacts = true;
                    ContactListGroupUtils.addContact(abstractContact, group, online, accounts, groups, contacts,
                            showAccounts, showGroups);
                }

            //hasActiveChats = activeChats != null && activeChats.getTotal() > 0;

            // Remove empty groups, sort and apply structure.
            items.clear();
            items.add(new ToolbarVO());
            if (hasVisibleContacts) {

                // add recent chats
                //rosterItemVOs.addAll(ChatVO.convert(chatsGroup.getAbstractContacts()));

                if (currentChatsState == ContactListAdapter.ChatListState.recent) {

                    if (showAccounts) {
                        //boolean isFirst = items.isEmpty();
                        for (AccountConfiguration rosterAccount : accounts.values()) {
//                            if (isFirst) {
//                                isFirst = false;
//                            } else {
//                                items.add(new TopAccountSeparatorVO());
//                            }

                            items.add(AccountVO.convert(rosterAccount));

                            if (showGroups) {
                                //if (rosterAccount.isExpanded()) {
                                    for (GroupConfiguration rosterConfiguration : rosterAccount
                                            .getSortedGroupConfigurations()) {
                                        if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                                            items.add(GroupVO.convert(rosterConfiguration));
                                            rosterConfiguration.sortAbstractContacts(comparator);
//                                            rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
//                                                    ? ExtContactVO.convert(rosterConfiguration.getAbstractContacts())
//                                                    : com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO.convert(rosterConfiguration.getAbstractContacts()));
                                            items.addAll(ContactVO.convert(rosterConfiguration.getAbstractContacts()));
                                        }
                                    }
                                //}
                            } else {
                                rosterAccount.sortAbstractContacts(comparator);
//                                rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
//                                        ? ExtContactVO.convert(rosterAccount.getAbstractContacts())
//                                        : com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO.convert(rosterAccount.getAbstractContacts()));
                            }

                            if (rosterAccount.getTotal() > 0 && !rosterAccount.isExpanded()) {
                                //rosterItemVOs.add(BottomAccountSeparatorVO.convert(rosterAccount.getAccount()));
                            }

                            //if (rosterAccount.getTotal() == 0)
//                                rosterItemVOs.add(ButtonVO.convert(null,
//                                        context.getString(R.string.contact_add),
//                                        ButtonVO.ACTION_ADD_CONTACT));
                        }
                    } else {
//                        if (showGroups) {
//                            for (GroupConfiguration rosterConfiguration : groups.values()) {
//                                if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
//                                    rosterItemVOs.add(com.xabber.android.ui.adapter.contactlist.viewobjects.GroupVO.convert(rosterConfiguration));
//                                    rosterConfiguration.sortAbstractContacts(comparator);
//                                    rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
//                                            ? ExtContactVO.convert(rosterConfiguration.getAbstractContacts())
//                                            : com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO.convert(rosterConfiguration.getAbstractContacts()));
//                                }
//                            }
//                        } else {
//                            Collections.sort(contacts, comparator);
//                            rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
//                                    ? ExtContactVO.convert(contacts)
//                                    : com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO.convert(contacts));
//                        }
                    }
                } else {
//                    if (chatsGroup.getAbstractContacts().size() == 0) {
//                        if (currentChatsState == ContactListAdapter.ChatListState.unread)
//                            listener.showPlaceholder(context.getString(R.string.placeholder_no_unread));
//                        if (currentChatsState == ContactListAdapter.ChatListState.archived)
//                            listener.showPlaceholder(context.getString(R.string.placeholder_no_archived));
//                    }
                }
            }
        } else { // Search
//            final ArrayList<AbstractContact> baseEntities = getSearchResults(rosterContacts, comparator, abstractChats);
//            this.rosterItemVOs.clear();
//            this.rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
//                    ? ExtContactVO.convert(baseEntities)
//                    : com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO.convert(baseEntities));
//            hasVisibleContacts = baseEntities.size() > 0;
        }

        //listener.onContactListChanged(commonState, hasContacts, hasVisibleContacts, filterString != null);

//        synchronized (refreshLock) {
//            nextRefresh = new Date(new Date().getTime() + REFRESH_INTERVAL);
//            refreshInProgress = false;
//            handler.removeCallbacks(this); // Just to be sure.
//            if (refreshRequested) {
//                handler.postDelayed(this, REFRESH_INTERVAL);
//            }
//        }
    }

}
