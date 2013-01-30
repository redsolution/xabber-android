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

import java.util.Date;

/**
 * Represents result of oauth response.
 * 
 * @author alexander.ivanov
 * 
 */
public class OAuthResult {

	/**
	 * Number of milliseconds before actual expiration. Used to avoid token
	 * expiration while authorization is in progress.
	 */
	private static final long SHIFT = 2 * 60 * 1000;

	/**
	 * Time in milliseconds when token expires.
	 */
	private final long expires;

	private final String accessToken;

	private final String refreshToken;

	public OAuthResult(String accessToken, String refreshToken, long expiresIn) {
		this.expires = new Date().getTime() + expiresIn - SHIFT;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public boolean isExpired() {
		return new Date().getTime() > expires;
	}

}