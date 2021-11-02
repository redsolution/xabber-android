package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.IntentHelpersKt;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.xtoken.SessionVO;
import com.xabber.android.data.extension.xtoken.XTokenManager;
import com.xabber.android.ui.OnXTokenSessionsUpdatedListener;
import com.xabber.android.ui.adapter.SessionAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.AndroidUtilsKt;

import java.util.List;

public class ActiveSessionsActivity extends ManagedActivity implements SessionAdapter.Listener,
        OnXTokenSessionsUpdatedListener {

    private SessionAdapter adapter;
    private TextView tvCurrentClient;
    private TextView tvCurrentDevice;
    private TextView tvCurrentIPAddress;
    private TextView tvCurrentDate;
    private TextView tvActiveSessions;
    private View terminateAll;
    private ProgressBar progressBar;
    private View contentView;

    private boolean isXTokenEnabled = false;
    private SessionVO currentSession;

    private AccountItem accountItem;

    public static Intent createIntent(Context context, AccountJid account) {
        return IntentHelpersKt.createAccountIntent(context, ActiveSessionsActivity.class, account);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_sessions);

        AccountJid account = IntentHelpersKt.getAccountJid(getIntent());
        if (account == null) {
            finish();
            return;
        }

        accountItem = AccountManager.INSTANCE.getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        isXTokenEnabled = accountItem.getConnectionSettings().getXToken() != null &&
                !accountItem.getConnectionSettings().getXToken().isExpired();

        Toolbar toolbar = findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(R.string.account_active_sessions);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);
        progressBar = findViewById(R.id.progressBar);
        contentView = findViewById(R.id.contentView);
        tvActiveSessions = findViewById(R.id.tvActiveSessions);
        terminateAll = findViewById(R.id.llTerminateAll);
        terminateAll.setOnClickListener(view -> showTerminateAllSessionsDialog());
        TextView tvTokensUnavailable = findViewById(R.id.tvTokensUnavailable);
        TextView tvTokensUnavailableHeader = findViewById(R.id.tvTokensUnavailableHeader);

        // other sessions
        if (isXTokenEnabled) {
            RecyclerView recyclerView = findViewById(R.id.rvSessions);
            adapter = new SessionAdapter(this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            recyclerView.setNestedScrollingEnabled(false);

            // current session
            RelativeLayout rvCurrentSession = findViewById(R.id.current_session_layout);
            tvCurrentClient = findViewById(R.id.tvClient);
            tvCurrentDevice = findViewById(R.id.tvDevice);
            tvCurrentIPAddress = findViewById(R.id.tvIPAddress);
            tvCurrentDate = findViewById(R.id.tvDate);

            tvTokensUnavailable.setVisibility(View.GONE);
            tvTokensUnavailableHeader.setVisibility(View.GONE);
            getSessionsData();

            rvCurrentSession.setOnClickListener(v -> showChangeDescriptionDialog());
        } else {
            tvTokensUnavailable.setVisibility(View.VISIBLE);
            tvTokensUnavailable.setText(
                    getString(
                            R.string.account_active_sessions_not_supported,
                            account.getFullJid().getDomain()
                    )
            );
            tvTokensUnavailableHeader.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnXTokenSessionsUpdatedListener.class, this);
    }

    @Override
    protected void onPause() {
        Application.getInstance().removeUIListener(OnXTokenSessionsUpdatedListener.class, this);
        super.onPause();
    }

    private void refreshData(){
        XTokenManager.INSTANCE.requestSessions(
                accountItem.getConnectionSettings().getXToken().getUid(),
                accountItem.getConnection(),
                new XTokenManager.SessionsListener() {
                    @Override
                    public void onResult(
                            @Nullable SessionVO currentSession, @NonNull List<SessionVO> sessions
                    ) {
                        setCurrentSession(currentSession);
                        adapter.setItems(sessions);
                        adapter.notifyDataSetChanged();
                        terminateAll.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                        tvActiveSessions.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onError() {
                        Toast.makeText(ActiveSessionsActivity.this,
                                R.string.account_active_sessions_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onAction() {
        Application.getInstance().runOnUiThread(() -> { if (isXTokenEnabled) refreshData();} );
    }

    private void getSessionsData() {
        progressBar.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        XTokenManager.INSTANCE.requestSessions(
                accountItem.getConnectionSettings().getXToken().getUid(),
                accountItem.getConnection(),
                new XTokenManager.SessionsListener() {
                    @Override
                    public void onResult(@Nullable SessionVO currentSession, @NonNull List<SessionVO> sessions) {
                        progressBar.setVisibility(View.GONE);
                        contentView.setVisibility(View.VISIBLE);
                        setCurrentSession(currentSession);
                        adapter.setItems(sessions);
                        terminateAll.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                        tvActiveSessions.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onError() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ActiveSessionsActivity.this,
                                R.string.account_active_sessions_error, Toast.LENGTH_LONG).show();
                    }
                }
            );
    }

    private void setCurrentSession(SessionVO session) {
        currentSession = session;
        if (session.getDescription() != null && !session.getDescription().isEmpty()) {
            tvCurrentClient.setText(session.getDescription());
        } else {
            tvCurrentClient.setText(session.getClient());
        }
        tvCurrentDevice.setText(session.getDevice());
        tvCurrentIPAddress.setText(session.getIp());
        tvCurrentDate.setText(session.getLastAuth());
    }

    @Override
    public void onItemClick(String tokenUID) {
        showTerminateSessionDialog(tokenUID);
    }

    private void showChangeDescriptionDialog() {
        EditText descriptionEditText = new EditText(this);
        descriptionEditText.setPadding(
                AndroidUtilsKt.dipToPx(32f, this),
                descriptionEditText.getPaddingTop(),
                AndroidUtilsKt.dipToPx(32f, this),
                descriptionEditText.getPaddingBottom()
        );

        if (currentSession != null && currentSession.getDescription() != null) {
            descriptionEditText.setText(currentSession.getDescription());
        }

        new AlertDialog.Builder(this)
                .setTitle("Device description") //todo use right resources strings
                .setView(descriptionEditText)
                .setPositiveButton("Set description", (dialog, which) ->
                        XTokenManager.INSTANCE.sendChangeXTokenDescriptionRequest(
                                accountItem.getConnection(),
                                currentSession.getUid(),
                                descriptionEditText.getText().toString()
                        )
                )
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }

    private void showTerminateAllSessionsDialog() {
        new AlertDialog.Builder(ActiveSessionsActivity.this)
                .setMessage(R.string.terminate_all_sessions_title)
                .setPositiveButton(R.string.button_terminate, (dialogInterface, i) -> {
                    XTokenManager.INSTANCE.sendRevokeAllRequest(accountItem.getConnection());
                    getSessionsData();
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                .create()
                .show();
    }

    private void showTerminateSessionDialog(final String uid) {
        new AlertDialog.Builder(ActiveSessionsActivity.this)
                .setMessage(R.string.terminate_session_title)
                .setPositiveButton(R.string.button_terminate, (dialogInterface, i) -> {
                    XTokenManager.INSTANCE.sendRevokeXTokenRequest(accountItem.getConnection(), uid);
                    getSessionsData();
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                .create()
                .show();
    }

    @Override
    public void onAuthErrorEvent(AccountErrorEvent accountErrorEvent) {
        super.onAuthErrorEvent(accountErrorEvent);
        finish();
    }
}
