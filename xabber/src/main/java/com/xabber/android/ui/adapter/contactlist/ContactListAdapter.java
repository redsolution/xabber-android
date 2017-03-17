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
package com.xabber.android.ui.adapter.contactlist;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
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
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.adapter.ComparatorByChat;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.helper.ContextMenuHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Adapter for contact list in the main activity.
 *
 * @author alexander.ivanov
 */
public class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements Runnable, Filterable, UpdatableAdapter, ContactListItemViewHolder.ContactClickListener,
        AccountGroupViewHolder.AccountGroupClickListener, GroupViewHolder.GroupClickListener {

    private static final String LOG_TAG = ContactListAdapter.class.getSimpleName();

    /**
     * Number of milliseconds between lazy refreshes.
     */
    private static final long REFRESH_INTERVAL = 1000;
    /**
     * View type used for contact items.
     */
    private static final int TYPE_CONTACT = 0;
    /**
     * View type used for groups and accounts expanders.
     */
    private static final int TYPE_GROUP = 1;
    private static final int TYPE_ACCOUNT = 2;
    private static final int TYPE_ACCOUNT_TOP_SEPARATOR = 3;
    private static final int TYPE_ACCOUNT_BOTTOM_SEPARATOR = 4;
    private final ArrayList<Object> baseEntities = new ArrayList<>();

    /**
     * Handler for deferred refresh.
     */
    private final Handler handler;

    /**
     * Lock for refresh requests.
     */
    private final Object refreshLock;
    /**
     * Layout inflater
     */
    private final LayoutInflater layoutInflater;
    private final ManagedActivity activity;
    private final int[] accountSubgroupColors;
    private final int activeChatsColor;
    private final ContactItemInflater contactItemInflater;
    private final int accountElevation;
    protected Locale locale = Locale.getDefault();
    private AccountGroupViewHolder.AccountGroupClickListener accountGroupClickListener;

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
    private ContactFilter contactFilter;

    /**
     * Filter string. Can be <code>null</code> if filter is disabled.
     */
    private String filterString;

    private final ContactListAdapterListener listener;
    private boolean hasActiveChats = false;
    private int[] accountGroupColors;

    public ContactListAdapter(ManagedActivity activity, ContactListAdapterListener listener) {
        this.activity = activity;
        this.accountGroupClickListener = null;

        layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Resources resources = activity.getResources();


        accountGroupColors = resources.getIntArray(getThemeResource(R.attr.contact_list_account_group_background));
        accountSubgroupColors = resources.getIntArray(getThemeResource(R.attr.contact_list_subgroup_background));

        TypedValue typedValue = new TypedValue();
        TypedArray a = activity.obtainStyledAttributes(typedValue.data, new int[] { R.attr.contact_list_active_chat_subgroup_background });
        activeChatsColor = a.getColor(0, 0);
        a.recycle();

        contactItemInflater = new ContactItemInflater(activity);

        accountElevation = activity.getResources().getDimensionPixelSize(R.dimen.account_group_elevation);


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
        final boolean showActiveChats = SettingsManager.contactsShowActiveChats();
        final boolean stayActiveChats = SettingsManager.contactsStayActiveChats();
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
                for (Entry<AccountJid, AccountConfiguration> entry : accounts.entrySet()) {
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
                final AccountJid account = rosterContact.getAccount();
                final Map<UserJid, AbstractChat> users = abstractChats.get(account);
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

        notifyDataSetChanged();

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

    @Override
    public void run() {
        onChange();
    }

    private int getThemeResource(int themeResourceId) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = activity.obtainStyledAttributes(typedValue.data, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }

    @Override
    public int getItemCount() {
        return baseEntities.size();
    }

    public Object getItem(int position) {
        return baseEntities.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        Object object = getItem(position);
        if (object instanceof AbstractContact) {
            return TYPE_CONTACT;
        } else if (object instanceof AccountConfiguration) {
            return TYPE_ACCOUNT;
        } else if (object instanceof GroupConfiguration) {
            return TYPE_GROUP;
        } else if (object instanceof AccountTopSeparator) {
            return TYPE_ACCOUNT_TOP_SEPARATOR;
        } else if (object instanceof AccountBottomSeparator) {
            return TYPE_ACCOUNT_BOTTOM_SEPARATOR;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_CONTACT:
                return new ContactListItemViewHolder(layoutInflater
                        .inflate(R.layout.item_contact, parent, false), this);
            case TYPE_GROUP:
                return new GroupViewHolder(layoutInflater
                        .inflate(R.layout.item_base_group, parent, false), this);
            case TYPE_ACCOUNT:
                return new AccountGroupViewHolder(layoutInflater
                        .inflate(R.layout.contact_list_account_group_item, parent, false), this);
            case TYPE_ACCOUNT_TOP_SEPARATOR:
                return new TopSeparatorHolder(layoutInflater
                        .inflate(R.layout.account_group_item_top_separator, parent, false));
            case TYPE_ACCOUNT_BOTTOM_SEPARATOR:
                return new BottomSeparatorHolder(layoutInflater
                        .inflate(R.layout.account_group_item_bottom_separator, parent, false));

            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int viewType = getItemViewType(position);

        final Object item = getItem(position);

        switch (viewType) {
            case TYPE_CONTACT:
                contactItemInflater.bindViewHolder((ContactListItemViewHolder) holder, (AbstractContact) item);
                break;

            case TYPE_ACCOUNT:
                bindAccount((AccountGroupViewHolder)holder, (AccountConfiguration) item);
                break;

            case TYPE_GROUP:
                bindGroup((GroupViewHolder) holder, (GroupConfiguration) item);
                break;

            case TYPE_ACCOUNT_TOP_SEPARATOR:
                break;

            case TYPE_ACCOUNT_BOTTOM_SEPARATOR:
                bindBottomSeparator((BottomSeparatorHolder) holder,
                        (AccountBottomSeparator) item);
                break;

            default:
                throw new IllegalStateException();
        }
    }

    private void bindBottomSeparator(BottomSeparatorHolder holder, AccountBottomSeparator accountBottomSeparator) {
        final int level = AccountManager.getInstance().getColorLevel(accountBottomSeparator.getAccount());

        holder.bottomLayer.setBackgroundDrawable(new ColorDrawable(accountSubgroupColors[level]));
        holder.topLayer.setBackgroundDrawable(new ColorDrawable(accountSubgroupColors[level]));

        StatusMode statusMode = AccountManager.getInstance().getAccount(accountBottomSeparator.getAccount()).getDisplayStatusMode();

        if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection) {
            holder.offlineShadowBottom.setVisibility(View.VISIBLE);
            holder.offlineShadowTop.setVisibility(View.VISIBLE);
        } else {
            holder.offlineShadowBottom.setVisibility(View.GONE);
            holder.offlineShadowTop.setVisibility(View.GONE);
        }
    }

    private void bindAccount(AccountGroupViewHolder viewHolder, AccountConfiguration configuration) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewHolder.itemView.setElevation(accountElevation);
        }

        final AccountJid account = configuration.getAccount();

        final int level = AccountManager.getInstance().getColorLevel(account);
        viewHolder.itemView.setBackgroundColor(accountGroupColors[level]);

        viewHolder.name.setText(GroupManager.getInstance().getGroupName(account, configuration.getGroup()));
        viewHolder.smallRightText.setText(configuration.getOnline() + "/" + configuration.getTotal());

        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        String statusText = accountItem.getStatusText().trim();

        if (statusText.isEmpty()) {
            statusText = activity.getString(accountItem.getDisplayStatusMode().getStringID());
        }

        viewHolder.secondLineMessage.setText(statusText);

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatar.setVisibility(View.VISIBLE);
            viewHolder.avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
        } else {
            viewHolder.avatar.setVisibility(View.GONE);
        }

        viewHolder.statusIcon.setImageLevel(accountItem.getDisplayStatusMode().getStatusLevel());

        ShowOfflineMode showOfflineMode = configuration.getShowOfflineMode();
        if (showOfflineMode == ShowOfflineMode.normal) {
            if (SettingsManager.contactsShowOffline()) {
                showOfflineMode = ShowOfflineMode.always;
            } else {
                showOfflineMode = ShowOfflineMode.never;
            }
        }

        viewHolder.smallRightIcon.setImageLevel(showOfflineMode.ordinal());


        StatusMode statusMode = AccountManager.getInstance().getAccount(configuration.getAccount()).getDisplayStatusMode();

        if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection) {
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        } else {
            viewHolder.offlineShadow.setVisibility(View.GONE);
        }
    }

    private void bindGroup(GroupViewHolder viewHolder, GroupConfiguration configuration) {
        final int level = AccountManager.getInstance().getColorLevel(configuration.getAccount());

        final String name = GroupManager.getInstance().getGroupName(configuration.getAccount(), configuration.getGroup());


        viewHolder.indicator.setImageLevel(configuration.isExpanded() ? 1 : 0);
        viewHolder.groupOfflineIndicator.setImageLevel(configuration.getShowOfflineMode().ordinal());

        int color;

        viewHolder.groupOfflineIndicator.setVisibility(View.GONE);
        viewHolder.offlineShadow.setVisibility(View.GONE);

        if (configuration.getGroup().equals(GroupManager.ACTIVE_CHATS)) {
            color = activeChatsColor;
            viewHolder.name.setText(name);
        } else {
            viewHolder.name.setText(String.format("%s (%d/%d)", name, configuration.getOnline(), configuration.getTotal()));

            color = accountSubgroupColors[level];
            viewHolder.groupOfflineIndicator.setVisibility(View.VISIBLE);

            AccountItem accountItem = AccountManager.getInstance().getAccount(configuration.getAccount());

            if (accountItem != null) {
                StatusMode statusMode = accountItem.getDisplayStatusMode();
                if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection) {
                    viewHolder.offlineShadow.setVisibility(View.VISIBLE);
                }
            }
        }

        viewHolder.itemView.setBackgroundDrawable(new ColorDrawable(color));
    }

    @Override
    public void onContactClick(int adapterPosition) {
        listener.onContactClick((AbstractContact) baseEntities.get(adapterPosition));
    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        contactItemInflater.onAvatarClick((BaseEntity) getItem(adapterPosition));
    }

    @Override
    public void onContactCreateContextMenu(int adapterPosition, ContextMenu menu) {
        AbstractContact abstractContact = (AbstractContact) getItem(adapterPosition);
        ContextMenuHelper.createContactContextMenu(activity, this, abstractContact, menu);
    }

    @Override
    public void onAccountAvatarClick(int adapterPosition) {
        activity.startActivity(AccountActivity.createIntent(activity,
                ((AccountConfiguration)getItem(adapterPosition)).getAccount()));
    }

    @Override
    public void onAccountMenuClick(int adapterPosition, View view) {
        listener.onAccountMenuClick(
                ((AccountConfiguration)baseEntities.get(adapterPosition)).getAccount(), view);
    }

    @Override
    public void onAccountGroupClick(int adapterPosition) {
        toggleGroupExpand(adapterPosition);
    }

    @Override
    public void onAccountGroupCreateContextMenu(int adapterPosition, ContextMenu menu) {
        AccountConfiguration accountConfiguration = (AccountConfiguration) getItem(adapterPosition);
        ContextMenuHelper.createAccountContextMenu(activity, this, accountConfiguration.getAccount(), menu);
    }

    @Override
    public void onGroupClick(int adapterPosition) {
        toggleGroupExpand(adapterPosition);
    }

    @Override
    public void onGroupCreateContextMenu(int adapterPosition, ContextMenu menu) {
        GroupConfiguration groupConfiguration = (GroupConfiguration) getItem(adapterPosition);

        ContextMenuHelper.createGroupContextMenu(activity, this,
                groupConfiguration.getAccount(), groupConfiguration.getGroup(), menu);
    }

    private void toggleGroupExpand(int adapterPosition) {
        GroupConfiguration groupConfiguration = (GroupConfiguration) getItem(adapterPosition);
        GroupManager.getInstance().setExpanded(groupConfiguration.getAccount(), groupConfiguration.getGroup(),
                !groupConfiguration.isExpanded());
        onChange();
    }

    /**
     * Listener for contact list appearance changes.
     *
     * @author alexander.ivanov
     */
    public interface ContactListAdapterListener {

        void onContactListChanged(CommonState commonState, boolean hasContacts,
                                  boolean hasVisibleContacts, boolean isFilterEnabled);
        void onContactClick(AbstractContact contact);
        void onAccountMenuClick(AccountJid accountJid, View view);

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
