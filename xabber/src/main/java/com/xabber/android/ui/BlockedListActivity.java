package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.adapter.BlockedListAdapter;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.ManagedActivity;

public class BlockedListActivity extends ManagedActivity implements BlockedListAdapter.OnBlockedContactClickListener, OnBlockedListChangedListener {

    private BlockedListAdapter adapter;
    private String account;

    public static Intent createIntent(Context context, String account) {
        return new AccountIntentBuilder(context, BlockedListActivity.class).setAccount(account).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        account = getAccount(getIntent());
        if (account == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_with_toolbar_and_container);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(R.string.block_list);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        RecyclerView recyclerView = new RecyclerView(this);
        ((RelativeLayout)findViewById(R.id.fragment_container)).addView(recyclerView);

        adapter = new BlockedListAdapter(this, account);
        adapter.setListener(this);

        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.onChange();

        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
    }

    @Override
    public void onBlockedContactClick(View itemView, final String contact) {
        PopupMenu popup = new PopupMenu(this, itemView);
        popup.inflate(R.menu.blocked_contact_context_menu);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                BlockingManager.getInstance().unblockContact(account, contact, new BlockingManager.UnblockContactListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(BlockedListActivity.this, getString(R.string.contact_unblocked_successfully), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError() {
                        Toast.makeText(BlockedListActivity.this, getString(R.string.error_unblocking_contact), Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }
        });
        popup.show();
    }

    @Override
    public void onBlockedListChanged(String account) {
        adapter.onChange();
    }
}
