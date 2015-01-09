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

import java.security.KeyPair;
import java.util.Date;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;

import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.androiddev.R;

/**
 * Represent account settings and status.
 * 
 * @author alexander.ivanov
 * 
 */
public class AccountItem extends ConnectionItem {

	public static final String UNDEFINED_PASSWORD = "com.xabber.android.data.core.AccountItem.UNDEFINED_PASSWORD";

	/**
	 * Id in database.
	 * 
	 * MUST BE USED FROM BACKGROUND THREAD ONLY!
	 */
	private Long id;

	/**
	 * Full jid calculated according to {@link #userName}, {@link #serverName},
	 * {@link #resource}.
	 */
	private final String account;

	private final int colorIndex;

	/**
	 * Whether account is enabled.
	 */
	private boolean enabled;

	/**
	 * Whether roster contacts can be synchronized with system contact list.
	 */
	private boolean syncable;

	/**
	 * Whether password must be stored in database.
	 */
	private boolean storePassword;

	/**
	 * Whether authorization was failed.
	 */
	private boolean authFailed;

	/**
	 * Whether certificate is invalid.
	 */
	private boolean invalidCertificate;

	/**
	 * Whether password was requested.
	 */
	private boolean passwordRequested;

	private int priority;

	private StatusMode statusMode;

	private String statusText;

	/**
	 * OTR key pair.
	 */
	private KeyPair keyPair;

	/**
	 * Last history synchronization.
	 */
	private Date lastSync;

	private ArchiveMode archiveMode;

	public AccountItem(AccountProtocol protocol, boolean custom, String host,
			int port, String serverName, String userName, String resource,
			boolean storePassword, String password, int colorIndex,
			int priority, StatusMode statusMode, String statusText,
			boolean enabled, boolean saslEnabled, TLSMode tlsMode,
			boolean compression, ProxyType proxyType, String proxyHost,
			int proxyPort, String proxyUser, String proxyPassword,
			boolean syncable, KeyPair keyPair, Date lastSync,
			ArchiveMode archiveMode) {
		super(protocol, custom, host, port, serverName, userName, resource,
				storePassword, password, saslEnabled, tlsMode, compression,
				proxyType, proxyHost, proxyPort, proxyUser, proxyPassword);
		this.id = null;
		this.account = userName + "@" + serverName + "/" + resource;
		this.colorIndex = colorIndex;

		this.enabled = enabled;
		this.priority = getValidPriority(priority);
		this.statusMode = statusMode;
		this.statusText = statusText;
		this.syncable = syncable;
		this.storePassword = storePassword;
		this.keyPair = keyPair;
		this.lastSync = lastSync;
		this.archiveMode = archiveMode;
		authFailed = false;
		invalidCertificate = false;
		passwordRequested = false;
	}

	/**
	 * @return ID in database.
	 */
	Long getId() {
		return id;
	}

	/**
	 * Set id in db.
	 * 
	 * MUST BE MANAGED FROM BACKGROUND THREAD ONLY.
	 * 
	 * @param id
	 */
	void setId(long id) {
		this.id = id;
	}

	/**
	 * @return Account's JID.
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * @return Assigned color.
	 */
	public int getColorIndex() {
		return colorIndex;
	}

	/**
	 * @return Whether roster contacts can be synchronized with system contact
	 *         list.
	 */
	public boolean isSyncable() {
		return syncable;
	}

