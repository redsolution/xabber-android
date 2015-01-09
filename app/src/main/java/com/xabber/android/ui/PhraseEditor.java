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
import java.util.regex.PatternSyntaxException;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.intent.SegmentIntentBuilder;
import com.xabber.android.data.message.phrase.Phrase;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.ui.helper.BaseSettingsActivity;
import com.xabber.androiddev.R;

public class PhraseEditor extends BaseSettingsActivity {

	private Phrase phrase;

	@Override
	protected void onInflate(Bundle savedInstanceState) {
		addPreferencesFromResource(R.xml.phrase_editor);
		Integer index = getPhraseIndex(getIntent());
		if (index == null) {
			phrase = null;
			setTitle(R.string.phrase_add);
		} else {
			phrase = PhraseManager.getInstance().getPhrase(index);
			if (phrase == null) {
				finish();
				return;
			}
			String title = phrase.getText();
			if ("".equals(title))
				title = Application.getInstance().getString(
						R.string.phrase_empty);
			setTitle(title);
		}
	}

	@Override
	protected Map<String, Object> getValues() {
		Map<String, Object> source = new HashMap<String, Object>();
		putValue(source, R.string.phrase_text_key,
				phrase == null ? "" : phrase.getText());
		putValue(source, R.string.phrase_user_key,
				phrase == null ? "" : phrase.getUser());
		putValue(source, R.string.phrase_group_key, phrase == null ? ""
				: phrase.getGroup());
		putValue(source, R.string.phrase_regexp_key, phrase == null ? false
				: phrase.isRegexp());
		putValue(source, R.string.phrase_sound_key,
				phrase == null ? Settings.System.DEFAULT_NOTIFICATION_URI
						: phrase.getSound());
		return source;
	}

	@Override
	protected boolean setValues(Map<String, Object> source,
			Map<String, Object> result) {
		String text = getString(result, R.string.phrase_text_key);
		String user = getString(result, R.string.phrase_user_key);
		String group = getString(result, R.string.phrase_group_key);
		boolean regexp = getBoolean(result, R.string.phrase_regexp_key);
		Uri sound = getUri(result, R.string.phrase_sound_key);
		if (regexp)
			try {
				Phrase.compile(text);
				Phrase.compile(user);
				Phrase.compile(group);
			} catch (PatternSyntaxException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				return false;
			}
		if (phrase == null && "".equals(text) && "".equals(user)
				&& "".equals(group))
			return true;
		PhraseManager.getInstance().updateOrCreatePhrase(phrase, text, user,
				group, regexp, sound);
		return true;
	}

	public static Intent createIntent(Context context) {
		return createIntent(context, null);
	}

	public static Intent createIntent(Context context, Integer phraseIndex) {
		SegmentIntentBuilder<?> builder = new SegmentIntentBuilder<SegmentIntentBuilder<?>>(
				context, PhraseEditor.class);
		if (phraseIndex != null)
			builder.addSegment(phraseIndex.toString());
		return builder.build();
	}

	private Integer getPhraseIndex(Intent intent) {
		String value = SegmentIntentBuilder.getSegment(intent, 0);
		if (value == null)
			return null;
		else
			return Integer.valueOf(value);
	}

}
