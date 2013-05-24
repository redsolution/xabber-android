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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.xabber.android.ui.dialog.GroupAddDialogFragment;
import com.xabber.android.ui.dialog.GroupAddDialogFragment.OnGroupAddConfirmed;
import com.xabber.android.ui.helper.ManagedListActivity;
import com.xabber.androiddev.R;

/**
 * Manage list of selected groups.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class GroupListActivity extends ManagedListActivity implements
		OnItemClickListener, OnGroupAddConfirmed {

	private static final String SAVED_GROUPS = "com.xabber.android.ui.ContactList.SAVED_GROUPS";
	private static final String SAVED_SELECTED = "com.xabber.android.ui.ContactList.SAVED_SELECTED";

	static final int OPTION_MENU_ADD_GROUP_ID = 1;

	private ArrayAdapter<String> arrayAdapter;

	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;
		onInflate(savedInstanceState);
		if (isFinishing())
			return;
		Collection<String> groups;
		Collection<String> selected;
		if (savedInstanceState != null) {
			groups = savedInstanceState.getStringArrayList(SAVED_GROUPS);
			selected = savedInstanceState.getStringArrayList(SAVED_SELECTED);
		} else {
			groups = getInitialGroups();
			selected = getInitialSelected();
		}

		listView = getListView();
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);
		arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice,
				new ArrayList<String>());
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.add_item, listView, false);
		((TextView) view.findViewById(android.R.id.message))
				.setText(R.string.group_add);
		listView.addFooterView(view, null, true);
		setListAdapter(arrayAdapter);
		setGroups(groups, selected);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(SAVED_GROUPS, getGroups());
		outState.putStringArrayList(SAVED_SELECTED, getSelected());
	}

	/**
	 * Inflates layout.
	 * 
	 * @param savedInstanceState
	 */
	protected abstract void onInflate(Bundle savedInstanceState);

	/**
	 * @return List of initial allowed groups.
	 */
	abstract Collection<String> getInitialGroups();

	/**
	 * @return List of initially selected groups.
	 */
	abstract Collection<String> getInitialSelected();

	/**
	 * @return Actual groups from adapter.
	 */
	private ArrayList<String> getGroups() {
		ArrayList<String> groups = new ArrayList<String>();
		for (int position = 0; position < arrayAdapter.getCount(); position++)
			groups.add(arrayAdapter.getItem(position));
		return groups;
	}

	/**
	 * @return Actual selected groups from adapter.
	 */
	public ArrayList<String> getSelected() {
		ArrayList<String> groups = new ArrayList<String>();
		for (int position = 0; position < arrayAdapter.getCount(); position++)
			if (listView.isItemChecked(position
					+ listView.getHeaderViewsCount())) {
				groups.add(arrayAdapter.getItem(position));
			}
		return groups;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, OPTION_MENU_ADD_GROUP_ID, 0,
				getResources().getText(R.string.group_add)).setIcon(
				android.R.drawable.ic_menu_add);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case OPTION_MENU_ADD_GROUP_ID:
			showGroupAddDialog();
			return true;
		}
		return false;
	}

	private void showGroupAddDialog() {
		GroupAddDialogFragment.newInstance(getGroups()).show(
				getSupportFragmentManager(), "GROUP-ADD");
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (listView.getItemAtPosition(position) == null) // Footer
			showGroupAddDialog();
	}

	/**
	 * Sets new list of groups and select specified groups.
	 * 
	 * @param groups
	 * @param selected
	 */
	void setGroups(Collection<String> groups, Collection<String> selected) {
		ArrayList<String> list = new ArrayList<String>(groups);
		Collections.sort(list);
		arrayAdapter.clear();
		for (int position = 0; position < list.size(); position++) {
			String group = list.get(position);
			arrayAdapter.add(group);
			listView.setItemChecked(position + listView.getHeaderViewsCount(),
					selected.contains(group));
		}
	}

	@Override
	public void onGroupAddConfirmed(String group) {
		ArrayList<String> groups = getGroups();
		groups.add(group);
		ArrayList<String> selected = getSelected();
		selected.add(group);
		setGroups(groups, selected);
	}

}
