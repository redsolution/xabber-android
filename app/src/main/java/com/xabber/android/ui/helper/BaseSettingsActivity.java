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
package com.xabber.android.ui.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.xabber.android.ui.widget.RingtonePreference;
import com.xabber.androiddev.R;

/**
 * Provide possibility to edit, apply and discard settings.
 * 
 * String resource id is used to identify preferences.
 * 
 * @author alexander.ivanov
 * 
 * @param <T>
 */
public abstract class BaseSettingsActivity extends ManagedPreferenceActivity
		implements Preference.OnPreferenceChangeListener {

	private static final int MENU_SAVE = Menu.FIRST;
	private static final int MENU_CANCEL = Menu.FIRST + 1;
	private static final int CONFIRM_DIALOG_ID = 0;

	/**
	 * Set of initial values is in progress.
	 */
	private boolean initialChange;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;
		onInflate(savedInstanceState);
		if (isFinishing())
			return;
		PreferenceSummaryHelper.updateSummary(getPreferenceScreen());
		if (savedInstanceState == null)
			operation(Operation.read);
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		initialChange = true;
		for (int index = 0; index < preferenceScreen.getPreferenceCount(); index++) {
			Preference preference = preferenceScreen.getPreference(index);
			preference.setOnPreferenceChangeListener(this);
			if (preference instanceof EditTextPreference)
				onPreferenceChange(preference,
						((EditTextPreference) preference).getText());
			else if (preference instanceof CheckBoxPreference)
				onPreferenceChange(preference,
						((CheckBoxPreference) preference).isChecked());
			else if (preference instanceof ListPreference)
				onPreferenceChange(preference,
						((ListPreference) preference).getValue());
		}
		initialChange = false;
	}

	protected boolean isInitialChange() {
		return initialChange;
	}

	/**
	 * Inflates layout.
	 * 
	 * @param savedInstanceState
	 */
	protected abstract void onInflate(Bundle savedInstanceState);

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SAVE, 0, android.R.string.ok).setIcon(
				android.R.drawable.ic_menu_save);
		menu.add(0, MENU_CANCEL, 0, android.R.string.cancel).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SAVE:
			if (operation(Operation.save))
				finish();
			return true;
		case MENU_CANCEL:
			if (operation(Operation.discard))
				finish();
			else
				showDialog(CONFIRM_DIALOG_ID);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference instanceof EditTextPreference)
			preference.setSummary((String) newValue);
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == CONFIRM_DIALOG_ID) {
			return new AlertDialog.Builder(this)
					.setTitle(android.R.string.dialog_alert_title)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(R.string.confirm_cancellation)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int w) {
									finish();
								}
							}).setNegativeButton(android.R.string.no, null)
					.create();
		}
		return super.onCreateDialog(id);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (operation(Operation.save))
				finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Possible operations.
	 */
	private static enum Operation {
		save, discard, read
	};

	protected void putValue(Map<String, Object> map, int resoureId, Object value) {
		map.put(getString(resoureId), value);
	}

	protected String getString(Map<String, Object> map, int resoureId) {
		return (String) map.get(getString(resoureId));
	}

	protected int getInt(Map<String, Object> map, int resoureId) {
		return (Integer) map.get(getString(resoureId));
	}

	protected boolean getBoolean(Map<String, Object> map, int resoureId) {
		return (Boolean) map.get(getString(resoureId));
	}

	protected Uri getUri(Map<String, Object> map, int resoureId) {
		return (Uri) map.get(getString(resoureId));
	}

	/**
	 * @param intent
	 * @return Whether an operation succeed.
	 */
	private boolean operation(Operation selected) {
		Map<String, Object> source = getValues();
		if (selected == Operation.read)
			setPreferences(source);
		else {
			Map<String, Object> result = getPreferences(source);
			for (Entry<String, Object> entry : source.entrySet())
				if (!result.containsKey(entry.getKey()))
					result.put(entry.getKey(), entry.getValue());
			if (selected == Operation.save)
				return setValues(source, result);
			else if (selected == Operation.discard) {
				for (String key : source.keySet())
					if (hasChanges(source, result, key))
						return false;
			} else
				throw new IllegalStateException();
		}
		return true;
	}

	/**
	 * @param source
	 * @param result
	 * @param key
	 * @return Whether value has been changed.
	 */
	protected boolean hasChanges(Map<String, Object> source,
			Map<String, Object> result, String key) {
		Object sourceValue = source.get(key);
		Object targetValue = result.get(key);
		return (sourceValue == null && targetValue != null)
				|| (sourceValue != null && !sourceValue.equals(targetValue));
	}

	/**
	 * @param source
	 * @param result
	 * @param resourceId
	 * @return Whether value has been changed.
	 */
	protected boolean hasChanges(Map<String, Object> source,
			Map<String, Object> result, int resourceId) {
		return hasChanges(source, result, getString(resourceId));
	}

	/**
	 * @return Map with source values.
	 */
	protected abstract Map<String, Object> getValues();

	/**
	 * Set values to the UI elements.
	 * 
	 * @param source
	 */
	protected void setPreferences(Map<String, Object> source) {
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		for (int index = 0; index < preferenceScreen.getPreferenceCount(); index++) {
			Preference preference = preferenceScreen.getPreference(index);
			Object value = source.get(preference.getKey());
			setPreference(preference, value);
		}
	}

	/**
	 * Set value to the UI element.
	 * 
	 * @param preference
	 * @param value
	 */
	protected void setPreference(Preference preference, Object value) {
		if (preference instanceof EditTextPreference)
			((EditTextPreference) preference)
					.setText(value instanceof Integer ? String.valueOf(value)
							: (String) value);
		else if (preference instanceof CheckBoxPreference)
			((CheckBoxPreference) preference).setChecked((Boolean) value);
		else if (preference instanceof ListPreference)
			((ListPreference) preference).setValueIndex((Integer) value);
		else if (preference instanceof RingtonePreference)
			((RingtonePreference) preference).setUri((Uri) value);
	}

	/**
	 * Get values from the UI elements.
	 * 
	 * @param source
	 * @return
	 */
	protected Map<String, Object> getPreferences(Map<String, Object> source) {
		Map<String, Object> result = new HashMap<String, Object>();
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		for (int index = 0; index < preferenceScreen.getPreferenceCount(); index++) {
			Preference preference = preferenceScreen.getPreference(index);
			result.put(preference.getKey(), getPrefecence(preference, source));
		}
		return result;
	}

	/**
	 * Get value from the UI element.
	 * 
	 * @param preference
	 * @param source
	 * @return
	 */
	protected Object getPrefecence(Preference preference,
			Map<String, Object> source) {
		if (preference instanceof PreferenceScreen)
			return null;
		else if (preference instanceof EditTextPreference) {
			String value = ((EditTextPreference) preference).getText();
			if (source.get(preference.getKey()) instanceof Integer)
				try {
					return Integer.parseInt(value);
				} catch (Exception NumberFormatException) {
					return null;
				}
			else
				return value;
		} else if (preference instanceof CheckBoxPreference)
			return ((CheckBoxPreference) preference).isChecked();
		else if (preference instanceof ListPreference)
			return Integer
					.valueOf(((ListPreference) preference)
							.findIndexOfValue(((ListPreference) preference)
									.getValue()));
		else if (preference instanceof RingtonePreference)
			return ((RingtonePreference) preference).getUri();
		throw new IllegalStateException();
	}

	/**
	 * Apply result values.
	 * 
	 * @param source
	 * @param result
	 * @return Whether operation succeed.
	 */
	protected abstract boolean setValues(Map<String, Object> source,
			Map<String, Object> result);

}
