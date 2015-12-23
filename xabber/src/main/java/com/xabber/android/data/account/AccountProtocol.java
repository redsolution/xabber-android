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
package com.xabber.android.data.account;

import com.xabber.android.R;

/**
 * Supported account protocols.
 *
 * @author alexander.ivanov
 */
public enum AccountProtocol {

    /**
     * XMPP protocol.
     */
    xmpp,

    /**
     * GTalk.
     */
    gtalk,

    /**
     * Windows Live Messenger.
     */
    wlm;

    /**
     * @return Whether protocol support OAuth authorization.
     */
    public boolean isOAuth() {
        return this == wlm;
    }

    /**
     * @return Display name.
     */
    public int getNameResource() {
        if (this == xmpp)
            return R.string.xmpp;
        else if (this == gtalk)
            return R.string.google_talk;
        else
            throw new UnsupportedOperationException();
    }

    /**
     * @return Short name.
     */
    public int getShortResource() {
        if (this == xmpp)
            return R.string.xmpp;
        else if (this == gtalk)
            return R.string.google_talk;
        else
            throw new UnsupportedOperationException();
    }

}
