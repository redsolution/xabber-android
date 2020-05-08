/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * <p>
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * <p>
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * <p>
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment.OnChooseListener;
import com.xabber.android.ui.dialog.ContactSubscriptionDialog;
import com.xabber.android.ui.dialog.EnterPassDialog;
import com.xabber.android.ui.dialog.TranslationDialog;
import com.xabber.android.ui.fragment.CallsFragment;
import com.xabber.android.ui.fragment.MainActivitySettingsFragment;
import com.xabber.android.ui.fragment.DiscoverFragment;
import com.xabber.android.ui.fragment.chatListFragment.ChatListFragment;
import com.xabber.android.ui.fragment.contactListFragment.ContactListFragment;
import com.xabber.android.ui.preferences.PreferenceEditor;
import com.xabber.android.ui.widget.bottomnavigation.BottomBar;
import com.xabber.xmpp.uri.XMPPUri;

import org.greenrobot.eventbus.EventBus;
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
public class MainActivity extends ManagedActivity implements OnAccountChangedListener,
        View.OnClickListener, OnChooseListener, ContactListFragment.ContactListFragmentListener,
        ChatListFragment.ChatListFragmentListener, MainActivitySettingsFragment.ContactListDrawerListener,
        BottomBar.OnClickListener {

    /**
     * Select contact to be invited to the room was requested.
     */
    private static final int CODE_OPEN_CHAT = 301;

    private static final long CLOSE_ACTIVITY_AFTER_DELAY = 300;

    private static final String SAVED_ACTION = "com.xabber.android.ui.activity.ContactList.SAVED_ACTION";
    private static final String SAVED_SEND_TEXT = "com.xabber.android.ui.activity.ContactList.SAVED_SEND_TEXT";

    private static final int DIALOG_CLOSE_APPLICATION_ID = 0x57;

    private static final String ACTION_CONTACT_SUBSCRIPTION = "com.xabber.android.ui.activity.SearchActivity.ACTION_CONTACT_SUBSCRIPTION";
    private static final String ACTION_CLEAR_STACK = "com.xabber.android.ui.activity.SearchActivity.ACTION_CLEAR_STACK";

    private static final String ACTIVE_FRAGMENT = "com.xabber.android.ui.activity.ContactList.ACTIVE_FRAGMENT";
    private static final String CONTACT_LIST_TAG = "CONTACT_LIST";
    private static final String CHAT_LIST_TAG = "CHAT_LIST";
    private static final String DISCOVER_TAG = "DISCOVER_TAG";
    private static final String CALLS_TAG = "CALLS_TAG";
    private static final String BOTTOM_BAR_TAG = "BOTTOM_BAR_TAG";

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public ActiveFragmentType currentActiveFragmentType = ActiveFragmentType.CHATS;
    /**
     * Current action.
     */
    private String action;
    private int unreadMessagesCount;
    private Fragment contentFragment;
    private Fragment currentActiveFragment = null;

    private ChatListFragment.ChatListState currentChatListState;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static Intent createPersistentIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    public static Intent createFragmentIntent(Context context, ActiveFragmentType fragment) {
        Intent intent = createIntent(context);
        Bundle bundle = new Bundle();
        bundle.putSerializable(ACTIVE_FRAGMENT, fragment);
        return intent.putExtra(ACTIVE_FRAGMENT, bundle);
    }

    public static Intent createClearStackIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(ACTION_CLEAR_STACK);
        return intent;
    }

    public static Intent createContactSubscriptionIntent(Context context, AccountJid account,
                                                         ContactJid user) {
        Intent intent = new EntityIntentBuilder(context, MainActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setAction(ACTION_CONTACT_SUBSCRIPTION);
        return intent;
    }

    private static AccountJid getRoomInviteAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static ContactJid getRoomInviteUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_contact_list);
        getWindow().setBackgroundDrawable(null);

        action = getIntent().getAction();
        getIntent().setAction(null);

        if (savedInstanceState != null) {
            currentActiveFragmentType = (ActiveFragmentType) savedInstanceState
                    .getSerializable(ACTIVE_FRAGMENT);
            if (currentActiveFragmentType == null)
                currentActiveFragmentType = ActiveFragmentType.CHATS;
        } else if (getIntent().hasExtra(ACTIVE_FRAGMENT)) {
            Bundle bundle = getIntent().getBundleExtra(ACTIVE_FRAGMENT);
            if (bundle != null) {
                currentActiveFragmentType = (ActiveFragmentType) bundle
                        .getSerializable(ACTIVE_FRAGMENT);
            }
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ACTIVE_FRAGMENT, currentActiveFragmentType);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        action = getIntent().getAction();
        getIntent().setAction(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    /**
     * Open chat with specified contact.
     * <p/>
     * Show dialog to choose account if necessary.
     *
     * @param user
     * @param text can be <code>null</code>.
     */
    private void openChat(ContactJid user, String text) {
        ContactJid bareAddress = user.getBareUserJid();
        ArrayList<BaseEntity> entities = new ArrayList<>();
        for (AbstractChat check : ChatManager.getInstance().getChats()) {
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
            openChat(rosterManager
                    .getBestContact(enabledAccounts.iterator().next(), bareAddress), text);
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
    private void openChat(AccountJid account, ContactJid user, String text) {
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

        if (!(AccountManager.getInstance().hasAccountsInRealm()
                || AccountManager.getInstance().hasAccounts())
                && XabberAccountManager.getInstance().getAccount() == null) {
            startActivity(TutorialActivity.createIntent(this));
            finish();
            return;
        }

        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);

        if (action != null) {
            switch (action) {
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

                            ContactJid user = null;
                            try {
                                user = ContactJid.from(xmppUri.getPath());
                            } catch (ContactJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }

                            if (user != null) {
                                openChat(user, text);
                            }
                        }
                    }
                    break;
                }
                case ACTION_CLEAR_STACK:
                    ActivityManager.getInstance().clearStack(false);
                    currentActiveFragmentType = ActiveFragmentType.CHATS;
                    break;

                case ACTION_CONTACT_SUBSCRIPTION:
                    action = null;
                    showContactSubscriptionDialog();
                    break;
            }
        }

        if (Application.getInstance().doNotify()) {
            if (!SettingsManager.isTranslationSuggested()) {
                Locale currentLocale = getResources().getConfiguration().locale;
                if (!currentLocale.getLanguage().equals("en")
                        && !getResources().getBoolean(R.bool.is_translated)) {
                    new TranslationDialog().show(getFragmentManager(), "TRANSLATION_DIALOG");
                }
            }
        }
        showPassDialogs();

        // remove all message notifications
        MessageNotificationManager.getInstance().removeAllMessageNotifications();
        showBottomNavigation();
        showSavedOrCurrentFragment(currentActiveFragmentType);
        setStatusBarColor();

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        updateUnreadCount();
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
                        } else
                            EnterPassDialog.newInstance(item).show(getFragmentManager(), EnterPassDialog.class.getName());
                    }
                }
            }
            XabberAccountManager.getInstance().clearXmppAccountsForCreate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    private void hideKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
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

    public void onChatListStateChanged(ChatListFragment.ChatListState chatListState) {
        currentChatListState = chatListState;
        getBottomBarFragment().setChatStateIcon(chatListState);
    }

    private void exit() {
        Application.getInstance().requestToClose();
        showDialog(DIALOG_CLOSE_APPLICATION_ID);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Close activity if application was not killed yet.
                finish();
            }
        }, CLOSE_ACTIVITY_AFTER_DELAY);
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
    public void onChatClick(AbstractContact contact) {
        //TODO fulfill this, maybe like method below
        onContactClick(contact);
    }

    @Override
    public void onContactClick(AbstractContact abstractContact) {
        if (action == null) {
            startActivityForResult(ChatActivity.createSendIntent(this,
                    abstractContact.getAccount(), abstractContact.getUser(), null),
                    CODE_OPEN_CHAT);
            return;
        }
        startActivityForResult(ChatActivity.createSpecificChatIntent(this,
                abstractContact.getAccount(), abstractContact.getUser()), CODE_OPEN_CHAT);
    }

    @Override
    public void onContactListChange(CommonState commonState) {
    }

    @Override
    public void onManageAccountsClick() {
        showMenuFragment();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        getBottomBarFragment().setColoredButton(currentActiveFragmentType);
        setStatusBarColor();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageChangedEvent(MessageUpdateEvent chatUpdatedEvent) {
        updateUnreadCount();
    }

    private void updateUnreadCount() {
        unreadMessagesCount = 0;
        for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts())
            if (abstractChat.notifyAboutMessage() && !abstractChat.isArchived())
                unreadMessagesCount += abstractChat.getUnreadMessageCount();
        getBottomBarFragment().setUnreadMessages(unreadMessagesCount);
    }

    @Override
    public void onChoose(AccountJid account, ContactJid user, String text) {
        openChat(account, user, text);
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

    /** Bottom Bar Chats click handler */
    @Override
    public void onChatsClick() {

        /* Show ChatsList fragment if another fragment on top */
        if (currentActiveFragmentType != ActiveFragmentType.CHATS) {
            showChatListFragment();
            return;
        }

        /* Scroll to top if has no unread */
        if (!getChatListFragment().isOnTop() && getChatListFragment().getListSize() != 0
                && unreadMessagesCount == 0) {
            getChatListFragment().scrollToTop();
            return;
        }

        /* Show recent if archived displayed */
        if (currentChatListState == ChatListFragment.ChatListState.archived) {
            getChatListFragment().onStateSelected(ChatListFragment.ChatListState.recent);
            return;
        }

        /* Toggle between recent and unread when has unread */
        if (unreadMessagesCount > 0 && getChatListFragment().getCurrentChatsState()
                != ChatListFragment.ChatListState.unread)
            getChatListFragment().onStateSelected(ChatListFragment.ChatListState.unread);
        else getChatListFragment().onStateSelected(ChatListFragment.ChatListState.recent);
    }

    @Override
    public void onContactsClick() {
        showContactListFragment(null);
        getBottomBarFragment().setChatStateIcon(ChatListFragment.ChatListState.recent);
        if (currentActiveFragmentType == ActiveFragmentType.CONTACTS)
            getContactListFragment().scrollTo(0);
    }

    @Override
    public void onSettingsClick() {
        showMenuFragment();
        getBottomBarFragment().setChatStateIcon(ChatListFragment.ChatListState.recent);
        setStatusBarColor();
    }

    @Override
    public void onCallsClick() {
        showCallsFragment();
        getBottomBarFragment().setChatStateIcon(ChatListFragment.ChatListState.recent);
        setStatusBarColor();
    }

    @Override
    public void onDiscoverClick() {
        showDiscoverFragment();
        getBottomBarFragment().setChatStateIcon(ChatListFragment.ChatListState.recent);
        setStatusBarColor();
    }

    private void onXabberAccountClick() {
        startActivity(XabberAccountActivity.createIntent(this));
    }

    private ContactListFragment getContactListFragment() {
        if (getSupportFragmentManager().findFragmentByTag(CONTACT_LIST_TAG) != null) {
            return (ContactListFragment) getSupportFragmentManager()
                    .findFragmentByTag(CONTACT_LIST_TAG);
        } else return ContactListFragment.newInstance(null);
    }

    private BottomBar getBottomBarFragment() {
        if (getSupportFragmentManager().findFragmentByTag(BOTTOM_BAR_TAG) != null) {
            return (BottomBar) getSupportFragmentManager().findFragmentByTag(BOTTOM_BAR_TAG);
        } else return BottomBar.Companion.newInstance();
    }

    private ChatListFragment getChatListFragment() {
        if (getSupportFragmentManager().findFragmentByTag(CHAT_LIST_TAG) != null) {
            return (ChatListFragment) getSupportFragmentManager().findFragmentByTag(CHAT_LIST_TAG);
        } else return ChatListFragment.newInstance(null);
    }

    private DiscoverFragment getDiscoverFragment() {
        if (getSupportFragmentManager().findFragmentByTag(DISCOVER_TAG) != null) {
            return (DiscoverFragment) getSupportFragmentManager().findFragmentByTag(DISCOVER_TAG);
        } else return DiscoverFragment.newInstance();
    }

    private CallsFragment getCallsFragment() {
        if (getSupportFragmentManager().findFragmentByTag(CALLS_TAG) != null) {
            return (CallsFragment) getSupportFragmentManager().findFragmentByTag(CALLS_TAG);
        } else return CallsFragment.newInstance();
    }

    private void showBottomNavigation() {
        if (!isFinishing()) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.containerBottomNavigation, getBottomBarFragment(), BOTTOM_BAR_TAG);
            fTrans.commit();
            if (currentActiveFragmentType != null)
                getBottomBarFragment().setColoredButton(currentActiveFragmentType);
        }
    }

    private void showSavedOrCurrentFragment(ActiveFragmentType fragment) {
        switch (fragment) {
            case CHATS:
                showChatListFragment();
                break;
            case CALLS:
                showCallsFragment();
                break;
            case CONTACTS:
                showContactListFragment(null);
                break;
            case DISCOVER:
                showDiscoverFragment();
                break;
            case SETTINGS:
                showMenuFragment();
                break;
        }
    }

    private void showMenuFragment() {
        if (!isFinishing()) {
            contentFragment = MainActivitySettingsFragment.newInstance();
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.container, contentFragment);
            fTrans.commit();
        }
    }

    private void showContactListFragment(@Nullable AccountJid account) {
        if (!isFinishing()) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.container, getContactListFragment(), CONTACT_LIST_TAG);
            fTrans.commit();
        }
    }

    private void showChatListFragment() {
        if (!isFinishing()) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.container, getChatListFragment(), CHAT_LIST_TAG);
            fTrans.commit();
        }
    }

    private void showDiscoverFragment() {
        if (!isFinishing()) {
            FragmentTransaction ftrans = getSupportFragmentManager().beginTransaction();
            ftrans.replace(R.id.container, getDiscoverFragment(), DISCOVER_TAG);
            ftrans.commit();
        }
    }

    private void showCallsFragment() {
        if (!isFinishing()) {
            FragmentTransaction ftrans = getSupportFragmentManager().beginTransaction();
            ftrans.replace(R.id.container, getCallsFragment(), CALLS_TAG);
            ftrans.commit();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        currentActiveFragment = fragment;
        switch (fragment.getClass().getSimpleName()) {
            case "ContactListFragment":
                currentActiveFragmentType = ActiveFragmentType.CONTACTS;
                break;
            case "ChatListFragment":
                currentActiveFragmentType = ActiveFragmentType.CHATS;
                break;
            case "MainActivitySettingsFragment":
                currentActiveFragmentType = ActiveFragmentType.SETTINGS;
                break;
            case "DiscoverFragment":
                currentActiveFragmentType = ActiveFragmentType.DISCOVER;
                break;
            case "CallsFragment":
                currentActiveFragmentType = ActiveFragmentType.CALLS;
                break;
        }
        getBottomBarFragment().setColoredButton(currentActiveFragmentType);
        setStatusBarColor();
    }

    private void showContactSubscriptionDialog() {
        Intent intent = getIntent();
        AccountJid account = getRoomInviteAccount(intent);
        ContactJid user = getRoomInviteUser(intent);
        if (account != null && user != null) {
            ContactSubscriptionDialog.newInstance(account, user).show(getFragmentManager(),
                    ContactSubscriptionDialog.class.getName());
        }
    }

    public void setStatusBarColor(AccountJid account) {
        StatusBarPainter.instanceUpdateWithAccountName(this, account);
    }

    public void setStatusBarColor() {
        StatusBarPainter.instanceUpdateWithDefaultColor(this);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            StatusBarPainter.instanceUpdateWithDefaultColor(this);
        else {
            TypedValue typedValue = new TypedValue();
            this.getTheme().resolveAttribute(R.attr.bars_color, typedValue, true);
            StatusBarPainter.instanceUpdateWIthColor(this, typedValue.data);
        }

    }

    public enum ActiveFragmentType {
        CHATS,
        CALLS,
        CONTACTS,
        DISCOVER,
        SETTINGS
    }

}
