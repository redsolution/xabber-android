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

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.OnAuthorizedListener;

/**
 * Manager for OAuth authorization and account's name mapping.
 * 
 * @author alexander.ivanov
 * 
 */
public class OAuthManager implements OnAuthorizedListener,
		OnAccountRemovedListener {

	/**
	 * Real full jids assigned to accounts.
	 */
	private final Map<String, String> jids;

	/**
	 * Access tokens for refresh tokens.
	 */
	private final Map<String, OAuthResult> tokens;

	private final static OAuthManager instance;

	static {
		instance = new OAuthManager(Application.getInstance());
		Application.getInstance().addManager(instance);
	}

	public static OAuthManager getInstance() {
		return instance;
	}

	private OAuthManager(Application application) {
		jids = new HashMap<String, String>();
		tokens = new HashMap<String, OAuthResult>();
	}

	private OAuthProvider getOAuthProvider(AccountProtocol protocol)
			throws UnsupportedOperationException {
		for (OAuthProvider provider : Application.getInstance().getManagers(
				OAuthProvider.class))
			if (provider.getAccountProtocol() == protocol)
				return provider;
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets refresh token using Request Access Token.
	 * 
	 * DON'T CALL THIS FUNTCION FROM UI THREAD.
	 * 
	 * @param protocol
	 * @param code
	 * @return refresh token or <code>null</code> if auth failed.
	 * @throws NetworkException
	 */
	public String requestRefreshToken(AccountProtocol protocol, String code)
			throws NetworkException {
		return getOAuthProvider(protocol).requestRefreshToken(code);
	}

	/**
	 * Returns password to be used in connection.
	 * 
	 * @param protocol
	 * @param password
	 * @return <code>null</code> if password must be requested using refresh
	 *         token.
	 */
	public String getPassword(AccountProtocol protocol, String password) {
		if (protocol.isOAuth()) {
			String accessToken = getAccessToken(password);
			if (accessToken != null)
				return accessToken;
			return null;
		} else
			return password;
	}

	/**
	 * Requests password renew from server based on refreshToken.
	 * 
	 * DON'T CALL THIS FUNTCION FROM UI THREAD.
	 * 
	 * @param protocol
	 * @param refreshToken
	 * @return <code>null</code> on authorization fail.
	 * @throws NetworkException
	 */
	public OAuthResult requestAccessToken(AccountProtocol protocol,
			String refreshToken) throws NetworkException {
		return getOAuthProvider(protocol).requestAccessToken(refreshToken);
	}

	/**
	 * Update token cache.
	 * 
	 * Must be call from IU thread with the value returned by
	 * {@link #requestAccessToken(AccountProtocol, String)}.
	 * 
	 * @param oAuthResult
	 */
	public void onAccessTokenReceived(OAuthResult oAuthResult) {
		if (oAuthResult != null)
			tokens.put(oAuthResult.getRefreshToken(), oAuthResult);
	}

	/**
	 * @param password
	 * @return Not expired access token or <code>null</code> if there is no
	 *         access token or it was expired.
	 */
	private String getAccessToken(String password) {
		OAuthResult result = tokens.get(password);
		if (result == null)
			return null;
		if (!result.isExpired())
			return result.getAccessToken();
		tokens.remove(password);
		return null;
	}

	/**
	 * @param account
	 * @return Assigned jid or <code>null</code> if there is no assigned jid.
	 */
	public String getAssignedJid(String account) {
		return jids.get(account);
	}

	/**
	 * @param protocol
	 * @return Url to be opened in browser.
	 */
	public String getUrl(AccountProtocol protocol) {
		return getOAuthProvider(protocol).getUrl();
	}

	/**
	 * @param protocol
	 * @param uri
	 * @return Whether redirect is valid.
	 */
	public boolean isValidUri(AccountProtocol protocol, Uri uri) {
		return getOAuthProvider(protocol).isValidUri(uri);
	}

	@Override
	public void onAuthorized(ConnectionItem connection) {
		if (!(connection instanceof AccountItem))
			return;
		String account = ((AccountItem) connection).getAccount();
		if (!connection.getConnectionSettings().getProtocol().isOAuth())
			return;
		String jid = connection.getRealJid();
		if (jid == null)
			return;
		jids.put(account, jid);
	}

	@Override
	public void onAccountRemoved(AccountItem accountItem) {
		jids.remove(accountItem.getAccount());
	}

}
