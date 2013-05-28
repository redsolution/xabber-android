package com.xabber.android.ui;

import java.util.Collection;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.adapter.AccountConfiguration;
import com.xabber.android.ui.adapter.ContactListAdapter;
import com.xabber.android.ui.adapter.GroupConfiguration;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.androiddev.R;

public class ContactListFragment extends Fragment implements
		OnAccountChangedListener, OnContactChangedListener,
		OnChatChangedListener, OnItemClickListener {

	private ContactListAdapter adapter;
	private ListView listView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.contact_list_fragment, container,
				false);
		listView = (ListView) view.findViewById(android.R.id.list);
		listView.setOnItemClickListener(this);
		listView.setItemsCanFocus(true);
		registerForContextMenu(listView);
		adapter = new ContactListAdapter(getActivity(), listView,
				view.findViewById(R.id.info));
		listView.setAdapter(adapter);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		Application.getInstance().addUIListener(OnAccountChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnContactChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnChatChangedListener.class,
				this);
		adapter.onChange();
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterListeners();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		BaseEntity baseEntity = (BaseEntity) listView
				.getItemAtPosition(info.position);
		if (baseEntity instanceof AbstractContact) {
			ContextMenuHelper.createContactContextMenu(getActivity(), adapter,
					(AbstractContact) baseEntity, menu);
		} else if (baseEntity instanceof AccountConfiguration) {
			ContextMenuHelper.createAccountContextMenu(getActivity(), adapter,
					baseEntity.getAccount(), menu);
		} else if (baseEntity instanceof GroupConfiguration) {
			ContextMenuHelper.createGroupContextMenu(getActivity(), adapter,
					baseEntity.getAccount(), baseEntity.getUser(), menu);
		} else
			throw new IllegalStateException();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Object object = parent.getAdapter().getItem(position);
		if (object instanceof AbstractContact) {
			((OnContactClickListener) getActivity())
					.onContactClick((AbstractContact) object);
		} else if (object instanceof GroupConfiguration) {
			GroupConfiguration groupConfiguration = (GroupConfiguration) object;
			adapter.setExpanded(groupConfiguration.getAccount(),
					groupConfiguration.getUser(),
					!groupConfiguration.isExpanded());
		} else
			throw new IllegalStateException();
	}

	@Override
	public void onContactsChanged(Collection<BaseEntity> addresses) {
		adapter.refreshRequest();
	}

	@Override
	public void onAccountsChanged(Collection<String> accounts) {
		adapter.refreshRequest();
	}

	@Override
	public void onChatChanged(String account, String user, boolean incoming) {
		if (incoming)
			adapter.refreshRequest();
	}

	/**
	 * Force stop contact list updates before pause or application close.
	 */
	void unregisterListeners() {
		Application.getInstance().removeUIListener(
				OnAccountChangedListener.class, this);
		Application.getInstance().removeUIListener(
				OnContactChangedListener.class, this);
		Application.getInstance().removeUIListener(OnChatChangedListener.class,
				this);
		adapter.removeRefreshRequests();
	}

	UpdatableAdapter getAdapter() {
		return adapter;
	}

	/**
	 * Scroll contact list to specified account.
	 * 
	 * @param account
	 */
	void scrollTo(String account) {
		long count = listView.getCount();
		for (int position = 0; position < (int) count; position++) {
			BaseEntity baseEntity = (BaseEntity) listView
					.getItemAtPosition(position);
			if (baseEntity != null
					&& baseEntity instanceof AccountConfiguration
					&& baseEntity.getAccount().equals(account)) {
				stopMovement();
				listView.setSelection(position);
				break;
			}
		}
	}

	/**
	 * Filter out contact list for selected account.
	 * 
	 * @param account
	 */
	void setSelectedAccount(String account) {
		if (account.equals(AccountManager.getInstance().getSelectedAccount()))
			SettingsManager.setContactsSelectedAccount("");
		else
			SettingsManager.setContactsSelectedAccount(account);
		stopMovement();
		adapter.onChange();
	}

	/**
	 * Scroll to the top of contact list.
	 */
	void scrollUp() {
		if (listView.getCount() > 0)
			listView.setSelection(0);
		stopMovement();
	}

	/**
	 * Stop fling scrolling.
	 */
	private void stopMovement() {
		listView.onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
				SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
	}

	public interface OnContactClickListener {

		void onContactClick(AbstractContact contact);

	}

}
