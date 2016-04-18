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
package com.xabber.android.data.connection;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnInitializedListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.receiver.ConnectivityReceiver;

/**
 * Manage network connectivity.
 *
 * @author alexander.ivanov
 */
public class NetworkManager implements OnCloseListener, OnInitializedListener {

    private final ConnectivityReceiver connectivityReceiver;

    private final ConnectivityManager connectivityManager;

    // strange hidden action
    public static final String INET_CONDITION_ACTION = "android.net.conn.INET_CONDITION_ACTION";

    /**
     * Type of last active network.
     */
    private Integer type;

    /**
     * Whether last active network was suspended.
     */
    private boolean suspended;

    private final WifiLock wifiLock;

    private final WakeLock wakeLock;

    /**
     * Current network state.
     */
    private NetworkState state;

    private static final NetworkManager instance;

    static {
        instance = new NetworkManager(Application.getInstance());
        Application.getInstance().addManager(instance);
    }

    public static NetworkManager getInstance() {
        return instance;
    }

    private NetworkManager(Application application) {
        connectivityReceiver = new ConnectivityReceiver();
        connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        updateNetworkInfo();

        wifiLock = ((WifiManager) application
                .getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "Xabber Wifi Lock");
        wifiLock.setReferenceCounted(false);

        wakeLock = ((PowerManager) application
                .getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Xabber Wake Lock");
        wakeLock.setReferenceCounted(false);

        state = NetworkState.available;
    }

    private void updateNetworkInfo() {
        NetworkInfo active = connectivityManager.getActiveNetworkInfo();
        type = getType(active);
        suspended = isSuspended(active);
    }

    /**
     * @return Type of network. <code>null</code> if network is
     * <code>null</code> or it is not connected and is not suspended.
     */
    @Nullable
    private Integer getType(NetworkInfo networkInfo) {
        if (networkInfo != null
                && (networkInfo.getState() == State.CONNECTED
                || networkInfo.getState() == State.SUSPENDED)) {
            return networkInfo.getType();
        }
        return null;
    }

    /**
     * @return <code>true</code> if network is not <code>null</code> and is suspended.
     */
    private boolean isSuspended(NetworkInfo networkInfo) {
        return networkInfo != null && networkInfo.getState() == State.SUSPENDED;
    }

    public NetworkState getState() {
        return state;
    }

    @Override
    public void onInitialized() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(INET_CONDITION_ACTION);
        Application.getInstance().registerReceiver(connectivityReceiver, filter);
        onWakeLockSettingsChanged();
        onWifiLockSettingsChanged();
    }

    @Override
    public void onClose() {
        Application.getInstance().unregisterReceiver(connectivityReceiver);
    }

    public void onNetworkChange() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        LogManager.i(this, "Active network info: " + networkInfo);
        if (networkInfo != null && networkInfo.getState() == State.CONNECTED) {
            onAvailable(getType(networkInfo));
            state = NetworkState.available;
        } else {
            state = NetworkState.unavailable;
        }
//
//        if (this.type != null && this.type.equals(type)) {
//            if (this.suspended == suspended) {
//                LogManager.i(this, "State does not changed.");
//            } else if (suspended) {
//                onSuspend();
//            } else {
//                onResume();
//            }
//        } else {
//            if (suspended) {
//                type = null;
//                suspended = false;
//            }
//            if (type == null) {
//                onUnavailable();
//            } else {
//                onAvailable(type);
//            }
//        }
//        this.type = type;
//        this.suspended = suspended;
    }

    /**
     * New network is available. Start connection.
     */
    private void onAvailable(Integer type) {
        state = NetworkState.available;
        LogManager.i(this, "Available");
//        if (type == ConnectivityManager.TYPE_WIFI) {
//            ConnectionManager.getInstance().forceReconnect();
//        } else {
//            ConnectionManager.getInstance().reconnect();
//        }
    }

    /**
     * Network is temporary unavailable.
     */
    private void onSuspend() {
        state = NetworkState.suspended;
        LogManager.i(this, "Suspend");
        // TODO: ConnectionManager.getInstance().pauseKeepAlive();
    }

    /**
     * Network becomes available after suspend.
     */
    private void onResume() {
        state = NetworkState.available;
        LogManager.i(this, "Resume");
//        ConnectionManager.getInstance().updateConnections(false);
        // TODO: ConnectionManager.getInstance().forceKeepAlive();
    }

    /**
     * Network is not available. Stop connections.
     */
    private void onUnavailable() {
        state = NetworkState.unavailable;
        LogManager.i(this, "Unavailable");
//        ConnectionManager.getInstance().updateConnections(false);
    }

    public void onWifiLockSettingsChanged() {
        if (SettingsManager.connectionWifiLock()) {
            wifiLock.acquire();
        } else {
            wifiLock.release();
        }
    }

    public void onWakeLockSettingsChanged() {
        if (SettingsManager.connectionWakeLock()) {
            wakeLock.acquire();
        } else {
            wakeLock.release();
        }
    }

}
