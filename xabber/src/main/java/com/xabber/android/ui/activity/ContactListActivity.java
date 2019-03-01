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
package com.xabber.android.ui.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.http.CrowdfundingManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.presentation.mvp.contactlist.ContactListPresenter;
import com.xabber.android.presentation.ui.contactlist.ContactListFragment;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment.OnChooseListener;
import com.xabber.android.ui.dialog.ContactSubscriptionDialog;
import com.xabber.android.ui.dialog.EnterPassDialog;
import com.xabber.android.ui.dialog.MucInviteDialog;
import com.xabber.android.ui.dialog.MucPrivateChatInvitationDialog;
import com.xabber.android.ui.dialog.TranslationDialog;
import com.xabber.android.ui.fragment.ContactListDrawerFragment;
import com.xabber.android.ui.preferences.PreferenceEditor;
import com.xabber.android.ui.widget.bottomnavigation.BottomMenu;
import com.xabber.xmpp.uri.XMPPUri;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import rx.subscriptions.CompositeSubscription;

/**
 * Main application activity.
 *
 * @author alexander.ivanov
 */
public class ContactListActivity extends ManagedActivity implements OnAccountChangedListener,
        View.OnClickListener, OnChooseListener, ContactListFragment.ContactListFragmentListener,
        ContactListDrawerFragment.ContactListDrawerListener, BottomMenu.OnClickListener {

    /**
     * Select contact to be invited to the room was requested.
     */
    private static final int CODE_OPEN_CHAT = 301;

    private static final String ACTION_CLEAR_STACK = "com.xabber.android.ui.activity.ContactList.ACTION_CLEAR_STACK";
    private static final String ACTION_ROOM_INVITE = "com.xabber.android.ui.activity.ContactList.ACTION_ROOM_INVITE";
    private static final String ACTION_MUC_PRIVATE_CHAT_INVITE = "com.xabber.android.ui.activity.ContactList.ACTION_MUC_PRIVATE_CHAT_INVITE";
    private static final String ACTION_CONTACT_SUBSCRIPTION = "com.xabber.android.ui.activity.ContactList.ACTION_CONTACT_SUBSCRIPTION";
    private static final String ACTION_INCOMING_MUC_INVITE = "com.xabber.android.ui.activity.ContactList.ACTION_INCOMING_MUC_INVITE";

    private static final long CLOSE_ACTIVITY_AFTER_DELAY = 300;

    private static final String SAVED_ACTION = "com.xabber.android.ui.activity.ContactList.SAVED_ACTION";
    private static final String SAVED_SEND_TEXT = "com.xabber.android.ui.activity.ContactList.SAVED_SEND_TEXT";

    private static final int DIALOG_CLOSE_APPLICATION_ID = 0x57;

    private static final String CONTACT_LIST_TAG = "CONTACT_LIST";
    private static final String LOG_TAG = ContactListActivity.class.getSimpleName();

    /**
     * Current action.
     */
    private String action;

    /**
     * Dialog related values.
     */
    private String sendText;

    private BottomMenu bottomMenu;
    private Fragment contentFragment;

    private View showcaseView;
    private Button btnShowcaseGotIt;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static Intent createPersistentIntent(Context context) {
        Intent intent = new Intent(context, ContactListActivity.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, ContactListActivity.class);
    }

    public static Intent createClearStackIntent(Context context) {
        Intent intent = new Intent(context, ContactListActivity.class);
        intent.setAction(ACTION_CLEAR_STACK);
        return intent;
    }

    public static Intent createRoomInviteIntent(Context context, AccountJid account, UserJid room) {
        Intent intent = new EntityIntentBuilder(context, ContactListActivity.class)
                .setAccount(account).setUser(room).build();
        intent.setAction(ACTION_ROOM_INVITE);
        return intent;
    }

    public static Intent createMucPrivateChatInviteIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, ContactListActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setAction(ACTION_MUC_PRIVATE_CHAT_INVITE);
        return intent;
    }

    public static Intent createContactSubscriptionIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, ContactListActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setAction(ACTION_CONTACT_SUBSCRIPTION);
        return intent;
    }

    public static Intent createMucInviteIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, ContactListActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setAction(ACTION_INCOMING_MUC_INVITE);
        return intent;
    }

    private static AccountJid getRoomInviteAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static UserJid getRoomInviteUser(Intent intent) {
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

        if (ACTION_CLEAR_STACK.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
        }

        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        if (!isTaskRoot() && !ACTION_ROOM_INVITE.equals(getIntent().getAction())) {
            finish();
            return;
        }

        setContentView(R.layout.activity_contact_list);
        getWindow().setBackgroundDrawable(null);

        if (savedInstanceState != null) {
            sendText = savedInstanceState.getString(SAVED_SEND_TEXT);
            action = savedInstanceState.getString(SAVED_ACTION);
        } else {
            showContactListFragment(null);

            sendText = null;
            action = getIntent().getAction();
        }
        getIntent().setAction(null);

        showcaseView = findViewById(R.id.showcaseView);
        btnShowcaseGotIt = (Button) findViewById(R.id.btnGotIt);
        btnShowcaseGotIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsManager.setContactShowcaseSuggested();
                showShowcase(false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
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
        outState.putString(SAVED_ACTION, action);
        outState.putString(SAVED_SEND_TEXT, sendText);
        super.onSaveInstanceState(outState);
    }

    /**
     * Open chat with specified contact.
     * <p/>
     * Show dialog to choose account if necessary.
     *
     * @param user
     * @param text can be <code>null</code>.
     */
    private void openChat(UserJid user, String text) {
        UserJid bareAddress = user.getBareUserJid();
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

        Collection<AccountJid> enabledAccounts = AccountManager.getInstance().getEnabledAccounts();
        RosterManager rosterManager = RosterManager.getInstance();

        for (AccountJid accountJid : enabledAccounts) {
            RosterContact rosterContact = rosterManager.getRosterContact(accountJid, user);
            if (rosterContact != null && rosterContact.isEnabled()) {
                entities.add(rosterContact);
            }
        }

        if (entities.size() == 1) {
            openChat(entities.get(0), text);
            return;
        }

        if (enabledAccounts.isEmpty()) {
            return;
        }
        if (enabledAccounts.size() == 1) {
            openChat(rosterManager.getBestContact(enabledAccounts.iterator().next(), bareAddress), text);
            return;
        }
        AccountChooseDialogFragment.newInstance(bareAddress, text)
                .show(getFragmentManager(), "OPEN_WITH_ACCOUNT");
    }

    /**
     * Open chat with specified contact and enter text to be sent.
     *
     * @param text       can be <code>null</code>.
     */
    private void openChat(AccountJid account, UserJid user, String text) {
        if (text == null) {
            startActivity(ChatActivity.createSendIntent(this, account, user, null));
        } else {
            startActivity(ChatActivity.createSendIntent(this, account, user, text));
        }
        finish();
    }

    private void openChat(BaseEntity entity, String text) {
        openChat(entity.getAccount(), entity.getUser(), text);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!AccountManager.getInstance().hasAccounts() && XabberAccountManager.getInstance().getAccount() == null) {
            startActivity(TutorialActivity.createIntent(this));
            finish();
            return;
        }

        rebuildAccountToggle();
        setStatusBarColor();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);

        if (action != null) {
            switch (action) {
                case ContactListActivity.ACTION_ROOM_INVITE:
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

                            UserJid user = null;
                            try {
                                user = UserJid.from(xmppUri.getPath());
                            } catch (UserJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }

                            if (user != null) {
                                openChat(user, text);
                            }
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
                            try {
                                UserJid user = UserJid.from(path.substring(1));
                                openChat(user, null);
                            } catch (UserJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }
                        }
                    }
                    break;
                }

                case ContactListActivity.ACTION_MUC_PRIVATE_CHAT_INVITE:
                    action = null;
                    showMucPrivateChatDialog();
                    break;

                case ContactListActivity.ACTION_CONTACT_SUBSCRIPTION:
                    action = null;
                    showContactSubscriptionDialog();
                    break;

                case ContactListActivity.ACTION_INCOMING_MUC_INVITE:
                    action = null;
                    showMucInviteDialog();
                    break;
            }
        }

        if (Application.getInstance().doNotify()) {
            if (!SettingsManager.isTranslationSuggested()) {
                Locale currentLocale = getResources().getConfiguration().locale;
                if (!currentLocale.getLanguage().equals("en") && !getResources().getBoolean(R.bool.is_translated)) {
                    new TranslationDialog().show(getFragmentManager(), "TRANSLATION_DIALOG");
                }
            }
        }

        showPassDialogs();

        //showcase
        if (!SettingsManager.contactShowcaseSuggested()) {
            showShowcase(true);
        }

        // update crowdfunding info
        CrowdfundingManager.getInstance().onLoad();

        // remove all message notifications
        MessageNotificationManager.getInstance().removeAllMessageNotifications();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        showBottomNavigation();
    }

    public void showPassDialogs() {
        List<XMPPAccountSettings> items = XabberAccountManager.getInstance().getXmppAccountsForCreate();
        if (items != null && items.size() > 0) {
            for (XMPPAccountSettings item : items) {
                if (XabberAccountManager.getInstance().isAccountSynchronize(item.getJid()) || SettingsManager.isSyncAllAccounts()) {
                    if (!item.isDeleted() && XabberAccountManager.getInstance().getExistingAccount(item.getJid()) == null) {
                        if (item.getToken() != null && !item.getToken().isEmpty()) {
                            // create account if exist token
                            try {
                                AccountJid accountJid = AccountManager.getInstance().addAccount(item.getJid(),
                                        "", item.getToken(), false, true,
                                        true, false, false,
                                        true, false);
                                AccountManager.getInstance().setColor(accountJid, ColorManager.getInstance().convertColorNameToIndex(item.getColor()));
                                AccountManager.getInstance().setOrder(accountJid, item.getOrder());
                                AccountManager.getInstance().setTimestamp(accountJid, item.getTimestamp());
                                AccountManager.getInstance().onAccountChanged(accountJid);
                            } catch (NetworkException e) {
                                Application.getInstance().onError(e);
                            }
                            // require pass if token not exist
                        } else EnterPassDialog.newInstance(item).show(getFragmentManager(), EnterPassDialog.class.getName());
                    }
                }
            }
            XabberAccountManager.getInstance().clearXmppAccountsForCreate();
        }
    }

    private void showMucInviteDialog() {
        Intent intent = getIntent();
        AccountJid account = getRoomInviteAccount(intent);
        UserJid user = getRoomInviteUser(intent);
        if (account != null && user != null) {
            MucInviteDialog.newInstance(account, user).show(getFragmentManager(), MucInviteDialog.class.getName());
        }
    }

    private void showContactSubscriptionDialog() {
        Intent intent = getIntent();
        AccountJid account = getRoomInviteAccount(intent);
        UserJid user = getRoomInviteUser(intent);
        if (account != null && user != null) {
            ContactSubscriptionDialog.newInstance(account, user).show(getFragmentManager(), ContactSubscriptionDialog.class.getName());
        }
    }

    private void showMucPrivateChatDialog() {
        Intent intent = getIntent();
        AccountJid account = getRoomInviteAccount(intent);
        UserJid user = getRoomInviteUser(intent);
        if (account != null && user != null) {
            MucPrivateChatInvitationDialog.newInstance(account, user).show(getFragmentManager(), MucPrivateChatInvitationDialog.class.getName());
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
        getMenuInflater().inflate(R.menu.toolbar_contact_list, menu);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void exit() {
        Application.getInstance().requestToClose();
        showDialog(DIALOG_CLOSE_APPLICATION_ID);
        //getContactListFragment().unregisterListeners();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Close activity if application was not killed yet.
                finish();
            }
        }, CLOSE_ACTIVITY_AFTER_DELAY);
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
                getContactListFragment().scrollTo(0);
                break;
        }
    }

    @Override
    public void onContactClick(AbstractContact abstractContact) {
        if (contentFragment != null)
            ((ContactListFragment) contentFragment).filterContactList("");
        if (bottomMenu != null) bottomMenu.closeSearch();

        if (action == null) {
            startActivityForResult(ChatActivity.createSendIntent(this, abstractContact.getAccount(),
                    abstractContact.getUser(), null), CODE_OPEN_CHAT);
            return;
        }
        switch (action) {
            case ACTION_ROOM_INVITE: {
                action = null;
                Intent intent = getIntent();
                AccountJid account = getRoomInviteAccount(intent);
                UserJid user = getRoomInviteUser(intent);
                if (account != null && user != null) {
                    try {
                        MUCManager.getInstance().invite(account, user.getJid().asEntityBareJidIfPossible(), abstractContact.getUser());
                    } catch (NetworkException e) {
                        Application.getInstance().onError(e);
                    }
                }
                finish();
                break;
            }
            case Intent.ACTION_SEND:
                if (!isSharedText(getIntent().getType())) {
                    // share file
                    if (getIntent().getExtras() != null) {
                        action = null;
                        startActivity(ChatActivity.createSendUriIntent(this,
                                abstractContact.getAccount(), abstractContact.getUser(),
                                (Uri)getIntent().getParcelableExtra(Intent.EXTRA_STREAM)));
                        finish();
                    }
                } else {
                    action = null;
                    startActivity(ChatActivity.createSendIntent(this,
                            abstractContact.getAccount(), abstractContact.getUser(), sendText));
                    finish();
                }
                break;
            case Intent.ACTION_CREATE_SHORTCUT: {
                createShortcut(abstractContact);
                finish();
                break;
            }
            case ChatActivity.ACTION_FORWARD: {
                forwardMessages(abstractContact, getIntent());
                break;
            }
            default:
                startActivityForResult(ChatActivity.createSpecificChatIntent(this, abstractContact.getAccount(),
                        abstractContact.getUser()), CODE_OPEN_CHAT);
                break;
        }
    }

    @Override
    public void onContactListChange(CommonState commonState) {}

    @Override
    public void onManageAccountsClick() {
        showMenuFragment();
    }

    private void createShortcut(AbstractContact abstractContact) {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, ChatActivity.createClearTopIntent(this,
                abstractContact.getAccount(), abstractContact.getUser()));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, abstractContact.getName());
        Bitmap bitmap;
        if (MUCManager.getInstance().hasRoom(abstractContact.getAccount(),
                abstractContact.getUser())) {
            bitmap = AvatarManager.getInstance().getRoomBitmap(abstractContact.getUser());
        } else {
            bitmap = AvatarManager.getInstance().getUserBitmap(abstractContact.getUser(), abstractContact.getName());
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON,
                    AvatarManager.getInstance().createShortcutBitmap(bitmap));
        setResult(RESULT_OK, intent);
    }

    private void forwardMessages(AbstractContact abstractContact, Intent intent) {
        ArrayList<String> messages = intent.getStringArrayListExtra(ChatActivity.KEY_MESSAGES_ID);
        if (messages != null)
            startActivity(ChatActivity.createForwardIntent(this,
                    abstractContact.getAccount(), abstractContact.getUser(), messages));
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        rebuildAccountToggle();
        setStatusBarColor();
    }

    @Override
    public void onChoose(AccountJid account, UserJid user, String text) {
        openChat(account, user, text);
    }

    private void rebuildAccountToggle() {
        BottomMenu bottomMenu = ((BottomMenu)getSupportFragmentManager().findFragmentById(R.id.containerBottomNavigation));
        if (bottomMenu != null)
            bottomMenu.update();
    }

    @Override
    public void onContactListDrawerListener(int viewId) {
        switch (viewId) {
            case R.id.drawer_action_settings:
                startActivity(PreferenceEditor.createIntent(this));
                break;
            case R.id.drawer_action_about:
                startActivity(AboutActivity.createIntent(this));
                break;
            case R.id.drawer_action_exit:
                exit();
                break;
            case R.id.drawer_header_action_xmpp_accounts:
                startActivity(PreferenceEditor.createIntent(this));
                break;
            case R.id.drawer_header_action_xabber_account:
                onXabberAccountClick();
                break;
            case R.id.drawer_action_patreon:
                startActivity(PatreonAppealActivity.createIntent(this));
                break;
        }
    }

    @Override
    public void onAccountSelected(AccountJid account) {
        startActivity(AccountActivity.createIntent(this, account));
    }

    @Override
    public void onRecentClick() {
        if (contentFragment != null && contentFragment instanceof ContactListFragment) {
            ((ContactListFragment) contentFragment).showRecent();
            ((ContactListFragment) contentFragment).scrollTo(0);
            ((ContactListFragment) contentFragment).closeSnackbar();
        } else showContactListFragment(null);
    }

    @Override
    public void onMenuClick() {
        //drawerLayout.openDrawer(Gravity.START);
        showMenuFragment();
    }

    @Override
    public void onAccountShortcutClick(AccountJid jid) {
        if (contentFragment != null && contentFragment instanceof ContactListFragment) {
            //((ContactListFragment) contentFragment).showRecent();
            ((ContactListFragment) contentFragment).scrollToAccount(jid);
            ((ContactListFragment) contentFragment).closeSnackbar();
        } else showContactListFragment(jid);
    }

    @Override
    public void onSearch(String filter) {
        if (contentFragment != null && contentFragment instanceof ContactListFragment)
            ((ContactListFragment) contentFragment).filterContactList(filter);
        else showContactListFragment(null);
    }

    @Override
    public void onSearchClick() {
        if (contentFragment != null && contentFragment instanceof ContactListFragment) {
            ((ContactListFragment) contentFragment).closeSnackbar();
        } else showContactListFragment(null);
    }

    private void onXabberAccountClick() {
        startActivity(XabberAccountActivity.createIntent(this));
    }

    private void showBottomNavigation() {
        if (!isFinishing()) {
            if (bottomMenu == null)
                bottomMenu = BottomMenu.newInstance();

            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.containerBottomNavigation, bottomMenu);
            fTrans.commit();
        }
    }

    private void showMenuFragment() {
        if (!isFinishing()) {
            contentFragment = ContactListDrawerFragment.newInstance();

            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.container, contentFragment);
            fTrans.commit();
        }
    }

    private void showContactListFragment(@Nullable AccountJid account) {
        if (!isFinishing()) {
            contentFragment = ContactListFragment.newInstance(account);

            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.container, contentFragment, CONTACT_LIST_TAG);
            fTrans.commit();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadMessagesCountChanged(ContactListPresenter.UpdateUnreadCountEvent event) {
        if (bottomMenu != null)
            bottomMenu.setUnreadMessages(event.getCount());
    }

    public void setStatusBarColor(AccountJid account) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
        }
    }

    public void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (contentFragment != null && contentFragment instanceof ContactListFragment ) {
            ContactListPresenter.ChatListState currentState = ((ContactListFragment) contentFragment).getListState();
            if (requestCode == CODE_OPEN_CHAT &&
                    (currentState == (ContactListPresenter.ChatListState.unread)
                    || currentState == (ContactListPresenter.ChatListState.archived))) {
                ((ContactListFragment) contentFragment).showRecent();
            }
        }
    }

    private boolean isSharedText(String type) {
        return type.contains("text/plain");
    }

    public void showShowcase(boolean show) {
        showcaseView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    public void closeSearch() {
        if (bottomMenu != null) bottomMenu.closeSearch();
    }
}
