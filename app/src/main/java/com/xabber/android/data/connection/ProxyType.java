package com.xabber.android.data.connection;

import org.jivesoftware.smack.proxy.ProxyInfo;

public enum ProxyType {

	none,

	http,

	socks4,

	socks5,

	orbot;

	public ProxyInfo getProxyInfo(String proxyHost, int proxyPort,
			String proxyUser, String proxyPassword) {
		ProxyInfo.ProxyType proxyType;
		if (this == none)
			proxyType = ProxyInfo.ProxyType.NONE;
		else if (this == http)
			proxyType = ProxyInfo.ProxyType.HTTP;
		else if (this == socks4)
			proxyType = ProxyInfo.ProxyType.SOCKS4;
		else if (this == socks5)
			proxyType = ProxyInfo.ProxyType.SOCKS5;
		else if (this == orbot) {
			proxyType = ProxyInfo.ProxyType.SOCKS5;
			proxyHost = "localhost";
			proxyPort = 9050;
			proxyUser = "";
			proxyPassword = "";
		} else
			throw new IllegalStateException();
		return new ProxyInfo(proxyType, proxyHost, proxyPort, proxyUser,
				proxyPassword);
	}

}
