package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.chat.groupchat.GroupchatManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.dialog.GroupchatInviteReasonDialog;
import com.xabber.android.ui.fragment.GroupchatInviteContactFragment;

import java.util.ArrayList;
import java.util.List;

public class GroupchatInviteContactActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener,
        GroupchatInviteContactFragment.OnNumberOfSelectedInvitesChanged, GroupchatInviteReasonDialog.GroupchatInviteReasonListener {

    private static final String LOG_TAG = GroupchatInviteContactActivity.class.getSimpleName();

    private AccountJid account;
    private ContactJid groupchatContact;

    private Toolbar toolbar;
    private BarPainter barPainter;
    private List<ContactJid> jidsToInvite;

    private int selectionCounter;

    public static Intent createIntent(Context context, AccountJid account, ContactJid groupchatJid) {
        return new EntityIntentBuilder(context, GroupchatInviteContactActivity.class)
                .setAccount(account)
                .setUser(groupchatJid)
                .build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static ContactJid getGroupchatContact(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        account = getAccount(intent);
        groupchatContact = getGroupchatContact(intent);

        setContentView(R.layout.activity_with_toolbar_and_container);

        boolean lightTheme = SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light;

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(lightTheme ? R.drawable.ic_arrow_left_grey_24dp : R.drawable.ic_arrow_left_white_24dp);
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setColorFilter(lightTheme ?
                            getResources().getColor(R.color.grey_900) : getResources().getColor(R.color.white),
                    PorterDuff.Mode.SRC_IN);
        }

        toolbar.inflateMenu(R.menu.toolbar_groupchat_list_selector);
        toolbar.setOnMenuItemClickListener(this);

        barPainter = new BarPainter(this, toolbar);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
                    GroupchatInviteContactFragment.newInstance(account, groupchatContact),
                    GroupchatInviteContactFragment.LOG_TAG).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToolbar();
        Toast.makeText(this, getString(R.string.groupchat_long_press_for_select_multiple_contacts), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_perform_on_selected:
               openInvitationDialog();
               break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void openInvitationDialog(){
        jidsToInvite = getInviteFragment().getSelectedContacts();
        GroupchatInviteReasonDialog dialog = new GroupchatInviteReasonDialog();
        dialog.show(getSupportFragmentManager(), GroupchatInviteReasonDialog.LOG_TAG);
    }

    public void openInvitationDialogForContact(ContactJid contactJid){
        jidsToInvite = new ArrayList<>();
        jidsToInvite.add(contactJid);
        GroupchatInviteReasonDialog dialog = new GroupchatInviteReasonDialog();
        dialog.show(getSupportFragmentManager(), GroupchatInviteReasonDialog.LOG_TAG);
    }

    private void updateMenu() {
        onPrepareOptionsMenu(toolbar.getMenu());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_perform_on_selected)
                .setVisible(selectionCounter > 0)
                .setTitle(getString(R.string.groupchat_invite));
        return true;
    }

    @Override
    public void onInviteCountChange(int newCount) {
        selectionCounter = newCount;
        updateToolbar();
    }

    private void updateToolbar() {
        if (selectionCounter == 0) {
            //todo change to resource
            toolbar.setTitle(getString(R.string.groupchat_invite_members));
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
            else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
            barPainter.updateWithAccountName(account);
            toolbar.setNavigationOnClickListener(v -> finish());
        } else {
            toolbar.setTitle(String.valueOf(selectionCounter));
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp);
            else toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
            barPainter.setGrey();
            toolbar.setNavigationOnClickListener(v -> {
                GroupchatInviteContactFragment fragment = getInviteFragment();
                if (fragment != null) {
                    fragment.cancelSelection();
                    selectionCounter = 0;
                }
                updateToolbar();
            });
        }
        updateMenu();
    }

    private GroupchatInviteContactFragment getInviteFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(GroupchatInviteContactFragment.LOG_TAG);
        if (fragment instanceof GroupchatInviteContactFragment) {
            return (GroupchatInviteContactFragment) fragment;
        }
        return null;
    }

    @Override
    public void onReasonSelected(String reason) {
        GroupchatInviteContactFragment fragment = getInviteFragment();
        if (fragment != null) {
            GroupchatManager.getInstance().sendGroupchatInvitations(account, groupchatContact, jidsToInvite, reason.trim());
            finish();
        }
    }

}
