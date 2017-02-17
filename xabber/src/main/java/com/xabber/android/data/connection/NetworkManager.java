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

import com.xabber.android.data.Application;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.OnInitializedListener;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.receiver.ConnectivityReceiver;

/**
 * Manage network connectivity.
 *
 * @author alexander.ivanov
 */
public class  NetworkManager implements OnCloseListener, OnInitializedListener {

    private static final String LOG_TAG = NetworkManager.class.getSimpleName();
    private final ConnectivityReceiver connectivityReceiver;

    private final ConnectivityManager connectivityManager;



    private static NetworkManager instance;

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }

        return instance;
    }

    private NetworkManager() {
        Application application = Application.getInstance();

        connectivityReceiver = new ConnectivityReceiver();
        connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onInitialized() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        Application.getInstance().registerReceiver(connectivityReceiver, filter);
        WakeLockManager.onWakeLockSettingsChanged();
        WakeLockManager.onWifiLockSettingsChanged();
    }

    @Override
    public void onClose() {
        try {
            Application.getInstance().unregisterReceiver(connectivityReceiver);
        } catch (IllegalArgumentException e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    public void onNetworkChange() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        LogManager.i(LOG_TAG, "Active network info: " + networkInfo);

        if (networkInfo != null && networkInfo.getState() == State.CONNECTED) {
            onAvailable();
        }
    }

    /**
     * New network is available. Start connection.
     */
    private void onAvailable() {
        LogManager.i(LOG_TAG, "onAvailable");
        ConnectionManager.getInstance().connectAll();
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) Application.getInstance()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
