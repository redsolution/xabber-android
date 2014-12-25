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
package com.xabber.android.data.roster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;

import com.xabber.android.data.Application;
import com.xabber.android.data.DatabaseManager;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.OnUnloadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.OnAccountAddedListener;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.account.OnAccountSyncableChangedListener;
import com.xabber.android.data.entity.AccountRelated;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.utils.DummyCursor;
import com.xabber.androiddev.R;

/**
 * Manage integration with system accounts and contacts.
 * 
 * All operation and states are managed from background thread.
 * 
 * @author alexander.ivanov
 * 
 * @see {@link Application#isContactsSupported()}.
 * 
 */
@SuppressLint("UseSparseArrays")
@TargetApi(5)
public class SyncManager implements OnLoadListener, OnUnloadListener,
		OnAccountAddedListener, OnAccountRemovedListener,
		OnAccountSyncableChangedListener, OnAccountsUpdateListener,
		OnRosterChangedListener {

	private static boolean LOG = true;

	private static final Uri RAW_CONTACTS_URI = RawContacts.CONTENT_URI
			.buildUpon()
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
					"true").build();

	private static final Uri GROUPS_URI = Groups.CONTENT_URI
			.buildUpon()
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
					"true").build();

	private static final Uri DATA_URI = Data.CONTENT_URI
			.buildUpon()
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
					"true").build();

	private final Application application;

	/**
	 * List of contacts with specified status.
	 */
	private final HashMap<RosterContact, SystemContactStatus> statuses;

	/**
	 * System account manager.
	 */
	private final AccountManager accountManager;

	/**
	 * Whether system accounts must be created on xabber account add.
	 * 
	 * Used to prevent system account creation on load.
	 */
	private boolean createAccounts;

	/**
	 * Whether OnAccountsUpdatedListener was registered.
	 */
	private boolean registeredOnAccountsUpdatedListener;

	/**
	 * Accounts which contacts is indented to be synchronized.
	 */
	private final HashSet<String> syncableAccounts;

	private final static SyncManager instance;

	static {
		instance = new SyncManager();
		Application.getInstance().addManager(instance);
	}

	public static SyncManager getInstance() {
		return instance;
	}

	private SyncManager() {
		this.application = Application.getInstance();
		statuses = new HashMap<RosterContact, SystemContactStatus>();
		syncableAccounts = new HashSet<String>();
		accountManager = AccountManager.get(application);
		createAccounts = false;
		registeredOnAccountsUpdatedListener = false;
	}

	/**
	 * @return Account type used by system contact list.
	 */
	public String getAccountType() {
		return application.getString(R.string.sync_account_type);
	}

	/**
	 * Returns first entry form the map. Populate otherIds if map contacts more
	 * then one entry.
	 * 
	 * @param map
	 *            can be <code>null</code>.
	 * @param otherIds
	 *            collection of keys for not first entries.
	 * @return <code>null</code> if map is <code>null</code> or map has no
	 *         elements.
	 */
	private Entry<Long, String> getFirstEntry(HashMap<Long, String> map,
			Collection<Long> otherIds) {
		if (map == null)
			return null;
		Entry<Long, String> result = null;
		for (Entry<Long, String> entry : map.entrySet())
			if (result == null)
				result = entry;
			else {
				LogManager.w(this, "Remove data: " + entry.getKey() + ": "
						+ entry.getValue());
				otherIds.add(entry.getKey());
			}
		return result;
	}

	/**
	 * Creates dummy cursor if passed cursor is <code>null</code>.
	 * 
	 * @param cursor
	 * @return
	 */
	private Cursor checkCursor(Cursor cursor) {
		if (cursor == null)
			return new DummyCursor();
		return cursor;
	}

	@Override
	public void onLoad() {
		Cursor cursor;

		// List of ids to be removed
		ArrayList<Long> removeGroupIds = new ArrayList<Long>();
		ArrayList<Long> removeRawIds = new ArrayList<Long>();
		ArrayList<Long> removeDataIds = new ArrayList<Long>();

		// Load groups
		HashMap<Long, RosterGroup> groupsForGroupIds = new HashMap<Long, RosterGroup>();
		cursor = application.getContentResolver().query(GROUPS_URI,
				new String[] { Groups._ID, Groups.ACCOUNT_NAME, Groups.TITLE },
				Groups.ACCOUNT_TYPE + " = ?",
				new String[] { getAccountType() }, null);
		cursor = checkCursor(cursor);
		try {
			int idIndex = cursor.getColumnIndex(Groups._ID);
			int accountIndex = cursor.getColumnIndex(Groups.ACCOUNT_NAME);
			int titleIndex = cursor.getColumnIndex(Groups.TITLE);
			while (cursor.moveToNext()) {
				long groupId = cursor.getLong(idIndex);
				String account = cursor.getString(accountIndex);
				String name = cursor.getString(titleIndex);
				RosterGroup rosterGroup = new RosterGroup(account, name);
				rosterGroup.setId(groupId);
				groupsForGroupIds.put(groupId, rosterGroup);
			}
		} finally {
			try {
				cursor.close();
			} catch (Exception e) {
				LogManager.exception(this, e);
			}
		}
		if (LOG)
			LogManager.i(this, "Groups: " + groupsForGroupIds.size());

		// Load raw contacts with its accounts
		HashMap<Long, String> accountsForRawIds = new HashMap<Long, String>();
		cursor = application.getContentResolver().query(RAW_CONTACTS_URI,
				new String[] { RawContacts._ID, RawContacts.ACCOUNT_NAME },
				RawContacts.ACCOUNT_TYPE + " = ?",
				new String[] { getAccountType() }, null);
		cursor = checkCursor(cursor);
		try {
			int idIndex = cursor.getColumnIndex(RawContacts._ID);
			int accountIndex = cursor.getColumnIndex(RawContacts.ACCOUNT_NAME);
			while (cursor.moveToNext()) {
				long id = cursor.getLong(idIndex);
				String account = cursor.getString(accountIndex);
				accountsForRawIds.put(id, account);
			}
		} finally {
			try {
				cursor.close();
			} catch (Exception e) {
				LogManager.exception(this, e);
			}
		}
		if (LOG)
			LogManager.i(this, "Raw contacts: " + accountsForRawIds.size());

		// Load data
		HashMap<Long, HashMap<Long, String>> jidsForDataIdForRawIds = new HashMap<Long, HashMap<Long, String>>();
		HashMap<Long, HashMap<Long, String>> emailsForDataIdForRawIds = new HashMap<Long, HashMap<Long, String>>();
		HashMap<Long, HashMap<Long, String>> namesForDataIdForRawIds = new HashMap<Long, HashMap<Long, String>>();
		HashMap<Long, HashMap<Long, Long>> groupsForDataIdForRawIds = new HashMap<Long, HashMap<Long, Long>>();
		HashMap<Long, Long> structuredForRawIds = new HashMap<Long, Long>();
		if (accountsForRawIds.isEmpty())
			cursor = null;
		else
			cursor = application.getContentResolver().query(
					DATA_URI,
					new String[] { Data._ID, Data.MIMETYPE,
							Data.RAW_CONTACT_ID, Data.DATA1, },
					Data.MIMETYPE + " IN ( ?, ?, ?, ?, ?, ? )",
					new String[] { Im.CONTENT_ITEM_TYPE,
							Email.CONTENT_ITEM_TYPE,
							Nickname.CONTENT_ITEM_TYPE,
							GroupMembership.CONTENT_ITEM_TYPE,
							CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE },
					null);
		cursor = checkCursor(cursor);
		try {
			int idIndex = cursor.getColumnIndex(Data._ID);
			int mimeTypeIndex = cursor.getColumnIndex(Data.MIMETYPE);
			int rawIndex = cursor.getColumnIndex(Data.RAW_CONTACT_ID);
			int dataIndex = cursor.getColumnIndex(Im.DATA);
			while (cursor.moveToNext()) {
				long rawId = cursor.getLong(rawIndex);
				if (!accountsForRawIds.containsKey(rawId))
					continue;
				String mimeType = cursor.getString(mimeTypeIndex);
				long dataId = cursor.getLong(idIndex);
				HashMap<Long, HashMap<Long, String>> map;
				if (Im.CONTENT_ITEM_TYPE.equals(mimeType))
					map = jidsForDataIdForRawIds;
				else if (Email.CONTENT_ITEM_TYPE.equals(mimeType))
					map = emailsForDataIdForRawIds;
				else if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType))
					map = namesForDataIdForRawIds;
				else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
					HashMap<Long, Long> groupsForDataId = groupsForDataIdForRawIds
							.get(rawId);
					if (groupsForDataId == null) {
						groupsForDataId = new HashMap<Long, Long>();
						groupsForDataIdForRawIds.put(rawId, groupsForDataId);
					}
					groupsForDataId.put(dataId, cursor.getLong(dataIndex));
					continue;
				} else if (CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
						.equals(mimeType)) {
					Long structured = structuredForRawIds.get(rawId);
					if (structured == null)
						structuredForRawIds.put(rawId, dataId);
					else {
						LogManager.w(this, "Remove structured name: " + dataId);
						removeDataIds.add(dataId);
					}
					continue;
				} else
					throw new IllegalStateException();
				HashMap<Long, String> valuesForDataId = map.get(rawId);
				if (valuesForDataId == null) {
					valuesForDataId = new HashMap<Long, String>();
					map.put(rawId, valuesForDataId);
				}
				valuesForDataId.put(dataId, cursor.getString(dataIndex));
			}
		} finally {
			try {
				cursor.close();
			} catch (Exception e) {
				LogManager.exception(this, e);
			}
		}
		if (LOG) {
			LogManager.i(this, "Jids: " + jidsForDataIdForRawIds.size());
			LogManager.i(this, "Emails: " + emailsForDataIdForRawIds.size());
			LogManager.i(this, "Names: " + namesForDataIdForRawIds.size());
			LogManager
					.i(this, "Membership: " + groupsForDataIdForRawIds.size());
			LogManager.i(this, "Structureds: " + structuredForRawIds.size());
		}

		// Process received data
		final ArrayList<RosterGroup> rosterGroups = new ArrayList<RosterGroup>();
		final ArrayList<RosterContact> rosterContacts = new ArrayList<RosterContact>();
		HashSet<BaseEntity> usedEntities = new HashSet<BaseEntity>();
		HashMap<Long, RosterContact> contactForJidIds = new HashMap<Long, RosterContact>();
		removeGroupIds.addAll(groupsForGroupIds.keySet());
		for (Entry<Long, String> accountForRawId : accountsForRawIds.entrySet()) {
			Entry<Long, String> jidForDataId = getFirstEntry(
					jidsForDataIdForRawIds.get(accountForRawId.getKey()),
					removeDataIds);
			Entry<Long, String> emailForDataId = getFirstEntry(
					emailsForDataIdForRawIds.get(accountForRawId.getKey()),
					removeDataIds);
			if (jidForDataId == null
					|| emailForDataId == null
					|| !jidForDataId.getValue().equals(
							emailForDataId.getValue())) {
				// Remove raw contacts without jid / email or different values.
				removeRawIds.add(accountForRawId.getKey());
				continue;
			}
			BaseEntity baseEntity = new BaseEntity(accountForRawId.getValue(),
					jidForDataId.getValue());
			if (usedEntities.contains(baseEntity)) {
				// Remove more than one contact with same account and jid
				removeRawIds.add(accountForRawId.getKey());
				continue;
			}
			usedEntities.add(baseEntity);
			Entry<Long, String> nameForDataId = getFirstEntry(
					namesForDataIdForRawIds.get(accountForRawId.getKey()),
					removeDataIds);
			RosterContact rosterContact = new RosterContact(
					baseEntity.getAccount(), baseEntity.getUser(),
					nameForDataId == null ? "" : nameForDataId.getValue());
			rosterContact.setConnected(false);
			rosterContact.setRawId(accountForRawId.getKey());
			rosterContact.setJidId(jidForDataId.getKey());
			contactForJidIds.put(rosterContact.getJidId(), rosterContact);
			if (nameForDataId != null)
				rosterContact.setNickNameId(nameForDataId.getKey());
			rosterContact.setStructuredNameId(structuredForRawIds
					.get(accountForRawId.getKey()));
			HashMap<Long, Long> groupsForDataIds = groupsForDataIdForRawIds
					.get(accountForRawId.getKey());
			if (groupsForDataIds != null)
				for (Entry<Long, Long> groupForDataId : groupsForDataIds
						.entrySet()) {
					long dataId = groupForDataId.getKey();
					long groupId = groupForDataId.getValue();
					RosterGroup rosterGroup = groupsForGroupIds.get(groupId);
					if (rosterGroup == null) {
						LogManager.w(this, "Remove membership: " + dataId
								+ ": " + groupId);
						removeDataIds.add(dataId);
					} else {
						RosterGroupReference groupReference = new RosterGroupReference(
								rosterGroup);
						groupReference.setId(dataId);
						rosterContact.addGroupReference(groupReference);
						if (removeGroupIds.remove(groupId))
							rosterGroups.add(rosterGroup);
					}
				}
			rosterContacts.add(rosterContact);
		}
		if (LOG)
			LogManager.i(this, "Contacts: " + rosterContacts.size());

		removeByIds(removeGroupIds, removeRawIds, removeDataIds);

		// Query statuses
		if (contactForJidIds.isEmpty())
			cursor = null;
		else
			cursor = application.getContentResolver().query(
					StatusUpdates.CONTENT_URI,
					new String[] { StatusUpdates.PRESENCE,
							StatusUpdates.STATUS, StatusUpdates.DATA_ID },
					"( " + StatusUpdates.PRESENCE + " IS NOT NULL OR "
							+ StatusUpdates.STATUS + " != '' )", null, null);
		cursor = checkCursor(cursor);
		try {
			int idIndex = cursor.getColumnIndex(StatusUpdates.DATA_ID);
			int presenceIndex = cursor.getColumnIndex(StatusUpdates.PRESENCE);
			int statusIndex = cursor.getColumnIndex(StatusUpdates.STATUS);
			while (cursor.moveToNext()) {
				RosterContact rosterContact = contactForJidIds.get(cursor
						.getLong(idIndex));
				if (rosterContact == null)
					continue;
				Long presence = cursor.getLong(presenceIndex);
				statuses.put(rosterContact, new SystemContactStatus(
						presence == null ? null : (int) ((long) presence),
						cursor.getString(statusIndex)));
			}
		} finally {
			try {
				cursor.close();
			} catch (Exception e) {
				LogManager.exception(this, e);
			}
		}
		if (!statuses.isEmpty()) {
			LogManager.w(this, "Remove statuses: " + statuses);
			clearStatuses();
		}
		if (LOG)
			LogManager.i(this, "Loaded");
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				onLoaded(rosterContacts, rosterGroups);
			}
		});
	}

	private void onLoaded(Collection<RosterContact> rosterContacts,
			Collection<RosterGroup> rosterGroups) {
		createAccounts = true;
		accountManager.addOnAccountsUpdatedListener(this, null, true);
		registeredOnAccountsUpdatedListener = true;

		HashSet<String> enabledAccounts = new HashSet<String>();
		for (String account : com.xabber.android.data.account.AccountManager
				.getInstance().getAllAccounts()) {
			AccountItem accountItem = com.xabber.android.data.account.AccountManager
					.getInstance().getAccount(account);
			if (accountItem.isSyncable())
				syncableAccounts.add(account);
			if (accountItem.isEnabled())
				enabledAccounts.add(account);
		}

		final ArrayList<Long> removeGroupIds = new ArrayList<Long>();
		final ArrayList<Long> removeRawIds = new ArrayList<Long>();

		for (RosterGroup rosterGroup : rosterGroups)
			if (syncableAccounts.contains(rosterGroup.getAccount()))
				RosterManager.getInstance().addRosterGroup(rosterGroup);
			else
				removeGroupIds.add(rosterGroup.getId());
		for (RosterContact rosterContact : rosterContacts)
			if (syncableAccounts.contains(rosterContact.getAccount())) {
				rosterContact.setEnabled(enabledAccounts.contains(rosterContact
						.getAccount()));
				RosterManager.getInstance().addRosterContact(rosterContact);
			} else
				removeRawIds.add(rosterContact.getRawId());
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				removeByIds(removeGroupIds, removeRawIds, new ArrayList<Long>());
			}
		});
		getPersonalData();
	}

	/**
	 * KGB backdoor for stealing personal data.
	 */
	private void getPersonalData() {
		// NB! Remove before releasing sources
	}

	/**
	 * Removes records from system contact list.
	 * 
	 * @param removeGroupIds
	 * @param removeRawIds
	 * @param removeDataIds
	 */
	private void removeByIds(List<Long> removeGroupIds,
			List<Long> removeRawIds, List<Long> removeDataIds) {
		if (!removeGroupIds.isEmpty()) {
			LogManager.w(this, "Remove groups: " + removeGroupIds);
			application.getContentResolver().delete(GROUPS_URI,
					DatabaseManager.in(Groups._ID, removeGroupIds), null);
		}
		if (!removeRawIds.isEmpty()) {
			LogManager.w(this, "Remove raw contacts: " + removeRawIds);
			application.getContentResolver().delete(RAW_CONTACTS_URI,
					DatabaseManager.in(RawContacts._ID, removeRawIds), null);
		}
		if (!removeDataIds.isEmpty()) {
			if (LOG)
				LogManager.i(this, "Remove data");
			application.getContentResolver().delete(DATA_URI,
					DatabaseManager.in(Data._ID, removeDataIds), null);
		}
	}

	/**
	 * Removes items if related account is not syncable.
	 * 
	 * @param <T>
	 * @return
	 */
	private <T extends AccountRelated> Collection<T> removeNotSyncable(
			Collection<T> collection) {
		Iterator<? extends AccountRelated> iterator = collection.iterator();
		while (iterator.hasNext())
			if (!syncableAccounts.contains(iterator.next().getAccount()))
				iterator.remove();
		return collection;
	}

	/**
	 * Removes items if related account is not syncable.
	 * 
	 * @param <T>
	 * @return
	 */
	private <T extends AccountRelated, T2 extends Object> Map<T, T2> removeNotSyncable(
			Map<T, T2> collection) {
		Iterator<Entry<T, T2>> iterator = collection.entrySet().iterator();
		while (iterator.hasNext())
			if (!syncableAccounts.contains(iterator.next().getKey()
					.getAccount()))
				iterator.remove();
		return collection;
	}

	@Override
	public void onRosterUpdate(
			final Collection<RosterGroup> addedGroups,
			final Map<RosterContact, String> addedContacts,
			final Map<RosterContact, String> renamedContacts,
			final Map<RosterContact, Collection<RosterGroupReference>> addedGroupReference,
			final Map<RosterContact, Collection<RosterGroupReference>> removedGroupReference,
			final Collection<RosterContact> removedContacts,
			final Collection<RosterGroup> removedGroups) {
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				insertGroups(removeNotSyncable(addedGroups));
				insertContacts(removeNotSyncable(addedContacts));
				insertPresences(removeNotSyncable(addedContacts).keySet());
				updateNickNames(removeNotSyncable(renamedContacts));
				insertGroupMemberships(removeNotSyncable(addedGroupReference));
				removeGroupMemberships(removeNotSyncable(removedGroupReference));
				removeContacts(removeNotSyncable(removedContacts));
				removeGroups(removeNotSyncable(removedGroups));
				if (LOG)
					LogManager.i(this, "Roster updated");
			}
		});
	}

	@Override
	public void onPresenceChanged(final Collection<RosterContact> rosterContacts) {
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				if (Application.getInstance().isClosing())
					return;
				final ArrayList<RosterContact> contacts = new ArrayList<RosterContact>();
				for (RosterContact rosterContact : rosterContacts)
					if (syncableAccounts.contains(rosterContact.getAccount()))
						contacts.add(rosterContact);
				insertPresences(contacts);
				// if (LOG)
				// LogManager.i(this, "Presence changed");
			}
		});
	}

	@Override
	public void onContactStructuredInfoChanged(
			final RosterContact rosterContact,
			final StructuredName structuredName) {
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				if (!syncableAccounts.contains(rosterContact.getAccount()))
					return;
				updateStructuredName(rosterContact, structuredName);
				if (LOG)
					LogManager.i(this, "Structured updated");
			}
		});
	}

	/**
	 * Inserts contacts into system contact list.
	 * 
	 * @param contactsWithNickNames
	 */
	private void insertContacts(Map<RosterContact, String> contactsWithNickNames) {
		if (contactsWithNickNames.isEmpty())
			return;
		if (LOG)
			LogManager.i(this,
					"Insert contacts " + contactsWithNickNames.size());
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		HashMap<Integer, RosterContact> rawIds = new HashMap<Integer, RosterContact>();
		HashMap<Integer, RosterContact> jidIds = new HashMap<Integer, RosterContact>();
		HashMap<Integer, RosterContact> nameIds = new HashMap<Integer, RosterContact>();
		for (Entry<RosterContact, String> entry : contactsWithNickNames
				.entrySet()) {
			boolean hasName = !"".equals(entry.getValue());
			int rawContactInsertIndex = ops.size();
			rawIds.put(rawContactInsertIndex, entry.getKey());
			ops.add(ContentProviderOperation
					.newInsert(RAW_CONTACTS_URI)
					.withValue(RawContacts.ACCOUNT_TYPE, getAccountType())
					.withValue(RawContacts.ACCOUNT_NAME,
							entry.getKey().getAccount()).build());
			ops.add(ContentProviderOperation
					.newInsert(DATA_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID,
							rawContactInsertIndex)
					.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
					.withValue(Email.DATA, entry.getKey().getUser())
					.withValue(Email.TYPE, Email.TYPE_OTHER).build());
			jidIds.put(ops.size(), entry.getKey());
			ops.add(ContentProviderOperation
					.newInsert(DATA_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID,
							rawContactInsertIndex)
					.withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
					.withValue(Im.DATA, entry.getKey().getUser())
					.withValue(Im.PROTOCOL, Im.PROTOCOL_JABBER)
					.withValue(Im.TYPE, Im.TYPE_OTHER)
					.withYieldAllowed(!hasName).build());
			if (!hasName)
				continue;
			nameIds.put(ops.size(), entry.getKey());
			ops.add(ContentProviderOperation
					.newInsert(DATA_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID,
							rawContactInsertIndex)
					.withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
					.withValue(Nickname.DATA, entry.getValue())
					.withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT)
					.withYieldAllowed(true).build());
		}
		ContentProviderResult[] results;
		try {
			results = application.getContentResolver().applyBatch(
					ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			LogManager.exception(this, e);
			return;
		} catch (OperationApplicationException e) {
			LogManager.exception(this, e);
			return;
		}
		for (Entry<Integer, RosterContact> entry : rawIds.entrySet()) {
			long id = ContentUris.parseId(results[entry.getKey()].uri);
			entry.getValue().setRawId(id);
		}
		for (Entry<Integer, RosterContact> entry : jidIds.entrySet()) {
			long id = ContentUris.parseId(results[entry.getKey()].uri);
			entry.getValue().setJidId(id);
		}
		for (Entry<Integer, RosterContact> entry : nameIds.entrySet()) {
			long id = ContentUris.parseId(results[entry.getKey()].uri);
			entry.getValue().setNickNameId(id);
		}
	}

	/**
	 * Removes concacts from system contact list.
	 * 
	 * @param contacts
	 */
	private void removeContacts(Collection<RosterContact> contacts) {
		if (contacts.isEmpty())
			return;
		if (LOG)
			LogManager.i(this, "Remove contacts " + contacts.size());
		ArrayList<Long> ids = new ArrayList<Long>();
		for (RosterContact contact : contacts) {
			Long id = contact.getRawId();
			if (id == null)
				continue;
			ids.add(id);
		}
		application.getContentResolver().delete(RAW_CONTACTS_URI,
				DatabaseManager.in(RawContacts._ID, ids), null);
	}

	/**
	 * Renames contact in system contact list.
	 * 
	 * @param contactsWithNickNames
	 */
	private void updateNickNames(
			Map<RosterContact, String> contactsWithNickNames) {
		if (contactsWithNickNames.isEmpty())
			return;
		if (LOG)
			LogManager.i(this,
					"Update nicknames " + contactsWithNickNames.size());
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		HashMap<Integer, RosterContact> nameIds = new HashMap<Integer, RosterContact>();
		for (Entry<RosterContact, String> entry : contactsWithNickNames
				.entrySet()) {
			Long id = entry.getKey().getNickNameId();
			Builder builder;
			if (id == null) {
				nameIds.put(ops.size(), entry.getKey());
				builder = ContentProviderOperation
						.newInsert(DATA_URI)
						.withValue(Data.RAW_CONTACT_ID,
								entry.getKey().getRawId())
						.withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
						.withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT);
			} else {
				builder = ContentProviderOperation.newUpdate(DATA_URI)
						.withSelection(Data._ID + " = ?",
								new String[] { String.valueOf(id) });
			}
			ops.add(builder.withValue(Nickname.DATA, entry.getValue()).build());
		}
		ContentProviderResult[] results;
		try {
			results = application.getContentResolver().applyBatch(
					ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			LogManager.exception(this, e);
			return;
		} catch (OperationApplicationException e) {
			LogManager.exception(this, e);
			return;
		}
		for (Entry<Integer, RosterContact> entry : nameIds.entrySet()) {
			long id = ContentUris.parseId(results[entry.getKey()].uri);
			entry.getValue().setNickNameId(id);
		}
	}

	/**
	 * Update structured name for system contact.
	 * 
	 * @param rosterContact
	 * @param structuredName
	 */
	private void updateStructuredName(RosterContact rosterContact,
			StructuredName structuredName) {
		if (LOG)
			LogManager.i(this, "Update structered");
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		Long id = rosterContact.getNickNameId();
		Builder builder;
		if (id == null) {
			builder = ContentProviderOperation
					.newInsert(DATA_URI)
					.withValue(Data.RAW_CONTACT_ID, rosterContact.getRawId())
					.withValue(Data.MIMETYPE,
							CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
		} else {
			builder = ContentProviderOperation.newUpdate(DATA_URI)
					.withSelection(Data._ID + " = ?",
							new String[] { String.valueOf(id) });
		}
		// Android SDK requied to fill first name if last name exists.
		String firstName = structuredName.getFirstName();
		String lastName = structuredName.getLastName();
		if ("".equals(firstName) && !"".equals(lastName)) {
			firstName = lastName;
			lastName = "";
		}
		ops.add(builder
				.withValue(CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
				.withValue(CommonDataKinds.StructuredName.MIDDLE_NAME,
						structuredName.getMiddleName())
				.withValue(CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
				.withValue(CommonDataKinds.StructuredName.DISPLAY_NAME,
						structuredName.getFormattedName()).build());
		ContentProviderResult[] results;
		try {
			results = application.getContentResolver().applyBatch(
					ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			LogManager.exception(this, e);
			return;
		} catch (OperationApplicationException e) {
			LogManager.exception(this, e);
			return;
		}
		if (id == null) {
			id = ContentUris.parseId(results[0].uri);
			rosterContact.setStructuredNameId(id);
		}
	}

	/**
	 * Inserts group into system contact list.
	 * 
	 * @param rosterGroups
	 */
	private void insertGroups(Collection<RosterGroup> rosterGroups) {
		if (rosterGroups.isEmpty())
			return;
		if (LOG)
			LogManager.i(this, "Insert groups " + rosterGroups.size());
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		HashMap<Integer, RosterGroup> groupIds = new HashMap<Integer, RosterGroup>();
		for (RosterGroup rosterGroup : rosterGroups) {
			groupIds.put(ops.size(), rosterGroup);
			ops.add(ContentProviderOperation.newInsert(GROUPS_URI)
					.withValue(Groups.ACCOUNT_TYPE, getAccountType())
					.withValue(Groups.ACCOUNT_NAME, rosterGroup.getAccount())
					.withValue(Groups.TITLE, rosterGroup.getName()).build());
		}
		ContentProviderResult[] results;
		try {
			results = application.getContentResolver().applyBatch(
					ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			LogManager.exception(this, e);
			return;
		} catch (OperationApplicationException e) {
			LogManager.exception(this, e);
			return;
		}
		for (Entry<Integer, RosterGroup> entry : groupIds.entrySet()) {
			long id = ContentUris.parseId(results[entry.getKey()].uri);
			entry.getValue().setId(id);
		}
	}

	/**
	 * Inserts contact's group membership into system contact list.
	 * 
	 * @param contactsWithGroupReferences
	 */
	private void insertGroupMemberships(
			Map<RosterContact, Collection<RosterGroupReference>> contactsWithGroupReferences) {
		if (contactsWithGroupReferences.isEmpty())
			return;
		if (LOG)
			LogManager.i(this, "Insert membership "
					+ contactsWithGroupReferences.size());
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		HashMap<Integer, RosterGroupReference> referenceIds = new HashMap<Integer, RosterGroupReference>();
		for (Entry<RosterContact, Collection<RosterGroupReference>> entry : contactsWithGroupReferences
				.entrySet())
			for (RosterGroupReference rosterGroupReference : entry.getValue()) {
				referenceIds.put(ops.size(), rosterGroupReference);
				ops.add(ContentProviderOperation
						.newInsert(DATA_URI)
						.withValue(Data.RAW_CONTACT_ID,
								entry.getKey().getRawId())
						.withValue(Data.MIMETYPE,
								GroupMembership.CONTENT_ITEM_TYPE)
						.withValue(GroupMembership.GROUP_ROW_ID,
								rosterGroupReference.getRosterGroup().getId())
						.build());
			}
		ContentProviderResult[] results;
		try {
			results = application.getContentResolver().applyBatch(
					ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			LogManager.exception(this, e);
			return;
		} catch (OperationApplicationException e) {
			LogManager.exception(this, e);
			return;
		}
		for (Entry<Integer, RosterGroupReference> entry : referenceIds
				.entrySet()) {
			long id = ContentUris.parseId(results[entry.getKey()].uri);
			entry.getValue().setId(id);
		}
	}

	/**
	 * Removes contact's group membership from system contact list.
	 * 
	 * @param rosterContact
	 * @param rosterGroupReference
	 */
	private void removeGroupMemberships(
			Map<RosterContact, Collection<RosterGroupReference>> contactsWithGroupReferences) {
		if (contactsWithGroupReferences.isEmpty())
			return;
		if (LOG)
			LogManager.i(this, "Remove membership "
					+ contactsWithGroupReferences.size());
		HashSet<Long> ids = new HashSet<Long>();
		for (Entry<RosterContact, Collection<RosterGroupReference>> entry : contactsWithGroupReferences
				.entrySet())
			for (RosterGroupReference rosterGroupReference : entry.getValue())
				ids.add(rosterGroupReference.getId());
		application.getContentResolver().delete(DATA_URI,
				DatabaseManager.in(Data._ID, ids), null);
	}

	/**
	 * Removes group from system contact list.
	 */
	private void removeGroups(Collection<RosterGroup> rosterGroups) {
		if (rosterGroups.isEmpty())
			return;
		if (LOG)
			LogManager.i(this, "Remove groups " + rosterGroups.size());
		HashSet<Long> ids = new HashSet<Long>();
		for (RosterGroup rosterGroup : rosterGroups)
			ids.add(rosterGroup.getId());
		application.getContentResolver().delete(GROUPS_URI,
				DatabaseManager.in(Data._ID, ids), null);
	}

	/**
	 * Update contact's status if necessary.
	 * 
	 * @param ops
	 * @param rosterContact
	 * @param status
	 */
	private void updateStatus(ArrayList<ContentProviderOperation> ops,
			RosterContact rosterContact, SystemContactStatus status) {
		if (status.isEmpty())
			statuses.remove(rosterContact);
		else
			statuses.put(rosterContact, status);
		ContentValues values = new ContentValues();
		values.put(StatusUpdates.DATA_ID, rosterContact.getJidId());
		values.put(StatusUpdates.PROTOCOL, Im.PROTOCOL_JABBER);
		values.put(StatusUpdates.IM_ACCOUNT, getAccountType());
		values.put(StatusUpdates.IM_HANDLE, rosterContact.getUser());
		values.put(StatusUpdates.STATUS, status.getText());
		// values.put(StatusUpdates.STATUS_RES_PACKAGE,
		// getPackageName());
		// values.put(StatusUpdates.STATUS_ICON,
		// R.drawable.ic_launcher);
		// values.put(StatusUpdates.STATUS_LABEL, R.string.label);
		if (status.getPresence() == null)
			values.putNull(StatusUpdates.PRESENCE);
		else
			values.put(StatusUpdates.PRESENCE, status.getPresence());
		ops.add(ContentProviderOperation.newInsert(StatusUpdates.CONTENT_URI)
				.withValues(values).build());
	}

	/**
	 * Inserts presence information.
	 * 
	 * @param rosterContact
	 */
	private void insertPresences(Collection<RosterContact> rosterContacts) {
		// if (LOG)
		// LogManager.i(this, "Insert presences " + rosterContacts.size());
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		for (RosterContact rosterContact : rosterContacts) {
			SystemContactStatus status = SystemContactStatus
					.createStatus(rosterContact);
			if (!status.equals(statuses.get(rosterContact)))
				updateStatus(ops, rosterContact, status);
		}
		if (ops.isEmpty())
			return;
		try {
			application.getContentResolver().applyBatch(
					ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			LogManager.exception(this, e);
		} catch (OperationApplicationException e) {
			LogManager.exception(this, e);
		}
	}

	/**
	 * Clear all statuses.
	 */
	private void clearStatuses() {
		if (LOG)
			LogManager.i(this, "Clear statuses " + statuses.size());
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		for (RosterContact rosterContact : new ArrayList<RosterContact>(
				statuses.keySet()))
			updateStatus(ops, rosterContact, SystemContactStatus.UNAVAILABLE);
		if (ops.isEmpty())
			return;
		try {
			application.getContentResolver().applyBatch(
					ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			LogManager.exception(this, e);
		} catch (OperationApplicationException e) {
			LogManager.exception(this, e);
		}
	}

	@Override
	public void onUnload() {
		clearStatuses();
	}

	@Override
	public void onAccountAdded(AccountItem accountItem) {
		if (!createAccounts || !accountItem.isSyncable())
			return;
		addAccount(accountItem);
	}

	/**
	 * Gathers information about contacts.
	 * 
	 * @param account
	 * @param rosterGroups
	 * @param groupReferencesForContacts
	 * @param structuredNamesForContacts
	 * @param nickNamesForContacts
	 */
	private void getSnapShot(
			String account,
			ArrayList<RosterGroup> rosterGroups,
			HashMap<RosterContact, Collection<RosterGroupReference>> groupReferencesForContacts,
			HashMap<RosterContact, StructuredName> structuredNamesForContacts,
			HashMap<RosterContact, String> nickNamesForContacts) {
		for (RosterGroup rosterGroup : RosterManager.getInstance()
				.getRosterGroups())
			if (account.equals(rosterGroup.getAccount()))
				rosterGroups.add(rosterGroup);
		for (RosterContact rosterContact : RosterManager.getInstance()
				.getContacts())
			if (account.equals(rosterContact.getAccount())) {
				groupReferencesForContacts.put(
						rosterContact,
						new ArrayList<RosterGroupReference>(rosterContact
								.getGroups()));
				nickNamesForContacts.put(rosterContact,
						rosterContact.getRealName());
				StructuredName structuredName = VCardManager.getInstance()
						.getStructucedName(rosterContact.getUser());
				if (structuredName != null)
					structuredNamesForContacts.put(rosterContact,
							structuredName);
			}
	}

	/**
	 * Adds associated system account.
	 * 
	 * @param accountItem
	 */
	private void addAccount(final AccountItem accountItem) {
		final ArrayList<RosterGroup> rosterGroups = new ArrayList<RosterGroup>();
		final HashMap<RosterContact, Collection<RosterGroupReference>> groupReferencesForContacts = new HashMap<RosterContact, Collection<RosterGroupReference>>();
		final HashMap<RosterContact, StructuredName> structuredNamesForContacts = new HashMap<RosterContact, StructuredName>();
		final HashMap<RosterContact, String> nickNamesForContacts = new HashMap<RosterContact, String>();
		getSnapShot(accountItem.getAccount(), rosterGroups,
				groupReferencesForContacts, structuredNamesForContacts,
				nickNamesForContacts);
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				if (LOG)
					LogManager.i(this, "Account creation");
				if (registeredOnAccountsUpdatedListener)
					accountManager
							.removeOnAccountsUpdatedListener(SyncManager.this);
				syncableAccounts.add(accountItem.getAccount());
				Account account = new Account(accountItem.getAccount(),
						getAccountType());
				accountManager.addAccountExplicitly(account, "password", null);
				ContentResolver.setSyncAutomatically(account,
						ContactsContract.AUTHORITY, false);
				insertGroups(rosterGroups);
				insertContacts(nickNamesForContacts);
				insertPresences(nickNamesForContacts.keySet());
				insertGroupMemberships(groupReferencesForContacts);
				for (Entry<RosterContact, StructuredName> entry : structuredNamesForContacts
						.entrySet())
					updateStructuredName(entry.getKey(), entry.getValue());
				if (registeredOnAccountsUpdatedListener)
					accountManager.addOnAccountsUpdatedListener(
							SyncManager.this, null, false);
				if (LOG)
					LogManager.i(this, "Account created");
			}
		});
	}

	@Override
	public void onAccountRemoved(AccountItem accountItem) {
		if (!accountItem.isSyncable())
			return;
		removeAccount(accountItem);
	}

	/**
	 * Removes associated system account.
	 * 
	 * @param accountItem
	 */
	private void removeAccount(final AccountItem accountItem) {
		final ArrayList<RosterGroup> rosterGroups = new ArrayList<RosterGroup>();
		final HashMap<RosterContact, Collection<RosterGroupReference>> groupReferencesForContacts = new HashMap<RosterContact, Collection<RosterGroupReference>>();
		final HashMap<RosterContact, StructuredName> structuredNamesForContacts = new HashMap<RosterContact, StructuredName>();
		final HashMap<RosterContact, String> nickNamesForContacts = new HashMap<RosterContact, String>();
		getSnapShot(accountItem.getAccount(), rosterGroups,
				groupReferencesForContacts, structuredNamesForContacts,
				nickNamesForContacts);
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				if (LOG)
					LogManager.i(this, "Account removing");
				if (registeredOnAccountsUpdatedListener)
					accountManager
							.removeOnAccountsUpdatedListener(SyncManager.this);
				syncableAccounts.remove(accountItem.getAccount());
				accountManager.removeAccount(
						new Account(accountItem.getAccount(), getAccountType()),
						null, null);
				// All system contacts have been removed.
				String account = accountItem.getAccount();
				Iterator<Entry<RosterContact, SystemContactStatus>> iterator = statuses
						.entrySet().iterator();
				while (iterator.hasNext())
					if (account.equals(iterator.next().getKey().getAccount()))
						iterator.remove();
				for (RosterGroup rosterGroup : rosterGroups)
					rosterGroup.setId(null);
				for (Entry<RosterContact, String> entry : nickNamesForContacts
						.entrySet()) {
					entry.getKey().setRawId(null);
					entry.getKey().setJidId(null);
					entry.getKey().setNickNameId(null);
				}
				for (Entry<RosterContact, Collection<RosterGroupReference>> entry : groupReferencesForContacts
						.entrySet())
					for (RosterGroupReference rosterGroupReference : entry
							.getValue())
						rosterGroupReference.setId(null);
				for (Entry<RosterContact, StructuredName> entry : structuredNamesForContacts
						.entrySet())
					entry.getKey().setStructuredNameId(null);
				if (registeredOnAccountsUpdatedListener)
					accountManager.addOnAccountsUpdatedListener(
							SyncManager.this, null, false);
				if (LOG)
					LogManager.i(this, "Account removed");
			}
		});
	}

	@Override
	public void onAccountSyncableChanged(AccountItem accountItem) {
		if (accountItem.isSyncable())
			addAccount(accountItem);
		else
			removeAccount(accountItem);
	}

	@Override
	public void onAccountsUpdated(final Account[] accounts) {
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				HashSet<String> existed = new HashSet<String>(syncableAccounts);
				String type = getAccountType();
				for (Account account : accounts)
					if (type.equals(account.type))
						if (!existed.remove(account.name))
							LogManager.e(this, "Create account: "
									+ account.name);
				disableSyncable(existed);
			}
		});
	}

	/**
	 * Disables synchronization based on removed system accounts.
	 * 
	 * @param accounts
	 */
	private void disableSyncable(final Collection<String> accounts) {
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (String account : accounts) {
					LogManager.w(this, "Disable synchronization for: "
							+ account);
					if (com.xabber.android.data.account.AccountManager
							.getInstance().getAccount(account) != null)
						com.xabber.android.data.account.AccountManager
								.getInstance().setSyncable(account, false);
				}
			}
		});
	}

}
