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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xabber.android.data.Application;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.ui.adapter.BaseListEditorAdapter;
import com.xabber.android.ui.adapter.PhraseListAdapter;
import com.xabber.android.ui.helper.BaseListEditor;
import com.xabber.androiddev.R;

public class PhraseList extends BaseListEditor<Integer> {

	@Override
	protected int getAddTextResourceId() {
		return R.string.phrase_add;
	}

	@Override
	protected Intent getAddIntent() {
		return PhraseEditor.createIntent(this);
	}

	@Override
	protected Intent getEditIntent(Integer actionWith) {
		return PhraseEditor.createIntent(this, actionWith);
	}

	@Override
	protected int getRemoveTextResourceId() {
		return R.string.phrase_delete;
	}

	@Override
	protected String getRemoveConfirmation(Integer actionWith) {
		String text = PhraseManager.getInstance().getPhrase(actionWith)
				.getText();
		if ("".equals(text))
			text = Application.getInstance().getString(R.string.phrase_empty);
		return getString(R.string.phrase_delete_confirm, text);
	}

	@Override
	protected void removeItem(Integer actionWith) {
		PhraseManager.getInstance().removePhrase(actionWith);
	}

	@Override
	protected BaseListEditorAdapter<Integer> createListAdapter() {
		return new PhraseListAdapter(this);
	}

	@Override
	protected Integer getSavedValue(Bundle bundle, String key) {
		return bundle.getInt(key);
	}

	@Override
	protected void putSavedValue(Bundle bundle, String key, Integer actionWith) {
		bundle.putInt(key, actionWith);
	}

	public static Intent createIntent(Context context) {
		return new Intent(context, PhraseList.class);
	}

}
