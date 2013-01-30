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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.ui.AccountList;
import com.xabber.androiddev.R;

/**
 * Adapter for the list of accounts for {@link AccountList}.
 * 
 * @author alexander.ivanov
 * 
 */
public class AccountListAdapter extends BaseListEditorAdapter<String> {

	public AccountListAdapter(Activity activity) {
		super(activity);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		AccountManager accountManager = AccountManager.getInstance();
		if (convertView == null) {
			view = getActivity().getLayoutInflater().inflate(
					R.layout.account_list_item, parent, false);
		} else {
			view = convertView;
		}
		String account = getItem(position);

		((ImageView) view.findViewById(R.id.color))
				.setImageLevel(accountManager.getColorLevel(account));
		((ImageView) view.findViewById(R.id.avatar))
				.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(
						account));
		((TextView) view.findViewById(R.id.name)).setText(accountManager
				.getVerboseName(account));
		AccountItem accountItem = accountManager.getAccount(account);
		ConnectionState state;
		if (accountItem == null)
			state = ConnectionState.offline;
		else
			state = accountItem.getState();
		((TextView) view.findViewById(R.id.status)).setText(getActivity()
				.getString(state.getStringId()));
		return view;
	}

	@Override
	protected Collection<String> getTags() {
		List<String> list = new ArrayList<String>();
		list.addAll(AccountManager.getInstance().getAllAccounts());
		Collections.sort(list);
		return list;
	}

}
