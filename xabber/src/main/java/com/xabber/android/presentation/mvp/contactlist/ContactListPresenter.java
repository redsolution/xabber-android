package com.xabber.android.presentation.mvp.contactlist;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.database.messagerealm.MessageItem;
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
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountWithContactsVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountWithGroupsVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ButtonVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ChatVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ExtContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.GroupVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ToolbarVO;
import com.xabber.android.ui.adapter.ChatComparator;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListAdapter;
import com.xabber.android.ui.adapter.contactlist.ContactListGroupUtils;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public class ContactListPresenter implements Runnable, OnContactChangedListener, OnAccountChangedListener {

    private static final int MAX_RECENT_ITEMS = 12;
    private static final long REFRESH_INTERVAL = 1000;

    private static ContactListPresenter instance;
    private ContactListView view;
    private Context context;

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

    private String filterString = null;
    protected Locale locale = Locale.getDefault();
    private ContactListAdapter.ChatListState currentChatsState = ContactListAdapter.ChatListState.recent;

    public static ContactListPresenter getInstance(Context context) {
        if (instance == null) instance = new ContactListPresenter(context);
        return instance;
    }

    public ContactListPresenter(Context context) {
        this.context = context;
        handler = new Handler();
        refreshLock = new Object();
        refreshRequested = false;
        refreshInProgress = false;
        nextRefresh = new Date();

        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
    }

    public void onLoadContactList(ContactListView view) {
        this.view = view;
        buildStructure();
    }

    public void onItemClick(IFlexible item) {
        if (item instanceof ContactVO) {
            AccountJid accountJid = ((ContactVO) item).getAccountJid();
            UserJid userJid = ((ContactVO) item).getUserJid();
            view.onContactClick(RosterManager.getInstance().getAbstractContact(accountJid, userJid));
        }
    }

    public void setFilterString(String filter) {
        filterString = filter;
        onLoadContactList(view);
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        refreshRequest();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        // вызывается всякий раз, когда собеседник набирает сообщение - это плохо
        refreshRequest();
    }

    @Override
    public void run() {
        buildStructure();
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

    private void buildStructure() {

        synchronized (refreshLock) {
            refreshRequested = false;
            refreshInProgress = true;
            handler.removeCallbacks(this);
        }

//        listener.hidePlaceholder();

        List<IFlexible> items = new ArrayList<>();

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
         * Chat list on top of contact list.
         */
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

        if (filterString == null || filterString.isEmpty()) {

            // BUILD STRUCTURE //

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

            // chats on top
            Collection<AbstractChat> chats = MessageManager.getInstance().getChatsOfEnabledAccount();
            chatsGroup = getChatsGroup(chats, currentChatsState);

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

            // BUILD STRUCTURE //

            // Remove empty groups, sort and apply structure.
            items.clear();
            items.add(new ToolbarVO());
            if (hasVisibleContacts) {

                // add recent chats
                items.addAll(ChatVO.convert(chatsGroup.getAbstractContacts()));

                if (currentChatsState == ContactListAdapter.ChatListState.recent) {

                    if (showAccounts) {
                        //boolean isFirst = items.isEmpty();
                        for (AccountConfiguration rosterAccount : accounts.values()) {
//                            if (isFirst) {
//                                isFirst = false;
//                            } else {
//                                items.add(new TopAccountSeparatorVO());
//                            }

                            if (showGroups) {
                                createContactListWithAccountsAndGroups(items, rosterAccount, showEmptyGroups, comparator);
                            } else {
                                createContactListWithAccounts(items, rosterAccount, comparator);
                            }

                            if (rosterAccount.getTotal() == 0)
                                items.add(ButtonVO.convert(rosterAccount,
                                        context.getString(R.string.contact_add),
                                        ButtonVO.ACTION_ADD_CONTACT));
                        }
                    } else {
                        if (showGroups) {
                            createContactListWithGroups(items, showEmptyGroups, groups, comparator);
                        } else {
                            createContactList(items, contacts, comparator);
                        }
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
            final ArrayList<AbstractContact> baseEntities = getSearchResults(rosterContacts, comparator, abstractChats);
            items.clear();
            items.addAll(SettingsManager.contactsShowMessages()
                    ? ExtContactVO.convert(baseEntities)
                    : ContactVO.convert(baseEntities));
            hasVisibleContacts = baseEntities.size() > 0;
        }

        //listener.onContactListChanged(commonState, hasContacts, hasVisibleContacts, filterString != null);
        this.view.updateItems(items);

        synchronized (refreshLock) {
            nextRefresh = new Date(new Date().getTime() + REFRESH_INTERVAL);
            refreshInProgress = false;
            handler.removeCallbacks(this); // Just to be sure.
            if (refreshRequested) {
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    }

    /**
     * @param chats which must be filtered
     * @param state for which you want to filter
     * @return GroupConfiguration that may contains recent, unread or archived chats.
     */
    private GroupConfiguration getChatsGroup(Collection<AbstractChat> chats, ContactListAdapter.ChatListState state) {
        GroupConfiguration chatsGroup = new GroupConfiguration(GroupManager.NO_ACCOUNT,
                com.xabber.android.ui.adapter.contactlist.viewobjects.GroupVO.RECENT_CHATS_TITLE, GroupManager.getInstance());

        List<AbstractChat> newChats = new ArrayList<>();

        int unreadMessageCount = 0;
        for (AbstractChat abstractChat : chats) {
            MessageItem lastMessage = abstractChat.getLastMessage();

            if (lastMessage != null && !TextUtils.isEmpty(lastMessage.getText())) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(abstractChat.getAccount());
                if (accountItem != null && accountItem.isEnabled()) {
                    int unread = abstractChat.getUnreadMessageCount();
                    if (abstractChat.notifyAboutMessage()) unreadMessageCount = unreadMessageCount + unread;

                    switch (state) {
                        case unread:
                            if (!abstractChat.isArchived() && unread > 0) newChats.add(abstractChat);
                            break;
                        case archived:
                            if (abstractChat.isArchived()) newChats.add(abstractChat);
                            break;
                        default:
                            // recent
                            if (!abstractChat.isArchived()) newChats.add(abstractChat);
                            break;
                    }
                }
            }
        }
        EventBus.getDefault().post(new ContactListAdapter.UpdateUnreadCountEvent(unreadMessageCount));

        Collections.sort(newChats, ChatComparator.CHAT_COMPARATOR);

        chatsGroup.setNotEmpty();

        int itemsCount = 0;
        for (AbstractChat chat : newChats) {
            if (itemsCount < MAX_RECENT_ITEMS || state != ContactListAdapter.ChatListState.recent) {
                chatsGroup.addAbstractContact(RosterManager.getInstance()
                        .getBestContact(chat.getAccount(), chat.getUser()));
                chatsGroup.increment(true);
                itemsCount++;
            } else break;
        }

        return chatsGroup;
    }

    private void createContactListWithAccountsAndGroups(List<IFlexible> items, AccountConfiguration rosterAccount,
                                                        boolean showEmptyGroups, Comparator<AbstractContact> comparator) {
        AccountWithGroupsVO account = AccountWithGroupsVO.convert(rosterAccount);
        for (GroupConfiguration rosterConfiguration : rosterAccount
                .getSortedGroupConfigurations()) {
            if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                GroupVO group = GroupVO.convert(rosterConfiguration);
                rosterConfiguration.sortAbstractContacts(comparator);

                for (AbstractContact contact : rosterConfiguration.getAbstractContacts()) {
                    group.addSubItem(SettingsManager.contactsShowMessages()
                            ? ExtContactVO.convert(contact)
                            : ContactVO.convert(contact));
                }
                account.addSubItem(group);
            }
        }
        items.add(account);
    }

    private void createContactListWithAccounts(List<IFlexible> items, AccountConfiguration rosterAccount,
                                               Comparator<AbstractContact> comparator) {
        AccountWithContactsVO account = AccountWithContactsVO.convert(rosterAccount);
        rosterAccount.sortAbstractContacts(comparator);

        for (AbstractContact contact : rosterAccount.getAbstractContacts()) {
            account.addSubItem(SettingsManager.contactsShowMessages()
                    ? ExtContactVO.convert(contact)
                    : ContactVO.convert(contact));
        }
        items.add(account);
    }

    private void createContactListWithGroups(List<IFlexible> items, boolean showEmptyGroups,
                                             Map<String, GroupConfiguration> groups,
                                             Comparator<AbstractContact> comparator) {
        for (GroupConfiguration rosterConfiguration : groups.values()) {
            if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                GroupVO group = GroupVO.convert(rosterConfiguration);
                rosterConfiguration.sortAbstractContacts(comparator);

                for (AbstractContact contact : rosterConfiguration.getAbstractContacts()) {
                    group.addSubItem(SettingsManager.contactsShowMessages()
                            ? ExtContactVO.convert(contact)
                            : ContactVO.convert(contact));
                }
                items.add(group);
            }
        }
    }

    private void createContactList(List<IFlexible> items, List<AbstractContact> contacts,
                                   Comparator<AbstractContact> comparator) {
        Collections.sort(contacts, comparator);
        items.addAll(SettingsManager.contactsShowMessages()
                ? ExtContactVO.convert(contacts)
                : ContactVO.convert(contacts));
    }

    private ArrayList<AbstractContact> getSearchResults(Collection<RosterContact> rosterContacts,
                                                        Comparator<AbstractContact> comparator,
                                                        Map<AccountJid, Map<UserJid, AbstractChat>> abstractChats) {
        final ArrayList<AbstractContact> baseEntities = new ArrayList<>();

        // Build structure.
        for (RosterContact rosterContact : rosterContacts) {
            if (!rosterContact.isEnabled()) {
                continue;
            }
            final AccountJid account = rosterContact.getAccount();
            final Map<UserJid, AbstractChat> users = abstractChats.get(account);
            if (users != null) {
                users.remove(rosterContact.getUser());
            }
            if (rosterContact.getName().toLowerCase(locale).contains(filterString)) {
                baseEntities.add(rosterContact);
            }
        }
        for (Map<UserJid, AbstractChat> users : abstractChats.values()) {
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

}
