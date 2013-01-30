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
import java.util.Locale;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.roster.AbstractContact;

/**
 * Base adapter for the list of contacts.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class BaseContactAdapter<Inflater extends BaseContactInflater>
		extends BaseAdapter implements UpdatableAdapter, Filterable {

	final Activity activity;

	final Locale locale;

	/**
	 * List of entities.
	 */
	final ArrayList<BaseEntity> baseEntities;

	/**
	 * Used view inflater.
	 */
	final Inflater inflater;

	/**
	 * Contact filter.
	 */
	ContactFilter contactFilter;

	/**
	 * Filter string. Can be <code>null</code> if filter is disabled.
	 */
	String filterString;

	public BaseContactAdapter(Activity activity, Inflater inflater) {
		this.activity = activity;
		this.locale = Locale.getDefault();
		this.baseEntities = new ArrayList<BaseEntity>();
		this.inflater = inflater;
		inflater.setAdapter(this);
		contactFilter = null;
		filterString = null;
	}

	/**
	 * @return View inflater.
	 */
	public Inflater getInflater() {
		return inflater;
	}

	@Override
	public void onChange() {
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return baseEntities.size();
	}

	@Override
	public Object getItem(int position) {
		return baseEntities.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view;
		if (convertView == null) {
			view = inflater.createView(position, parent);
			view.setTag(inflater.createViewHolder(position, view));
		} else {
			view = convertView;
		}
		inflater.getView(view, (AbstractContact) getItem(position));
		return view;
	}

	@Override
	public Filter getFilter() {
		if (contactFilter == null)
			contactFilter = new ContactFilter();
		return contactFilter;
	}

	private class ContactFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			return null;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			if (constraint == null || constraint.length() == 0)
				filterString = null;
			else
				filterString = constraint.toString().toLowerCase(locale);
			onChange();
		}

	}

}
