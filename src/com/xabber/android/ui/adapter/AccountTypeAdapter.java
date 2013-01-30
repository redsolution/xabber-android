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

import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.AccountType;
import com.xabber.androiddev.R;

/**
 * Adapter for drop down list of account's types.
 * 
 * @author alexander.ivanov
 * 
 */
public class AccountTypeAdapter extends BaseAdapter {

	private final Activity activity;
	private final List<AccountType> accountTypes;

	public AccountTypeAdapter(Activity activity) {
		super();
		this.activity = activity;
		accountTypes = AccountManager.getInstance().getAccountTypes();
	}

	@Override
	public int getCount() {
		return accountTypes.size();
	}

	@Override
	public Object getItem(int position) {
		return accountTypes.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view;
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.account_type_item, parent, false);
		} else {
			view = convertView;
		}
		final AccountType type = (AccountType) getItem(position);
		((ImageView) view.findViewById(R.id.avatar)).setImageDrawable(type
				.getIcon());
		((TextView) view.findViewById(R.id.name)).setText(type.getName());
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		final View view;
		if (convertView == null) {
			view = activity.getLayoutInflater().inflate(
					R.layout.account_type_dropdown, parent, false);
		} else {
			view = convertView;
		}
		final AccountType type = (AccountType) getItem(position);
		((ImageView) view.findViewById(R.id.avatar)).setImageDrawable(type
				.getIcon());
		((TextView) view.findViewById(R.id.name)).setText(type.getName());
		return view;
	}

}
