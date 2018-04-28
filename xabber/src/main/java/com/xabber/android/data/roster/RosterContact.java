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
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.iqlast.LastActivityInteractor;
import com.xabber.android.utils.StringUtils;

import java.lang.ref.WeakReference;
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
    private final Map<String, RosterGroupReference> groupReferences;

    /**
     * Whether contact`s account is connected.
     */
    protected boolean connected;

    /**
     * Whether contact`s account is enabled.
     */
    protected boolean enabled;

    private static final  NestedMap<WeakReference<RosterContact>> instances = new NestedMap<>();

    static RosterContact getRosterContact(AccountJid account, UserJid user, String name) {
        WeakReference<RosterContact> contactWeakReference = instances.get(account.toString(), user.toString());
        if (contactWeakReference != null && contactWeakReference.get() != null) {
            contactWeakReference.get().setName(name);
            return contactWeakReference.get();
        }

        RosterContact rosterContact = new RosterContact(account, user, name);
        instances.put(account.toString(), user.toString(), new WeakReference<>(rosterContact));
        return rosterContact;
    }

    private RosterContact(AccountJid account, UserJid user, String name) {
        super(account, user);

        if (name == null) {
            this.name = null;
        } else {
            this.name = name.trim();
        }

        groupReferences = new HashMap<>();
        connected = true;
        enabled = true;
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

    void clearGroupReferences() {
        groupReferences.clear();
    }

    void addGroupReference(RosterGroupReference groupReference) {
        groupReferences.put(groupReference.getName(), groupReference);
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

    public String getLastActivity() {
        return StringUtils.getLastActivityString(LastActivityInteractor.getInstance().getLastActivity(getUser()));
    }
}
