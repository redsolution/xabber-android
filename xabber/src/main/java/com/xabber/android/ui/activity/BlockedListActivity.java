package com.xabber.android.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.IntentHelpersKt;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.OnBlockedListChangedListener;
import com.xabber.android.ui.adapter.BlockedListAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.dialog.BlockByJidDialog;
import com.xabber.android.ui.dialog.UnblockAllContactsDialog;
import com.xabber.android.ui.widget.DividerItemDecoration;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class BlockedListActivity extends ManagedActivity implements BlockedListAdapter.OnBlockedContactClickListener,
        OnBlockedListChangedListener, BlockingManager.UnblockContactListener, Toolbar.OnMenuItemClickListener {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BLOCKED_LIST, GROUP_INVITES})
    public @interface BlockedListState {}
    public static final int BLOCKED_LIST = 0;
    public static final int GROUP_INVITES = 1;

    public static final String SAVED_CHECKED_CONTACTS = "com.xabber.android.ui.activity.BlockedListActivity.SAVED_CHECKED_CONTACTS";
    public static final String SAVED_BLOCKLIST_STATE = "com.xabber.android.ui.activity.BlockedListActivity.SAVED_BLOCKLIST_STATE";
    BlockedListAdapter adapter;
    private AccountJid account;
    private Toolbar toolbar;
    private BarPainter barPainter;
    private int previousSize;
    @BlockedListState
    private int previousState;

    public static Intent createIntent(Context context, AccountJid account) {
        return IntentHelpersKt.createAccountIntent(context, BlockedListActivity.class, account);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        account = IntentHelpersKt.getAccountJid(getIntent());
        if (account == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_with_toolbar_and_container);
        boolean lightTheme = SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light;

        toolbar = findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(lightTheme ? R.drawable.ic_arrow_left_grey_24dp : R.drawable.ic_arrow_left_white_24dp);
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setColorFilter(lightTheme ?
                    getResources().getColor(R.color.grey_900) : getResources().getColor(R.color.white),
                    PorterDuff.Mode.SRC_IN);
        }

        toolbar.inflateMenu(R.menu.toolbar_block_list);
        toolbar.setOnMenuItemClickListener(this);

        barPainter = new BarPainter(this, toolbar);

        RecyclerView recyclerView = new RecyclerView(this);
        ((RelativeLayout)findViewById(R.id.content_container)).addView(recyclerView);



        adapter = new BlockedListAdapter(account);
        adapter.setListener(this);


        if (savedInstanceState != null) {
            final ArrayList<String> checkedContacts = savedInstanceState.getStringArrayList(SAVED_CHECKED_CONTACTS);
            int state = savedInstanceState.getInt(SAVED_BLOCKLIST_STATE);
            if (checkedContacts != null) {
                List<ContactJid> checkedJids = new ArrayList<>();
                for (String contactString : checkedContacts) {
                    try {
                        checkedJids.add(ContactJid.from(contactString));
                    } catch (ContactJid.ContactJidCreateException e) {
                        LogManager.exception(this, e);
                    }
                }

                adapter.setCheckedContacts(checkedJids);
                adapter.setBlockedListState(state);
            }
        } else {
            adapter.setBlockedListState(BLOCKED_LIST);
            previousState = BLOCKED_LIST;
        }

        previousSize = -1;
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration divider = new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation());
        divider.skipDividerOnLastItem(true);
        recyclerView.addItemDecoration(divider);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.toolbar_block_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean checkContactsIsEmpty = adapter.getCheckedContacts().isEmpty();
        menu.findItem(R.id.action_unblock_all).setVisible(adapter.getItemCount() > 0 && checkContactsIsEmpty);
        menu.findItem(R.id.action_unblock_selected).setVisible(!checkContactsIsEmpty);
        menu.findItem(R.id.action_block_manual).setVisible(checkContactsIsEmpty && adapter.getCurrentBlockListState() == BLOCKED_LIST);
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
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<ContactJid> checkedContacts = adapter.getCheckedContacts();
        ArrayList<String> checkedContactsStringList = new ArrayList<>();
        for (ContactJid jid : checkedContacts) {
            checkedContactsStringList.add(jid.toString());
        }

        outState.putInt(SAVED_BLOCKLIST_STATE, adapter.getCurrentBlockListState());
        outState.putStringArrayList(SAVED_CHECKED_CONTACTS, checkedContactsStringList);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_unblock_all:
                UnblockAllContactsDialog.newInstance(account, adapter.getBlockedContacts()).show(getFragmentManager(), UnblockAllContactsDialog.class.getName());
                return true;
            case R.id.action_block_manual:
                BlockByJidDialog.newInstance(account).show(getSupportFragmentManager(), BlockByJidDialog.class.getName());
                return true;
            case R.id.action_unblock_selected:
                BlockingManager.getInstance().unblockContacts(account, adapter.getCheckedContacts(), this);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBlockedContactClick() {
        updateToolbar();
        updateMenu();
    }

    @Override
    public void onGroupInvitesClick() {
        adapter.setBlockedListState(GROUP_INVITES);
        adapter.onChange();
        updateToolbar();
        updateMenu();
    }

    private void restoreBlockList() {
        adapter.setBlockedListState(BLOCKED_LIST);
        adapter.onChange();
        updateToolbar();
        updateMenu();
    }

    private void updateMenu() {
        onPrepareOptionsMenu(toolbar.getMenu());
    }

    private void updateToolbar() {
        final ArrayList<ContactJid> checkedContacts = adapter.getCheckedContacts();

        final int currentSize = checkedContacts.size();

        if (currentSize == previousSize && previousState == adapter.getCurrentBlockListState()) {
            return;
        }

        if (currentSize == 0) {
            toolbar.setTitle(adapter.getCurrentBlockListState() == BLOCKED_LIST ? getString(R.string.blocked_contacts) : getString(R.string.blocked_group_invitations));
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
            else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
            LogManager.i(this, "toolbar.setTitle " + toolbar.getTitle());
            barPainter.updateWithAccountName(account);

            toolbar.setNavigationOnClickListener(v -> {
                if (adapter.getCurrentBlockListState() == BLOCKED_LIST) {
                    finish();
                } else {
                    restoreBlockList();
                }
            });

        } else {
            toolbar.setTitle(String.valueOf(currentSize));
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp);
            else toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
            LogManager.i(this, "toolbar.setTitle " + toolbar.getTitle());

            barPainter.setGrey();

            toolbar.setNavigationOnClickListener(v -> {
                adapter.setCheckedContacts(new ArrayList<>());
                adapter.onChange();
                update();
            });
        }

        previousSize = currentSize;
        previousState = adapter.getCurrentBlockListState();
    }

    @Override
    public void onBlockedListChanged(AccountJid account) {
        Application.getInstance().runOnUiThread(this::update);
    }

    public void update() {
        adapter.onChange();
        updateToolbar();
        updateMenu();
    }

    @Override
    public void onSuccessUnblock() {
        update();
        Toast.makeText(BlockedListActivity.this, getString(R.string.contacts_unblocked_successfully), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onErrorUnblock() {
        Toast.makeText(BlockedListActivity.this, getString(R.string.error_unblocking_contacts), Toast.LENGTH_SHORT).show();
    }

}
