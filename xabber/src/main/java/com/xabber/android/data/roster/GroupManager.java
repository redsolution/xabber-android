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
package com.xabber.android.data.roster;

import android.database.Cursor;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.database.sqlite.GroupTable;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.NestedMap.Entry;

import org.jxmpp.stringprep.XmppStringprepException;

public class GroupManager implements OnLoadListener, OnAccountRemovedListener,
        GroupStateProvider {

    /**
     * Reserved group name for the rooms.
     */
    public static final String IS_ROOM = "com.xabber.android.data.IS_ROOM";

    /**
     * Reserved group name for active chat group.
     */
    public static final String ACTIVE_CHATS = "com.xabber.android.data.ACTIVE_CHATS";

    /**
     * Reserved group name to store information about group "out of groups".
     */
    public static final String NO_GROUP = "com.xabber.android.data.NO_GROUP";

    /**
     * Group name used to store information about account itself.
     */
    public static final String IS_ACCOUNT = "com.xabber.android.data.IS_ACCOUNT";

    /**
     * Account name used to store information that don't belong to any account.
     */
    public static AccountJid NO_ACCOUNT;
    private static GroupManager instance;

    static {
        try {
            // TODO: looks ugly, comes from times, when account was string.
            NO_ACCOUNT = AccountJid.from("com.xabber.android@data/NO_ACCOUNT");
        } catch (XmppStringprepException e) {
            LogManager.exception(GroupManager.class.getSimpleName(), e);
            NO_ACCOUNT = null;
        }
    }

    /**
     * List of settings for roster groups in accounts.
     */
    private final NestedMap<GroupConfiguration> groupConfigurations;

    public static GroupManager getInstance() {
        if (instance == null) {
            instance = new GroupManager();
        }

        return instance;
    }

    private GroupManager() {
        groupConfigurations = new NestedMap<>();
    }

    @Override
    public void onLoad() {
        final NestedMap<GroupConfiguration> groupConfigurations = new NestedMap<>();
        Cursor cursor = GroupTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    GroupConfiguration rosterConfiguration = new GroupConfiguration();
                    rosterConfiguration.setExpanded(GroupTable
                            .isExpanded(cursor));
                    rosterConfiguration.setShowOfflineMode(GroupTable
                            .getShowOfflineMode(cursor));
                    groupConfigurations.put(GroupTable.getAccount(cursor),
                            GroupTable.getGroup(cursor), rosterConfiguration);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(groupConfigurations);
            }
        });
    }

    private void onLoaded(NestedMap<GroupConfiguration> groupConfigurations) {
        this.groupConfigurations.addAll(groupConfigurations);
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        groupConfigurations.clear(accountItem.getAccount().toString());
    }

    /**
     * @return Group's name to be display.
     * @see {@link #IS_ROOM}, {@link #ACTIVE_CHATS}, {@link #NO_GROUP},
     * {@link #IS_ACCOUNT}, {@link #NO_ACCOUNT}.
     */
    public String getGroupName(AccountJid account, String group) {
        if (GroupManager.NO_GROUP.equals(group)) {
            return Application.getInstance().getString(R.string.group_none);
        } else if (GroupManager.IS_ROOM.equals(group)) {
            return Application.getInstance().getString(R.string.group_room);
        } else if (GroupManager.ACTIVE_CHATS.equals(group)) {
            return Application.getInstance().getString(R.string.group_active_chat);
        } else if (GroupManager.IS_ACCOUNT.equals(group)) {
            return AccountManager.getInstance().getVerboseName(account);
        }
        return group;
    }

    @Override
    public boolean isExpanded(AccountJid account, String group) {
        if (account == null) {
            return true;
        }
        GroupConfiguration configuration = groupConfigurations.get(account.toString(), group);
        if (configuration == null) {
            return true;
        }
        return configuration.isExpanded();
    }

    @Override
    public ShowOfflineMode getShowOfflineMode(AccountJid account, String group) {
        if (account == null) {
            return ShowOfflineMode.normal;
        }
        GroupConfiguration configuration = groupConfigurations.get(account.toString(), group);
        if (configuration == null) {
            return ShowOfflineMode.normal;
        }
        return configuration.getShowOfflineMode();
    }

    @Override
    public void setExpanded(AccountJid account, String group, boolean expanded) {
        GroupConfiguration configuration = groupConfigurations.get(account.toString(), group);
        if (configuration == null) {
            configuration = new GroupConfiguration();
            groupConfigurations.put(account.toString(), group, configuration);
        }
        configuration.setExpanded(expanded);
        requestToWriteGroup(account, group, configuration.isExpanded(),
                configuration.getShowOfflineMode());
    }

    @Override
    public void setShowOfflineMode(AccountJid account, String group,
                                   ShowOfflineMode showOfflineMode) {
        GroupConfiguration configuration = groupConfigurations.get(account.toString(), group);
        if (configuration == null) {
            configuration = new GroupConfiguration();
            groupConfigurations.put(account.toString(), group, configuration);
        }
        configuration.setShowOfflineMode(showOfflineMode);
        requestToWriteGroup(account, group, configuration.isExpanded(),
                configuration.getShowOfflineMode());
    }

    /**
     * Reset all show offline modes.
     */
    public void resetShowOfflineModes() {
        for (Entry<GroupConfiguration> entry : groupConfigurations) {
            if (entry.getValue().getShowOfflineMode() != ShowOfflineMode.normal) {
                try {
                    setShowOfflineMode(AccountJid.from(entry.getFirst()), entry.getSecond(),
                            ShowOfflineMode.normal);
                } catch (XmppStringprepException e) {
                    LogManager.exception(this, e);
                }
            }
        }
    }

    private void requestToWriteGroup(final AccountJid account, final String group,
                                     final boolean expanded, final ShowOfflineMode showOfflineMode) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                GroupTable.getInstance().write(account.toString(), group, expanded, showOfflineMode);
            }
        });
    }

}
