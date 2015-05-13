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
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.Group;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.android.ui.ContactViewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provide grouping implementation for the list of contacts.
 *
 * @author alexander.ivanov
 */
public abstract class GroupedContactAdapter extends BaseAdapter implements UpdatableAdapter {

    /**
     * List of groups used if contact has no groups.
     */
    static final Collection<Group> NO_GROUP_LIST;

    static final int TYPE_COUNT = 5;

    /**
     * View type used for contact items.
     */
    static final int TYPE_CONTACT = 0;

    /**
     * View type used for groups and accounts expanders.
     */
    static final int TYPE_GROUP = 1;
    static final int TYPE_ACCOUNT = 2;
    static final int TYPE_ACCOUNT_TOP_SEPARATOR = 3;
    static final int TYPE_ACCOUNT_BOTTOM_SEPARATOR = 4;

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

    final ArrayList<BaseEntity> baseEntities = new ArrayList<>();
    /**
     * Layout inflater
     */
    private final LayoutInflater layoutInflater;
    private final Activity activity;
    private final int[] accountSubgroupColors;
    private final int activeChatsColor;
    private final OnClickListener onClickListener;
    private final ContactItemInflater contactItemInflater;
    private final int accountElevation;
    protected Locale locale = Locale.getDefault();
    private int[] accountGroupColors;

    public GroupedContactAdapter(Activity activity, OnClickListener onClickListener) {
        this.activity = activity;

        layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Resources resources = activity.getResources();


        accountGroupColors = resources.getIntArray(R.array.account_200);
        accountSubgroupColors = resources.getIntArray(R.array.account_50);
        activeChatsColor = resources.getColor(R.color.color_primary_light);

        contactItemInflater = new ContactItemInflater(activity);

        accountElevation = activity.getResources().getDimensionPixelSize(R.dimen.account_group_elevation);

        this.onClickListener = onClickListener;
    }

