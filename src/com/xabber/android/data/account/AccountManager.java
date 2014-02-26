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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.util.StringUtils;

import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Build;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.OnWipeListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.notification.BaseAccountNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;

/**
 * This class manage all operations with accounts.
 * 
 * <p>
 * Each account has unique full jid (userName@serverName/resource). This jid is
 * persistent and independent from real jid assigned by server. Real full jid
 * (assigned by server) of account can be taken by
 * {@link AccountItem#getRealAccount()}.
 * 
 * @author alexander.ivanov
 * 
 */
public class AccountManager implements OnLoadListener, OnWipeListener {

	/**
	 * List of account presets.
	 */
	private final List<AccountType> accountTypes;

	/**
	 * List of saved statuses.
	 */
	private final Collection<SavedStatus> savedStatuses;

	/**
	 * Number of different account colors.
	 */
	private final int colors;

	/**
	 * List of accounts.
	 */
	private final Map<String, AccountItem> accountItems;

	/**
	 * List of enabled account.
	 */
	private final Collection<String> enabledAccounts;

	/**
	 * Whether away status mode is enabled.
	 */
	private boolean away;

	/**
	 * Whether extended away mode is enabled.
	 */
	private boolean xa;

	private final BaseAccountNotificationProvider<AccountAuthorizationError> authorizationErrorProvider;

	private final BaseAccountNotificationProvider<PasswordRequest> passwordRequestProvider;

	private final Application application;

	private final static AccountManager instance;

	static {
		instance = new AccountManager();
		Application.getInstance().addManager(instance);
	}

	public static AccountManager getInstance() {
		return instance;
	}

	private AccountManager() {
		this.application = Application.getInstance();
		accountItems = new HashMap<String, AccountItem>();
		enabledAccounts = new HashSet<String>();
		savedStatuses = new ArrayList<SavedStatus>();
		authorizationErrorProvider = new BaseAccountNotificationProvider<AccountAuthorizationError>(
				R.drawable.ic_stat_auth_failed);
		passwordRequestProvider = new BaseAccountNotificationProvider<PasswordRequest>(
				R.drawable.ic_stat_request);

		TypedArray accountAvatars = application.getResources()
				.obtainTypedArray(R.array.account_avatars);
		colors = accountAvatars.length();
		accountAvatars.recycle();

		TypedArray types = application.getResources().obtainTypedArray(
				R.array.account_types);
		accountTypes = new ArrayList<AccountType>();
		for (int index = 0; index < types.length(); index++) {
			int id = types.getResourceId(index, 0);
			TypedArray values = application.getResources().obtainTypedArray(id);
			AccountProtocol protocol = AccountProtocol.valueOf(values
					.getString(0));
			if (Build.VERSION.SDK_INT < 8 && protocol == AccountProtocol.wlm) {
				values.recycle();
				continue;
			}
			ArrayList<String> servers = new ArrayList<String>();
			servers.add(values.getString(9));
			for (int i = 10; i < values.length(); i++)
				servers.add(values.getString(i));
			accountTypes.add(new AccountType(id, protocol, values.getString(1),
					values.getString(2), values.getString(3), values
							.getDrawable(4), values.getBoolean(5, false),
					values.getString(6), values.getInt(7, 5222), values
							.getBoolean(8, false), servers));
			values.recycle();
		}
		types.recycle();
		away = false;
		xa = false;
	}

