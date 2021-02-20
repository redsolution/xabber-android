package com.xabber.android.ui.fragment.contactListFragment;

import android.view.ContextMenu;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.CircleManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.OnAccountChangedListener;
import com.xabber.android.ui.OnContactChangedListener;
import com.xabber.android.ui.OnMessageUpdatedListener;
import com.xabber.android.ui.OnNewMessageListener;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListGroupUtils;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.AccountVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.AccountWithButtonsVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.AccountWithContactsVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.AccountWithGroupsVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.ButtonVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.ContactVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.ExtContactVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.GroupVO;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.android.ui.helper.UpdateBackpressure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        ContactVO.ContactClickListener, AccountVO.AccountClickListener, ContextMenuHelper.ListPresenter,
        GroupVO.GroupClickListener, UpdateBackpressure.UpdatableObject, OnNewMessageListener, OnMessageUpdatedListener {

    private static ContactListPresenter instance;
    private ContactListView view;

    private UpdateBackpressure updateBackpressure;

    public ContactListPresenter() {
        updateBackpressure = new UpdateBackpressure(this);
    }

    public static ContactListPresenter getInstance() {
        if (instance == null) instance = new ContactListPresenter();
        return instance;
    }

    public void bindView(ContactListView view) {
        this.view = view;
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnNewMessageListener.class, this);
        Application.getInstance().addUIListener(OnMessageUpdatedListener.class, this);
        updateBackpressure.build();
    }

    /**
     * Force stop contact list updates before pause or application close.
     */
    public void unbindView() {
        this.view = null;
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnNewMessageListener.class, this);
        Application.getInstance().removeUIListener(OnMessageUpdatedListener.class, this);
        updateBackpressure.removeRefreshRequests();
    }

    public void updateContactList() {
        updateBackpressure.refreshRequest();
    }

    public void onItemClick(IFlexible item) {
        if (item instanceof ContactVO) {
            AccountJid accountJid = ((ContactVO) item).getAccountJid();
            ContactJid contactJid = ((ContactVO) item).getContactJid();
            if (view != null) view.onContactClick(
                    RosterManager.getInstance().getAbstractContact(accountJid, contactJid));
        } else if (item instanceof ButtonVO) {
            ButtonVO button = (ButtonVO) item;
            if (view != null) view.onButtonItemClick(button);
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
    public void onAccountsChanged(@Nullable Collection<? extends AccountJid> accounts) {
        Application.getInstance().runOnUiThread(() -> updateBackpressure.refreshRequest());
    }

    @Override
    public void onContactsChanged(@NotNull Collection<? extends RosterContact> entities) {
        Application.getInstance().runOnUiThread(() -> updateBackpressure.refreshRequest());
    }

    @Override
    public void onNewMessage() {
        Application.getInstance().runOnUiThread(() -> updateBackpressure.refreshRequest());
    }

    @Override
    public void onMessageUpdated() {
        Application.getInstance().runOnUiThread(() -> updateBackpressure.refreshRequest());
    }

    @Override
    public void update() {

        List<IFlexible> items = new ArrayList<>();

        final Collection<RosterContact> allRosterContacts = RosterManager.getInstance().getAllContacts();

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
        final Map<AccountJid, Map<ContactJid, AbstractChat>> abstractChats = new TreeMap<>();

        for (AbstractChat abstractChat : ChatManager.getInstance().getChats()) {
            if ((abstractChat.isActive()) && accounts.containsKey(abstractChat.getAccount())) {
                final AccountJid account = abstractChat.getAccount();
                Map<ContactJid, AbstractChat> users = abstractChats.get(account);
                if (users == null) {
                    users = new TreeMap<>();
                    abstractChats.put(account, users);
                }
                users.put(abstractChat.getContactJid(), abstractChat);
            }
        }

        // BUILD STRUCTURE //

        // Create arrays.
        if (showAccounts) {
            groups = null;
            contacts = null;
            for (Map.Entry<AccountJid, AccountConfiguration> entry : accounts.entrySet()) {
                entry.setValue(new AccountConfiguration(entry.getKey(),
                        CircleManager.IS_ACCOUNT, CircleManager.getInstance()));
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
        for (RosterContact rosterContact : allRosterContacts) {
            if (!rosterContact.isEnabled()) {
                continue;
            }
            hasContacts = true;
            final boolean online = rosterContact.getStatusMode().isOnline();
            final AccountJid account = rosterContact.getAccount();
            final Map<ContactJid, AbstractChat> users = abstractChats.get(account);

            if (selectedAccount != null && !selectedAccount.equals(account)) {
                continue;
            }
            if (ContactListGroupUtils.addContact(rosterContact, online, accounts, groups,
                    contacts, showAccounts, showGroups, showOffline)) {
                hasVisibleContacts = true;
            }
        }
        for (Map<ContactJid, AbstractChat> users : abstractChats.values())
            for (AbstractChat abstractChat : users.values()) {
                final AbstractContact abstractContact;
                abstractContact = new ChatContact(abstractChat);
                if (selectedAccount != null && !selectedAccount.equals(abstractChat.getAccount())) {
                    continue;
                }
                final String group;
                final boolean online;
                group = CircleManager.NO_GROUP;
                online = false;
                hasVisibleContacts = true;
                ContactListGroupUtils.addContact(abstractContact, group, online, accounts, groups, contacts,
                        showAccounts, showGroups);
            }

        // BUILD STRUCTURE //

        // Remove empty groups, sort and apply structure.
        items.clear();

        //if (hasVisibleContacts) {
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
        //}

        if (view != null) view.onContactListChanged(commonState, hasContacts, hasVisibleContacts);

        view.updateItems(items);
        view.updateAccountsList();
    }

    private void createContactListWithAccountsAndGroups(List<IFlexible> items, AccountConfiguration rosterAccount,
                                                        boolean showEmptyGroups, Comparator<AbstractContact> comparator) {
        AccountWithGroupsVO account = AccountWithGroupsVO.convert(rosterAccount, this);
        boolean firstGroupInAccount = true;
        for (GroupConfiguration rosterConfiguration : rosterAccount
                .getNotSortedGroupConfigurations()) {
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

}
