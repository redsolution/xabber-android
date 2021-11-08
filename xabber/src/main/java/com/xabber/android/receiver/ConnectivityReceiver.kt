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
package com.xabber.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xabber.android.data.connection.NetworkManager
import com.xabber.android.data.log.LogManager

/**
 * Receiver for network events.
 *
 * @author alexander.ivanov
 */
class ConnectivityReceiver : BroadcastReceiver() {

    var isRegistered = false

    override fun onReceive(context: Context, intent: Intent) {
        LogManager.i(this, "onReceive " + intent.action)
        NetworkManager.getInstance().onNetworkChange()
    }
}