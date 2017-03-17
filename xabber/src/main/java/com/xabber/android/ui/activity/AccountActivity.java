package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
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
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.ContactTitleInflater;

public class AccountActivity extends ManagedActivity {

    public static final int ACCOUNT_VIEWER_MENU = R.menu.account_viewer;
    private static final String LOG_TAG = AccountActivity.class.getSimpleName();

    private AccountJid account;
    private View contactTitleView;
    private AbstractContact bestContact;
    private View statusIcon;
    private TextView statusText;

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

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        UserJid fakeAccountUser;
        try {
            fakeAccountUser = UserJid.from(account.getFullJid().asBareJid());
        } catch (UserJid.UserJidCreateException e) {
            throw new IllegalStateException();
        }

        bestContact = RosterManager.getInstance().getBestContact(account, fakeAccountUser);

        contactTitleView = findViewById(R.id.contact_title_expanded);
        contactTitleView.setBackgroundColor(barPainter.getAccountPainter().getAccountMainColor(account));
        statusIcon = findViewById(R.id.status_icon);
        statusText = (TextView) findViewById(R.id.status_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTitle();
    }

    private void updateTitle() {
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact);
        statusIcon.setVisibility(View.GONE);
        statusText.setText(account.getFullJid().asBareJid().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(ACCOUNT_VIEWER_MENU, menu);
        return true;
    }
}
