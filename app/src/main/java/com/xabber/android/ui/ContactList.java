/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.ContactListFragment.OnContactClickListener;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment.OnChoosedListener;
import com.xabber.android.ui.dialog.ContactIntegrationDialogFragment;
import com.xabber.android.ui.dialog.StartAtBootDialogFragment;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.ui.preferences.AboutViewer;
import com.xabber.android.ui.preferences.AccountEditor;
import com.xabber.android.ui.preferences.PreferenceEditor;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.uri.XMPPUri;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Main application activity.
 *
 * @author alexander.ivanov
 */
public class ContactList extends ManagedActivity implements OnAccountChangedListener,
        View.OnClickListener, OnChoosedListener, OnContactClickListener, ContactListDrawerFragment.ContactListDrawerListener {

    /**
     * Select contact to be invited to the room was requested.
     */
    private static final String ACTION_ROOM_INVITE = "com.xabber.android.ui.ContactList.ACTION_ROOM_INVITE";

    private static final long CLOSE_ACTIVITY_AFTER_DELAY = 300;

    private static final String SAVED_ACTION = "com.xabber.android.ui.ContactList.SAVED_ACTION";
    private static final String SAVED_SEND_TEXT = "com.xabber.android.ui.ContactList.SAVED_SEND_TEXT";

    private static final int DIALOG_CLOSE_APPLICATION_ID = 0x57;

    private static final String CONTACT_LIST_TAG = "CONTACT_LIST";

    /**
     * Current action.
     */
    private String action;

    /**
     * Dialog related values.
     */
    private String sendText;

    private SearchView searchView;
    private BarPainter barPainter;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;

    public static Intent createPersistentIntent(Context context) {
        Intent intent = new Intent(context, ContactList.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, ContactList.class);
    }

    public static Intent createRoomInviteIntent(Context context, String account, String room) {
        Intent intent = new EntityIntentBuilder(context, ContactList.class)
                .setAccount(account).setUser(room).build();
        intent.setAction(ACTION_ROOM_INVITE);
        return intent;
    }

    private static String getRoomInviteAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static String getRoomInviteUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())
                || Intent.ACTION_SEND.equals(getIntent().getAction())
                || Intent.ACTION_SENDTO.equals(getIntent().getAction())
                || Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            ActivityManager.getInstance().startNewTask(this);
        }
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.contact_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setOnClickListener(this);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.application_title_short, R.string.application_title_short);
        drawerLayout.setDrawerListener(drawerToggle);

        getSupportActionBar().setHomeButtonEnabled(true);


        barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        setTitle(getString(R.string.application_title_full));

        if (savedInstanceState != null) {
            sendText = savedInstanceState.getString(SAVED_SEND_TEXT);
            action = savedInstanceState.getString(SAVED_ACTION);
        } else {
            getSupportFragmentManager().beginTransaction().add(R.id.container,
                    new ContactListFragment(), CONTACT_LIST_TAG).commit();

            sendText = null;
            action = getIntent().getAction();
        }
        getIntent().setAction(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        action = getIntent().getAction();
        getIntent().setAction(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_ACTION, action);
        outState.putString(SAVED_SEND_TEXT, sendText);
    }

    /**
     * Open chat with specified contact.
     * <p/>
     * Show dialog to choose account if necessary.
     *
     * @param user
     * @param text can be <code>null</code>.
     */
    private void openChat(String user, String text) {
        String bareAddress = Jid.getBareAddress(user);
        ArrayList<BaseEntity> entities = new ArrayList<>();
        for (AbstractChat check : MessageManager.getInstance().getChats()) {
            if (check.isActive() && check.getUser().equals(bareAddress)) {
                entities.add(check);
            }
        }
        if (entities.size() == 1) {
            openChat(entities.get(0), text);
            return;
        }
        entities.clear();
        for (RosterContact check : RosterManager.getInstance().getContacts()) {
            if (check.isEnabled() && check.getUser().equals(bareAddress)) {
                entities.add(check);
            }
        }
        if (entities.size() == 1) {
            openChat(entities.get(0), text);
            return;
        }
        Collection<String> accounts = AccountManager.getInstance()
                .getAccounts();
        if (accounts.isEmpty()) {
            return;
        }
        if (accounts.size() == 1) {
            openChat(new BaseEntity(accounts.iterator().next(), bareAddress), text);
            return;
        }
        AccountChooseDialogFragment.newInstance(bareAddress, text)
                .show(getFragmentManager(), "OPEN_WITH_ACCOUNT");
    }

    /**
     * Open chat with specified contact and enter text to be sent.
     *
     * @param baseEntity
     * @param text       can be <code>null</code>.
     */
    private void openChat(BaseEntity baseEntity, String text) {
        if (text == null) {
            startActivity(ChatViewer.createSendIntent(this,
                    baseEntity.getAccount(), baseEntity.getUser(), null));
        } else {
            startActivity(ChatViewer.createSendIntent(this,
                    baseEntity.getAccount(), baseEntity.getUser(), text));
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        barPainter.setDefaultColor();
        rebuildAccountToggle();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);

        if (action != null) {
            switch (action) {
                case ContactList.ACTION_ROOM_INVITE:
                case Intent.ACTION_SEND:
                case Intent.ACTION_CREATE_SHORTCUT:
                    if (Intent.ACTION_SEND.equals(action)) {
                        sendText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                    }
                    Toast.makeText(this, getString(R.string.select_contact), Toast.LENGTH_LONG).show();
                    break;
                case Intent.ACTION_VIEW: {
                    action = null;
                    Uri data = getIntent().getData();
                    if (data != null && "xmpp".equals(data.getScheme())) {
                        XMPPUri xmppUri;
                        try {
                            xmppUri = XMPPUri.parse(data);
                        } catch (IllegalArgumentException e) {
                            xmppUri = null;
                        }
                        if (xmppUri != null && "message".equals(xmppUri.getQueryType())) {
                            ArrayList<String> texts = xmppUri.getValues("body");
                            String text = null;
                            if (texts != null && !texts.isEmpty()) {
                                text = texts.get(0);
                            }
                            openChat(xmppUri.getPath(), text);
                        }
                    }
                    break;
                }
                case Intent.ACTION_SENDTO: {
                    action = null;
                    Uri data = getIntent().getData();
                    if (data != null) {
                        String path = data.getPath();
                        if (path != null && path.startsWith("/")) {
                            openChat(path.substring(1), null);
                        }
                    }
                    break;
                }
            }
        }

        if (Application.getInstance().doNotify()) {
            if (SettingsManager.bootCount() > 2 && !SettingsManager.connectionStartAtBoot()
                    && !SettingsManager.startAtBootSuggested()) {
                StartAtBootDialogFragment.newInstance().show(getFragmentManager(), "START_AT_BOOT");
            }
            if (!SettingsManager.contactIntegrationSuggested()
                    && Application.getInstance().isContactsSupported()) {
                if (AccountManager.getInstance().getAllAccounts().isEmpty()) {
                    SettingsManager.setContactIntegrationSuggested();
                } else {
                    ContactIntegrationDialogFragment.newInstance()
                            .show(getFragmentManager(), "CONTACT_INTEGRATION");
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    private void hideKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.contact_list, menu);
        setUpSearchView(menu);
        return true;
    }

    private void setUpSearchView(final Menu menu) {
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setQueryHint("Search contact");

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                menu.findItem(R.id.action_search).collapseActionView();
                return true;
            }
        });

        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.action_search), new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                        toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                LogManager.i(this, "onMenuItemActionCollapse");
                searchView.setQuery("", true);
                searchView.clearFocus();
                return true;
            }
        });


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Fragment fragmentById = getSupportFragmentManager().findFragmentById(R.id.container);
                ((ContactListFragment) fragmentById).getFilterableAdapter().getFilter().filter(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_search:
                searchView.setIconified(false);
                return true;
            case R.id.action_change_status:
                startActivity(StatusEditor.createIntent(this));
                return true;
            case R.id.action_add_contact:
                startActivity(ContactAdd.createIntent(this));
                return true;
            case R.id.action_close_chats:
                closeAllChats();
                return true;
            case R.id.action_join_conference:
                startActivity(ConferenceAdd.createIntent(this));
                return true;
            case R.id.action_chat_list:
                startActivity(ChatViewer.createRecentChatsIntent(this));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
            drawerLayout.closeDrawers();
            return;
        }
        super.onBackPressed();
    }

    private void exit() {
        Application.getInstance().requestToClose();
        showDialog(DIALOG_CLOSE_APPLICATION_ID);
        getContactListFragment().unregisterListeners();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Close activity if application was not killed yet.
                finish();
            }
        }, CLOSE_ACTIVITY_AFTER_DELAY);
    }

    private void closeAllChats() {
        for (AbstractChat chat : MessageManager.getInstance().getActiveChats()) {
            MessageManager.getInstance().closeChat(chat.getAccount(), chat.getUser());
            NotificationManager.getInstance().
                    removeMessageNotification(chat.getAccount(), chat.getUser());
        }
        getContactListFragment().getAdapter().onChange();
    }

    private ContactListFragment getContactListFragment() {
        return (ContactListFragment) getSupportFragmentManager().findFragmentByTag(CONTACT_LIST_TAG);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        super.onCreateDialog(id);
        switch (id) {
            case DIALOG_CLOSE_APPLICATION_ID:
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(getString(R.string.application_state_closing));
                progressDialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
                progressDialog.setIndeterminate(true);
                return progressDialog;
            default:
                return null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar_default:
                getContactListFragment().scrollUp();
                break;
        }
    }

    @Override
    public void onContactClick(AbstractContact abstractContact) {
        if (action == null) {
            startActivity(ChatViewer.createSpecificChatIntent(this,
                    abstractContact.getAccount(), abstractContact.getUser()));
            return;
        }
        switch (action) {
            case ACTION_ROOM_INVITE: {
                action = null;
                Intent intent = getIntent();
                String account = getRoomInviteAccount(intent);
                String user = getRoomInviteUser(intent);
                if (account != null && user != null) {
                    try {
                        MUCManager.getInstance().invite(account, user, abstractContact.getUser());
                    } catch (NetworkException e) {
                        Application.getInstance().onError(e);
                    }
                }
                finish();
                break;
            }
            case Intent.ACTION_SEND:
                action = null;
                startActivity(ChatViewer.createSendIntent(this,
                        abstractContact.getAccount(), abstractContact.getUser(), sendText));
                finish();
                break;
            case Intent.ACTION_CREATE_SHORTCUT: {
                createShortcut(abstractContact);
                finish();
                break;
            }
            default:
                startActivity(ChatViewer.createSpecificChatIntent(this,
                        abstractContact.getAccount(), abstractContact.getUser()));
                break;
        }
    }

    private void createShortcut(AbstractContact abstractContact) {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, ChatViewer.createShortCutIntent(this,
                abstractContact.getAccount(), abstractContact.getUser()));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, abstractContact.getName());
        Bitmap bitmap;
        if (MUCManager.getInstance().hasRoom(abstractContact.getAccount(),
                abstractContact.getUser())) {
            bitmap = AvatarManager.getInstance().getRoomBitmap(abstractContact.getUser());
        } else {
            bitmap = AvatarManager.getInstance().getUserBitmap(abstractContact.getUser());
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON,
                    AvatarManager.getInstance().createShortcutBitmap(bitmap));
        setResult(RESULT_OK, intent);
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        ((ContactListFragment)getSupportFragmentManager().findFragmentById(R.id.container)).onAccountsChanged();
        barPainter.setDefaultColor();
    }

    @Override
    public void onChoose(String account, String user, String text) {
        openChat(new BaseEntity(account, user), text);
    }

    private void rebuildAccountToggle() {
        ((ContactListFragment)getSupportFragmentManager().findFragmentById(R.id.container)).rebuild();
    }

    @Override
    public void onContactListDrawerListener(int viewId) {
        drawerLayout.closeDrawers();
        switch (viewId) {
            case R.id.drawer_action_settings:
                startActivity(PreferenceEditor.createIntent(this));
                break;
            case R.id.drawer_action_about:
                startActivity(AboutViewer.createIntent(this));
                break;
            case R.id.drawer_action_exit:
                exit();
                break;

        }
    }

    @Override
    public void onAccountSelected(String account) {
        drawerLayout.closeDrawers();
        startActivity(AccountEditor.createIntent(this, account));
    }
}
