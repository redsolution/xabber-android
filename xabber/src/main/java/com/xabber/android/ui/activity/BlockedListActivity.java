package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.extension.blocking.PrivateMucChatBlockingManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.adapter.BlockedListAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.dialog.UnblockAllContactsDialog;

import java.util.ArrayList;

public class BlockedListActivity extends ManagedActivity implements BlockedListAdapter.OnBlockedContactClickListener,
        OnBlockedListChangedListener, BlockingManager.UnblockContactListener, Toolbar.OnMenuItemClickListener {

    public static final String SAVED_CHECKED_CONTACTS = "com.xabber.android.ui.activity.BlockedListActivity.SAVED_CHECKED_CONTACTS";
    private BlockedListAdapter adapter;
    private String account;
    private Toolbar toolbar;
    private BarPainter barPainter;
    private int previousSize;

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

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.inflateMenu(R.menu.block_list);
        toolbar.setOnMenuItemClickListener(this);

        barPainter = new BarPainter(this, toolbar);

        RecyclerView recyclerView = new RecyclerView(this);
        ((RelativeLayout)findViewById(R.id.fragment_container)).addView(recyclerView);



        adapter = new BlockedListAdapter(account);
        adapter.setListener(this);


        if (savedInstanceState != null) {
            final ArrayList<String> checkedContacts = savedInstanceState.getStringArrayList(SAVED_CHECKED_CONTACTS);
            if (checkedContacts != null) {
                adapter.setCheckedContacts(checkedContacts);
            }
        }

        previousSize = -1;
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.block_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_unblock_all).setVisible(adapter.getItemCount() > 0);
        final boolean checkContactsIsEmpty = adapter.getCheckedContacts().isEmpty();
        menu.findItem(R.id.action_unblock_selected).setVisible(!checkContactsIsEmpty);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);
        update();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SAVED_CHECKED_CONTACTS, adapter.getCheckedContacts());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_unblock_all:
                UnblockAllContactsDialog.newInstance(account).show(getFragmentManager(), UnblockAllContactsDialog.class.getName());
                return true;
            case R.id.action_unblock_selected:
                BlockingManager.getInstance().unblockContacts(account, adapter.getCheckedContacts(), this);
                PrivateMucChatBlockingManager.getInstance().unblockContacts(account, adapter.getCheckedContacts());
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBlockedContactClick() {
        updateToolbar();
        updateMenu();
    }

    private void updateMenu() {
        onPrepareOptionsMenu(toolbar.getMenu());
    }

    private void updateToolbar() {
        final ArrayList<String> checkedContacts = adapter.getCheckedContacts();

        final int currentSize = checkedContacts.size();

        if (currentSize == previousSize) {
            return;
        }

        if (currentSize == 0) {
            toolbar.setTitle(R.string.block_list);
            barPainter.updateWithAccountName(account);

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

        } else {
            toolbar.setTitle(String.valueOf(currentSize));
            barPainter.setGrey();

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.setCheckedContacts(new ArrayList<String>());
                    adapter.onChange();
                }
            });
        }

        previousSize = currentSize;
    }

    @Override
    public void onBlockedListChanged(String account) {
        update();
    }

    private void update() {
        adapter.onChange();
        updateToolbar();
        updateMenu();
    }

    @Override
    public void onSuccess() {
        Toast.makeText(BlockedListActivity.this, getString(R.string.contacts_unblocked_successfully), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError() {
        Toast.makeText(BlockedListActivity.this, getString(R.string.error_unblocking_contacts), Toast.LENGTH_SHORT).show();
    }
}
