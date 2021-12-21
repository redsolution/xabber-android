/*
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

import androidx.annotation.NonNull;

import com.xabber.android.R;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.database.repositories.AccountRepository;
import com.xabber.android.data.extension.archive.LoadHistorySettings;
import com.xabber.android.data.extension.devices.DeviceVO;

import org.jetbrains.annotations.NotNull;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;

import java.security.KeyPair;
import java.util.Date;
import java.util.UUID;

/**
 * Represent account settings and status.
 *
 * @author alexander.ivanov
 */
public class AccountItem extends ConnectionItem implements Comparable<AccountItem> {

    public static final String UNDEFINED_PASSWORD = "";
    private static final long GRACE_PERIOD = 45000;

    /**
     * Id in database.
     * <p/>
     * MUST BE USED FROM BACKGROUND THREAD ONLY!
     */
    private String id;
    private int colorIndex;
    private int order;
    private int timestamp;
    private boolean syncNotAllowed;
    private boolean xabberAutoLoginEnabled;

    /**
     * Whether account is enabled.
     */
    private volatile boolean enabled;

    /**
     * Whether roster contacts can be synchronized with system contact list.
     */
    private boolean syncable;

    private boolean streamError;

    /**
     * Whether password must be stored in database.
     */
    private boolean storePassword;

    private int priority;

    private StatusMode statusMode;

    private String statusText;

    /**
     * OTR key pair.
     */
    private KeyPair keyPair;

    private ArchiveMode archiveMode;

    /**
     * Delete all chat messages for account before explicit app exiting
     */
    private boolean clearHistoryOnExit;

    /**
     * Default behavior of Message Archive Management
     * https://xmpp.org/extensions/xep-0313.html
     */
    private MamPrefsIQ.DefaultBehavior mamDefaultBehaviour;

    /**
     * Options for loading history from MAM
     * https://xmpp.org/extensions/xep-0313.html
     */
    private LoadHistorySettings loadHistorySettings;

    /**
     * Flag indication that successful connection and authorization
     * happen at least ones with current connection settings
     */
    private volatile boolean successfulConnectionHappened;

    private long gracePeriodEndTime = 0L;

    /** Timestamp начала работы приложения,
     *  сообщения с timestamp раньше этого считаются прочитанными,
     *  позднее - непрочитанными.
     *  */
    private Date startHistoryTimestamp;

    private String retractVersion;

    public AccountItem(boolean custom, String host, int port, DomainBareJid serverName,
                       Localpart userName, Resourcepart resource, boolean storePassword,
                       String password, String token, DeviceVO deviceVO, int colorIndex, int order,
                       boolean syncNotAllowed, int timestamp, int priority, StatusMode statusMode,
                       String statusText, boolean enabled, boolean saslEnabled, TLSMode tlsMode,
                       boolean compression, ProxyType proxyType, String proxyHost, int proxyPort,
                       String proxyUser, String proxyPassword, boolean syncable, KeyPair keyPair,
                       ArchiveMode archiveMode, boolean xabberAutoLoginEnabled,
                       String retractVersion) {
        super(custom, host, port, serverName, userName, resource, storePassword, password, token,
                deviceVO, saslEnabled, tlsMode, compression, proxyType, proxyHost, proxyPort,
                proxyUser, proxyPassword);

        this.id = UUID.randomUUID().toString();
        this.colorIndex = colorIndex;
        this.order = order;
        this.timestamp = timestamp;
        this.syncNotAllowed = syncNotAllowed;
        this.xabberAutoLoginEnabled = xabberAutoLoginEnabled;

        this.enabled = enabled;
        this.priority = getValidPriority(priority);
        this.statusMode = statusMode;
        this.statusText = statusText;
        this.syncable = syncable;
        this.storePassword = storePassword;
        this.keyPair = keyPair;
        this.archiveMode = archiveMode;
        this.clearHistoryOnExit = false;
        this.mamDefaultBehaviour = MamPrefsIQ.DefaultBehavior.always;
        this.loadHistorySettings = LoadHistorySettings.all;
        this.successfulConnectionHappened = false;
        this.retractVersion = retractVersion;
    }

    /**
     * @return Valid priority value between -128 and 127.
     */
    static private int getValidPriority(int priority) {
        return Math.min(127, Math.max(-128, priority));
    }

    /**
     * @return ID in database.
     */
    public String getId() {
        return id;
    }

