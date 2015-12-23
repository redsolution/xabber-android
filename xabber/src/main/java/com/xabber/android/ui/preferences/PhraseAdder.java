package com.xabber.android.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.xabber.android.R;
import com.xabber.android.data.intent.SegmentIntentBuilder;

public class PhraseAdder extends BasePhrasePreferences {

    public static Intent createIntent(Context context) {
        return new SegmentIntentBuilder<>(context, PhraseAdder.class).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.phrase_add);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:

                boolean success = ((PhraseEditorFragment) getFragmentManager()
                        .findFragmentById(R.id.fragment_container)).saveChanges();

                if (success) {
                    finish();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

}
