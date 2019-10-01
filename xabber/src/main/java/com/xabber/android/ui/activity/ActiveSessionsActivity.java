package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.xtoken.SessionVO;
import com.xabber.android.data.extension.xtoken.XTokenManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.color.BarPainter;


import java.util.List;

public class ActiveSessionsActivity extends ManagedActivity implements SessionAdapter.Listener {

    private Toolbar toolbar;
    private BarPainter barPainter;
    private SessionAdapter adapter;
    private TextView tvCurrentClient;
    private TextView tvCurrentDevice;
    private TextView tvCurrentIPAddress;
    private TextView tvCurrentDate;
    private TextView tvActiveSessions;
    private View terminateAll;
    private ProgressBar progressBar;
    private View contentView;

    private AccountItem accountItem;

    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, ActiveSessionsActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_sessions);

        final Intent intent = getIntent();
        AccountJid account = getAccount(intent);
        if (account == null) {
            finish();
            return;
        }

        accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(R.string.account_active_sessions);

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);
        progressBar = findViewById(R.id.progressBar);
        contentView = findViewById(R.id.contentView);
        tvActiveSessions = findViewById(R.id.tvActiveSessions);
        terminateAll = findViewById(R.id.llTerminateAll);
        terminateAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTerminateAllSessionsDialog();
            }
        });


        // other sessions
        RecyclerView recyclerView = findViewById(R.id.rvSessions);
        adapter = new SessionAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        // current session
        tvCurrentClient = findViewById(R.id.tvClient);
        tvCurrentDevice = findViewById(R.id.tvDevice);
        tvCurrentIPAddress = findViewById(R.id.tvIPAddress);
        tvCurrentDate = findViewById(R.id.tvDate);

        updateData();
    }

    private void updateData() {
        progressBar.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        XTokenManager.getInstance().requestSessions(
            accountItem.getConnectionSettings().getXToken().getUid(),
            accountItem.getConnection(), new XTokenManager.SessionsListener() {
                @Override
                public void onResult(SessionVO currentSession, List<SessionVO> sessions) {
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
            });
    }

    private void setCurrentSession(SessionVO session) {
        tvCurrentClient.setText(session.getClient());
        tvCurrentDevice.setText(session.getDevice());
        tvCurrentIPAddress.setText(session.getIp());
        tvCurrentDate.setText(session.getLastAuth());
    }

    @Override
    public void onItemClick(String tokenUID) {
        showTerminateSessionDialog(tokenUID);
    }

    private void showTerminateAllSessionsDialog() {
        new AlertDialog.Builder(ActiveSessionsActivity.this)
            .setMessage(R.string.terminate_all_sessions_title)
            .setPositiveButton(R.string.button_terminate, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    XTokenManager.getInstance().sendRevokeXTokenRequest(
                            accountItem.getConnection(), adapter.getItemsIDs());
                    updateData();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            })
            .create().show();
    }

    private void showTerminateSessionDialog(final String uid) {
        new AlertDialog.Builder(ActiveSessionsActivity.this)
            .setMessage(R.string.terminate_session_title)
            .setPositiveButton(R.string.button_terminate, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    XTokenManager.getInstance().sendRevokeXTokenRequest(
                            accountItem.getConnection(), uid);
                    updateData();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            })
            .create().show();
    }

    @Override
    public void onAuthErrorEvent(AccountErrorEvent accountErrorEvent) {
        super.onAuthErrorEvent(accountErrorEvent);
        finish();
    }
}
