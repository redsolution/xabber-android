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

import java.util.Collection;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xabber.android.data.message.phrase.Phrase;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.androiddev.R;

/**
 * This class manage list of phrases.
 * 
 * @author alexander.ivanov
 * 
 */
public class PhraseListAdapter extends BaseListEditorAdapter<Integer> {

	public PhraseListAdapter(Activity activity) {
		super(activity);
	}

	private String append(String message, String value) {
		if (!"".equals(message))
			message += "\n";
		return message + value;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = getActivity().getLayoutInflater().inflate(
					R.layout.preference, parent, false);
		} else {
			view = convertView;
		}
		Phrase phrase = PhraseManager.getInstance()
				.getPhrase(getItem(position));
		String title = phrase.getText();
		if ("".equals(title))
			title = getActivity().getString(R.string.phrase_empty);
		String message = "";
		if (!"".equals(phrase.getUser()))
			message = append(message,
					getActivity().getString(R.string.phrase_user) + ": "
							+ phrase.getUser());
		if (!"".equals(phrase.getGroup()))
			message = append(message,
					getActivity().getString(R.string.phrase_group) + ": "
							+ phrase.getGroup());
		if (phrase.isRegexp())
			message = append(message,
					getActivity().getString(R.string.phrase_regexp));
		((TextView) view.findViewById(android.R.id.title)).setText(title);
		((TextView) view.findViewById(android.R.id.summary)).setText(message);
		return view;
	}

	@Override
	protected Collection<Integer> getTags() {
		return PhraseManager.getInstance().getPhrases();
	}

}
