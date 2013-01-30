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

import com.xabber.android.data.account.AccountProtocol;

/**
 * Settings for connection.
 * 
 * @author alexander.ivanov
 * 
 */
public class ConnectionSettings {

	/**
	 * Protocol.
	 */
	private final AccountProtocol protocol;

	/**
	 * User part of jid.
	 */
	private final String userName;

	/**
	 * Server part of jid.
	 */
	private final String serverName;

	/**
	 * Resource part of jid.
	 */
	private final String resource;

	/**
	 * Use custom connection host and port.
	 */
	private boolean custom;

	/**
	 * Host for connection.
	 */
	private String host;

	/**
	 * Port for connection.
	 */
	private int port;

	/**
	 * Password.
	 */
	private String password;

	/**
	 * Whether SASL Authentication Enabled.
	 */
	private boolean saslEnabled;

	/**
	 * TLS mode.
	 */
	private TLSMode tlsMode;

	/**
	 * Use compression.
	 */
	private boolean compression;

	public ConnectionSettings(AccountProtocol protocol, String userName,
			String serverName, String resource, boolean custom, String host,
			int port, String password, boolean saslEnabled, TLSMode tlsMode,
			boolean compression) {
		super();
		this.protocol = protocol;
		this.userName = userName;
		this.serverName = serverName;
		this.resource = resource;
		this.custom = custom;
		this.host = host;
		this.port = port;
		this.password = password;
		this.saslEnabled = saslEnabled;
		this.tlsMode = tlsMode;
		this.compression = compression;
	}

	public AccountProtocol getProtocol() {
		return protocol;
	}

	/**
	 * @return User part of jid.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return Server part of jid.
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * @return Whether custom host and port must be used.
	 */
	public boolean isCustom() {
		return custom;
	}

	/**
	 * @return Custom host to connect to.
	 */
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getResource() {
		return resource;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * @return Whether SASL Authentication Enabled.
	 */
	public boolean isSaslEnabled() {
		return saslEnabled;
	}

	/**
	 * @return TLS mode.
	 */
	public TLSMode getTlsMode() {
		return tlsMode;
	}

	/**
	 * @return Whether compression is used.
	 */
	public boolean useCompression() {
		return compression;
	}

	/**
	 * Updates options.
	 * 
	 * @param custom
	 * @param host
	 * @param port
	 * @param password
	 * @param saslEnabled
	 * @param tlsMode
	 * @param compression
	 */
	public void update(boolean custom, String host, int port, String password,
			boolean saslEnabled, TLSMode tlsMode, boolean compression) {
		this.custom = custom;
		this.host = host;
		this.port = port;
		this.password = password;
		this.saslEnabled = saslEnabled;
		this.tlsMode = tlsMode;
		this.compression = compression;
	}

	/**
	 * Sets password.
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
