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
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.intent.SegmentIntentBuilder;
import com.xabber.android.data.message.phrase.Phrase;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.dialog.ConfirmDialog;
import com.xabber.android.ui.helper.ToolbarHelper;

public class PhraseEditor extends BasePhrasePreferences implements ConfirmDialog.Listener {

    private Integer index;

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

        index = getPhraseIndex(getIntent());
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

        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(this, title);
        toolbar.inflateMenu(R.menu.toolbar_delete);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.toolbar_delete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                ConfirmDialog.newInstance(getRemoveConfirmationText(index))
                        .show(getFragmentManager(), ConfirmDialog.class.getName());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        ((PhraseEditorFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_container)).saveChanges();
    }

    @Override
    public void onConfirm() {
        PhraseManager.getInstance().removePhrase(index);
        finish();
    }

    private Integer getPhraseIndex(Intent intent) {
        String value = SegmentIntentBuilder.getSegment(intent, 0);
        if (value == null)
            return null;
        else
            return Integer.valueOf(value);
    }

    private String getRemoveConfirmationText(Integer actionWith) {
        String text = PhraseManager.getInstance().getPhrase(actionWith)
                .getText();
        if ("".equals(text))
            text = Application.getInstance().getString(R.string.phrase_empty);
        return getString(R.string.phrase_delete_confirm, text);
    }
}
