package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.accountoptions.AccountOption;
import com.xabber.android.ui.adapter.accountoptions.AccountOptionsAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.AccountColorDialog;
import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
import com.xabber.android.ui.helper.ContactTitleInflater;

import org.greenrobot.eventbus.Subscribe;

import java.util.Collection;

public class AccountActivity extends ManagedActivity implements AccountOptionsAdapter.Listener,
        OnAccountChangedListener, OnBlockedListChangedListener, ContactVcardViewerFragment.Listener {

    private static final String LOG_TAG = AccountActivity.class.getSimpleName();
    private static final String ACTION_CONNECTION_SETTINGS = AccountActivity.class.getName() + "ACTION_CONNECTION_SETTINGS";

    private AccountJid account;
    private View contactTitleView;
    private AbstractContact bestContact;
    private View statusIcon;
    private TextView statusText;
    private AccountOptionsAdapter accountOptionsAdapter;
    private BarPainter barPainter;
    private SwitchCompat switchCompat;
    private AccountItem accountItem;
    private boolean isConnectionSettingsAction;
    private ImageView qrImage;
    private IntentIntegrator integrator;

    public AccountActivity() {
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @NonNull
    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, AccountActivity.class).setAccount(account).build();
    }

    @NonNull
    public static Intent createConnectionSettingsIntent(Context context, AccountJid account) {
        Intent intent = new AccountIntentBuilder(context, AccountActivity.class).setAccount(account).build();
        intent.setAction(ACTION_CONNECTION_SETTINGS);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        integrator = new IntentIntegrator(this);

        account = getAccount(intent);
        if (account == null) {
            LogManager.i(LOG_TAG, "Account is null, finishing!");
            finish();
            return;
        }

        accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        if (ACTION_CONNECTION_SETTINGS.equals(intent.getAction())) {
            isConnectionSettingsAction = true;
            startAccountSettingsActivity();
            setIntent(null);
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
        switchCompat = (SwitchCompat) item.getActionView().findViewById(R.id.account_switch_view);

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AccountManager.getInstance().setEnabled(accountItem.getAccount(), isChecked);
            }
        });

        barPainter = new BarPainter(this, toolbar);

        final UserJid fakeAccountUser;
        try {
            fakeAccountUser = UserJid.from(account.getFullJid().asBareJid());
        } catch (UserJid.UserJidCreateException e) {
            throw new IllegalStateException();
        }

        bestContact = RosterManager.getInstance().getBestContact(account, fakeAccountUser);

        contactTitleView = findViewById(R.id.contact_title_expanded);
        statusIcon = findViewById(R.id.ivStatus);
        statusText = (TextView) findViewById(R.id.status_text);

        qrImage = (ImageView) findViewById(R.id.imgQRcode);
        qrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, fakeAccountUser);
                Intent intent = QRCodeActivity.createIntent(AccountActivity.this, account);
                String textName = rosterContact != null ? rosterContact.getName() : "";
                intent.putExtra("account_name", textName);
                String textAddress =  account.getFullJid().asBareJid().toString();
                intent.putExtra("account_address", textAddress);
                intent.putExtra("caller", "AccountActivity");

                //integrator.setOrientationLocked(true)
                //        .setBeepEnabled(false)
                //        .setCameraId(0)
                //        .setPrompt("")
                //        .addExtra("account_name", textName)
                //        .addExtra("account_address", textAddress)
                //        .setCaptureActivity(QRCodeScannerActivity.class)
                //        .initiateScan(Collections.unmodifiableList(Collections.singletonList(IntentIntegrator.QR_CODE)));

                startActivity(intent);
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.account_options_recycler_view);


        accountOptionsAdapter = new AccountOptionsAdapter(AccountOption.getValues(), this, accountItem);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(accountOptionsAdapter);
        recyclerView.setNestedScrollingEnabled(false);

        Fragment fragmentById = getFragmentManager().findFragmentById(R.id.account_fragment_container);

        if (fragmentById == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.account_fragment_container, ContactVcardViewerFragment.newInstance(account))
                    .commit();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode, data);
        if(result!=null){
            if(result.getContents()==null){
                Toast.makeText(AccountActivity.this, "no-go", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(AccountActivity.this, "Scanned = " + result.getContents(), Toast.LENGTH_LONG).show();
                if(result.getContents().length()>5)
                    if(result.getContents().substring(0,5).equals("xmpp:")) {
                        Intent intent = ContactAddActivity.createIntent(this, account);
                        intent.putExtra("contact",result.getContents().substring(5));
                        startActivity(intent);
                        //startActivity(intent);
                    }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateOptions() {
        AccountOption.SYNCHRONIZATION.setDescription(getString(R.string.account_sync_summary));

        AccountOption.CONNECTION_SETTINGS.setDescription(account.getFullJid().asBareJid().toString());

        AccountOption.PUSH_NOTIFICATIONS.setDescription(getString(accountItem.isPushWasEnabled()
                ? R.string.account_push_state_enabled : R.string.account_push_state_disabled));

        AccountOption.COLOR.setDescription(ColorManager.getInstance().getAccountPainter().getAccountColorName(account));

        updateBlockListOption();

        AccountOption.SERVER_INFO.setDescription(getString(R.string.account_server_info_description));

        AccountOption.CHAT_HISTORY.setDescription(getString(R.string.account_history_options_summary));

        AccountOption.BOOKMARKS.setDescription(getString(R.string.account_bookmarks_summary));

        accountOptionsAdapter.notifyDataSetChanged();
    }

    private void updateBlockListOption() {
        BlockingManager blockingManager = BlockingManager.getInstance();

        Boolean supported = blockingManager.isSupported(account);

        String description;
        if (supported == null) {
            description  = getString(R.string.blocked_contacts_unknown);
        } else if (!supported) {
            description  = getString(R.string.blocked_contacts_not_supported);
        } else {
            int size = blockingManager.getCachedBlockedContacts(account).size();
            if (size == 0) {
                description = getString(R.string.blocked_contacts_empty);
            } else {
                description = getResources().getQuantityString(R.plurals.blocked_contacts_number, size, size);
            }
        }

        AccountOption.BLOCK_LIST.setDescription(description);
        accountOptionsAdapter.notifyItemChanged(AccountOption.BLOCK_LIST.ordinal());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AccountManager.getInstance().getAccount(account) == null) {
            // in case if account was removed
            finish();
            return;
        }

        updateTitle();
        updateOptions();

        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);
    }

    @Override
    protected void onPause() {
        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);

        isConnectionSettingsAction = false;
        super.onPause();
    }

    private void updateTitle() {
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact);
        statusIcon.setVisibility(View.GONE);
        statusText.setText(account.getFullJid().asBareJid().toString());

        contactTitleView.setBackgroundColor(barPainter.getAccountPainter().getAccountMainColor(account));
        barPainter.updateWithAccountName(account);

        switchCompat.setChecked(accountItem.isEnabled());
    }

    @Override
    public void onAccountOptionClick(AccountOption option) {
        switch (option) {
            case CONNECTION_SETTINGS:
                startAccountSettingsActivity();
                break;
            case PUSH_NOTIFICATIONS:
                startActivity(AccountPushActivity.createIntent(this, account));
                break;
            case COLOR:
                AccountColorDialog.newInstance(account).show(getFragmentManager(),
                        AccountColorDialog.class.getSimpleName());
                break;
            case BLOCK_LIST:
                startActivity(BlockedListActivity.createIntent(this, account));
                break;
            case SERVER_INFO:
                startActivity(ServerInfoActivity.createIntent(this, account));
                break;
            case CHAT_HISTORY:
                startActivity(AccountHistorySettingsActivity.createIntent(this, account));
                break;
            case BOOKMARKS:
                startActivity(BookmarksActivity.createIntent(this, account));
                break;
            case SYNCHRONIZATION:
                if (XabberAccountManager.getInstance().getAccount() != null) {
                    if (accountItem.isSyncNotAllowed()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.sync_not_allowed_summary)
                                .setTitle(R.string.sync_status_not_allowed)
                                .setPositiveButton(R.string.ok, null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else startActivity(AccountSyncActivity.createIntent(this, account));
                } else startActivity(TutorialActivity.createIntent(this));
                break;
            case SESSIONS:
                if (accountItem.getConnectionSettings().getXToken() != null &&
                        !accountItem.getConnectionSettings().getXToken().isExpired()) {
                    startActivity(ActiveSessionsActivity.createIntent(this, account));
                }
                break;
        }
    }

    private void startAccountSettingsActivity() {
        startActivity(AccountSettingsActivity.createIntent(this, account));
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        LogManager.i(LOG_TAG, "onAccountsChanged " + accounts);

        if (accounts.contains(account)) {
            updateTitle();
            updateOptions();
        }
    }

    @Override
    public void onBlockedListChanged(AccountJid account) {
        if (this.account.equals(account)) {
            updateBlockListOption();
        }
    }

    @Override
    public void onVCardReceived() {
        updateTitle();
    }

    @Override
    public void registerVCardFragment(ContactVcardViewerFragment fragment) {}

    @Subscribe(sticky = true)
    @Override
    public void onAuthErrorEvent(AccountErrorEvent accountErrorEvent) {
        LogManager.i(LOG_TAG, "onAuthErrorEvent ");

        if (!isConnectionSettingsAction) {
            super.onAuthErrorEvent(accountErrorEvent);
        }
    }

}
