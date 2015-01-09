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

import android.net.Uri;

import com.xabber.android.data.BaseManagerInterface;
import com.xabber.android.data.NetworkException;

/**
 * OAuth implementation.
 * 
 * @author alexander.ivanov
 * 
 */
interface OAuthProvider extends BaseManagerInterface {

	/**
	 * @return Supported protocol.
	 */
	public AccountProtocol getAccountProtocol();

	/**
	 * Gets refresh token using Request Access Token.
	 * 
	 * DON'T CALL THIS FUNTCION FROM UI THREAD.
	 * 
	 * @param code
	 * @return refresh token or <code>null</code> if auth failed.
	 * @throws NetworkException
	 */
	public String requestRefreshToken(String code) throws NetworkException;

	/**
	 * Requests password renew from server based on refreshToken.
	 * 
	 * DON'T CALL THIS FUNTCION FROM UI THREAD.
	 * 
	 * @param refreshToken
	 * @return <code>null</code> on authorization fail.
	 * @throws NetworkException
	 */
	public OAuthResult requestAccessToken(String refreshToken)
			throws NetworkException;

	/**
	 * @return Url to be opened in browser.
	 */
	String getUrl();

	/**
	 * @param uri
	 * @return Whether redirect is valid.
	 */
	boolean isValidUri(Uri uri);

}
