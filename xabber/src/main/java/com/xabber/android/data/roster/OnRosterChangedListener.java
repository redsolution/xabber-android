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

import com.xabber.android.data.BaseManagerInterface;

import java.util.Collection;
import java.util.Map;

/**
 * Listener for any contact changes.
 *
 * @author alexander.ivanov
 */
public interface OnRosterChangedListener extends BaseManagerInterface {

    /**
     * Roster update received.
     *
     */
    void onRosterUpdate(
            Collection<RosterGroup> addedGroups,
            Map<RosterContact, String> addedContacts,
            Map<RosterContact, String> renamedContacts,
            Map<RosterContact, Collection<RosterGroupReference>> addedGroupReference,
            Map<RosterContact, Collection<RosterGroupReference>> removedGroupReference,
            Collection<RosterContact> removedContacts,
            Collection<RosterGroup> removedGroups);

    /**
     * Contact's presence has been changed.
     *
     * @param rosterContact
     */
    void onPresenceChanged(Collection<RosterContact> rosterContact);

    /**
     * Contact's structured name has been changed.
     *
     * @param rosterContact
     */
    void onContactStructuredInfoChanged(RosterContact rosterContact,
                                        StructuredName structuredName);

}
