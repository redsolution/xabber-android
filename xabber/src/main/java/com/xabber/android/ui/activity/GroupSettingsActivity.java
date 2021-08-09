package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.OnGroupSelectorListToolbarActionResultListener;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.groups.GroupBlockListFragment;
import com.xabber.android.ui.fragment.groups.GroupInvitesFragment;
import com.xabber.android.ui.fragment.groups.GroupchatInfoFragment.GroupchatSelectorListItemActions;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

public class GroupSettingsActivity extends ManagedActivity implements
        Toolbar.OnMenuItemClickListener, GroupchatSelectorListItemActions,
        OnGroupSelectorListToolbarActionResultListener {

    private static final String GROUPCHAT_SETTINGS_TYPE = "GROUPCHAT_SETTINGS_TYPE";
    private AccountJid account;
    private ContactJid groupchatContact;
    private GroupchatSettingsType settingsType;

    private Toolbar toolbar;
    private ProgressBar toolbarProgress;
    private boolean activeProgress;
    private BarPainter barPainter;

    private int selectionCounter = 0;

    public static Intent createIntent(Context context, AccountJid account, ContactJid groupchatJid, GroupchatSettingsType type) {
        Intent intent = new EntityIntentBuilder(context, GroupSettingsActivity.class)
                .setAccount(account)
                .setUser(groupchatJid)
                .build();
        intent.putExtra(GROUPCHAT_SETTINGS_TYPE, type);
        return intent;
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static ContactJid getGroupchatContact(Intent intent) {
        return EntityIntentBuilder.getContactJid(intent);
    }

    private static GroupchatSettingsType getSettingsType(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Serializable type = bundle.getSerializable(GROUPCHAT_SETTINGS_TYPE);
            if (type instanceof GroupchatSettingsType) {
                return (GroupchatSettingsType) type;
            }
        }
        return GroupchatSettingsType.None;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        account = getAccount(intent);
        groupchatContact = getGroupchatContact(intent);
        settingsType = getSettingsType(intent);

        if (settingsType == GroupchatSettingsType.None) {
            finish();
        }

        setContentView(R.layout.activity_with_toolbar_progress_and_container);

        boolean lightTheme = SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light;

        toolbar = findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(lightTheme ? R.drawable.ic_arrow_left_grey_24dp : R.drawable.ic_arrow_left_white_24dp);
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setColorFilter(lightTheme ?
                            getResources().getColor(R.color.grey_900) : getResources().getColor(R.color.white),
                    PorterDuff.Mode.SRC_IN);
        }

        toolbar.inflateMenu(R.menu.toolbar_groupchat_list_selector);
        toolbar.setOnMenuItemClickListener(this);

        toolbarProgress = findViewById(R.id.toolbarProgress);

        barPainter = new BarPainter(this, toolbar);

        if (savedInstanceState == null) {
            Fragment fragment = null;
            switch (settingsType) {
                case Settings:
                case Restrictions:
                    break;
                case Invitations:
                    fragment = GroupInvitesFragment.newInstance(account, groupchatContact);
                    break;
                case Blocked:
                    fragment = GroupBlockListFragment.newInstance(account, groupchatContact);
                    break;
                default:
                    finish();
            }
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment, settingsType.name()).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnGroupSelectorListToolbarActionResultListener.class, this);
        updateToolbar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnGroupSelectorListToolbarActionResultListener.class, this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_perform_on_selected) {
            Fragment fragment = getCurrentFragment();
            if (fragment instanceof GroupchatSelectorListToolbarActions) {
                ((GroupchatSelectorListToolbarActions) fragment).actOnSelection();
                showToolbarProgress(true);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showToolbarProgress(boolean show) {
        activeProgress = show;
        toolbarProgress.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        updateToolbar();
    }

    @Override
    public void onListItemSelected() {
        selectionCounter++;
        updateToolbar();
    }

    @Override
    public void onListItemDeselected() {
        selectionCounter--;
        updateToolbar();
    }

    private void updateToolbar() {
        switch (settingsType) {
            case Invitations:
            case Blocked:
                if (selectionCounter == 0 || activeProgress) {
                    toolbar.setTitle(settingsType.toString());
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
                    //barPainter.setGrey();
                    toolbar.setNavigationOnClickListener(v -> {
                        Fragment fragment = getCurrentFragment();
                        if (fragment instanceof GroupchatSelectorListToolbarActions) {
                            ((GroupchatSelectorListToolbarActions) fragment).cancelSelection();
                            selectionCounter = 0;
                            updateToolbar();
                        }
                    });
                }
                break;
        }
        updateMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        switch (settingsType) {
            case Invitations:
            case Blocked:
                getMenuInflater().inflate(R.menu.toolbar_groupchat_list_selector, menu);
                break;
        }
        return true;
    }

    private void updateMenu() {
        onPrepareOptionsMenu(toolbar.getMenu());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (settingsType) {
            case Blocked:
                menu.findItem(R.id.action_perform_on_selected).setVisible(selectionCounter > 0 && !activeProgress);
                menu.findItem(R.id.action_perform_on_selected).setTitle(R.string.groupchat_unblock);
                break;
            case Invitations:
                menu.findItem(R.id.action_perform_on_selected).setVisible(selectionCounter > 0 && !activeProgress);
                menu.findItem(R.id.action_perform_on_selected).setTitle(getString(R.string.groupchat_revoke));
                break;
        }
        return true;
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentByTag(settingsType.name());
    }

    @Override
    public void onActionSuccess(@NotNull AccountJid account, @NotNull ContactJid groupchatJid,
                                @NotNull List<String> successfulJids) {
        if (checkIfWrongEntity(account, groupchatJid)) return;
        Application.getInstance().runOnUiThread(() -> {
            selectionCounter -= successfulJids.size();
            showToolbarProgress(false);
        });
    }

    @Override
    public void onPartialSuccess(@NotNull AccountJid account, @NotNull ContactJid groupchatJid,
                                 @NotNull List<String> successfulJids, @NotNull List<String> failedJids) {
        Application.getInstance().runOnUiThread(() -> onActionSuccess(account, groupchatJid, successfulJids));
    }

    @Override
    public void onActionFailure(@NotNull AccountJid account, @NotNull ContactJid groupchatJid,
                                @NotNull List<String> failedJids) {
        if (checkIfWrongEntity(account, groupchatJid)) return;
        Application.getInstance().runOnUiThread(() -> showToolbarProgress(false));
    }

    private boolean checkIfWrongEntity(AccountJid account, ContactJid groupchatJid) {
        if (account == null) return true;
        if (groupchatJid == null) return true;
        if (!account.getBareJid().equals(this.account.getBareJid())) return true;
        return !groupchatJid.getBareJid().equals(this.groupchatContact.getBareJid());
    }

    public enum GroupchatSettingsType {
        None, Settings, Restrictions, Invitations, Blocked
    }

    public interface GroupchatSelectorListToolbarActions {
        void actOnSelection();
        void cancelSelection();
    }

}
