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
package com.xabber.android.data.extension.muc;

import com.xabber.android.data.account.StatusMode;

/**
 * States of chat room.
 *
 * @author alexander.ivanov
 */
public enum RoomState {

    /**
     * Room is available.
     */
    available,

    /**
     * Receiving occupants.
     */
    occupation,

    /**
     * Joining is in progress.
     */
    joining,

    /**
     * Creation is in progress.
     */
    creating,

    /**
     * Room is unavailable, i.e. not connected or not joined.
     */
    unavailable,

    /**
     * Waiting for connection to join the room.
     */
    waiting,

    /**
     * Authentication error.
     */
    error;

    /**
     * @return Status mode used in contact list.
     */
    StatusMode toStatusMode() {
        switch (this) {
            case available:
                return StatusMode.available;
            case occupation:
            case joining:
            case creating:
            case waiting:
                return StatusMode.connection;
            case unavailable:
                return StatusMode.unavailable;
            case error:
                return StatusMode.unsubscribed;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * @return Connected is establish or connection is in progress.
     */
    boolean inUse() {
        switch (this) {

            case available:
            case occupation:
            case joining:
            case creating:
                return true;

            case unavailable:
            case waiting:
            case error:
            default:
                return false;
        }
    }
}
