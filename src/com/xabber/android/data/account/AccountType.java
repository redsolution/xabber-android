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

import java.util.Collections;
import java.util.List;

import android.graphics.drawable.Drawable;

/**
 * Account presets.
 * 
 * @author alexander.ivanov
 */
public class AccountType {

	/**
	 * String resoure ID.
	 */
	final private int id;

	/**
	 * Protocol.
	 */
	final private AccountProtocol protocol;

	/**
	 * Name of account type.
	 */
	final private String name;

	/**
	 * Default server.
	 */
	final private List<String> servers;

	/**
	 * Icon.
	 */
	final private Drawable icon;

	/**
	 * Hint in user field.
	 */
	final private String hint;

	/**
	 * Help text.
	 */
	final private String help;

	/**
	 * Allow to enter server name.
	 */
	final private boolean allowServer;

	/**
	 * Specific host.
	 */
	final private String host;

	/**
	 * Specific port.
	 */
	final private int port;

	/**
	 * TLS is required.
	 */
	final private boolean tlsRequired;

	public AccountType(int id, AccountProtocol protocol, String name,
			String hint, String help, Drawable icon, boolean allowServer,
			String host, int port, boolean tlsRequired, List<String> servers) {
		this.id = id;
		this.protocol = protocol;
		this.name = name;
		this.hint = hint;
		this.help = help;
		this.icon = icon;
		this.allowServer = allowServer;
		this.servers = Collections.unmodifiableList(servers);
		this.host = host;
		this.port = port;
		this.tlsRequired = tlsRequired;
	}

	public int getId() {
		return id;
	}

	public AccountProtocol getProtocol() {
		return protocol;
	}

	public String getName() {
		return name;
	}

	public String getFirstServer() {
		return servers.get(0);
	}

	public List<String> getServers() {
		return servers;
	}

	public Drawable getIcon() {
		return icon;
	}

	public String getHint() {
		return hint;
	}

	public String getHelp() {
		return help;
	}

	public boolean isAllowServer() {
		return allowServer;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public boolean isTLSRequired() {
		return tlsRequired;
	}
}