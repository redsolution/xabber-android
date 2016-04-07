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

import com.xabber.android.data.entity.AccountJid;

/**
 * Provide information about group.
 *
 * @author alexander.ivanov
 */
public interface GroupStateProvider {

    /**
     * @return Whether specified group in specified account is expanded.
     */
    boolean isExpanded(AccountJid account, String group);

    /**
     * @return Whether to show offline contacts for specified group in specified
     * account.
     */
    ShowOfflineMode getShowOfflineMode(AccountJid account, String group);

    /**
     * Sets whether group in specified account is expanded.
     *
     */
    void setExpanded(AccountJid account, String group, boolean expanded);

    /**
     * Sets whether to show offline contacts for specified group.
     *
     */
    void setShowOfflineMode(AccountJid account, String group,
                            ShowOfflineMode showOfflineMode);

}
