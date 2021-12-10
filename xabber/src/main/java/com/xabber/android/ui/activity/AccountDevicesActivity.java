package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
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
import com.xabber.android.data.extension.devices.SessionVO;
import com.xabber.android.data.extension.devices.DevicesManager;
import com.xabber.android.ui.OnDevicesSessionsUpdatedListener;
import com.xabber.android.ui.adapter.SessionAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.DeviceInfoBottomSheetDialog;
import com.xabber.android.ui.helper.AndroidUtilsKt;

import org.jivesoftware.smack.packet.IQ;

import java.util.List;

public class AccountDevicesActivity extends ManagedActivity implements SessionAdapter.Listener,
        OnDevicesSessionsUpdatedListener {

    private SessionAdapter adapter;
    private TextView tvCurrentClient;
    private TextView tvCurrentDevice;
    private TextView tvCurrentIPAddress;
    private TextView tvCurrentDate;
    private TextView tvActiveSessions;
    private View terminateAll;
    private ProgressBar progressBar;
    private View contentView;

    private boolean isDeviceManagementEnabled = false;

    private AccountItem accountItem;

    public static Intent createIntent(Context context, AccountJid account) {
        return IntentHelpersKt.createAccountIntent(context, AccountDevicesActivity.class, account);
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

        isDeviceManagementEnabled = accountItem.getConnectionSettings().getDevice() != null &&
                !accountItem.getConnectionSettings().getDevice().isExpired();

        Toolbar toolbar = findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(R.string.account_devices);

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
        if (isDeviceManagementEnabled) {
            RecyclerView recyclerView = findViewById(R.id.rvSessions);
            adapter = new SessionAdapter(account, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            recyclerView.setNestedScrollingEnabled(false);

            // current session
            tvCurrentClient = findViewById(R.id.tvClient);
            tvCurrentDevice = findViewById(R.id.tvDevice);
            tvCurrentIPAddress = findViewById(R.id.tvIPAddress);
            tvCurrentDate = findViewById(R.id.tvDate);

            tvTokensUnavailable.setVisibility(View.GONE);
            tvTokensUnavailableHeader.setVisibility(View.GONE);
            getSessionsData();
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
        Application.getInstance().addUIListener(OnDevicesSessionsUpdatedListener.class, this);
    }

    @Override
    protected void onPause() {
        Application.getInstance().removeUIListener(OnDevicesSessionsUpdatedListener.class, this);
        super.onPause();
    }

    private void refreshData(){
        DevicesManager.INSTANCE.requestSessions(
                accountItem.getConnectionSettings().getDevice().getId(),
                accountItem.getConnection(),
                new DevicesManager.SessionsListener() {
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
                        Toast.makeText(AccountDevicesActivity.this,
                                R.string.account_active_sessions_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onAction() {
        Application.getInstance().runOnUiThread(() -> { if (isDeviceManagementEnabled) refreshData();} );
    }

    private void getSessionsData() {
        progressBar.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        DevicesManager.INSTANCE.requestSessions(
                accountItem.getConnectionSettings().getDevice().getId(),
                accountItem.getConnection(),
                new DevicesManager.SessionsListener() {
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
                        Toast.makeText(AccountDevicesActivity.this,
                                R.string.account_active_sessions_error, Toast.LENGTH_LONG).show();
                    }
                }
            );
    }

    private void setCurrentSession(SessionVO session) {
        if (session.getDescription() != null && !session.getDescription().isEmpty()) {
            tvCurrentClient.setText(session.getDescription());
        } else {
            tvCurrentClient.setText(session.getClient());
        }
        tvCurrentDevice.setText(session.getClient() + ", " + session.getDevice());
        tvCurrentIPAddress.setText(session.getIp());
        tvCurrentDate.setText(session.getLastAuth());

        findViewById(R.id.current_session_root).setOnClickListener(v -> {
                    DeviceInfoBottomSheetDialog fragment = DeviceInfoBottomSheetDialog.Companion
                            .newInstance(accountItem.getAccount(), session, true);
                    fragment.setOnDismissListener(d -> refreshData());
                    fragment.show(getSupportFragmentManager(), DeviceInfoBottomSheetDialog.TAG);
                }
            );
    }

    @Override
    public void onItemClick(SessionVO token) {
        DeviceInfoBottomSheetDialog.Companion
                .newInstance(accountItem.getAccount(), token, false)
                .show(getSupportFragmentManager(), DeviceInfoBottomSheetDialog.TAG);
    }

    private void showTerminateAllSessionsDialog() {
        new AlertDialog.Builder(AccountDevicesActivity.this)
                .setMessage(R.string.terminate_all_sessions_title)
                .setPositiveButton(R.string.button_terminate, (dialogInterface, i) -> {
                    DevicesManager.INSTANCE.sendRevokeAllDevicesRequest(accountItem.getConnection());
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
