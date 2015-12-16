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

import android.text.TextUtils;

import com.xabber.android.data.account.StatusMode;

import org.jivesoftware.smack.roster.RosterEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contact in roster.
 * <p/>
 * {@link #getUser()} always will be bare jid.
 *
 * @author alexander.ivanov
 */
public class RosterContact extends AbstractContact {

    /**
     * Contact's name.
     */
    protected String name;

    /**
     * Used groups with its names.
     */
    protected final Map<String, RosterGroupReference> groupReferences;

    /**
     * Whether there is subscription of type "both" or "to".
     */
    protected boolean subscribed;

    /**
     * Whether contact`s account is connected.
     */
    protected boolean connected;

    /**
     * Whether contact`s account is enabled.
     */
    protected boolean enabled;

    /**
     * Data id to view contact information from system contact list.
     * <p/>
     * Warning: not implemented yet.
     */
    private Long viewId;

    public RosterContact(String account, RosterEntry rosterEntry) {
        this(account, rosterEntry.getUser(), rosterEntry.getName());
    }

    public RosterContact(String account, String user, String name) {
        super(account, user);

        if (name == null) {
            this.name = null;
        } else {
            this.name = name.trim();
        }

        groupReferences = new HashMap<>();
        subscribed = true;
        connected = true;
        enabled = true;
        viewId = null;
    }

    void setName(String name) {
        this.name = name;
    }

    @Override
    public Collection<RosterGroupReference> getGroups() {
        return Collections.unmodifiableCollection(groupReferences.values());
    }

    Collection<String> getGroupNames() {
        return Collections.unmodifiableCollection(groupReferences.keySet());
    }

    void addGroupReference(RosterGroupReference groupReference) {
        groupReferences.put(groupReference.getName(), groupReference);
    }

    void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    @Override
    public StatusMode getStatusMode() {
        return PresenceManager.getInstance().getStatusMode(account, user);
    }

    @Override
    public String getName() {
        if (TextUtils.isEmpty(name)) {
            return super.getName();
        } else {
            return name;
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getViewId() {
        return viewId;
    }

}
