package com.xabber.android.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.xabber.android.R;
import com.xabber.android.data.intent.SegmentIntentBuilder;
import com.xabber.android.data.message.phrase.Phrase;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.ToolbarHelper;

public class PhraseAdder extends BasePhrasePreferences {

    public static Intent createIntent(Context context) {
        return new SegmentIntentBuilder<>(context, PhraseAdder.class).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(this, getString(R.string.phrase_add), R.drawable.ic_clear_white_24dp);
        toolbar.inflateMenu(R.menu.toolbar_save);
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
        getMenuInflater().inflate(R.menu.toolbar_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:

                boolean success = ((PhraseEditorFragment) getFragmentManager()
                        .findFragmentById(R.id.fragment_container)).saveChanges();

                if (success) {
                    Integer index = PhraseManager.getInstance().getLastIndex();
                    if (index != null) startActivity(PhraseEditor.createIntent(this, index));
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

}
