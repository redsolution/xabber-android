package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.xtoken.SessionVO;
import com.xabber.android.data.extension.xtoken.XTokenManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.color.BarPainter;

import java.util.List;

public class ActiveSessionsActivity extends ManagedActivity {

    private Toolbar toolbar;
    private BarPainter barPainter;
    private SessionAdapter adapter;
    private TextView tvCurrentClient;
    private TextView tvCurrentDevice;
    private TextView tvCurrentIPAddress;
    private TextView tvCurrentDate;
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

        // other sessions
        RecyclerView recyclerView = findViewById(R.id.rvSessions);
        adapter = new SessionAdapter();
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
                }

                @Override
                public void onError() {
                    progressBar.setVisibility(View.GONE);
                    // TODO: 19.07.19 show error
                }
            });
    }

    private void setCurrentSession(SessionVO session) {
        tvCurrentClient.setText(session.getClient());
        tvCurrentDevice.setText(session.getDevice());
        tvCurrentIPAddress.setText(session.getIp());
        tvCurrentDate.setText(session.getLastAuth());
    }
}
