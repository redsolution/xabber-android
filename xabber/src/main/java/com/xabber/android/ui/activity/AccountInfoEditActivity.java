package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.AccountInfoEditFragment;

public class AccountInfoEditActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener, AccountInfoEditFragment.Listener {

    public static final String ARG_VCARD = "com.xabber.android.ui.activity.AccountInfoEditor.ARG_VCARD";
    public static final int SAVE_MENU = R.menu.toolbar_save;
    public static final String ARGUMENT_SAVE_BUTTON_ENABLED = "com.xabber.android.ui.activity.AccountInfoEdit.ARGUMENT_SAVE_BUTTON_ENABLED";

    private Toolbar toolbar;

    public static Intent createIntent(Context context, AccountJid account, String vCard) {
        Intent intent = new EntityIntentBuilder(context, AccountInfoEditActivity.class).setAccount(account).build();
        intent.putExtra(ARG_VCARD, vCard);
        return intent;
    }

    public static Intent createIntent(Context context, AccountJid account) {
        return new EntityIntentBuilder(context, AccountInfoEditActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_with_toolbar_and_container);

        Intent intent = getIntent();
        AccountJid account = getAccount(intent);
        String vCard = intent.getStringExtra(ARG_VCARD);

        if (AccountManager.getInstance().getAccount(account) == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            setResult(RESULT_CANCELED);
            finish();
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.inflateMenu(SAVE_MENU);
        toolbar.setOnMenuItemClickListener(this);
        View view = toolbar.findViewById(R.id.action_save);
        if (view instanceof TextView)
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                ((TextView) view).setTextColor(getResources().getColor(R.color.grey_900));
            else ((TextView) view).setTextColor(Color.WHITE);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
            toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(R.string.edit_account_user_info);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        boolean isSaveButtonEnabled = false;
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, AccountInfoEditFragment.newInstance(account)).commit();
        } else {
            isSaveButtonEnabled = savedInstanceState.getBoolean(ARGUMENT_SAVE_BUTTON_ENABLED);
        }
        enableSave(isSaveButtonEnabled);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARGUMENT_SAVE_BUTTON_ENABLED, toolbar.getMenu().findItem(R.id.action_save).isEnabled());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(SAVE_MENU, menu);

        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                ((AccountInfoEditFragment) getFragmentManager().findFragmentById(R.id.fragment_container)).saveVCard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getFragmentManager().findFragmentById(R.id.fragment_container).onActivityResult(requestCode,
                resultCode, data);
    }

    @Override
    public void onProgressModeStarted(String message) {
        toolbar.setTitle(message);
        toolbar.getMenu().findItem(R.id.action_save).setEnabled(false);
    }

    @Override
    public void onProgressModeFinished() {
        toolbar.setTitle(R.string.edit_account_user_info);
    }

    @Override
    public void toggleSave(boolean value) {
        enableSave(value);
    }

    private void enableSave() {
        enableSave(!toolbar.getMenu().findItem(R.id.action_save).isEnabled());
    }

    private void enableSave(boolean enable) {
        toolbar.getMenu().findItem(R.id.action_save).setEnabled(enable);
        View view = toolbar.findViewById(R.id.action_save);
        if (view instanceof TextView) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                ((TextView) view).setTextColor(getResources().getColor(enable ? R.color.grey_900 : R.color.grey_700));
            } else {
                ((TextView) view).setTextColor(enable ? Color.WHITE : Color.GRAY);
            }
        }
    }
}