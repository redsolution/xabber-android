package com.xabber.android.data.connection;

/**
 * Information about reconnection attempts.
 *
 * @author alexander.ivanov
 */
class ReconnectionInfo {

    /**
     * Number of attempts to reconnect without success.
     */
    private int reconnectAttempts = 0;

    /**
     * Time of last reconnection.
     */
    private long lastReconnectionTimeMillis;

    public ReconnectionInfo() {
        reset();
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    public long getLastReconnectionTimeMillis() {
        return lastReconnectionTimeMillis;
    }

    public void reset() {
        reconnectAttempts = 0;
        resetReconnectionTime();
    }

    void resetReconnectionTime() {
        lastReconnectionTimeMillis = System.currentTimeMillis();
    }

    public void nextAttempt() {
        resetReconnectionTime();
        reconnectAttempts += 1;
    }
}
