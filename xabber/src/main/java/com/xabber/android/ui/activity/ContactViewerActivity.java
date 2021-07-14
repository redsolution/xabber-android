package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.dialog.BlockContactDialog;
import com.xabber.android.ui.dialog.ChatExportDialogFragment;
import com.xabber.android.ui.dialog.ChatHistoryClearDialog;
import com.xabber.android.ui.dialog.ContactDeleteDialog;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.utils.Utils;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.util.ArrayList;

public class ContactViewerActivity extends ContactActivity implements Toolbar.OnMenuItemClickListener {

    private static final int PERMISSIONS_REQUEST_EXPORT_CHAT = 27;

    public static Intent createIntent(Context context, AccountJid account, ContactJid user) {
        return new EntityIntentBuilder(context, ContactViewerActivity.class)
                .setAccount(account).setUser(user).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = getToolbar();

        toolbar.setOnMenuItemClickListener(this);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        if (toolbar.getOverflowIcon() != null) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE
                    && SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                toolbar.getOverflowIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            }
        }

        setSupportActionBar(toolbar);

        onCreateOptionsMenu(toolbar.getMenu());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getUser());
        menu.clear();

        if (isGroupchat)
            getMenuInflater().inflate(R.menu.group_info_toolbar_menu, menu);
        else getMenuInflater().inflate(R.menu.toolbar_contact, menu);

        setUpContactInfoMenu(menu, rosterContact);

        return true;
    }

    private void setUpContactInfoMenu(Menu menu, RosterContact contact) {
        if (!isGroupchat){
            if (contact == null) {
                menu.setGroupVisible(R.id.roster_actions, false);
                menu.findItem(R.id.action_add_contact).setVisible(true);
                menu.findItem(R.id.action_request_subscription).setVisible(false);
                changeTextColor();
                manageAvailableUsernameSpace();
            } else {
                getTitleView().setOnClickListener(v -> startActivity(ContactEditActivity.createIntent(v.getContext(), getAccount(), getUser())));
                menu.findItem(R.id.action_add_contact).setVisible(false);
                menu.findItem(R.id.action_generate_qrcode).setVisible(orientation == Configuration.ORIENTATION_PORTRAIT);
                menu.findItem(R.id.action_request_subscription).setVisible(!contact.isSubscribed() && !RosterManager.getInstance().hasSubscriptionPending(getAccount(), getUser()));
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getUser());
        setUpContactInfoMenu(menu, rosterContact);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_contact:
                addContact();
                return true;

            case R.id.action_request_subscription:
                try {
                    PresenceManager.INSTANCE.requestSubscription(getAccount(), getUser());
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
                return true;

            case R.id.action_send_contact:
                sendContact();
                return true;

            case R.id.action_generate_qrcode:
                generateQR();
                return true;

            case R.id.action_clear_history:
                ChatHistoryClearDialog.Companion.newInstance(
                        getAccount(),
                        getUser()).show(getSupportFragmentManager(),
                        ChatHistoryClearDialog.class.getSimpleName()
                );
                return true;

            case R.id.action_export_chat:
                if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_EXPORT_CHAT)) {
                    ChatExportDialogFragment.newInstance(getAccount(), getUser()).show(getSupportFragmentManager(), "CHAT_EXPORT");
                }
                return true;

            case R.id.action_block_contact:
                BlockContactDialog.newInstance(getAccount(), getUser()).show(getSupportFragmentManager(), BlockContactDialog.class.getName());
                return true;

            case R.id.action_edit_contact:
                startActivity(ContactEditActivity.createIntent(this, getAccount(), getUser()));
                return true;

            case R.id.action_remove_contact:
                ContactDeleteDialog.newInstance(getAccount(), getUser())
                        .show(getSupportFragmentManager(), ContactDeleteDialog.class.getName());
                return true;

            case R.id.action_group_settings:
                startActivity(GroupchatUpdateSettingsActivity.Companion
                        .createOpenGroupchatSettingsIntentForGroupchat(getAccount(), getUser()));
                return true;

            case R.id.action_group_default_restrictions:
                startActivity(GroupDefaultRestrictionsActivity.Companion.createIntent(this,
                        getAccount(), getUser()));
                return true;

            case R.id.action_group_invitations:
                startActivity(GroupSettingsActivity.createIntent(this, getAccount(),
                        getUser(), GroupSettingsActivity.GroupchatSettingsType.Invitations));
                return true;

            case R.id.action_group_blocked:
                startActivity(GroupSettingsActivity.createIntent(this, getAccount(),
                        getUser(), GroupSettingsActivity.GroupchatSettingsType.Blocked));
                return true;

            case R.id.action_search_members:
                startActivity(FilterGroupMembersActivity.Companion.createIntent(this,
                        getAccount(), getUser()));

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void editAlias() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_alias);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getUser());
        input.setText(rosterContact.getName());
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> RosterManager.getInstance().setName(getAccount(), getUser(), input.getText().toString()));

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addContact() {
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progressBar.setIndeterminateTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
        }
        getToolbar().getMenu().findItem(R.id.add_contact_progress)
                .setActionView(progressBar)
                .setVisible(true);
        getToolbar().getMenu().findItem(R.id.action_add_contact).setVisible(false);
        progressBar.getLayoutParams().height = Utils.dipToPx(24f, this);
        progressBar.getLayoutParams().width = Utils.dipToPx(48f, this);
        progressBar.setPadding(0,0,Utils.dipToPx(24f, this), 0);
        progressBar.requestLayout();

        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            AbstractContact bestContact = RosterManager.getInstance().getBestContact(getAccount(), getUser());
            String name = bestContact != null ? bestContact.getName() : getUser().toString();

            try {
                RosterManager.getInstance().createContact(getAccount(), getUser(), name, new ArrayList<String>());
                PresenceManager.INSTANCE.addAutoAcceptSubscription(getAccount(), getUser());
                stopAddContactProcess(true);
            } catch (SmackException.NotLoggedInException
                    | XMPPException.XMPPErrorException
                    | SmackException.NotConnectedException
                    | InterruptedException
                    | SmackException.NoResponseException
                    | NetworkException e) {
                LogManager.exception(getClass().getSimpleName(), e);
                stopAddContactProcess(false);
            }
        });
    }

    private void stopAddContactProcess(final boolean success) {
        Application.getInstance().runOnUiThread(() -> {
            getToolbar().getMenu().findItem(R.id.add_contact_progress).setVisible(false);
            if (success) {
                onCreateOptionsMenu(getToolbar().getMenu());
            }
        });
    }

    private void sendContact() {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getUser());
        String text = rosterContact != null ? rosterContact.getName() + "\nxmpp:" + getUser().toString() : "xmpp:" + getUser().toString();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
    }

    private void changeTextColor() {
        TextView view = findViewById(R.id.action_add_contact);
        if (view != null) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                view.setTextColor(getResources().getColor(R.color.white));
            } else {
                if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                    view.setTextColor(getResources().getColor(R.color.grey_900));
                else view.setTextColor(getResources().getColor(R.color.white));
            }
        }
    }

}
