package com.xabber.android.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.ManagedActivity;

public class AccountInfoEditor extends ManagedActivity implements Toolbar.OnMenuItemClickListener, AccountInfoEditorFragment.Listener {

    public static final String ARG_VCARD = "com.xabber.android.ui.AccountInfoEditor.ARG_VCARD";
    public static final int SAVE_MENU = R.menu.save;
    private Toolbar toolbar;


    public static Intent createIntent(Context context, String account, String vCard) {
        Intent intent = new EntityIntentBuilder(context, AccountInfoEditor.class).setAccount(account).build();
        intent.putExtra(ARG_VCARD, vCard);
        return intent;
    }

    private static String getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_with_toolbar_and_container);



        Intent intent = getIntent();
        String account = getAccount(intent);
        String vCard = intent.getStringExtra(ARG_VCARD);

        if (AccountManager.getInstance().getAccount(account) == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            setResult(RESULT_CANCELED);
            finish();
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(R.string.edit_account_user_info);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, AccountInfoEditorFragment.newInstance(account, vCard)).commit();
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        toolbar.inflateMenu(SAVE_MENU);
        toolbar.setOnMenuItemClickListener(this);
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
                ((AccountInfoEditorFragment) getFragmentManager().findFragmentById(R.id.fragment_container)).saveVCard();

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
    public void onVCardSavingStarted() {
        toolbar.setTitle(R.string.saving);
        toolbar.getMenu().findItem(R.id.action_save).setEnabled(false);
    }

    @Override
    public void onVCardSavingFinished() {
        toolbar.setTitle(R.string.edit_account_user_info);
        toolbar.getMenu().findItem(R.id.action_save).setEnabled(true);
    }
}
