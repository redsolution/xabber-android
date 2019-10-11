package com.xabber.android.presentation.mvp.contactlist;

import android.view.ContextMenu;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.CrowdfundingChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountWithButtonsVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountWithContactsVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountWithGroupsVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ButtonVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.CrowdfundingChatVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ExtContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.GroupVO;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListGroupUtils;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public class ContactListPresenter implements OnContactChangedListener, OnAccountChangedListener,
        ContactVO.ContactClickListener, AccountVO.AccountClickListener,
        GroupVO.GroupClickListener, UpdateBackpressure.UpdatableObject {

    private static ContactListPresenter instance;
    private ContactListView view;

    private UpdateBackpressure updateBackpressure;

    private String filterString = null;

    public static ContactListPresenter getInstance() {
        if (instance == null) instance = new ContactListPresenter();
        return instance;
    }

    public ContactListPresenter() {
        updateBackpressure = new UpdateBackpressure(this);
    }

    public void bindView(ContactListView view) {
        this.view = view;
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        EventBus.getDefault().register(this);
        updateBackpressure.build();
    }

    /**
     * Force stop contact list updates before pause or application close.
     */
    public void unbindView() {
        this.view = null;
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        EventBus.getDefault().unregister(this);
        updateBackpressure.removeRefreshRequests();
    }

    public void updateContactList() {
        updateBackpressure.refreshRequest();
    }

    public void onItemClick(IFlexible item) {
        if (item instanceof ContactVO) {
            AccountJid accountJid = ((ContactVO) item).getAccountJid();
            UserJid userJid = ((ContactVO) item).getUserJid();
            if (view != null) view.onContactClick(
                    RosterManager.getInstance().getAbstractContact(accountJid, userJid));
        } else if (item instanceof ButtonVO) {
            ButtonVO button = (ButtonVO) item;
            if (view != null) view.onButtonItemClick(button);
        } else if (item instanceof CrowdfundingChatVO) {
            if (view != null) {
                AccountJid accountJid = CrowdfundingChat.getDefaultAccount();
                UserJid userJid = CrowdfundingChat.getDefaultUser();
                if (accountJid != null && userJid != null)
                    view.onContactClick(RosterManager.getInstance().getAbstractContact(accountJid, userJid));
            }
        }
    }

    @Override
    public void onGroupCreateContextMenu(int adapterPosition, ContextMenu menu) {
        if (view != null) view.onItemContextMenu(adapterPosition, menu);
    }

    @Override
    public void onContactCreateContextMenu(int adapterPosition, ContextMenu menu) {
        if (view != null) view.onItemContextMenu(adapterPosition, menu);
    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        if (view != null) view.onContactAvatarClick(adapterPosition);
    }

    @Override
    public void onContactButtonClick(int adapterPosition) {
    }

    @Override
    public void onAccountAvatarClick(int adapterPosition) {
        if (view != null) view.onAccountAvatarClick(adapterPosition);
    }

    @Override
    public void onAccountMenuClick(int adapterPosition, View view) {
        if (this.view != null) this.view.onAccountMenuClick(adapterPosition, view);
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        updateBackpressure.refreshRequest();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        updateBackpressure.refreshRequest();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
        updateBackpressure.refreshRequest();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageUpdateEvent event) {
        updateBackpressure.refreshRequest();
    }

    @Override
    public void update() {
//        listener.hidePlaceholder();

        List<IFlexible> items = new ArrayList<>();

        final Collection<RosterContact> allRosterContacts = RosterManager.getInstance().getAllContacts();

        Map<AccountJid, Collection<UserJid>> blockedContacts = new TreeMap<>();
        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            blockedContacts.put(account, BlockingManager.getInstance().getCachedBlockedContacts(account));
        }

        final Collection<RosterContact> rosterContacts = new ArrayList<>();
        for (RosterContact contact : allRosterContacts) {
            if (blockedContacts.containsKey(contact.getAccount())) {
                Collection<UserJid> blockedUsers = blockedContacts.get(contact.getAccount());
                if (blockedUsers != null) {
                    if (!blockedUsers.contains(contact.getUser()))
                        rosterContacts.add(contact);
                } else rosterContacts.add(contact);
            } else rosterContacts.add(contact);
        }

        final boolean showOffline = SettingsManager.contactsShowOffline();
        final boolean showGroups = SettingsManager.contactsShowGroups();
        final boolean showEmptyGroups = SettingsManager.contactsShowEmptyGroups();
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

        // set hasVisibleContacts as true if have crowdfunding message
//        CrowdfundingMessage message = CrowdfundingManager.getInstance().getLastNotDelayedMessageFromRealm();
//        if (message != null) hasVisibleContacts = true;

        if (hasVisibleContacts) {
            if (showAccounts) {
                for (AccountConfiguration rosterAccount : accounts.values()) {
                    if (rosterAccount.getTotal() != 0) {
                        if (showGroups) {
                            createContactListWithAccountsAndGroups(items, rosterAccount, showEmptyGroups, comparator);
                        } else {
                            createContactListWithAccounts(items, rosterAccount, comparator);
                        }
                    } else {
                        AccountWithButtonsVO account = AccountWithButtonsVO.convert(rosterAccount, this);
                        ButtonVO button = ButtonVO.convert(rosterAccount,
                                Application.getInstance().getApplicationContext().getString(R.string.contact_add), ButtonVO.ACTION_ADD_CONTACT);
                        account.addSubItem(button);
                        items.add(account);
                    }
                }
            } else {
                if (showGroups) {
                    createContactListWithGroups(items, showEmptyGroups, groups, comparator);
                } else {
                    createContactList(items, contacts, comparator);
                }
            }
        }

        if (view != null) view.onContactListChanged(commonState, hasContacts, hasVisibleContacts,
                    filterString != null);

        view.updateItems(items);
        view.updateAccountsList();
        updateUnreadCount();
    }

    private void createContactListWithAccountsAndGroups(List<IFlexible> items, AccountConfiguration rosterAccount,
                                                        boolean showEmptyGroups, Comparator<AbstractContact> comparator) {
        AccountWithGroupsVO account = AccountWithGroupsVO.convert(rosterAccount, this);
        boolean firstGroupInAccount = true;
        for (GroupConfiguration rosterConfiguration : rosterAccount
                .getSortedGroupConfigurations()) {
            if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                GroupVO group = GroupVO.convert(rosterConfiguration, firstGroupInAccount, this);
                firstGroupInAccount = false;
                rosterConfiguration.sortAbstractContacts(comparator);

                for (AbstractContact contact : rosterConfiguration.getAbstractContacts()) {
                    group.addSubItem(SettingsManager.contactsShowMessages()
                            ? ExtContactVO.convert(contact, this)
                            : ContactVO.convert(contact, this));
                }
                account.addSubItem(group);
            }
        }
        items.add(account);
    }

    private void createContactListWithAccounts(List<IFlexible> items, AccountConfiguration rosterAccount,
                                               Comparator<AbstractContact> comparator) {
        AccountWithContactsVO account = AccountWithContactsVO.convert(rosterAccount, this);
        rosterAccount.sortAbstractContacts(comparator);

        for (AbstractContact contact : rosterAccount.getAbstractContacts()) {
            account.addSubItem(SettingsManager.contactsShowMessages()
                    ? ExtContactVO.convert(contact, this)
                    : ContactVO.convert(contact, this));
        }
        items.add(account);
    }

    private void createContactListWithGroups(List<IFlexible> items, boolean showEmptyGroups,
                                             Map<String, GroupConfiguration> groups,
                                             Comparator<AbstractContact> comparator) {
        for (GroupConfiguration rosterConfiguration : groups.values()) {
            if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                GroupVO group = GroupVO.convert(rosterConfiguration, false, this);
                rosterConfiguration.sortAbstractContacts(comparator);

                for (AbstractContact contact : rosterConfiguration.getAbstractContacts()) {
                    group.addSubItem(SettingsManager.contactsShowMessages()
                            ? ExtContactVO.convert(contact, this)
                            : ContactVO.convert(contact, this));
                }
                items.add(group);
            }
        }
    }

    private void createContactList(List<IFlexible> items, List<AbstractContact> contacts,
                                   Comparator<AbstractContact> comparator) {
        Collections.sort(contacts, comparator);
        items.addAll(SettingsManager.contactsShowMessages()
                ? ExtContactVO.convert(contacts, this)
                : ContactVO.convert(contacts, this));
    }

    public void updateUnreadCount() {
        int unreadMessageCount = 0;

        for (AbstractChat abstractChat : MessageManager.getInstance().getChatsOfEnabledAccount()) {
            if (abstractChat.notifyAboutMessage() && !abstractChat.isArchived())
                unreadMessageCount += abstractChat.getUnreadMessageCount();
        }

//        unreadMessageCount += CrowdfundingManager.getInstance().getUnreadMessageCount();
        EventBus.getDefault().post(new UpdateUnreadCountEvent(unreadMessageCount));
    }

    public static class UpdateUnreadCountEvent {
        private int count;

        public UpdateUnreadCountEvent(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }
}
