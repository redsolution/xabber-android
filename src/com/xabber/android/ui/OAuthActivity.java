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
package com.xabber.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountProtocol;
import com.xabber.android.data.account.OAuthManager;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

/**
 * Activity with WebView for OAuth authorization.
 * 
 * @author alexander.ivanov
 * 
 */
public class OAuthActivity extends ManagedActivity {

	private static final String EXTRA_INVALIDATE = "com.xabber.android.ui.OAuthVerifier.EXTRA_CANCELED";
	private static final String EXTRA_REFRESH_TOKEN = "com.xabber.android.ui.OAuthVerifier.EXTRA_REFRESH_TOKEN";

	private static final String SAVED_URL = "com.xabber.android.ui.OAuthActivity.URL";
	private static final String SAVED_CODE = "com.xabber.android.ui.OAuthActivity.CODE";

	private WebView webView;
	private String code;
	private boolean loaded;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;

		setContentView(R.layout.oauth);
		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new OAuthWebViewClient());
		loaded = false;
		if (savedInstanceState == null) {
			webView.loadUrl(OAuthManager.getInstance().getUrl(
					getAccountProtocol(getIntent())));
			code = null;
		} else {
			webView.loadUrl(savedInstanceState.getString(SAVED_URL));
			code = savedInstanceState.getString(SAVED_CODE);
		}
		update();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SAVED_URL, webView.getUrl());
		outState.putString(SAVED_CODE, code);
	}

	private void update() {
		boolean progress = !loaded || code != null;
		webView.setVisibility(progress ? View.INVISIBLE : View.VISIBLE);
		findViewById(R.id.progress_bar).setVisibility(
				progress ? View.VISIBLE : View.GONE);
	}

	private static Intent createResultIntent(Context context,
			String refreshToken) {
		Intent intent = new Intent();
		intent.putExtra(EXTRA_INVALIDATE, false);
		intent.putExtra(EXTRA_REFRESH_TOKEN, refreshToken);
		return intent;
	}

	private static Intent createInvalidateIntent(Context context) {
		Intent intent = new Intent();
		intent.putExtra(EXTRA_INVALIDATE, true);
		return intent;
	}

	public static Intent createIntent(Context context, AccountProtocol protocol) {
		Intent intent = new Intent(context, OAuthActivity.class);
		intent.setData(Uri.parse(String.valueOf(protocol.ordinal())));
		return intent;
	}

	public static boolean isInvalidated(Intent intent) {
		return intent.getBooleanExtra(OAuthActivity.EXTRA_INVALIDATE, false);
	}

	public static String getToken(Intent intent) {
		return intent.getStringExtra(OAuthActivity.EXTRA_REFRESH_TOKEN);
	}

	private static AccountProtocol getAccountProtocol(Intent intent) {
		int index = Integer.valueOf(intent.getData().toString());
		return AccountProtocol.values()[index];
	}

	private class OAuthWebViewClient extends WebViewClient {

		@Override
		public void onPageFinished(WebView webView, String url) {
			super.onPageFinished(webView, url);
			LogManager.i(this, url);
			if (code != null)
				return;
			loaded = true;
			Uri uri = Uri.parse(url);
			if (OAuthManager.getInstance().isValidUri(
					getAccountProtocol(getIntent()), uri)) {
				code = uri.getQueryParameter("code");
				if (code == null) {
					setResult(RESULT_OK,
							createInvalidateIntent(OAuthActivity.this));
					finish();
				} else {
					new OAuthTokenRequester().execute(code);
				}
			}
			update();
		}
	}

	private class OAuthTokenRequester extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			try {
				return OAuthManager.getInstance().requestRefreshToken(
						getAccountProtocol(getIntent()), params[0]);
			} catch (NetworkException e) {
				LogManager.exception(OAuthActivity.this, e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			setResult(RESULT_OK, createResultIntent(OAuthActivity.this, result));
			finish();
		}

	}

}
