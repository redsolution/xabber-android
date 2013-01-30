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
package com.xabber.android.ui.adapter;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.androiddev.R;

/**
 * Adapter for drop down list of accounts.
 * 
 * @author alexander.ivanov
 * 
 */
public class AccountChooseAdapter extends BaseAdapter {

	private final Activity activity;
	protected final ArrayList<String> accounts;

	public AccountChooseAdapter(Activity activity) {
		super();
		this.activity = activity;
		accounts = new ArrayList<String>(AccountManager.getInstance()
				.getAccounts());
		Collections.sort(accounts);
	}

	@Override
	public int getCount() {
		return accounts.size();
	}

	@Override
	public Object getItem(int position) {
		return accounts.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view;
		final AccountManager accountManager = AccountManager.getInstance();
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.account_choose_item, parent, false);
		} else {
			view = convertView;
		}
		final String account = (String) getItem(position);
		((ImageView) view.findViewById(R.id.avatar))
				.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(
						account));
		((TextView) view.findViewById(R.id.name)).setText(accountManager
				.getVerboseName(account));
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		final View view;
		final AccountManager accountManager = AccountManager.getInstance();
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.account_choose_dropdown, parent, false);
		} else {
			view = convertView;
		}
		final String account = (String) getItem(position);
		((ImageView) view.findViewById(R.id.avatar))
				.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(
						account));
		((TextView) view.findViewById(R.id.name)).setText(accountManager
				.getVerboseName(account));
		return view;
	}

}
