package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.color.BarPainter;

import java.util.ArrayList;
import java.util.List;

public class ActiveSessionsActivity extends ManagedActivity {

    private Toolbar toolbar;
    private BarPainter barPainter;
    private SessionAdapter adapter;
    private TextView tvCurrentClient;
    private TextView tvCurrentDevice;
    private TextView tvCurrentIPAddress;
    private TextView tvCurrentDate;

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

        // other sessions
        RecyclerView recyclerView = findViewById(R.id.rvSessions);
        adapter = new SessionAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // current session
        tvCurrentClient = findViewById(R.id.tvClient);
        tvCurrentDevice = findViewById(R.id.tvDevice);
        tvCurrentIPAddress = findViewById(R.id.tvIPAddress);
        tvCurrentDate = findViewById(R.id.tvDate);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // current session
        setCurrentSession(new SessionVO("Xabber Dev 2.6.4(636)",
                "Google Android SDK built for x86, Android 10", "123", "127.0.0.1", "Online"));

        // other sessions
        List<SessionVO> testItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            testItems.add(new SessionVO("Xabber for Web 1.0.131",
                    "PC, Linux x86_64", "123", "127.0.0.1", "14 Jul"));
        }
        adapter.setItems(testItems);
    }

    private void setCurrentSession(SessionVO session) {
        tvCurrentClient.setText(session.getClient());
        tvCurrentDevice.setText(session.getDevice());
        tvCurrentIPAddress.setText(session.getIp());
        tvCurrentDate.setText(session.getLastAuth());
    }
}
