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

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * Settings for connection.
 *
 * @author alexander.ivanov
 */
public class ConnectionSettings {
    /**
     * User part of jid.
     */
    private final Localpart userName;

    /**
     * Server part of jid.
     */
    private final DomainBareJid serverName;

    /**
     * Resource part of jid.
     */
    private final Resourcepart resource;

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
     * Token.
     */
    private String token;

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

    private ProxyType proxyType;

    private String proxyHost;

    private int proxyPort;

    private String proxyUser;

    private String proxyPassword;

    public ConnectionSettings(Localpart userName,
                              DomainBareJid serverName, Resourcepart resource, boolean custom, String host,
                              int port, String password, String token, boolean saslEnabled, TLSMode tlsMode,
                              boolean compression, ProxyType proxyType, String proxyHost,
                              int proxyPort, String proxyUser, String proxyPassword) {
        super();
        this.userName = userName;
        this.serverName = serverName;
        this.resource = resource;
        this.custom = custom;
        this.host = host;
        this.port = port;
        this.password = password;
        this.token = token;
        this.saslEnabled = saslEnabled;
        this.tlsMode = tlsMode;
        this.compression = compression;
        this.proxyType = proxyType;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
    }

    /**
     * @return User part of jid.
     */
    public Localpart getUserName() {
        return userName;
    }

    /**
     * @return Server part of jid.
     */
    public DomainBareJid getServerName() {
        return serverName;
    }

    /**
     * @return Whether custom host and port must be used.
     */
    public boolean isCustomHostAndPort() {
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

    public Resourcepart getResource() {
        return resource;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
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

    public ProxyType getProxyType() {
        return proxyType;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * Updates options.
     */
    public void update(boolean custom, String host, int port, String password,
                       boolean saslEnabled, TLSMode tlsMode, boolean compression,
                       ProxyType proxyType, String proxyHost, int proxyPort,
                       String proxyUser, String proxyPassword) {
        this.custom = custom;
        this.host = host;
        this.port = port;
        this.password = password;
        this.saslEnabled = saslEnabled;
        this.tlsMode = tlsMode;
        this.compression = compression;
        this.proxyType = proxyType;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
    }

    /**
     * Sets password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

}
