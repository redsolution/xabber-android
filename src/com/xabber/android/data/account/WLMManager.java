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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.net.Uri;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.androiddev.R;

class WLMManager implements OAuthProvider {

	private static enum GrantType {

		authorizationCode("authorization_code", "code"),

		refreshToken("refresh_token", "refresh_token");

		public final String name;
		public final String value;

		private GrantType(String name, String value) {
			this.name = name;
			this.value = value;
		}

	}

	private static final String WLM_CLIENT_SECRET = "XEazfSKu0Iu2pt6Z64Lqm-1cRxtEYgS0";
	private static final String WLM_CLIENT_ID = "00000000440923FF";
	private static final String WLM_SCOPE = "wl.messenger wl.offline_access";
	private static final String WLM_SCHEME = "https";
	private static final String WLM_AUTHORITY = "oauth.live.com";
	private static final String WLM_REDIRECT_PATH = "/desktop";
	private static final String WLM_REDIRECT_URL = new Uri.Builder()
			.scheme(WLM_SCHEME).authority(WLM_AUTHORITY)
			.path(WLM_REDIRECT_PATH).build().toString();

	private final static WLMManager instance;

	static {
		instance = new WLMManager();
		Application.getInstance().addManager(instance);
	}

	public static WLMManager getInstance() {
		return instance;
	}

	private WLMManager() {
	}

	/**
	 * @param protocol
	 * @param grantType
	 * @param value
	 * @return Access and refresh tokens or <code>null</code> if auth failed.
	 * @throws NetworkException
	 */
	private OAuthResult accessTokenOperation(GrantType grantType, String value)
			throws NetworkException {
		HttpPost httpPost = new HttpPost(new Uri.Builder().scheme(WLM_SCHEME)
				.authority(WLM_AUTHORITY).path("token").build().toString());
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs
				.add(new BasicNameValuePair("grant_type", grantType.name));
		nameValuePairs.add(new BasicNameValuePair(grantType.value, value));
		nameValuePairs.add(new BasicNameValuePair("redirect_uri",
				WLM_REDIRECT_URL));
		nameValuePairs.add(new BasicNameValuePair("client_id", WLM_CLIENT_ID));
		nameValuePairs.add(new BasicNameValuePair("client_secret",
				WLM_CLIENT_SECRET));
		UrlEncodedFormEntity encodedFormEntity;
		try {
			encodedFormEntity = new UrlEncodedFormEntity(nameValuePairs,
					HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new NetworkException(R.string.CONNECTION_FAILED, e);
		}
		String content;
		try {
			content = EntityUtils.toString(encodedFormEntity);
		} catch (ParseException e) {
			throw new NetworkException(R.string.CONNECTION_FAILED, e);
		} catch (IOException e) {
			throw new NetworkException(R.string.CONNECTION_FAILED, e);
		}
		LogManager.i(this, httpPost.getURI().toString() + "\n" + content);
		httpPost.setEntity(encodedFormEntity);
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse httpResponse;
		try {
			httpResponse = httpClient.execute(httpPost);
		} catch (ClientProtocolException e) {
			throw new NetworkException(R.string.CONNECTION_FAILED, e);
		} catch (IOException e) {
			throw new NetworkException(R.string.CONNECTION_FAILED, e);
		}
		HttpEntity entity = httpResponse.getEntity();
		try {
			content = EntityUtils.toString(entity);
		} catch (ParseException e) {
			throw new NetworkException(R.string.CONNECTION_FAILED, e);
		} catch (IOException e) {
			throw new NetworkException(R.string.CONNECTION_FAILED, e);
		} finally {
			try {
				entity.consumeContent();
			} catch (IOException e) {
				throw new NetworkException(R.string.CONNECTION_FAILED, e);
			}
		}
		LogManager.i(this, content);
		long expiresIn;
		String accessToken;
		String refreshToken;
		try {
			JSONObject jsonObject = (JSONObject) new JSONTokener(content)
					.nextValue();
			if (jsonObject.has("error"))
				return null;
			try {
				expiresIn = Long.valueOf(jsonObject.getString("expires_in")) * 1000;
			} catch (NumberFormatException e) {
				throw new NetworkException(R.string.CONNECTION_FAILED, e);
			}
			accessToken = jsonObject.getString("access_token");
			refreshToken = jsonObject.getString("refresh_token");
		} catch (JSONException e) {
			throw new NetworkException(R.string.CONNECTION_FAILED, e);
		}
		return new OAuthResult(accessToken, refreshToken, expiresIn);
	}

	@Override
	public AccountProtocol getAccountProtocol() {
		return AccountProtocol.wlm;
	}

	@Override
	public String requestRefreshToken(String code) throws NetworkException {
		OAuthResult result = accessTokenOperation(GrantType.authorizationCode,
				code);
		if (result == null)
			return null;
		else
			return result.getRefreshToken();
	}

	@Override
	public OAuthResult requestAccessToken(String refreshToken)
			throws NetworkException {
		return accessTokenOperation(GrantType.refreshToken, refreshToken);
	}

	@Override
	public String getUrl() {
		return new Uri.Builder().scheme(WLM_SCHEME).authority(WLM_AUTHORITY)
				.path("authorize")
				.appendQueryParameter("response_type", "code")
				.appendQueryParameter("client_id", WLM_CLIENT_ID)
				.appendQueryParameter("redirect_uri", WLM_REDIRECT_URL)
				.appendQueryParameter("scope", WLM_SCOPE)
				.appendQueryParameter("display", "touch").build().toString();
	}

	@Override
	public boolean isValidUri(Uri uri) {
		return WLM_SCHEME.equals(uri.getScheme())
				&& WLM_AUTHORITY.equals(uri.getAuthority())
				&& WLM_REDIRECT_PATH.equals(uri.getPath());
	}

}
