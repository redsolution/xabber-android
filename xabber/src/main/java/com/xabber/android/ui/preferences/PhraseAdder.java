package com.xabber.android.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.message.phrase.PhraseManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.ToolbarHelper;

public class PhraseAdder extends BasePhrasePreferences {

    public static Intent createIntent(Context context) {
        return new Intent(context, PhraseAdder.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar;
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar = ToolbarHelper.setUpDefaultToolbar(this, getString(R.string.phrase_add), R.drawable.ic_clear_grey_24dp);
        else toolbar = ToolbarHelper.setUpDefaultToolbar(this, getString(R.string.phrase_add), R.drawable.ic_clear_white_24dp);
        toolbar.inflateMenu(R.menu.toolbar_save);
        View view = toolbar.findViewById(R.id.action_save);
        if (view != null && view instanceof TextView)
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                ((TextView)view).setTextColor(getResources().getColor(R.color.grey_600));
            else ((TextView)view).setTextColor(Color.WHITE);
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
                        .findFragmentById(R.id.content_container)).saveChanges();

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
