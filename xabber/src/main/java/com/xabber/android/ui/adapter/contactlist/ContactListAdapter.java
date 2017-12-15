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
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler;
import com.brandongogetap.stickyheaders.exposed.StickyHeaderListener;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
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
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.adapter.ChatComparator;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.adapter.contactlist.viewobjects.AccountVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.BaseRosterItemVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.BottomAccountSeparatorVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ButtonVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ChatVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ExtContactVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.GroupVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.MainTitleVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.TopAccountSeparatorVO;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.helper.ContextMenuHelper;

import org.greenrobot.eventbus.EventBus;

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
        AccountGroupViewHolder.AccountGroupClickListener, GroupViewHolder.GroupClickListener,
        ButtonViewHolder.ButtonClickListener, StickyHeaderHandler, StickyHeaderListener, MainTitleViewHolder.Listener {

    private static final String LOG_TAG = ContactListAdapter.class.getSimpleName();

    private static final int MAX_RECENT_ITEMS = 8;

    /**
     * Number of milliseconds between lazy refreshes.
     */
    private static final long REFRESH_INTERVAL = 1000;
    /**
     * View type used for contact items.
     */
    public static final int TYPE_CONTACT = 0;
    /**
     * View type used for groups and accounts expanders.
     */
    private static final int TYPE_GROUP = 1;
    public static final int TYPE_ACCOUNT = 2;
    private static final int TYPE_ACCOUNT_TOP_SEPARATOR = 3;
    private static final int TYPE_ACCOUNT_BOTTOM_SEPARATOR = 4;
    public static final int TYPE_CHAT = 5;
    private static final int TYPE_BUTTON = 6;
    private static final int TYPE_MAIN_TITLE = 7;
    private static final int TYPE_EXT_CONTACT = 8;
    private final ArrayList<BaseRosterItemVO> rosterItemVOs = new ArrayList<>();

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
    private final ContactItemChatInflater contactItemChatInflater;
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

    private int currentHeaderPosition = 0;

    private ChatListState currentChatsState = ChatListState.recent;

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
        contactItemChatInflater = new ContactItemChatInflater(activity);

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

        listener.hidePlaceholder();

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
            rosterItemVOs.clear();
            rosterItemVOs.add(new MainTitleVO(1, false));
            if (hasVisibleContacts) {

                // add recent chats
                rosterItemVOs.addAll(ChatVO.convert(chatsGroup.getAbstractContacts()));

                if (currentChatsState == ChatListState.recent) {

                    if (showAccounts) {
                        boolean isFirst = rosterItemVOs.isEmpty();
                        for (AccountConfiguration rosterAccount : accounts.values()) {
                            if (isFirst) {
                                isFirst = false;
                            } else {
                                rosterItemVOs.add(new TopAccountSeparatorVO());
                            }

                            rosterItemVOs.add(AccountVO.convert(rosterAccount));

                            if (showGroups) {
                                if (rosterAccount.isExpanded()) {
                                    for (GroupConfiguration rosterConfiguration : rosterAccount
                                            .getSortedGroupConfigurations()) {
                                        if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                                            rosterItemVOs.add(GroupVO.convert(rosterConfiguration));
                                            rosterConfiguration.sortAbstractContacts(comparator);
                                            rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
                                                    ? ExtContactVO.convert(rosterConfiguration.getAbstractContacts())
                                            : ContactVO.convert(rosterConfiguration.getAbstractContacts()));
                                        }
                                    }
                                }
                            } else {
                                rosterAccount.sortAbstractContacts(comparator);
                                rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
                                        ? ExtContactVO.convert(rosterAccount.getAbstractContacts())
                                        : ContactVO.convert(rosterAccount.getAbstractContacts()));
                            }

                            if (rosterAccount.getTotal() > 0 && !rosterAccount.isExpanded()) {
                                rosterItemVOs.add(BottomAccountSeparatorVO.convert(rosterAccount.getAccount()));
                            }

                            if (rosterAccount.getTotal() == 0)
                                rosterItemVOs.add(ButtonVO.convert(null, ButtonVO.ACTION_ADD_CONTACT, ButtonVO.ACTION_ADD_CONTACT));
                        }
                    } else {
                        if (showGroups) {
                            for (GroupConfiguration rosterConfiguration : groups.values()) {
                                if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                                    rosterItemVOs.add(GroupVO.convert(rosterConfiguration));
                                    rosterConfiguration.sortAbstractContacts(comparator);
                                    rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
                                            ? ExtContactVO.convert(rosterConfiguration.getAbstractContacts())
                                            : ContactVO.convert(rosterConfiguration.getAbstractContacts()));
                                }
                            }
                        } else {
                            Collections.sort(contacts, comparator);
                            rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
                                    ? ExtContactVO.convert(contacts)
                                    : ContactVO.convert(contacts));
                        }
                    }
                } else {
                    if (chatsGroup.getAbstractContacts().size() == 0) {
                        if (currentChatsState == ChatListState.unread)
                            listener.showPlaceholder(activity.getString(R.string.placeholder_no_unread));
                        if (currentChatsState == ChatListState.archived)
                            listener.showPlaceholder(activity.getString(R.string.placeholder_no_archived));
                    }
                }
            }
        } else { // Search
            final ArrayList<AbstractContact> baseEntities = getSearchResults(rosterContacts, comparator, abstractChats);
            this.rosterItemVOs.clear();
            this.rosterItemVOs.addAll(SettingsManager.contactsShowMessages()
                    ? ExtContactVO.convert(baseEntities)
                    : ContactVO.convert(baseEntities));
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
        return rosterItemVOs.size();
    }

    public Object getItem(int position) {
        return rosterItemVOs.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        Object object = getItem(position);
        if (object instanceof ContactVO) {
            if (object instanceof ChatVO) {
                if (object instanceof ExtContactVO)
                    return TYPE_EXT_CONTACT;
                else return TYPE_CHAT;
            } else return TYPE_CONTACT;
        } else if (object instanceof AccountVO) {
            return TYPE_ACCOUNT;
        } else if (object instanceof GroupVO) {
            return TYPE_GROUP;
        } else if (object instanceof TopAccountSeparatorVO) {
            return TYPE_ACCOUNT_TOP_SEPARATOR;
        } else if (object instanceof BottomAccountSeparatorVO) {
            return TYPE_ACCOUNT_BOTTOM_SEPARATOR;
        } else if (object instanceof ButtonVO) {
            return TYPE_BUTTON;
        } else if (object instanceof MainTitleVO) {
            return TYPE_MAIN_TITLE;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_CONTACT:
                return new ContactListItemViewHolder(layoutInflater
                        .inflate(R.layout.item_contact_in_contact_list, parent, false), this);
            case TYPE_CHAT:
                return new RosterChatViewHolder(layoutInflater
                        .inflate(R.layout.item_chat_in_contact_list, parent, false), this);
            case TYPE_EXT_CONTACT:
                return new ExtContactViewHolder(layoutInflater
                        .inflate(R.layout.item_ext_contact_in_contact_list, parent, false), this);
            case TYPE_GROUP:
                return new GroupViewHolder(layoutInflater
                        .inflate(R.layout.item_group_in_contact_list, parent, false), this);
            case TYPE_ACCOUNT:
                return new AccountGroupViewHolder(layoutInflater
                        .inflate(R.layout.item_account_in_contact_list, parent, false), this);
            case TYPE_ACCOUNT_TOP_SEPARATOR:
                return new TopSeparatorHolder(layoutInflater
                        .inflate(R.layout.account_group_item_top_separator, parent, false));
            case TYPE_ACCOUNT_BOTTOM_SEPARATOR:
                return new BottomSeparatorHolder(layoutInflater
                        .inflate(R.layout.item_account_bottom_in_contact_list, parent, false));
            case TYPE_BUTTON:
                return new ButtonViewHolder(layoutInflater
                        .inflate(R.layout.item_button_in_contact_list, parent, false), this);
            case TYPE_MAIN_TITLE:
                return new MainTitleViewHolder(layoutInflater
                        .inflate(R.layout.item_main_title_in_contact_list, parent, false), activity, this);

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
                contactItemInflater.bindViewHolder((ContactListItemViewHolder) holder, (ContactVO) item);
                break;

            case TYPE_CHAT:
                contactItemChatInflater.bindViewHolderWithButton((RosterChatViewHolder) holder,
                        (ChatVO) item, position == MAX_RECENT_ITEMS
                                && currentChatsState == ChatListState.recent);
                break;

            case TYPE_EXT_CONTACT:
                contactItemChatInflater.bindViewHolderWithButton((ExtContactViewHolder) holder,
                        (ChatVO) item, position == MAX_RECENT_ITEMS
                                && currentChatsState == ChatListState.recent);
                break;

            case TYPE_ACCOUNT:
                bindAccount((AccountGroupViewHolder)holder, (AccountVO) item);
                break;

            case TYPE_GROUP:
                bindGroup((GroupViewHolder) holder, (GroupVO) item);
                break;

            case TYPE_ACCOUNT_TOP_SEPARATOR:
                break;

            case TYPE_ACCOUNT_BOTTOM_SEPARATOR:
                bindBottomSeparator((BottomSeparatorHolder) holder,
                        (BottomAccountSeparatorVO) item);
                break;

            case TYPE_BUTTON:
                bindButtonItem((ButtonViewHolder) holder, (ButtonVO) item);
                break;

            case TYPE_MAIN_TITLE:
                bindMainTitleItem((MainTitleViewHolder) holder);
                break;

            default:
                throw new IllegalStateException();
        }
    }

    private void bindButtonItem(ButtonViewHolder holder, ButtonVO viewObject) {
        holder.btnListAction.setText(viewObject.getTitle());
        //holder.btnListAction.setTextColor(viewObject.getAccountColorIndicator());
    }

    private void bindMainTitleItem(MainTitleViewHolder holder) {
        holder.accountColorIndicator.setBackgroundColor(
                ColorManager.getInstance().getAccountPainter().getDefaultMainColor());

        final int level = AccountManager.getInstance().getColorLevel(AccountPainter.getFirstAccount());
        holder.itemView.setBackgroundColor(accountGroupColors[level]);

        switch (currentChatsState) {
            case unread:
                holder.tvTitle.setText(R.string.unread_chats);
                break;
            case archived:
                holder.tvTitle.setText(R.string.archived_chats);
                break;
            case all:
                holder.tvTitle.setText(R.string.all_chats);
                break;
            default:
                holder.tvTitle.setText(R.string.recent_chats);
                break;
        }
    }

    private void bindBottomSeparator(BottomSeparatorHolder holder, BottomAccountSeparatorVO viewObject) {
        holder.accountColorIndicator.setBackgroundColor(viewObject.getAccountColorIndicator());

        if (viewObject.isShowOfflineShadow()) {
            holder.offlineShadowBottom.setVisibility(View.VISIBLE);
            holder.offlineShadowTop.setVisibility(View.VISIBLE);
        } else {
            holder.offlineShadowBottom.setVisibility(View.GONE);
            holder.offlineShadowTop.setVisibility(View.GONE);
        }
    }

    private void bindAccount(AccountGroupViewHolder viewHolder, AccountVO viewObject) {

        final int level = AccountManager.getInstance().getColorLevel(viewObject.getAccountJid());
        viewHolder.itemView.setBackgroundColor(accountGroupColors[level]);

        viewHolder.accountColorIndicator.setBackgroundColor(viewObject.getAccountColorIndicator());

        viewHolder.tvAccountName.setText(viewObject.getName());
        //viewHolder.tvAccountName.setTextColor(viewObject.getAccountColorIndicator());
        viewHolder.tvContactCount.setText(viewObject.getContactCount());
        viewHolder.tvJid.setText(viewObject.getJid());

        String statusText = viewObject.getStatus();
        if (statusText.isEmpty()) statusText = activity.getString(viewObject.getStatusId());

        viewHolder.tvStatus.setText(statusText);
        //viewHolder.tvStatus.setTextColor(viewObject.getAccountColorIndicator());

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.avatarView.setVisibility(View.VISIBLE);
            viewHolder.ivAvatar.setImageDrawable(viewObject.getAvatar());
        } else viewHolder.avatarView.setVisibility(View.GONE);

        viewHolder.ivStatus.setImageLevel(viewObject.getStatusLevel());

        viewHolder.smallRightIcon.setImageLevel(viewObject.getOfflineModeLevel());

        if (viewObject.isShowOfflineShadow())
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        else viewHolder.offlineShadow.setVisibility(View.GONE);
    }

    private void bindGroup(GroupViewHolder viewHolder, GroupVO viewObject) {

        viewHolder.accountColorIndicator.setBackgroundColor(viewObject.getAccountColorIndicator());

        viewHolder.indicator.setImageLevel(viewObject.getExpandIndicatorLevel());

        if (viewObject.getTitle().equals(GroupVO.RECENT_CHATS_TITLE))
            viewHolder.indicator.setVisibility(View.GONE);
        else viewHolder.indicator.setVisibility(View.VISIBLE);

        viewHolder.groupOfflineIndicator.setImageLevel(viewObject.getOfflineIndicatorLevel());

        viewHolder.groupOfflineIndicator.setVisibility(View.GONE);
        viewHolder.offlineShadow.setVisibility(View.GONE);

        viewHolder.name.setText(viewObject.getTitle());

        viewHolder.groupOfflineIndicator.setVisibility(View.VISIBLE);

        if (viewObject.isShowOfflineShadow())
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        else viewHolder.offlineShadow.setVisibility(View.GONE);
    }

    @Override
    public void onContactClick(int adapterPosition) {
        if (adapterPosition >= 0 && adapterPosition < rosterItemVOs.size()) {
            AccountJid accountJid = ((ContactVO) rosterItemVOs.get(adapterPosition)).getAccountJid();
            UserJid userJid = ((ContactVO) rosterItemVOs.get(adapterPosition)).getUserJid();
            listener.onContactClick(RosterManager.getInstance().getAbstractContact(accountJid, userJid));
        }
    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        contactItemInflater.onAvatarClick((ContactVO) getItem(adapterPosition));
    }

    @Override
    public void onContactCreateContextMenu(int adapterPosition, ContextMenu menu) {
        AccountJid accountJid = ((ContactVO) rosterItemVOs.get(adapterPosition)).getAccountJid();
        UserJid userJid = ((ContactVO) rosterItemVOs.get(adapterPosition)).getUserJid();
        AbstractContact abstractContact = RosterManager.getInstance().getAbstractContact(accountJid, userJid);
        ContextMenuHelper.createContactContextMenu(activity, this, abstractContact, menu);
    }

    @Override
    public void onContactButtonClick(int adapterPosition) {
        setState(ChatListState.all);
        onChange();
    }

    @Override
    public void onAccountAvatarClick(int adapterPosition) {
        if (adapterPosition == -1) adapterPosition = currentHeaderPosition;
        activity.startActivity(AccountActivity.createIntent(activity,
                ((AccountVO)getItem(adapterPosition)).getAccountJid()));
    }

    @Override
    public void onAccountMenuClick(int adapterPosition, View view) {
        if (adapterPosition == -1) adapterPosition = currentHeaderPosition;
        listener.onAccountMenuClick(
                ((AccountVO)rosterItemVOs.get(adapterPosition)).getAccountJid(), view);
    }

    @Override
    public void onAccountGroupClick(int adapterPosition) {
        if (adapterPosition == -1) {
            adapterPosition = currentHeaderPosition;
            toggleGroupExpand(adapterPosition);
            listener.onScrollToPosition(adapterPosition);
        } else toggleGroupExpand(adapterPosition);
    }

    @Override
    public void onAccountGroupCreateContextMenu(int adapterPosition, ContextMenu menu) {
        if (adapterPosition == -1) adapterPosition = currentHeaderPosition;
        AccountVO accountVO = (AccountVO) getItem(adapterPosition);
        ContextMenuHelper.createAccountContextMenu(activity, this, accountVO.getAccountJid(), menu);
    }

    @Override
    public void onGroupClick(int adapterPosition) {
        toggleGroupExpand(adapterPosition);
    }

    @Override
    public void onGroupCreateContextMenu(int adapterPosition, ContextMenu menu) {
        GroupVO groupVO = (GroupVO) getItem(adapterPosition);

        ContextMenuHelper.createGroupContextMenu(activity, this,
                groupVO.getAccountJid(), groupVO.getGroupName(), menu);
    }

    @Override
    public void onButtonClick(int position) {
        if (rosterItemVOs.size() > position) {
            if (rosterItemVOs.get(position) instanceof ButtonVO) {
                ButtonVO viewObject = (ButtonVO) rosterItemVOs.get(position);
                if (viewObject.getAction().equals(ButtonVO.ACTION_ADD_CONTACT)) {
                    if (activity != null)
                        activity.startActivity(ContactAddActivity.createIntent(activity));
                }
            }
        }
    }

    private void toggleGroupExpand(int adapterPosition) {
        BaseRosterItemVO viewObject = (BaseRosterItemVO) getItem(adapterPosition);
        if (viewObject instanceof GroupVO) {
            GroupVO groupVO = (GroupVO) getItem(adapterPosition);

            // recent chats not roll up or expand
            if (!groupVO.getGroupName().equals(GroupVO.RECENT_CHATS_TITLE)) {
                boolean expanded;
                expanded = groupVO.getExpandIndicatorLevel() == 1;

                GroupManager.getInstance().setExpanded(groupVO.getAccountJid(),
                        groupVO.getGroupName(), !expanded);

                onChange();
            }
        } else {
            AccountVO accountVO = (AccountVO) getItem(adapterPosition);
            GroupManager.getInstance().setExpanded(accountVO.getAccountJid(),
                    accountVO.getGroupName(), !accountVO.isExpand());

            onChange();
        }
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
        void onScrollToPosition(int position);
        void hidePlaceholder();
        void showPlaceholder(String message);
        void onChatListStateChanged();

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

    public static class UpdateUnreadCountEvent {
        private int count;

        public UpdateUnreadCountEvent(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }

    @Override
    public List<?> getAdapterData() {
        return rosterItemVOs;
    }

    @Override
    public void headerAttached(View headerView, int adapterPosition) {
        this.currentHeaderPosition = adapterPosition;
        if (rosterItemVOs.get(adapterPosition) instanceof AccountVO)
            ((ContactListActivity)activity).setStatusBarColor(((AccountVO)
                    rosterItemVOs.get(adapterPosition)).getAccountJid());
        else ((ContactListActivity)activity).setStatusBarColor();
    }

    @Override
    public void headerDetached(View headerView, int adapterPosition) {}

    @Override
    public void onStateChanged(ChatListState state) {
        this.currentChatsState = state;
        listener.onChatListStateChanged();
        onChange();
    }

    public void setState(ChatListState state) {
        this.currentChatsState = state;
    }

    public ChatListState getCurrentChatsState() {
        return currentChatsState;
    }

    public enum ChatListState {
        recent,
        unread,
        archived,
        all
    }

    private GroupConfiguration getChatsGroup(Collection<AbstractChat> chats, ChatListState state) {
        GroupConfiguration chatsGroup = new GroupConfiguration(GroupManager.NO_ACCOUNT,
                GroupVO.RECENT_CHATS_TITLE, GroupManager.getInstance());

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
        EventBus.getDefault().post(new UpdateUnreadCountEvent(unreadMessageCount));

        Collections.sort(newChats, ChatComparator.CHAT_COMPARATOR);

        chatsGroup.setNotEmpty();

        int itemsCount = 0;
        for (AbstractChat chat : newChats) {
            if (itemsCount < MAX_RECENT_ITEMS || state != ChatListState.recent) {
                chatsGroup.addAbstractContact(RosterManager.getInstance()
                        .getBestContact(chat.getAccount(), chat.getUser()));
                chatsGroup.increment(true);
                itemsCount++;
            } else break;
        }

        return chatsGroup;
    }

    public ArrayList<ChatVO> getTwoNextRecentChat() {
        Collection<AbstractChat> chats = MessageManager.getInstance().getChatsOfEnabledAccount();
        GroupConfiguration chatsGroup = getChatsGroup(chats, currentChatsState);

        ArrayList<ChatVO> items = new ArrayList<>();
        if (chatsGroup.getAbstractContacts() instanceof List) {
            items.add(ChatVO.convert(((ArrayList<AbstractContact>)chatsGroup.getAbstractContacts()).get(MAX_RECENT_ITEMS - 2)));
            items.add(ChatVO.convert(((ArrayList<AbstractContact>)chatsGroup.getAbstractContacts()).get(MAX_RECENT_ITEMS - 1)));
        }
        return items;
    }

    public void removeItem(int position) {
        // remove item
        rosterItemVOs.remove(position);
        notifyItemRemoved(position);

        // update end of list
        if (currentChatsState == ChatListState.recent) {
            ArrayList<ChatVO> items = getTwoNextRecentChat();
            rosterItemVOs.add(MAX_RECENT_ITEMS - 1, items.get(0));
            notifyItemInserted(MAX_RECENT_ITEMS - 1);
            rosterItemVOs.set(MAX_RECENT_ITEMS, items.get(1));
            notifyItemChanged(MAX_RECENT_ITEMS);
        }
    }

    public void restoreItem(BaseRosterItemVO item, int position) {
        // update end of list
        if (currentChatsState == ChatListState.recent) {
            BaseRosterItemVO lastChat = rosterItemVOs.get(MAX_RECENT_ITEMS - 1);
            rosterItemVOs.remove(MAX_RECENT_ITEMS - 1);
            notifyItemRemoved(MAX_RECENT_ITEMS - 1);
            rosterItemVOs.set(MAX_RECENT_ITEMS - 1, lastChat);
            notifyItemChanged(MAX_RECENT_ITEMS - 1);
        }

        // restore item
        rosterItemVOs.add(position, item);
        notifyItemInserted(position);
    }
}
