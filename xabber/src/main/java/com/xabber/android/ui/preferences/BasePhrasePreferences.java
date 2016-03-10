package com.xabber.android.ui.preferences;


import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.message.phrase.Phrase;
import com.xabber.android.ui.activity.ManagedActivity;

public abstract class BasePhrasePreferences extends ManagedActivity
        implements PhraseEditorFragment.OnPhraseEditorFragmentInteractionListener {

    private Phrase phrase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_with_toolbar_and_container);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new PhraseEditorFragment()).commit();
        }
    }

    @Override
    public Phrase getPhrase() {
        return phrase;
    }

    @Override
    public void setPhrase(Phrase phrase) {
        this.phrase = phrase;
    }
}
