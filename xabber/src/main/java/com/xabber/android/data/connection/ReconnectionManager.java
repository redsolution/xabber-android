package com.xabber.android.data.connection;

import androidx.annotation.NonNull;

import com.xabber.android.data.OnTimerListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.reliablemessagedelivery.ReliableMessageDeliveryManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.push.SyncManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ReconnectionManager implements OnConnectedListener,
        OnAccountRemovedListener, OnTimerListener {

    /**
     * Intervals in seconds to be used for attempt to reconnect. First value
     * will be used on first attempt. Next value will be used if reconnection
     * fails. Last value will be used if there is no more values in array.
     */
    private final static int[] RECONNECT_AFTER = new int[]{0, 5, 10, 30, 60};
    private final static int[] ERROR_RECONNECT_AFTER = new int[]{15, 30, 60, 120, 240};
    private int tenSecondsCounter = 0;
    private static final String LOG_TAG = ReconnectionManager.class.getSimpleName();

    /**
     * Managed connections.
     */
    private final HashMap<AccountJid, ReconnectionInfo> connections;

    private static ReconnectionManager instance;

    public static ReconnectionManager getInstance() {
        if (instance == null) {
            instance = new ReconnectionManager();
        }
        return instance;
    }

    private ReconnectionManager() {
        connections = new HashMap<>();
    }

    @Override
    public void onTimer() {
        tenSecondsCounter++;
        if (tenSecondsCounter >= 10){
            tenSecondsCounter = 0;
            ReliableMessageDeliveryManager.getInstance().resendMessagesWithoutReceipt();
        }
        Collection<AccountJid> allAccounts = AccountManager.getInstance().getAllAccounts();
//        checkCarbonStatus();
        for (AccountJid accountJid : allAccounts) {
            checkConnection(AccountManager.getInstance().getAccount(accountJid),
                    getReconnectionInfo(accountJid));
        }
    }

    private void checkCarbonStatus(){
        Collection<AccountItem> accountItems = AccountManager.getInstance().getAllAccountItems();
        for (AccountItem accountItem : accountItems){
            LogManager.d(LOG_TAG, "For account " + accountItem.getAccount().toString() + " carbons status is: " + CarbonManager.getInstance().isCarbonsEnabledForConnection((ConnectionItem) accountItem));
        }
    }

    private void checkConnection(AccountItem accountItem, ReconnectionInfo reconnectionInfo) {
        if (!accountItem.isEnabled()) {
            if (accountItem.getState() == ConnectionState.disconnecting) {
                return;
            }
            if (accountItem.getState() != ConnectionState.offline) {
                ((ConnectionItem)accountItem).updateState(ConnectionState.offline);
            }
        } else if (accountItem.getState() == ConnectionState.connecting)
            ((ConnectionItem)accountItem).updateState((ConnectionState.connecting));

        if ((!accountItem.isEnabled() || !accountItem.getRawStatusMode().isOnline())
                && accountItem.getConnection().isConnected()) {
            accountItem.disconnect();
            return;
        }

        if (accountItem.isEnabled() && accountItem.getStreamError()) {
            if (!isTimeToReconnectAfterError(reconnectionInfo)) {
                return;
            }
        }

        if (!isAccountNeedConnection(accountItem)) {
            reconnectionInfo.reset();
            return;
        }

        if (!isTimeToReconnect(reconnectionInfo)) {
            LogManager.i(LOG_TAG, accountItem.getAccount()
                    + " not authenticated. State: " + accountItem.getState()
                    + " waiting... seconds from last reconnection "
                    + getTimeSinceLastReconnectionSeconds(reconnectionInfo));
            return;
        }

        boolean newThreadStarted = accountItem.connect();
        if (newThreadStarted) {
            reconnectionInfo.nextAttempt();
            LogManager.i(LOG_TAG, accountItem.getAccount()
                    + " not authenticated. new thread started. next attempt "
                    + reconnectionInfo.getReconnectAttempts());
        } else {
            reconnectionInfo.resetReconnectionTime();
            LogManager.i(LOG_TAG, accountItem.getAccount()
                    + " not authenticated. already in progress. reset time. attempt "
                    + reconnectionInfo.getReconnectAttempts());
        }
    }

    private boolean isAccountNeedConnection(AccountItem accountItem) {
        return accountItem.isEnabled() && accountItem.getRawStatusMode().isOnline()
                && !accountItem.getConnection().isAuthenticated()
                && SyncManager.getInstance().isAccountNeedConnection(accountItem);
    }

    /**
     * Check whether it's time to reconnect after getting a stream error for the connection.
     * @param reconnectionInfo reconnection info used for the current connection
     * @return if we should reconnect
     */
    private boolean isTimeToReconnectAfterError(ReconnectionInfo reconnectionInfo) {
        int reconnectAfter;
        if (reconnectionInfo.getReconnectAttempts() < ERROR_RECONNECT_AFTER.length) {
            reconnectAfter = ERROR_RECONNECT_AFTER[reconnectionInfo.getReconnectAttempts()];
        } else {
            reconnectAfter = ERROR_RECONNECT_AFTER[ERROR_RECONNECT_AFTER.length - 1];
        }
        return getTimeSinceLastReconnectionSeconds(reconnectionInfo) >= reconnectAfter;
    }

    private boolean isTimeToReconnect(ReconnectionInfo reconnectionInfo) {
        int reconnectAfter;
        if (reconnectionInfo.getReconnectAttempts() < RECONNECT_AFTER.length) {
            reconnectAfter = RECONNECT_AFTER[reconnectionInfo.getReconnectAttempts()];
        } else {
            reconnectAfter = RECONNECT_AFTER[RECONNECT_AFTER.length - 1];
        }

        return getTimeSinceLastReconnectionSeconds(reconnectionInfo) >= reconnectAfter;
    }

    private long getTimeSinceLastReconnectionSeconds(ReconnectionInfo reconnectionInfo) {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()
                - reconnectionInfo.getLastReconnectionTimeMillis());
    }

    public void requestReconnect(AccountJid accountJid) {
        getReconnectionInfo(accountJid).reset();
    }

    @NonNull
    private ReconnectionInfo getReconnectionInfo(AccountJid accountJid) {
        ReconnectionInfo reconnectionInfo = connections.get(accountJid);
        if (reconnectionInfo == null) {
            LogManager.i(LOG_TAG, "getReconnectionInfo new reconnection info for  " + accountJid);
            reconnectionInfo = new ReconnectionInfo();
            connections.put(accountJid, reconnectionInfo);
        }
        return reconnectionInfo;
    }

    void resetReconnectionInfo(AccountJid accountJid) {
        ReconnectionInfo info = connections.get(accountJid);
        if (info != null) {
            info.reset();
        }
    }

    @Override
    public void onConnected(ConnectionItem connection) {
        LogManager.i(LOG_TAG, "onConnected " + connection.getAccount());
        resetReconnectionInfo(connection.getAccount());
        //ReliableMessageDeliveryManager.getInstance().resendMessagesWithoutReceipt();
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        connections.remove(accountItem.getAccount());
    }

}