    @Override
    public void onChange() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return baseEntities.size();
    }

    @Override
    public Object getItem(int position) {
        return baseEntities.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
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
        } else if (object instanceof ContactListAdapter.AccountTopSeparator) {
            return TYPE_ACCOUNT_TOP_SEPARATOR;
        } else if (object instanceof ContactListAdapter.AccountBottomSeparator) {
            return TYPE_ACCOUNT_BOTTOM_SEPARATOR;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case TYPE_CONTACT:
                return getContactView(position, convertView, parent);

            case TYPE_GROUP:
                return getGroupView(position, convertView, parent);

            case TYPE_ACCOUNT:
                return getAccountView(position, convertView, parent);

            case TYPE_ACCOUNT_TOP_SEPARATOR:
                return getView(convertView, parent, R.layout.account_group_item_top_separator);

            case TYPE_ACCOUNT_BOTTOM_SEPARATOR:
                return getAccountBottomSeparatorView(
                        getView(convertView, parent, R.layout.account_group_item_bottom_separator), position);

            default:
                throw new IllegalStateException();
        }
    }

    private View getView(View convertView, ViewGroup parent, int layoutId) {
        final View view;
        if (convertView == null) {
            view = layoutInflater.inflate(layoutId, parent, false);
        } else {
            view = convertView;
        }
        return view;
    }

    private View getAccountBottomSeparatorView(View view, int position) {
        final ContactListAdapter.AccountBottomSeparator accountBottomSeparator
                = (ContactListAdapter.AccountBottomSeparator) getItem(position);
        final int level = AccountManager.getInstance().getColorLevel(accountBottomSeparator.getAccount());

        View bottomLayer = view.findViewById(R.id.bottom_layer);
        View topLayer = view.findViewById(R.id.top_layer);

        bottomLayer.setBackgroundDrawable(new ColorDrawable(accountSubgroupColors[level]));
        topLayer.setBackgroundDrawable(new ColorDrawable(accountSubgroupColors[level]));

        StatusMode statusMode = AccountManager.getInstance().getAccount(accountBottomSeparator.getAccount()).getDisplayStatusMode();

        View offlineShadowBottom = view.findViewById(R.id.offline_shadow_top);
        View offlineShadowTop = view.findViewById(R.id.offline_shadow_bottom);

        if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection) {
            offlineShadowBottom.setVisibility(View.VISIBLE);
            offlineShadowTop.setVisibility(View.VISIBLE);
        } else {
            offlineShadowBottom.setVisibility(View.GONE);
            offlineShadowTop.setVisibility(View.GONE);
        }

        return view;
    }

    private View getAccountView(int position, View convertView, ViewGroup parent) {
        final View view;
        final ContactListItemViewHolder viewHolder;
        if (convertView == null) {
            view = layoutInflater.inflate(R.layout.contact_list_item, parent, false);

            viewHolder = new ContactListItemViewHolder(view);
            viewHolder.outgoingMessageIndicator.setVisibility(View.GONE);
            viewHolder.color.setVisibility(View.INVISIBLE);
            viewHolder.largeClientIcon.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setElevation(accountElevation);
            }
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ContactListItemViewHolder) view.getTag();
        }

        final AccountConfiguration configuration = (AccountConfiguration) getItem(position);

        final String account = configuration.getAccount();

        viewHolder.statusIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onAccountMenuClick(v, account);
            }
        });

        final int level = AccountManager.getInstance().getColorLevel(account);
        view.setBackgroundDrawable(new ColorDrawable(accountGroupColors[level]));

        viewHolder.name.setText(GroupManager.getInstance().getGroupName(account, configuration.getUser()));
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

        viewHolder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(ContactViewer.createIntent(activity, account, GroupManager.IS_ACCOUNT));
            }
        });

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

        return view;
    }

    private View getGroupView(int position, View convertView, ViewGroup parent) {
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
        viewHolder.offlineShadow.setVisibility(View.GONE);

        if (configuration.getUser().equals(GroupManager.ACTIVE_CHATS)) {
            color = activeChatsColor;
            viewHolder.name.setText(name);
        } else {
            viewHolder.name.setText(name + " (" + configuration.getOnline()
                    + "/" + configuration.getTotal() + ")");

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

        view.setBackgroundDrawable(new ColorDrawable(color));

        return view;
    }

    private View getContactView(int position, View convertView, ViewGroup parent) {
        final AbstractContact abstractContact = (AbstractContact) getItem(position);
        return contactItemInflater.setUpContactView(convertView, parent, abstractContact);
    }

    /**
     * Gets or creates roster group in roster account.
     *
     * @param accountConfiguration
     * @param name
     * @return
     */
    protected GroupConfiguration getGroupConfiguration(AccountConfiguration accountConfiguration, String name) {
        GroupConfiguration groupConfiguration = accountConfiguration.getGroupConfiguration(name);
        if (groupConfiguration != null) {
            return groupConfiguration;
        }
        groupConfiguration = new GroupConfiguration(
                accountConfiguration.getAccount(), name, GroupManager.getInstance());
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
    protected GroupConfiguration getGroupConfiguration(Map<String, GroupConfiguration> groups, String name) {
        GroupConfiguration groupConfiguration = groups.get(name);
        if (groupConfiguration != null) {
            return groupConfiguration;
        }
        groupConfiguration = new GroupConfiguration(GroupManager.NO_ACCOUNT, name, GroupManager.getInstance());
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
        Map<String, AccountConfiguration> accounts, Map<String, GroupConfiguration> groups,
        List<AbstractContact> contacts, boolean showAccounts, boolean showGroups) {
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
                                 boolean online, Map<String, AccountConfiguration> accounts,
                                 Map<String, GroupConfiguration> groups,
                                 List<AbstractContact> contacts, boolean showAccounts,
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
        GroupManager.getInstance().setExpanded(account, group, expanded);
        onChange();
    }

    public interface OnClickListener {
        void onAccountMenuClick(View view, String account);
    }

    /**
     * Holder for views in contact list group.
     */
    private static class GroupViewHolder {
        final ImageView indicator;
        final TextView name;
        final ImageView groupOfflineIndicator;
        final ImageView offlineShadow;

        public GroupViewHolder(View view) {
            indicator = (ImageView) view.findViewById(R.id.indicator);
            name = (TextView) view.findViewById(R.id.name);
            groupOfflineIndicator = (ImageView) view.findViewById(R.id.group_offline_indicator);
            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
        }
    }

}