	@Override
	public void onLoad() {
		final Collection<SavedStatus> savedStatuses = new ArrayList<SavedStatus>();
		final Collection<AccountItem> accountItems = new ArrayList<AccountItem>();
		Cursor cursor = StatusTable.getInstance().list();
		try {
			if (cursor.moveToFirst()) {
				do {
					savedStatuses.add(new SavedStatus(StatusTable
							.getStatusMode(cursor), StatusTable
							.getStatusText(cursor)));
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}

		cursor = AccountTable.getInstance().list();
		try {
			if (cursor.moveToFirst()) {
				do {
					AccountItem accountItem = new AccountItem(
							AccountTable.getProtocol(cursor),
							AccountTable.isCustom(cursor),
							AccountTable.getHost(cursor),
							AccountTable.getPort(cursor),
							AccountTable.getServerName(cursor),
							AccountTable.getUserName(cursor),
							AccountTable.getResource(cursor),
							AccountTable.isStorePassword(cursor),
							AccountTable.getPassword(cursor),
							AccountTable.getColorIndex(cursor),
							AccountTable.getPriority(cursor),
							AccountTable.getStatusMode(cursor),
							AccountTable.getStatusText(cursor),
							AccountTable.isEnabled(cursor),
							AccountTable.isSaslEnabled(cursor),
							AccountTable.getTLSMode(cursor),
							AccountTable.isCompression(cursor),
							AccountTable.getProxyType(cursor),
							AccountTable.getProxyHost(cursor),
							AccountTable.getProxyPort(cursor),
							AccountTable.getProxyUser(cursor),
							AccountTable.getProxyPassword(cursor),
							AccountTable.isSyncable(cursor),
							AccountTable.getKeyPair(cursor),
							AccountTable.getLastSync(cursor),
							AccountTable.getArchiveMode(cursor));
					accountItem.setId(AccountTable.getId(cursor));
					accountItems.add(accountItem);
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}

		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				onLoaded(savedStatuses, accountItems);
			}
		});
	}

	private void onLoaded(Collection<SavedStatus> savedStatuses,
			Collection<AccountItem> accountItems) {
		this.savedStatuses.addAll(savedStatuses);
		for (AccountItem accountItem : accountItems)
			addAccount(accountItem);
		NotificationManager.getInstance().registerNotificationProvider(
				authorizationErrorProvider);
		NotificationManager.getInstance().registerNotificationProvider(
				passwordRequestProvider);
	}

	private void addAccount(AccountItem accountItem) {
		accountItems.put(accountItem.getAccount(), accountItem);
		if (accountItem.isEnabled())
			enabledAccounts.add(accountItem.getAccount());
		for (OnAccountAddedListener listener : application
				.getManagers(OnAccountAddedListener.class))
			listener.onAccountAdded(accountItem);
		if (accountItem.isEnabled()) {
			onAccountEnabled(accountItem);
			if (accountItem.getRawStatusMode().isOnline())
				onAccountOnline(accountItem);
		}
		onAccountChanged(accountItem.getAccount());
	}

	/**
	 * @return List of supported account types.
	 */
	public List<AccountType> getAccountTypes() {
		return accountTypes;
	}

	/**
	 * @return Next color index for the next account.
	 */
	int getNextColorIndex() {
		int[] count = new int[colors];
		for (AccountItem accountItem : accountItems.values())
			count[accountItem.getColorIndex() % colors] += 1;
		int result = 0;
		int value = count[0];
		for (int index = 0; index < count.length; index++)
			if (count[index] < value)
				result = index;
		return result;
	}

	/**
	 * @param account
	 *            full jid.
	 * @return Specified account or <code>null</code> if account doesn't exists.
	 */
	public AccountItem getAccount(String account) {
		return accountItems.get(account);
	}

	/**
	 * Save account item to database.
	 * 
	 * @param accountItem
	 */
	void requestToWriteAccount(final AccountItem accountItem) {
		final AccountProtocol protocol = accountItem.getConnectionSettings()
				.getProtocol();
		final boolean custom = accountItem.getConnectionSettings().isCustom();
		final String host = accountItem.getConnectionSettings().getHost();
		final int port = accountItem.getConnectionSettings().getPort();
		final String serverName = accountItem.getConnectionSettings()
				.getServerName();
		final String userName = accountItem.getConnectionSettings()
				.getUserName();
		final String resource = accountItem.getConnectionSettings()
				.getResource();
		final boolean storePassword = accountItem.isStorePassword();
		final String password = accountItem.getConnectionSettings()
				.getPassword();
		final int colorIndex = accountItem.getColorIndex();
		final int priority = accountItem.getPriority();
		final StatusMode statusMode = accountItem.getRawStatusMode();
		final String statusText = accountItem.getStatusText();
		final boolean enabled = accountItem.isEnabled();
		final boolean saslEnabled = accountItem.getConnectionSettings()
				.isSaslEnabled();
		final TLSMode tlsMode = accountItem.getConnectionSettings()
				.getTlsMode();
		final boolean compression = accountItem.getConnectionSettings()
				.useCompression();
		final ProxyType proxyType = accountItem.getConnectionSettings()
				.getProxyType();
		final String proxyHost = accountItem.getConnectionSettings()
				.getProxyHost();
		final int proxyPort = accountItem.getConnectionSettings()
				.getProxyPort();
		final String proxyUser = accountItem.getConnectionSettings()
				.getProxyUser();
		final String proxyPassword = accountItem.getConnectionSettings()
				.getProxyPassword();
		final boolean syncable = accountItem.isSyncable();
		final KeyPair keyPair = accountItem.getKeyPair();
		final Date lastSync = accountItem.getLastSync();
		final ArchiveMode archiveMode = accountItem.getArchiveMode();
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				accountItem.setId(AccountTable.getInstance().write(
						accountItem.getId(), protocol, custom, host, port,
						serverName, userName, resource, storePassword,
						password, colorIndex, priority, statusMode, statusText,
						enabled, saslEnabled, tlsMode, compression, proxyType,
						proxyHost, proxyPort, proxyUser, proxyPassword,
						syncable, keyPair, lastSync, archiveMode));
			}
		});
	}

	/**
	 * Creates new account and starts connection.
	 */
	private AccountItem addAccount(AccountProtocol protocol, boolean custom,
			String host, int port, String serverName, String userName,
			boolean storePassword, String password, String resource, int color,
			int priority, StatusMode statusMode, String statusText,
			boolean enabled, boolean saslEnabled, TLSMode tlsMode,
			boolean compression, ProxyType proxyType, String proxyHost,
			int proxyPort, String proxyUser, String proxyPassword,
			boolean syncable, KeyPair keyPair, Date lastSync,
			ArchiveMode archiveMode) {
		AccountItem accountItem = new AccountItem(protocol, custom, host, port,
				serverName, userName, resource, storePassword, password, color,
				priority, statusMode, statusText, enabled, saslEnabled,
				tlsMode, compression, proxyType, proxyHost, proxyPort,
				proxyUser, proxyPassword, syncable, keyPair, lastSync,
				archiveMode);
		requestToWriteAccount(accountItem);
		addAccount(accountItem);
		accountItem.updateConnection(true);
		return accountItem;
	}

	/**
	 * Creates new account.
	 * 
	 * @param user
	 *            full or bare jid.
	 * @param password
	 * @param accountType
	 *            xmpp account type can be replaced depend on server part.
	 * @param syncable
	 * @param storePassword
	 * @param useOrbot
	 * @return assigned account name.
	 * @throws NetworkException
	 *             if user or server part are invalid.
	 */
	public String addAccount(String user, String password,
			AccountType accountType, boolean syncable, boolean storePassword,
			boolean useOrbot) throws NetworkException {
		if (accountType.getProtocol().isOAuth()) {
			int index = 1;
			while (true) {
				user = String.valueOf(index);
				boolean found = false;
				for (AccountItem accountItem : accountItems.values())
					if (accountItem.getConnectionSettings().getServerName()
							.equals(accountType.getFirstServer())
							&& accountItem.getConnectionSettings()
									.getUserName().equals(user)) {
						found = true;
						break;
					}
				if (!found)
					break;
				index++;
			}
		}

		if (user == null)
			throw new NetworkException(R.string.EMPTY_USER_NAME);

		if (user.indexOf("@") == -1) {
			if ("".equals(accountType.getFirstServer()))
				throw new NetworkException(R.string.EMPTY_SERVER_NAME);
			else
				user += "@" + accountType.getFirstServer();
		}

		String serverName = StringUtils.parseServer(user);
		String userName = StringUtils.parseName(user);
		String resource = StringUtils.parseResource(user);
		String host = accountType.getHost();
		int port = accountType.getPort();
		boolean tlsRequired = accountType.isTLSRequired();
		if (useOrbot)
			tlsRequired = true;

		if ("".equals(serverName)) {
			throw new NetworkException(R.string.EMPTY_SERVER_NAME);
		} else if (!accountType.isAllowServer()
				&& !serverName.equals(accountType.getFirstServer()))
			throw new NetworkException(R.string.INCORRECT_USER_NAME);

		if ("".equals(userName))
			throw new NetworkException(R.string.EMPTY_USER_NAME);
		if ("".equals(resource))
			resource = "android" + StringUtils.randomString(8);

		if (accountType.getId() == R.array.account_type_xmpp) {
			host = serverName;
			for (AccountType check : accountTypes)
				if (check.getServers().contains(serverName)) {
					accountType = check;
					host = check.getHost();
					port = check.getPort();
					tlsRequired = check.isTLSRequired();
					break;
				}
		}

		AccountItem accountItem;
		for (;;) {
			if (getAccount(userName + '@' + serverName + '/' + resource) == null)
				break;
			resource = "android" + StringUtils.randomString(8);
		}

		accountItem = addAccount(accountType.getProtocol(), true, host, port,
				serverName, userName, storePassword, password, resource,
				getNextColorIndex(), 0, StatusMode.available,
				SettingsManager.statusText(), true, true,
				tlsRequired ? TLSMode.required : TLSMode.enabled, false,
				useOrbot ? ProxyType.orbot : ProxyType.none, "localhost", 8080,
				"", "", syncable, null, null, ArchiveMode.available);
		onAccountChanged(accountItem.getAccount());
		if (accountItems.size() > 1
				&& SettingsManager.contactsEnableShowAccounts())
			SettingsManager.enableContactsShowAccount();
		return accountItem.getAccount();
	}

	/**
	 * Remove user`s account. Don't call any callbacks.
	 * 
	 * @param account
	 */
	private void removeAccountWithoutCallback(final String account) {
		final AccountItem accountItem = getAccount(account);
		boolean wasEnabled = accountItem.isEnabled();
		accountItem.setEnabled(false);
		accountItem.updateConnection(true);
		if (wasEnabled) {
			if (accountItem.getRawStatusMode().isOnline())
				onAccountOffline(accountItem);
			onAccountDisabled(accountItem);
		}
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				AccountTable.getInstance().remove(account, accountItem.getId());
			}
		});
		accountItems.remove(account);
		enabledAccounts.remove(account);
		for (OnAccountRemovedListener listener : application
				.getManagers(OnAccountRemovedListener.class))
			listener.onAccountRemoved(accountItem);
		removeAuthorizationError(account);
	}

	/**
	 * Remove user`s account.
	 * 
	 * @param account
	 */
	public void removeAccount(String account) {
		removeAccountWithoutCallback(account);
		onAccountChanged(account);
	}

	/**
	 * Update user`s account.
	 * 
	 * It will reconnect to the server if changes was made.
	 * 
	 * It will remove old account and create new one if full jid was changed.
	 * 
	 * @param account
	 *            full source jid
	 * @param host
	 * @param port
	 * @param serverName
	 * @param userName
	 * @param storePassword
	 * @param password
	 * @param resource
	 * @param priority
	 * @param enabled
	 * @param saslEnabled
	 * @param tlsMode
	 * @param compression
	 * @param syncable
	 * @param archiveMode
	 */
	public void updateAccount(String account, boolean custom, String host,
			int port, String serverName, String userName,
			boolean storePassword, String password, String resource,
			int priority, boolean enabled, boolean saslEnabled,
			TLSMode tlsMode, boolean compression, ProxyType proxyType,
			String proxyHost, int proxyPort, String proxyUser,
			String proxyPassword, boolean syncable, ArchiveMode archiveMode) {
		AccountItem result;
		AccountItem accountItem = getAccount(account);
		if (accountItem.getConnectionSettings().getServerName()
				.equals(serverName)
				&& accountItem.getConnectionSettings().getUserName()
						.equals(userName)
				&& accountItem.getConnectionSettings().getResource()
						.equals(resource)) {
			result = accountItem;
			boolean reconnect = false;
			if (accountItem.getConnectionSettings().isCustom() != custom
					|| !accountItem.getConnectionSettings().getHost()
							.equals(host)
					|| accountItem.getConnectionSettings().getPort() != port
					|| !accountItem.getConnectionSettings().getPassword()
							.equals(password)
					|| accountItem.getConnectionSettings().getTlsMode() != tlsMode
					|| accountItem.getConnectionSettings().isSaslEnabled() != saslEnabled
					|| accountItem.getConnectionSettings().useCompression() != compression
					|| accountItem.getConnectionSettings().getProxyType() != proxyType
					|| !accountItem.getConnectionSettings().getProxyHost()
							.equals(proxyHost)
					|| accountItem.getConnectionSettings().getProxyPort() != proxyPort
					|| !accountItem.getConnectionSettings().getProxyUser()
							.equals(proxyUser)
					|| !accountItem.getConnectionSettings().getProxyPassword()
							.equals(proxyPassword)) {
				result.updateConnectionSettings(custom, host, port, password,
						saslEnabled, tlsMode, compression, proxyType,
						proxyHost, proxyPort, proxyUser, proxyPassword);
				reconnect = true;
			}
			if (result.isSyncable() != syncable) {
				result.setSyncable(syncable);
				for (OnAccountSyncableChangedListener listener : application
						.getManagers(OnAccountSyncableChangedListener.class))
					listener.onAccountSyncableChanged(result);
			}
			result.setStorePassword(storePassword);
			boolean changed = result.isEnabled() != enabled;
			result.setEnabled(enabled);
			if (result.getPriority() != priority) {
				result.setPriority(priority);
				try {
					PresenceManager.getInstance().resendPresence(account);
				} catch (NetworkException e) {
				}
			}
			if (result.getArchiveMode() != archiveMode) {
				reconnect = (result.getArchiveMode() == ArchiveMode.server) != (archiveMode == ArchiveMode.server);
				result.setArchiveMode(archiveMode);
				for (OnAccountArchiveModeChangedListener listener : application
						.getManagers(OnAccountArchiveModeChangedListener.class))
					listener.onAccountArchiveModeChanged(result);
			}
			if (changed && enabled) {
				enabledAccounts.add(account);
				onAccountEnabled(result);
				if (result.getRawStatusMode().isOnline())
					onAccountOnline(result);
			}
			if (changed || reconnect) {
				result.updateConnection(true);
				result.forceReconnect();
			}
			if (changed && !enabled) {
				enabledAccounts.remove(account);
				if (result.getRawStatusMode().isOnline())
					onAccountOffline(result);
				onAccountDisabled(result);
			}
			requestToWriteAccount(result);
		} else {
			int colorIndex = accountItem.getColorIndex();
			StatusMode statusMode = accountItem.getRawStatusMode();
			String statusText = accountItem.getStatusText();
			AccountProtocol protocol = accountItem.getConnectionSettings()
					.getProtocol();
			KeyPair keyPair = accountItem.getKeyPair();
			Date lastSync = accountItem.getLastSync();
			removeAccountWithoutCallback(account);
			result = addAccount(protocol, custom, host, port, serverName,
					userName, storePassword, password, resource, colorIndex,
					priority, statusMode, statusText, enabled, saslEnabled,
					tlsMode, compression, proxyType, proxyHost, proxyPort,
					proxyUser, proxyPassword, syncable, keyPair, lastSync,
					archiveMode);
		}
		onAccountChanged(result.getAccount());
	}

	public void setKeyPair(String account, KeyPair keyPair) {
		AccountItem accountItem = getAccount(account);
		accountItem.setKeyPair(keyPair);
		requestToWriteAccount(accountItem);
	}

	public void setLastSync(String account, Date lastSync) {
		AccountItem accountItem = getAccount(account);
		accountItem.setLastSync(lastSync);
		requestToWriteAccount(accountItem);
	}

	public void setSyncable(String account, boolean syncable) {
		AccountItem accountItem = getAccount(account);
		updateAccount(account, accountItem.getConnectionSettings().isCustom(),
				accountItem.getConnectionSettings().getHost(), accountItem
						.getConnectionSettings().getPort(), accountItem
						.getConnectionSettings().getServerName(), accountItem
						.getConnectionSettings().getUserName(),
				accountItem.isStorePassword(), accountItem
						.getConnectionSettings().getPassword(), accountItem
						.getConnectionSettings().getResource(),
				accountItem.getPriority(), accountItem.isEnabled(), accountItem
						.getConnectionSettings().isSaslEnabled(), accountItem
						.getConnectionSettings().getTlsMode(), accountItem
						.getConnectionSettings().useCompression(), accountItem
						.getConnectionSettings().getProxyType(), accountItem
						.getConnectionSettings().getProxyHost(), accountItem
						.getConnectionSettings().getProxyPort(), accountItem
						.getConnectionSettings().getProxyUser(), accountItem
						.getConnectionSettings().getProxyPassword(), syncable,
				accountItem.getArchiveMode());
	}

	public void setPassword(String account, boolean storePassword,
			String password) {
		AccountItem accountItem = getAccount(account);
		updateAccount(account, accountItem.getConnectionSettings().isCustom(),
				accountItem.getConnectionSettings().getHost(), accountItem
						.getConnectionSettings().getPort(), accountItem
						.getConnectionSettings().getServerName(), accountItem
						.getConnectionSettings().getUserName(), storePassword,
				password, accountItem.getConnectionSettings().getResource(),
				accountItem.getPriority(), accountItem.isEnabled(), accountItem
						.getConnectionSettings().isSaslEnabled(), accountItem
						.getConnectionSettings().getTlsMode(), accountItem
						.getConnectionSettings().useCompression(), accountItem
						.getConnectionSettings().getProxyType(), accountItem
						.getConnectionSettings().getProxyHost(), accountItem
						.getConnectionSettings().getProxyPort(), accountItem
						.getConnectionSettings().getProxyUser(), accountItem
						.getConnectionSettings().getProxyPassword(),
				accountItem.isSyncable(), accountItem.getArchiveMode());
	}

	public void setArchiveMode(String account, ArchiveMode archiveMode) {
		AccountItem accountItem = AccountManager.getInstance().getAccount(
				account);
		AccountManager.getInstance().updateAccount(account,
				accountItem.getConnectionSettings().isCustom(),
				accountItem.getConnectionSettings().getHost(),
				accountItem.getConnectionSettings().getPort(),
				accountItem.getConnectionSettings().getServerName(),
				accountItem.getConnectionSettings().getUserName(),
				accountItem.isStorePassword(),
				accountItem.getConnectionSettings().getPassword(),
				accountItem.getConnectionSettings().getResource(),
				accountItem.getPriority(), accountItem.isEnabled(),
				accountItem.getConnectionSettings().isSaslEnabled(),
				accountItem.getConnectionSettings().getTlsMode(),
				accountItem.getConnectionSettings().useCompression(),
				accountItem.getConnectionSettings().getProxyType(),
				accountItem.getConnectionSettings().getProxyHost(),
				accountItem.getConnectionSettings().getProxyPort(),
				accountItem.getConnectionSettings().getProxyUser(),
				accountItem.getConnectionSettings().getProxyPassword(),
				accountItem.isSyncable(), archiveMode);
	}

	public ArchiveMode getArchiveMode(String account) {
		AccountItem accountItem = getAccount(account);
		if (accountItem == null)
			return ArchiveMode.available;
		return accountItem.getArchiveMode();
	}

	/**
	 * @return List of enabled accounts.
	 */
	public Collection<String> getAccounts() {
		return Collections.unmodifiableCollection(enabledAccounts);
	}

	/**
	 * @return List of all accounts including disabled.
	 */
	public Collection<String> getAllAccounts() {
		return Collections.unmodifiableCollection(accountItems.keySet());
	}

	public CommonState getCommonState() {
		boolean disabled = false;
		boolean offline = false;
		boolean waiting = false;
		boolean connecting = false;
		boolean roster = false;
		boolean online = false;

		for (AccountItem accountItem : accountItems.values()) {
			ConnectionState state = accountItem.getState();
			if (state == ConnectionState.connected)
				online = true;
			if (RosterManager.getInstance().isRosterReceived(
					accountItem.getAccount()))
				roster = true;
			if (state == ConnectionState.connecting
					|| state == ConnectionState.authentication)
				connecting = true;
			if (state == ConnectionState.waiting)
				waiting = true;
			if (accountItem.isEnabled())
				offline = true;
			disabled = true;
		}
		if (online)
			return CommonState.online;
		else if (roster)
			return CommonState.roster;
		else if (connecting)
			return CommonState.connecting;
		if (waiting)
			return CommonState.waiting;
		else if (offline)
			return CommonState.offline;
		else if (disabled)
			return CommonState.disabled;
		else
			return CommonState.empty;
	}

	/**
	 * @param account
	 * @return Color drawable level or default colors if account was not found.
	 */
	public int getColorLevel(String account) {
		AccountItem accountItem = getAccount(account);
		int colorIndex;
		if (accountItem == null)
			return 0;
		else
			colorIndex = accountItem.getColorIndex() % colors;
		if (colorIndex < 0)
			colorIndex += colors;
		return colorIndex;
	}

	/**
	 * @return Number of different account colors.
	 */
	public int getColorCount() {
		return colors;
	}

	private boolean hasSameBareAddress(String account) {
		String bareAddress = Jid.getBareAddress(account);
		for (AccountItem check : accountItems.values())
			if (!check.getAccount().equals(account)
					&& Jid.getBareAddress(check.getAccount()).equals(
							bareAddress))
				return true;
		return false;
	}

	private boolean hasSameProtocol(String account) {
		AccountProtocol protocol = getAccount(account).getConnectionSettings()
				.getProtocol();
		for (AccountItem check : accountItems.values())
			if (!check.getAccount().equals(account)
					&& check.getConnectionSettings().getProtocol() == protocol)
				return true;
		return false;
	}

	/**
	 * @param account
	 * @return Verbose account name.
	 */
	public String getVerboseName(String account) {
		AccountItem accountItem = getAccount(account);
		if (accountItem == null)
			return account;
		if (accountItem.getConnectionSettings().getProtocol().isOAuth()) {
			String jid = OAuthManager.getInstance().getAssignedJid(account);
			AccountProtocol accountProtocol = accountItem
					.getConnectionSettings().getProtocol();
			String name;
			if (jid == null) {
				if (hasSameProtocol(account))
					name = accountItem.getConnectionSettings().getUserName();
				else
					return application.getString(accountProtocol
							.getNameResource());

			} else {
				name = Jid.getBareAddress(jid);
				if (!hasSameBareAddress(jid))
					return name;
			}
			return application.getString(accountProtocol.getShortResource())
					+ " - " + name;
		} else {
			if (hasSameBareAddress(account))
				return account;
			else
				return Jid.getBareAddress(account);
		}
	}

	/**
	 * @param account
	 * @return Account vCard based nick name or verbose name if nick is not
	 *         specified.
	 */
	public String getNickName(String account) {
		String jid = OAuthManager.getInstance().getAssignedJid(account);
		String result = VCardManager.getInstance().getName(
				Jid.getBareAddress(jid));
		if ("".equals(result))
			return getVerboseName(account);
		else
			return result;
	}

	/**
	 * Sets status for account.
	 * 
	 * @param account
	 * @param statusMode
	 * @param statusText
	 */
	public void setStatus(String account, StatusMode statusMode,
			String statusText) {
		addSavedStatus(statusMode, statusText);
		AccountItem accountItem = getAccount(account);
		setStatus(accountItem, statusMode, statusText);
		try {
			PresenceManager.getInstance().resendPresence(account);
		} catch (NetworkException e) {
		}
		boolean found = false;
		for (AccountItem check : accountItems.values())
			if (check.isEnabled()
					&& SettingsManager.statusMode() == check.getRawStatusMode()) {
				found = true;
				break;
			}
		if (!found)
			SettingsManager.setStatusMode(statusMode);
		found = false;
		for (AccountItem check : accountItems.values())
			if (check.isEnabled()
					&& SettingsManager.statusText().equals(
							check.getStatusText())) {
				found = true;
				break;
			}
		if (!found)
			SettingsManager.setStatusText(statusText);
		onAccountChanged(account);
	}

	boolean isAway() {
		return away;
	}

	boolean isXa() {
		return xa;
	}

	/**
	 * Set status mode to away.
	 * 
	 * If we are already away or xa, do nothing.
	 */
	public void goAway() {
		if (away || xa)
			return;
		away = true;
		resendPresence();
	}

	/**
	 * Set status mode to xa.
	 * 
	 * If we are already xa, do nothing.
	 */
	public void goXa() {
		if (xa)
			return;
		xa = true;
		resendPresence();
	}

	/**
	 * Restore status mode to the state that was before we go away.
	 * 
	 * If we are already waked up, do nothing.
	 */
	public void wakeUp() {
		if (!away && !xa)
			return;
		away = false;
		xa = false;
		resendPresence();
	}

	/**
	 * Sends new presence information for all accounts.
	 */
	public void resendPresence() {
		for (AccountItem accountItem : accountItems.values())
			try {
				PresenceManager.getInstance().resendPresence(
						accountItem.getAccount());
			} catch (NetworkException e) {
			}
	}

	/**
	 * Sets status for account.
	 * 
	 * @param account
	 * @param statusMode
	 * @param statusText
	 */
	private void setStatus(AccountItem accountItem, StatusMode statusMode,
			String statusText) {
		boolean changed = accountItem.isEnabled()
				&& accountItem.getRawStatusMode().isOnline() != statusMode
						.isOnline();
		accountItem.setStatus(statusMode, statusText);
		if (changed && statusMode.isOnline())
			onAccountOnline(accountItem);
		accountItem.updateConnection(true);
		if (changed && !statusMode.isOnline())
			onAccountOffline(accountItem);
		requestToWriteAccount(accountItem);
	}

	/**
	 * Sets status for all accounts.
	 * 
	 * @param statusMode
	 * @param statusText
	 *            can be <code>null</code> if value was not changed.
	 */
	public void setStatus(StatusMode statusMode, String statusText) {
		SettingsManager.setStatusMode(statusMode);
		if (statusText != null) {
			addSavedStatus(statusMode, statusText);
			SettingsManager.setStatusText(statusText);
		}
		for (AccountItem accountItem : accountItems.values()) {
			setStatus(accountItem, statusMode,
					statusText == null ? accountItem.getStatusText()
							: statusText);
		}
		resendPresence();
		onAccountsChanged(new ArrayList<String>(AccountManager.getInstance()
				.getAllAccounts()));
	}

	/**
	 * Save status in presets.
	 * 
	 * @param statusMode
	 * @param statusText
	 */
	private void addSavedStatus(final StatusMode statusMode,
			final String statusText) {
		SavedStatus savedStatus = new SavedStatus(statusMode, statusText);
		if (savedStatuses.contains(savedStatus))
			return;
		savedStatuses.add(savedStatus);
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				StatusTable.getInstance().write(statusMode, statusText);
			}
		});
	}

	/**
	 * Remove status from presets.
	 * 
	 * @param statusMode
	 * @param statusText
	 */
	public void removeSavedStatus(final SavedStatus savedStatus) {
		if (!savedStatuses.remove(savedStatus))
			return;
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				StatusTable.getInstance().remove(savedStatus.getStatusMode(),
						savedStatus.getStatusText());
			}
		});
	}

	/**
	 * Clear list of status presets.
	 */
	public void clearSavedStatuses() {
		savedStatuses.clear();
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				StatusTable.getInstance().clear();
			}
		});
	}

	/**
	 * @return List of preset statuses.
	 */
	public Collection<SavedStatus> getSavedStatuses() {
		return Collections.unmodifiableCollection(savedStatuses);
	}

	/**
	 * @return Selected account to show contacts. <code>null</code> if
	 *         <ul>
	 *         <li>there is no selected account,</li>
	 *         <li>selected account does not exists or disabled,</li>
	 *         <li>Group by account is enabled.</li>
	 *         </ul>
	 */
	public String getSelectedAccount() {
		if (SettingsManager.contactsShowAccounts())
			return null;
		String selected = SettingsManager.contactsSelectedAccount();
		if (enabledAccounts.contains(selected))
			return selected;
		return null;
	}

	public void removeAuthorizationError(String account) {
		authorizationErrorProvider.remove(account);
	}

	public void addAuthenticationError(String account) {
		authorizationErrorProvider.add(new AccountAuthorizationError(account),
				true);
	}

	public void removePasswordRequest(String account) {
		passwordRequestProvider.remove(account);
	}

	public void addPasswordRequest(String account) {
		passwordRequestProvider.add(new PasswordRequest(account), true);
	}

	public void onAccountChanged(String account) {
		Collection<String> accounts = new ArrayList<String>(1);
		accounts.add(account);
		onAccountsChanged(accounts);
	}

	public void onAccountsChanged(final Collection<String> accounts) {
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (OnAccountChangedListener accountListener : Application
						.getInstance().getUIListeners(
								OnAccountChangedListener.class))
					accountListener.onAccountsChanged(accounts);
			}
		});
	}

	public void onAccountEnabled(AccountItem accountItem) {
		for (OnAccountEnabledListener listener : application
				.getManagers(OnAccountEnabledListener.class))
			listener.onAccountEnabled(accountItem);
	}

	public void onAccountOnline(AccountItem accountItem) {
		for (OnAccountOnlineListener listener : application
				.getManagers(OnAccountOnlineListener.class))
			listener.onAccountOnline(accountItem);
	}

	public void onAccountOffline(AccountItem accountItem) {
		accountItem.clearPassword();
		for (OnAccountOfflineListener listener : application
				.getManagers(OnAccountOfflineListener.class))
			listener.onAccountOffline(accountItem);
	}

	public void onAccountDisabled(AccountItem accountItem) {
		for (OnAccountDisabledListener listener : application
				.getManagers(OnAccountDisabledListener.class))
			listener.onAccountDisabled(accountItem);
	}

	@Override
	public void onWipe() {
		AccountTable.getInstance().wipe();
	}

}