	/**
	 * Sets whether roster contacts can be synchronized with system contact
	 * list.
	 * 
	 * @param syncable
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
	 * 
	 * @param storePassword
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

	public Date getLastSync() {
		return lastSync;
	}

	void setLastSync(Date lastSync) {
		this.lastSync = lastSync;
	}

	public ArchiveMode getArchiveMode() {
		return archiveMode;
	}

	void setArchiveMode(ArchiveMode archiveMode) {
		this.archiveMode = archiveMode;
	}

	public int getPriority() {
		return priority;
	}

	void setPriority(int priority) {
		this.priority = getValidPriority(priority);
	}

	void setStatus(StatusMode statusMode, String statusText) {
		this.statusMode = statusMode;
		this.statusText = statusText;
	}

	/**
	 * @return Saved status mode.
	 */
	StatusMode getRawStatusMode() {
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
	 *         {@link StatusMode#unavailable} to display.
	 */
	public StatusMode getDisplayStatusMode() {
		ConnectionState state = getState();
		if (state.isConnected())
			return statusMode;
		else if (state.isConnectable())
			return StatusMode.connection;
		else
			return StatusMode.unavailable;
	}

	/**
	 * @return Status mode or {@link StatusMode#unavailable} if account was not
	 *         authenticated to be used in status editor.
	 */
	public StatusMode getFactualStatusMode() {
		if (getState().isConnected())
			return statusMode;
		else
			return StatusMode.unavailable;
	}

	/**
	 * @return {@link Presence} to be send.
	 * @throws NetworkException
	 */
	public Presence getPresence() throws NetworkException {
		StatusMode statusMode = getFactualStatusMode();
		if (statusMode == StatusMode.unsubscribed)
			throw new IllegalStateException();
		if (statusMode == StatusMode.unavailable)
			throw new NetworkException(R.string.NOT_CONNECTED);
		if (statusMode == StatusMode.invisible)
			return new Presence(Type.unavailable);
		else {
			int priority;
			if (statusMode != StatusMode.dnd) {
				if (AccountManager.getInstance().isXa())
					statusMode = StatusMode.xa;
				else if (AccountManager.getInstance().isAway())
					statusMode = StatusMode.away;
			}
			if (SettingsManager.connectionAdjustPriority()) {
				if (statusMode == StatusMode.available)
					priority = SettingsManager.connectionPriorityAvailable();
				else if (statusMode == StatusMode.away)
					priority = SettingsManager.connectionPriorityAway();
				else if (statusMode == StatusMode.chat)
					priority = SettingsManager.connectionPriorityChat();
				else if (statusMode == StatusMode.dnd)
					priority = SettingsManager.connectionPriorityDnd();
				else if (statusMode == StatusMode.xa)
					priority = SettingsManager.connectionPriorityXa();
				else
					throw new IllegalStateException();
			} else
				priority = this.priority;
			return new Presence(Type.available, statusText, priority,
					statusMode.getMode());
		}
	}

	/**
	 * @return Whether account is enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Update connection options
	 * 
	 * @param custom
	 * @param host
	 * @param port
	 * @param password
	 * @param saslEnabled
	 * @param tlsMode
	 * @param compression
	 */
	void updateConnectionSettings(boolean custom, String host, int port,
			String password, boolean saslEnabled, TLSMode tlsMode,
			boolean compression, ProxyType proxyType, String proxyHost,
			int proxyPort, String proxyUser, String proxyPassword) {
		getConnectionSettings().update(custom, host, port, password,
				saslEnabled, tlsMode, compression, proxyType, proxyHost,
				proxyPort, proxyUser, proxyPassword);
		passwordRequested = false;
		AccountManager.getInstance().removePasswordRequest(account);
	}

	@Override
	protected boolean isConnectionAvailable(boolean userRequest) {
		// Check password before go online.
		if (statusMode.isOnline()
				&& enabled
				&& !passwordRequested
				&& UNDEFINED_PASSWORD.equals(getConnectionSettings()
						.getPassword())) {
			passwordRequested = true;
			AccountManager.getInstance().addPasswordRequest(account);
		}
		if (userRequest) {
			authFailed = false;
			invalidCertificate = false;
		}
		return statusMode.isOnline() && enabled && !authFailed
				&& !invalidCertificate && !passwordRequested;
	}

	/**
	 * Remove password and update notification if {@link #storePassword} is
	 * disabled.
	 */
	void clearPassword() {
		if (storePassword)
			return;
		passwordRequested = false;
		AccountManager.getInstance().removePasswordRequest(account);
		getConnectionSettings().setPassword(UNDEFINED_PASSWORD);
	}

	@Override
	protected void onPasswordChanged(String password) {
		super.onPasswordChanged(password);
		AccountManager.getInstance().requestToWriteAccount(this);
	}

	@Override
	protected void onSRVResolved(ConnectionThread connectionThread) {
		super.onSRVResolved(connectionThread);
		AccountManager.getInstance().onAccountChanged(account);
	}

	@Override
	protected void onInvalidCertificate() {
		super.onInvalidCertificate();
		invalidCertificate = true;
		updateConnection(false);
	}

	@Override
	protected void onConnected(ConnectionThread connectionThread) {
		super.onConnected(connectionThread);
		AccountManager.getInstance().onAccountChanged(account);
	}

	@Override
	protected void onAuthFailed() {
		super.onAuthFailed();
		// Login failed. We don`t want to reconnect.
		authFailed = true;
		updateConnection(false);
		AccountManager.getInstance().addAuthenticationError(account);
	}

	@Override
	protected void onAuthorized(ConnectionThread connectionThread) {
		super.onAuthorized(connectionThread);
		AccountManager.getInstance().onAccountChanged(account);
	}

	@Override
	protected void onClose(ConnectionThread connectionThread) {
		super.onClose(connectionThread);
		AccountManager.getInstance().onAccountChanged(account);
	}

	@Override
	public String toString() {
		return super.toString() + ":" + getAccount();
	}

	/**
	 * @param priority
	 * @return Valid priority value between -128 and 128.
	 */
	static private int getValidPriority(int priority) {
		return Math.min(128, Math.max(-128, priority));
	}

}
