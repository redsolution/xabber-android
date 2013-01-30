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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.androiddev.R;

/**
 * Adapter for the list of accounts in the title of contact list.
 * 
 * @author alexander.ivanov
 * 
 */
public class AccountToggleAdapter implements UpdatableAdapter {

	private final Activity activity;

	/**
	 * Listener for click on elements.
	 */
	private final OnClickListener onClickListener;

	/**
	 * Layout to be populated.
	 */
	private final LinearLayout linearLayout;

	/**
	 * List of accounts.
	 */
	private final ArrayList<String> accounts;

	public AccountToggleAdapter(Activity activity,
			OnClickListener onClickListener, LinearLayout linearLayout) {
		super();
		this.activity = activity;
		this.onClickListener = onClickListener;
		this.linearLayout = linearLayout;
		accounts = new ArrayList<String>();
	}

	/**
	 * Rebuild list of accounts.
	 * 
	 * Call it on account creation, deletion, enable or disable.
	 */
	public void rebuild() {
		accounts.clear();
		accounts.addAll(AccountManager.getInstance().getAccounts());
		Collections.sort(accounts);
		final int size = accounts.size();
		final LayoutInflater inflater = (LayoutInflater) activity
				.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		while (linearLayout.getChildCount() < size) {
			final View view = inflater.inflate(R.layout.account_toggler_item,
					linearLayout, false);
			linearLayout.addView(view);
			final AccountViewHolder accountViewHolder = new AccountViewHolder(
					view);
			view.setTag(accountViewHolder);
			activity.registerForContextMenu(accountViewHolder.statusMode);
			accountViewHolder.statusMode.setOnClickListener(onClickListener);
		}
		while (linearLayout.getChildCount() > size)
			linearLayout.removeViewAt(size);
		onChange();
	}

	@Override
	public void onChange() {
		boolean contactsShowAccounts = SettingsManager.contactsShowAccounts();
		String selected = AccountManager.getInstance().getSelectedAccount();
		for (int index = 0; index < accounts.size(); index++) {
			final View view = linearLayout.getChildAt(index);
			final AccountViewHolder accountViewHolder = (AccountViewHolder) view
					.getTag();
			final String account = accounts.get(index);
			StatusMode statusMode = AccountManager.getInstance()
					.getAccount(account).getDisplayStatusMode();
			int colorLevel = AccountManager.getInstance()
					.getColorLevel(account);
			view.getBackground().setLevel(colorLevel);
			if (contactsShowAccounts)
				accountViewHolder.statusMode
						.setBackgroundResource(R.drawable.account_border);
			else
				accountViewHolder.statusMode
						.setBackgroundResource(R.drawable.account_border_persistent);
			if (selected == null || account.equals(selected))
				accountViewHolder.disabled.setVisibility(View.GONE);
			else
				accountViewHolder.disabled.setVisibility(View.VISIBLE);
			accountViewHolder.statusMode.getBackground().setLevel(colorLevel);
			accountViewHolder.statusMode.setImageLevel(statusMode.ordinal());
			accountViewHolder.avatar.setImageDrawable(AvatarManager
					.getInstance().getAccountAvatar(account));
		}
	}

	public int getCount() {
		return accounts.size();
	}

	public Object getItem(int position) {
		return accounts.get(position);
	}

	/**
	 * Get the data item associated with the specified view.
	 * 
	 * @param view
	 *            direct child of linear layout or status_mode view in direct
	 *            child.
	 * @return The data for the specified view.
	 */
	public Object getItemForView(View view) {
		if (view.getId() == R.id.status_mode)
			view = (View) view.getParent();
		for (int index = 0; index < linearLayout.getChildCount(); index++)
			if (view == linearLayout.getChildAt(index))
				return accounts.get(index);
		return null;
	}

	private static class AccountViewHolder {
		final ImageView statusMode;
		final ImageView avatar;
		final ImageView disabled;

		public AccountViewHolder(View view) {
			statusMode = (ImageView) view.findViewById(R.id.status_mode);
			avatar = (ImageView) view.findViewById(R.id.avatar);
			disabled = (ImageView) view.findViewById(R.id.disabled);
		}
	}

}
