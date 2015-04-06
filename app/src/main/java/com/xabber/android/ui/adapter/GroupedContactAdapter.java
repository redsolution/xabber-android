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
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.Group;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.GroupStateProvider;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.androiddev.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

/**
 * Provide grouping implementation for the list of contacts.
 *
 * @author alexander.ivanov
 */
public abstract class GroupedContactAdapter<Inflater extends BaseContactInflater, StateProvider extends GroupStateProvider>
        extends SmoothContactAdapter<Inflater> {

    /**
     * List of groups used if contact has no groups.
     */
    static final Collection<Group> NO_GROUP_LIST;

    static final int TYPE_COUNT = 3;

    /**
     * View type used for contact items.
     */
    static final int TYPE_CONTACT = 0;

    /**
     * View type used for groups and accounts expanders.
     */
    static final int TYPE_GROUP = 1;


    static final int TYPE_ACCOUNT = 2;

    static {
        Collection<Group> groups = new ArrayList<>(1);
        groups.add(new Group() {
            @Override
            public String getName() {
                return GroupManager.NO_GROUP;
            }
        });
        NO_GROUP_LIST = Collections.unmodifiableCollection(groups);
    }

    /**
     * Group state provider.
     */
    final StateProvider groupStateProvider;

    /**
     * Layout inflater
     */
    private final LayoutInflater layoutInflater;

    private int[] accountColors;
    private final int[] accountSubgroupColors;
    private final int activeChatsColor;
    private final OnAccountClickListener onAccountClickListener;

    public GroupedContactAdapter(Activity activity, ListView listView, Inflater inflater,
                                 StateProvider groupStateProvider, OnAccountClickListener onAccountClickListener) {
        super(activity, listView, inflater);
        layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.groupStateProvider = groupStateProvider;

        Resources resources = activity.getResources();

        accountColors = resources.getIntArray(R.array.account_200);
        accountSubgroupColors = resources.getIntArray(R.array.account_50);
        activeChatsColor = resources.getColor(R.color.color_primary_light);

        this.onAccountClickListener = onAccountClickListener;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
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
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case TYPE_CONTACT:
                return super.getView(position, convertView, parent);

            case TYPE_GROUP:
            {
                final View view;
                final GroupViewHolder viewHolder;
                if (convertView == null) {
                    view = layoutInflater.inflate(R.layout.base_group_item, parent, false);
                    viewHolder = new GroupViewHolder(view);
                    view.setTag(viewHolder);
                } else {
                    view = convertView;
                    viewHolder = (GroupViewHolder) view.getTag();
                }

                final GroupConfiguration configuration = (GroupConfiguration) getItem(position);
                final int level = AccountManager.getInstance().getColorLevel(configuration.getAccount());

                final String name = GroupManager.getInstance()
                        .getGroupName(configuration.getAccount(), configuration.getUser());


                viewHolder.indicator.setImageLevel(configuration.isExpanded() ? 1 : 0);
                viewHolder.groupOfflineIndicator.setImageLevel(configuration.getShowOfflineMode().ordinal());

                int color;

                viewHolder.groupOfflineIndicator.setVisibility(View.GONE);

                if (configuration.getUser().equals(GroupManager.ACTIVE_CHATS)) {
                    color = activeChatsColor;
                    viewHolder.name.setText(name);
                } else {
                    viewHolder.name.setText(name + " (" + configuration.getOnline()
                            + "/" + configuration.getTotal() + ")");

                    color = accountSubgroupColors[level];
                    viewHolder.groupOfflineIndicator.setVisibility(View.VISIBLE);
                }

                view.setBackgroundDrawable(new ColorDrawable(color));

                return view;
            }

            case TYPE_ACCOUNT:
                final View view;
                final AccountViewHolder viewHolder;
                if (convertView == null) {
                    view = layoutInflater.inflate(R.layout.account_group_item, parent, false);

                    viewHolder = new AccountViewHolder(view);
                    view.setTag(viewHolder);
                } else {
                    view = convertView;
                    viewHolder = (AccountViewHolder) view.getTag();
                }

                final AccountConfiguration configuration = (AccountConfiguration) getItem(position);

                final String account = configuration.getAccount();

                viewHolder.statusIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onAccountClickListener.onAccountClick(v, account);
                    }
                });

                final int level = AccountManager.getInstance().getColorLevel(account);
                view.setBackgroundDrawable(new ColorDrawable(accountColors[level]));

                viewHolder.jid.setText(GroupManager.getInstance().getGroupName(account, configuration.getUser()));
                viewHolder.contactCounter.setText(configuration.getOnline() + "/" + configuration.getTotal());

                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                String statusText = accountItem.getStatusText().trim();

                if (statusText.isEmpty()) {
                    statusText = activity.getString(accountItem.getDisplayStatusMode().getStringID());
                }

                viewHolder.status.setText(statusText);

                viewHolder.avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
                viewHolder.statusIcon.setImageLevel(accountItem.getDisplayStatusMode().getStatusLevel());

                ShowOfflineMode showOfflineMode = configuration.getShowOfflineMode();
                if (showOfflineMode == ShowOfflineMode.normal) {
                    if (SettingsManager.contactsShowOffline()) {
                        showOfflineMode = ShowOfflineMode.always;
                    } else {
                        showOfflineMode = ShowOfflineMode.never;
                    }
                }

                viewHolder.offlineContactsIndicator.setImageLevel(showOfflineMode.ordinal());

                return view;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Gets or creates roster group in roster account.
     *
     * @param accountConfiguration
     * @param name
     * @return
     */
    protected GroupConfiguration getGroupConfiguration(
            AccountConfiguration accountConfiguration, String name) {
        GroupConfiguration groupConfiguration = accountConfiguration.getGroupConfiguration(name);
        if (groupConfiguration != null) {
            return groupConfiguration;
        }
        groupConfiguration = new GroupConfiguration(
                accountConfiguration.getAccount(), name, groupStateProvider);
        accountConfiguration.addGroupConfiguration(groupConfiguration);
        return groupConfiguration;
    }

    /**
     * Gets or creates roster group in tree map.
     *
     * @param groups
     * @param name
     * @return
     */
    protected GroupConfiguration getGroupConfiguration(
            TreeMap<String, GroupConfiguration> groups, String name) {
        GroupConfiguration groupConfiguration = groups.get(name);
        if (groupConfiguration != null) {
            return groupConfiguration;
        }
        groupConfiguration = new GroupConfiguration(GroupManager.NO_ACCOUNT, name, groupStateProvider);
        groups.put(name, groupConfiguration);
        return groupConfiguration;
    }

    /**
     * Adds contact to specified group.
     *
     * @param abstractContact
     * @param group
     * @param online
     * @param accounts
     * @param groups
     * @param contacts
     * @param showAccounts
     * @param showGroups
     */
    protected void addContact(AbstractContact abstractContact, String group, boolean online,
        TreeMap<String, AccountConfiguration> accounts, TreeMap<String, GroupConfiguration> groups,
        ArrayList<AbstractContact> contacts,   boolean showAccounts, boolean showGroups) {
        if (showAccounts) {
            final String account = abstractContact.getAccount();
            final AccountConfiguration accountConfiguration;
            accountConfiguration = accounts.get(account);
            if (accountConfiguration == null) {
                return;
            }
            if (showGroups) {
                GroupConfiguration groupConfiguration
                        = getGroupConfiguration(accountConfiguration, group);
                if (accountConfiguration.isExpanded()) {
                    groupConfiguration.setNotEmpty();
                    if (groupConfiguration.isExpanded()) {
                        groupConfiguration.addAbstractContact(abstractContact);
                    }
                }
                groupConfiguration.increment(online);
            } else {
                if (accountConfiguration.isExpanded()) {
                    accountConfiguration.addAbstractContact(abstractContact);
                }
            }
            accountConfiguration.increment(online);
        } else {
            if (showGroups) {
                GroupConfiguration groupConfiguration = getGroupConfiguration(groups, group);
                groupConfiguration.setNotEmpty();
                if (groupConfiguration.isExpanded()) {
                    groupConfiguration.addAbstractContact(abstractContact);
                }
                groupConfiguration.increment(online);
            } else {
                contacts.add(abstractContact);
            }
        }
    }

    /**
     * Adds contact to there groups.
     *
     * @param abstractContact
     * @param online
     * @param accounts
     * @param groups
     * @param contacts
     * @param showAccounts
     * @param showGroups
     * @param showOffline
     * @return whether contact is visible.
     */
    protected boolean addContact(AbstractContact abstractContact,
                                 boolean online, TreeMap<String, AccountConfiguration> accounts,
                                 TreeMap<String, GroupConfiguration> groups,
                                 ArrayList<AbstractContact> contacts, boolean showAccounts,
                                 boolean showGroups, boolean showOffline) {
        boolean hasVisible = false;
        if (showAccounts) {
            final AccountConfiguration accountConfiguration;
            accountConfiguration = accounts.get(abstractContact.getAccount());
            if (accountConfiguration == null) {
                return false;
            }
            if (showGroups) {
                Collection<? extends Group> abstractGroups = abstractContact.getGroups();
                if (abstractGroups.size() == 0) {
                    abstractGroups = NO_GROUP_LIST;
                }
                for (Group abstractGroup : abstractGroups) {
                    GroupConfiguration groupConfiguration = getGroupConfiguration(
                            accountConfiguration, abstractGroup.getName());
                    if (online
                            || (groupConfiguration.getShowOfflineMode() == ShowOfflineMode.always)
                            || (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.always && groupConfiguration
                            .getShowOfflineMode() == ShowOfflineMode.normal)
                            || (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.normal
                            && groupConfiguration.getShowOfflineMode() == ShowOfflineMode.normal && showOffline)) {
                        // ............. group
                        // ......... | A | N | E
                        // ....... A | + | + | -
                        // account N | + | ? | -
                        // ....... E | + | - | -
                        hasVisible = true;
                        if (accountConfiguration.isExpanded()) {
                            groupConfiguration.setNotEmpty();
                            if (groupConfiguration.isExpanded()) {
                                groupConfiguration.addAbstractContact(abstractContact);
                            }
                        }
                    }
                    groupConfiguration.increment(online);
                }
            } else {
                if (online || (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.always)
                        || (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.normal && showOffline)) {
                    hasVisible = true;
                    if (accountConfiguration.isExpanded()) {
                        accountConfiguration.addAbstractContact(abstractContact);
                    }
                }
            }
            accountConfiguration.increment(online);
        } else {
            if (showGroups) {
                Collection<? extends Group> abstractGroups = abstractContact.getGroups();
                if (abstractGroups.size() == 0) {
                    abstractGroups = NO_GROUP_LIST;
                }
                for (Group abstractGroup : abstractGroups) {
                    GroupConfiguration groupConfiguration
                            = getGroupConfiguration(groups, abstractGroup.getName());
                    if (online || (groupConfiguration.getShowOfflineMode() == ShowOfflineMode.always)
                            || (groupConfiguration.getShowOfflineMode() == ShowOfflineMode.normal && showOffline)) {
                        groupConfiguration.setNotEmpty();
                        hasVisible = true;
                        if (groupConfiguration.isExpanded()) {
                            groupConfiguration.addAbstractContact(abstractContact);
                        }
                    }
                    groupConfiguration.increment(online);
                }
            } else {
                if (online || showOffline) {
                    hasVisible = true;
                    contacts.add(abstractContact);
                }
            }
        }
        return hasVisible;
    }

    /**
     * Sets whether group in specified account is expanded.
     */
    public void setExpanded(String account, String group, boolean expanded) {
        groupStateProvider.setExpanded(account, group, expanded);
        onChange();
    }

    /**
     * Holder for views in contact list group.
     */
    private static class GroupViewHolder {
        final ImageView indicator;
        final TextView name;
        final ImageView groupOfflineIndicator;

        public GroupViewHolder(View view) {
            indicator = (ImageView) view.findViewById(R.id.indicator);
            name = (TextView) view.findViewById(R.id.name);
            groupOfflineIndicator = (ImageView) view.findViewById(R.id.group_offline_indicator);
        }
    }

    private static class AccountViewHolder {
        final TextView jid;
        final TextView status;
        final TextView contactCounter;

        final ImageView statusIcon;
        final ImageView avatar;
        final ImageView offlineContactsIndicator;


        public AccountViewHolder(View view) {
            jid = (TextView) view.findViewById(R.id.account_jid);
            status = (TextView) view.findViewById(R.id.account_status);
            contactCounter = (TextView) view.findViewById(R.id.contact_counter);

            statusIcon = (ImageView) view.findViewById(R.id.account_status_icon);
            avatar = (ImageView) view.findViewById(R.id.avatar);
            offlineContactsIndicator = (ImageView) view.findViewById(R.id.offline_contacts_indicator);
        }
    }

    public interface OnAccountClickListener {
        void onAccountClick(View view, String account);
    }

}
