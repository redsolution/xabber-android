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

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Adapter for drop down list of accounts.
 *
 * @author alexander.ivanov
 */
public class AccountChooseAdapter extends BaseAdapter {
    protected final ArrayList<AccountJid> accounts;
    private final LayoutInflater layoutInflater;

    public AccountChooseAdapter(Activity activity) {
        super();
        this.layoutInflater = activity.getLayoutInflater();
        accounts = new ArrayList<>(AccountManager.INSTANCE.getEnabledAccounts());
        Collections.sort(accounts);
    }

    public AccountChooseAdapter(LayoutInflater layoutInflater){
        super();
        this.layoutInflater = layoutInflater;
        accounts = new ArrayList<>(AccountManager.INSTANCE.getEnabledAccounts());
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
        if (convertView == null) {
            view = layoutInflater.inflate(
                    R.layout.item_account_choose, parent, false);
        } else {
            view = convertView;
        }
        final AccountJid account = (AccountJid) getItem(position);

        ((TextView) view.findViewById(R.id.name)).setText(
                AccountManager.INSTANCE.getVerboseName(account)
        );
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

}