    /**
     * Set id in db.
     * <p/>
     * MUST BE MANAGED FROM BACKGROUND THREAD ONLY.
     */
    void setId(String id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isSyncNotAllowed() {
        return syncNotAllowed;
    }

    public void setSyncNotAllowed(boolean syncNotAllowed) {
        this.syncNotAllowed = syncNotAllowed;
    }

    public boolean isXabberAutoLoginEnabled() {
        return xabberAutoLoginEnabled;
    }

    public void setXabberAutoLoginEnabled(boolean xabberAutoLoginEnabled) {
        this.xabberAutoLoginEnabled = xabberAutoLoginEnabled;
    }

    /**
     * @return Assigned color.
     */
    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    /**
     * @return Whether roster contacts can be synchronized with system contact list.
     */
    public boolean isSyncable() {
        return syncable;
    }

    /**
     * Sets whether roster contacts can be synchronized with system contact list.
     */
    void setSyncable(boolean syncable) {
        this.syncable = syncable;
    }

    /**
     * @return Whether password must be stored in database.
     */
    public boolean isStorePassword() {
        return storePassword;
    }

    /**
     * Sets whether password must be stored in database.
     */
    void setStorePassword(boolean storePassword) {
        this.storePassword = storePassword;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public ArchiveMode getArchiveMode() {
        return archiveMode;
    }
    void setArchiveMode(ArchiveMode archiveMode) {
        this.archiveMode = archiveMode;
    }

    public int getPriority() { return priority; }
    void setPriority(int priority) { this.priority = getValidPriority(priority); }

    void setStatus(StatusMode statusMode, String statusText) {
        this.statusMode = statusMode;
        this.statusText = statusText;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return Saved status mode.
     */
    public StatusMode getRawStatusMode() {
        return statusMode;
    }

    /**
     * @return Status text.
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * @return Actual status mode, {@link StatusMode#connection} or
     * {@link StatusMode#unavailable} to display.
     */
    public StatusMode getDisplayStatusMode() {
        ConnectionState state = getState();
        if (state.isConnected()) {
            return statusMode;
        } else return StatusMode.unavailable;
    }

    /**
     * @return Status mode or {@link StatusMode#unavailable} if account was not authenticated to be used in status
     * editor.
     */
    public StatusMode getFactualStatusMode() {
//        if (getState().isConnected()) { //todo check this temp fix
//            return statusMode;
//        } else return StatusMode.unavailable;
        return statusMode;
    }

    /**
     * @return {@link Presence} to be send.
     */
    public Presence getPresence() throws NetworkException {
        StatusMode statusMode = getFactualStatusMode();
        switch (statusMode){
            case unsubscribed: throw new IllegalStateException();
            case unavailable: throw new NetworkException(R.string.NOT_CONNECTED);
            case invisible: return new Presence(Type.unavailable);
            default:
                int priority;
                if (SettingsManager.connectionAdjustPriority()) {
                    switch (statusMode){
                        case available: priority = SettingsManager.connectionPriorityAvailable(); break;
                        case away: priority = SettingsManager.connectionPriorityAway(); break;
                        case chat: priority = SettingsManager.connectionPriorityChat(); break;
                        case dnd: priority = SettingsManager.connectionPriorityDnd(); break;
                        case xa: priority = SettingsManager.connectionPriorityXa(); break;
                        default: throw new IllegalStateException();
                    }
                } else priority = this.priority;

                return new Presence(
                        Type.available,
                        statusText,
                        AccountItem.getValidPriority(priority),
                        statusMode.getMode()
                );
        }
    }

    /**
     * @return Whether account is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        if (!this.enabled && enabled) {
            connect();
        } else if (this.enabled && !enabled) disconnect();

        this.enabled = enabled;
    }

    public void setStreamError(boolean error) { streamError = error; }

    public boolean getStreamError() { return streamError; }

    /**
     * Update connection options
     */
    void updateConnectionSettings(boolean custom, String host, int port, String password, boolean saslEnabled,
                                  TLSMode tlsMode, boolean compression, ProxyType proxyType, String proxyHost,
                                  int proxyPort, String proxyUser, String proxyPassword) {
        getConnectionSettings().update(custom, host, port, password, saslEnabled, tlsMode, compression, proxyType,
                proxyHost, proxyPort, proxyUser, proxyPassword);
        AccountManager.INSTANCE.removePasswordRequest(getAccount());
    }

    void setPassword(String password) {
        getConnectionSettings().setPassword(password);
        AccountManager.INSTANCE.removePasswordRequest(getAccount());
    }

    void setDevice(DeviceVO device) { getConnectionSettings().setDevice(device); }

    DeviceVO getDevice() { return getConnectionSettings().getDevice(); }

    /**
     * Remove password and update notification if {@link #storePassword} is disabled.
     */
    void clearPassword() {
        if (storePassword) return;
        AccountManager.INSTANCE.removePasswordRequest(getAccount());
        getConnectionSettings().setPassword(UNDEFINED_PASSWORD);
    }

    @Override
    protected void onPasswordChanged(String password) {
        super.onPasswordChanged(password);
        AccountRepository.saveAccountToRealm(this);
    }

    @NotNull
    @Override
    public String toString() { return super.toString() + ":" + getAccount(); }

    public boolean isClearHistoryOnExit() { return clearHistoryOnExit; }

    void setClearHistoryOnExit(boolean clearHistoryOnExit) { this.clearHistoryOnExit = clearHistoryOnExit; }

    public MamPrefsIQ.DefaultBehavior getMamDefaultBehaviour() { return mamDefaultBehaviour; }

    void setMamDefaultBehaviour(@NonNull MamPrefsIQ.DefaultBehavior mamDefaultBehaviour) {
        this.mamDefaultBehaviour = mamDefaultBehaviour;
    }

    public LoadHistorySettings getLoadHistorySettings() { return loadHistorySettings; }

    public void setLoadHistorySettings(LoadHistorySettings loadHistorySettings) {
        this.loadHistorySettings = loadHistorySettings;
    }

    public boolean isSuccessfulConnectionHappened() { return successfulConnectionHappened; }

    void setSuccessfulConnectionHappened(boolean successfulConnectionHappened) {
        this.successfulConnectionHappened = successfulConnectionHappened;
    }

    @Override
    public int compareTo(@NonNull AccountItem accountItem) { return order - accountItem.order; }

    public void startGracePeriod() { gracePeriodEndTime = System.currentTimeMillis() + GRACE_PERIOD; }

    public void stopGracePeriod() { gracePeriodEndTime = 0L; }

    public boolean inGracePeriod() { return gracePeriodEndTime > System.currentTimeMillis(); }

    public Date getStartHistoryTimestamp() { return startHistoryTimestamp; }
    public void setStartHistoryTimestamp(Date startHistoryTimestamp) {
        this.startHistoryTimestamp = startHistoryTimestamp;
    }

    public String getRetractVersion() { return retractVersion; }
    public void setRetractVersion(String retractVersion) { this.retractVersion = retractVersion; }

}
