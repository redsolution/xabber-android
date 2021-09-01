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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.database.repositories.CircleRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.NestedMap.Entry;
import com.xabber.android.data.log.LogManager;

import org.jxmpp.stringprep.XmppStringprepException;

public class CircleManager implements OnLoadListener, OnAccountRemovedListener, CircleStateProvider {

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
    private static CircleManager instance;

    static {
        try {
            NO_ACCOUNT = AccountJid.from("com.xabber.android@data/NO_ACCOUNT");
        } catch (XmppStringprepException e) {
            LogManager.exception(CircleManager.class.getSimpleName(), e);
            NO_ACCOUNT = null;
        }
    }

    /**
     * List of settings for roster groups in accounts.
     */
    private final NestedMap<CircleConfiguration> groupConfigurations;

    public static CircleManager getInstance() {
        if (instance == null) {
            instance = new CircleManager();
        }

        return instance;
    }

    private CircleManager() {
        groupConfigurations = new NestedMap<>();
    }

    @Override
    public void onLoad() {
        this.groupConfigurations.addAll(CircleRepository.getGroupConfigurationsFromRealm());
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
        if (CircleManager.NO_GROUP.equals(group)) {
            return Application.getInstance().getString(R.string.no_group_contacts);
        } else if (CircleManager.IS_ROOM.equals(group)) {
            return Application.getInstance().getString(R.string.group_room);
        } else if (CircleManager.ACTIVE_CHATS.equals(group)) {
            return Application.getInstance().getString(R.string.group_active_chat);
        } else if (CircleManager.IS_ACCOUNT.equals(group)) {
            return AccountManager.getInstance().getVerboseName(account);
        }
        return group;
    }

    @Override
    public boolean isExpanded(AccountJid account, String group) {
        if (account == null) {
            return true;
        }
        CircleConfiguration configuration = groupConfigurations.get(account.toString(), group);
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
        CircleConfiguration configuration = groupConfigurations.get(account.toString(), group);
        if (configuration == null) {
            return ShowOfflineMode.normal;
        }
        return configuration.getShowOfflineMode();
    }

    @Override
    public void setExpanded(AccountJid account, String group, boolean expanded) {
        CircleConfiguration configuration = groupConfigurations.get(account.toString(), group);
        if (configuration == null) {
            configuration = new CircleConfiguration();
            groupConfigurations.put(account.toString(), group, configuration);
        }
        configuration.setExpanded(expanded);
        CircleRepository.saveCircleToRealm(account.toString(), group, expanded,
                configuration.getShowOfflineMode());
    }

    @Override
    public void setShowOfflineMode(AccountJid account, String group,
                                   ShowOfflineMode showOfflineMode) {
        CircleConfiguration configuration = groupConfigurations.get(account.toString(), group);
        if (configuration == null) {
            configuration = new CircleConfiguration();
            groupConfigurations.put(account.toString(), group, configuration);
        }
        configuration.setShowOfflineMode(showOfflineMode);
        CircleRepository.saveCircleToRealm(account.toString(), group, configuration.isExpanded(),
                showOfflineMode);
    }

    /**
     * Reset all show offline modes.
     */
    public void resetShowOfflineModes() {
        for (Entry<CircleConfiguration> entry : groupConfigurations) {
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

}
