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
package com.xabber.android.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.intent.SegmentIntentBuilder;
import com.xabber.android.data.message.phrase.Phrase;
import com.xabber.android.data.message.phrase.PhraseManager;

public class PhraseEditor extends BasePhrasePreferences {

    public static Intent createIntent(Context context, Integer phraseIndex) {
        SegmentIntentBuilder<?> builder = new SegmentIntentBuilder<>(
                context, PhraseEditor.class);
        if (phraseIndex != null)
            builder.addSegment(phraseIndex.toString());
        return builder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Integer index = getPhraseIndex(getIntent());
        if (index == null) {
            finish();
            return;
        }

        Phrase phrase = PhraseManager.getInstance().getPhrase(index);
        if (phrase == null) {
            finish();
            return;
        }
        setPhrase(phrase);

        String title = phrase.getText();
        if ("".equals(title))
            title = Application.getInstance().getString(
                    R.string.phrase_empty);
        setTitle(title);

    }

    @Override
    protected void onPause() {
        super.onPause();

        ((PhraseEditorFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_container)).saveChanges();
    }

    private Integer getPhraseIndex(Intent intent) {
        String value = SegmentIntentBuilder.getSegment(intent, 0);
        if (value == null)
            return null;
        else
            return Integer.valueOf(value);
    }
}
