package com.xabber.android.ui.adapter.contactlist;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.Group;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.ShowOfflineMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class ContactListGroupUtils {
    /**
     * List of groups used if contact has no groups.
     */
    private static final Collection<Group> NO_GROUP_LIST;

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
     * Gets or creates roster group in roster account.
     */
    private static GroupConfiguration getGroupConfiguration(AccountConfiguration accountConfiguration, String name) {
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
     */
    private static GroupConfiguration getGroupConfiguration(Map<String, GroupConfiguration> groups, String name) {
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
     */
    public static void addContact(AbstractContact abstractContact, String group, boolean online,
                           Map<AccountJid, AccountConfiguration> accounts, Map<String, GroupConfiguration> groups,
                           List<AbstractContact> contacts, boolean showAccounts, boolean showGroups) {
        if (showAccounts) {
            final AccountJid account = abstractContact.getAccount();
            final AccountConfiguration accountConfiguration;
            accountConfiguration = accounts.get(account);
            if (accountConfiguration == null) {
                return;
            }
            if (showGroups) {
                GroupConfiguration groupConfiguration
                        = getGroupConfiguration(accountConfiguration, group);
//                if (accountConfiguration.isExpanded()) {
//                    groupConfiguration.setNotEmpty();
//                    if (groupConfiguration.isExpanded()) {
//                        groupConfiguration.addAbstractContact(abstractContact);
//                    }
//                }
                groupConfiguration.setNotEmpty();
                groupConfiguration.addAbstractContact(abstractContact);

                groupConfiguration.increment(online);
            } else {
//                if (accountConfiguration.isExpanded()) {
//                    accountConfiguration.addAbstractContact(abstractContact);
//                }
                accountConfiguration.addAbstractContact(abstractContact);
            }
            accountConfiguration.increment(online);
        } else {
            if (showGroups) {
                GroupConfiguration groupConfiguration = getGroupConfiguration(groups, group);
                groupConfiguration.setNotEmpty();
//                if (groupConfiguration.isExpanded()) {
//                    groupConfiguration.addAbstractContact(abstractContact);
//                }
                groupConfiguration.addAbstractContact(abstractContact);
                groupConfiguration.increment(online);
            } else {
                contacts.add(abstractContact);
            }
        }
    }

    /**
     * Adds contact to there groups.
     * @return whether contact is visible.
     */
    public static boolean addContact(AbstractContact abstractContact,
                              boolean online, Map<AccountJid, AccountConfiguration> accounts,
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
//                        if (accountConfiguration.isExpanded()) {
//                            groupConfiguration.setNotEmpty();
//                            if (groupConfiguration.isExpanded()) {
//                                groupConfiguration.addAbstractContact(abstractContact);
//                            }
//                        }
                        groupConfiguration.setNotEmpty();
                        groupConfiguration.addAbstractContact(abstractContact);
                    }
                    groupConfiguration.increment(online);
                }
            } else {
                if (online || (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.always)
                        || (accountConfiguration.getShowOfflineMode() == ShowOfflineMode.normal && showOffline)) {
                    hasVisible = true;
//                    if (accountConfiguration.isExpanded()) {
//                        accountConfiguration.addAbstractContact(abstractContact);
//                    }
                    accountConfiguration.addAbstractContact(abstractContact);
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
//                        if (groupConfiguration.isExpanded()) {
//                            groupConfiguration.addAbstractContact(abstractContact);
//                        }
                        groupConfiguration.addAbstractContact(abstractContact);
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

}
