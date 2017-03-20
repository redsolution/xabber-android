package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.accountoptions.AccountOption;
import com.xabber.android.ui.adapter.accountoptions.AccountOptionsAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.AccountColorDialog;
import com.xabber.android.ui.helper.ContactTitleInflater;

import java.util.Collection;

public class AccountActivity extends ManagedActivity implements AccountOptionsAdapter.Listener, OnAccountChangedListener {

    public static final int ACCOUNT_VIEWER_MENU = R.menu.account_viewer;
    private static final String LOG_TAG = AccountActivity.class.getSimpleName();

    private AccountJid account;
    private View contactTitleView;
    private AbstractContact bestContact;
    private View statusIcon;
    private TextView statusText;
    private AccountOptionsAdapter accountOptionsAdapter;
    private BarPainter barPainter;

    public AccountActivity() {
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @NonNull
    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, AccountActivity.class).setAccount(account).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        account = getAccount(intent);
        if (account == null) {
            finish();
            return;
        }

        final AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        setContentView(R.layout.activity_account);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(AccountActivity.this);
            }
        });

        toolbar.setTitle(R.string.contact_account);
        toolbar.inflateMenu(R.menu.toolbar_account);

        MenuItem item = toolbar.getMenu().findItem(R.id.action_account_switch);
        SwitchCompat switchCompat = (SwitchCompat) item.getActionView().findViewById(R.id.account_switch_view);

        switchCompat.setChecked(accountItem.isEnabled());

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AccountManager.getInstance().setEnabled(accountItem.getAccount(), isChecked);
            }
        });

        barPainter = new BarPainter(this, toolbar);

        UserJid fakeAccountUser;
        try {
            fakeAccountUser = UserJid.from(account.getFullJid().asBareJid());
        } catch (UserJid.UserJidCreateException e) {
            throw new IllegalStateException();
        }

        bestContact = RosterManager.getInstance().getBestContact(account, fakeAccountUser);

        contactTitleView = findViewById(R.id.contact_title_expanded);
        statusIcon = findViewById(R.id.status_icon);
        statusText = (TextView) findViewById(R.id.status_text);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.account_options_recycler_view);


        accountOptionsAdapter = new AccountOptionsAdapter(AccountOption.values(), this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(accountOptionsAdapter);

    }

    private void updateOptions() {
        AccountOption.CONNECTION_SETTINGS.setDescription(account.getFullJid().asBareJid().toString());

        AccountOption.COLOR.setDescription(ColorManager.getInstance().getAccountPainter().getAccountColorName(account));

        AccountOption.BLOCK_LIST.setDescription(String.valueOf(BlockingManager.getInstance().getBlockedContacts(account).size()));
        AccountOption.SERVER_INFO.setDescription(getString(R.string.account_server_info_description));

        accountOptionsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateTitle();
        updateOptions();

        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    protected void onPause() {
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);

        super.onPause();
    }

    private void updateTitle() {
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact);
        statusIcon.setVisibility(View.GONE);
        statusText.setText(account.getFullJid().asBareJid().toString());

        contactTitleView.setBackgroundColor(barPainter.getAccountPainter().getAccountMainColor(account));
        barPainter.updateWithAccountName(account);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(ACCOUNT_VIEWER_MENU, menu);
        return true;
    }

    @Override
    public void onAccountOptionClick(AccountOption option) {
        switch (option) {
            case CONNECTION_SETTINGS:
                startActivity(AccountSettingsActivity.createIntent(this, account));
                break;
            case COLOR:
                AccountColorDialog.newInstance(account).show(getFragmentManager(), AccountColorDialog.class.getSimpleName());
                break;
            case BLOCK_LIST:
                startActivity(BlockedListActivity.createIntent(this, account));
                break;
            case SERVER_INFO:
                startActivity(ServerInfoActivity.createIntent(this, account));
                break;
            case CHAT_HISTORY:

                break;
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        LogManager.i(LOG_TAG, "onAccountsChanged " + accounts);

        if (accounts.contains(account)) {
            updateTitle();
            updateOptions();
        }
    }
}
