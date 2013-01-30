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

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.ui.helper.BaseSettingsActivity;
import com.xabber.androiddev.R;

public class ChatEditor extends BaseSettingsActivity {

	private String account;
	private String user;

	@Override
	protected void onInflate(Bundle savedInstanceState) {
		account = getAccount(getIntent());
		user = getUser(getIntent());
		AccountItem accountItem = AccountManager.getInstance().getAccount(
				account);
		if (accountItem == null || user == null) {
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
			finish();
			return;
		}
		addPreferencesFromResource(R.xml.chat_editor);
		if (accountItem.getArchiveMode() == ArchiveMode.server
				|| accountItem.getArchiveMode() == ArchiveMode.dontStore)
			getPreferenceScreen().removePreference(
					getPreferenceScreen().findPreference(
							getString(R.string.chat_save_history_key)));
	}

	@Override
	protected Map<String, Object> getValues() {
		Map<String, Object> map = new HashMap<String, Object>();
		putValue(map, R.string.chat_save_history_key, ChatManager.getInstance()
				.isSaveMessages(account, user));
		putValue(map, R.string.chat_events_visible_chat_key, ChatManager
				.getInstance().isNotifyVisible(account, user));
		putValue(map, R.string.chat_events_show_text_key, ChatManager
				.getInstance().isShowText(account, user));
		putValue(map, R.string.chat_events_vibro_key, ChatManager.getInstance()
				.isMakeVibro(account, user));
		putValue(map, R.string.chat_events_sound_key, ChatManager.getInstance()
				.getSound(account, user));
		return map;
	}

	@Override
	protected boolean setValues(Map<String, Object> source,
			Map<String, Object> result) {
		if (hasChanges(source, result, R.string.chat_save_history_key))
			ChatManager.getInstance().setSaveMessages(account, user,
					getBoolean(result, R.string.chat_save_history_key));

		if (hasChanges(source, result, R.string.chat_events_visible_chat_key))
			ChatManager.getInstance().setNotifyVisible(account, user,
					getBoolean(result, R.string.chat_events_visible_chat_key));

		if (hasChanges(source, result, R.string.chat_events_show_text_key))
			ChatManager.getInstance().setShowText(account, user,
					getBoolean(result, R.string.chat_events_show_text_key));

		if (hasChanges(source, result, R.string.chat_events_vibro_key))
			ChatManager.getInstance().setMakeVibro(account, user,
					getBoolean(result, R.string.chat_events_vibro_key));

		if (hasChanges(source, result, R.string.chat_events_sound_key))
			ChatManager.getInstance().setSound(account, user,
					getUri(result, R.string.chat_events_sound_key));

		return true;
	}

	public static Intent createIntent(Context context, String account,
			String user) {
		return new EntityIntentBuilder(context, ChatEditor.class)
				.setAccount(account).setUser(user).build();
	}

	private static String getAccount(Intent intent) {
		return EntityIntentBuilder.getAccount(intent);
	}

	private static String getUser(Intent intent) {
		return EntityIntentBuilder.getUser(intent);
	}

}
