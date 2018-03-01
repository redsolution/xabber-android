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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.extension.file.FileUtils;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomState;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.message.RegularChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnChatStateListener;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.ChatViewerAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.dialog.BlockContactDialog;
import com.xabber.android.ui.dialog.ContactDeleteDialogFragment;
import com.xabber.android.ui.fragment.ChatFragment;
import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
import com.xabber.android.ui.fragment.RecentChatFragment;
import com.xabber.android.ui.helper.NewContactTitleInflater;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.ui.preferences.ChatContactSettings;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Collection;

import static com.xabber.android.ui.adapter.ChatViewerAdapter.PAGE_POSITION_CHAT_INFO;
import static com.xabber.android.ui.adapter.ChatViewerAdapter.PAGE_POSITION_RECENT_CHATS;


/**
 * Chat activity.
 * <p/>
 *
 * @author alexander.ivanov
 */
public class ChatActivity extends ManagedActivity implements OnContactChangedListener,
        OnAccountChangedListener, OnChatStateListener, ViewPager.OnPageChangeListener,
        ChatFragment.ChatViewerFragmentListener, OnBlockedListChangedListener,
        RecentChatFragment.Listener, ChatViewerAdapter.FinishUpdateListener,
        ContactVcardViewerFragment.Listener, Toolbar.OnMenuItemClickListener {

    private static final String LOG_TAG = ChatActivity.class.getSimpleName();

    private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";
    private static final String ACTION_RECENT_CHATS = "com.xabber.android.data.RECENT_CHATS";
    private static final String ACTION_SPECIFIC_CHAT = "com.xabber.android.data.ACTION_SPECIFIC_CHAT";

    public static final String EXTRA_OTR_REQUEST = "com.xabber.android.data.EXTRA_OTR_REQUEST";
    public static final String EXTRA_OTR_PROGRESS = "com.xabber.android.data.EXTRA_OTR_PROGRESS";
//    public static final String ACTION_OTR_REQUEST = "com.xabber.android.data.ACTION_OTR_REQUEST";
//    public static final String ACTION_OTR_PROGRESS = "com.xabber.android.data.ACTION_OTR_PROGRESS";
    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 24;
    public final static String KEY_ACCOUNT = "KEY_ACCOUNT";
    public final static String KEY_USER = "KEY_USER";
    public final static String KEY_QUESTION = "KEY_QUESTION";
    public final static String KEY_SHOW_ARCHIVED = "KEY_SHOW_ARCHIVED";

    private static final String SAVE_SELECTED_PAGE = "com.xabber.android.ui.activity.ChatActivity.SAVE_SELECTED_PAGE";
    private static final String SAVE_SELECTED_ACCOUNT = "com.xabber.android.ui.activity.ChatActivity.SAVE_SELECTED_ACCOUNT";
    private static final String SAVE_SELECTED_USER = "com.xabber.android.ui.activity.ChatActivity.SAVE_SELECTED_USER";
    private static final String SAVE_EXIT_ON_SEND = "com.xabber.android.ui.activity.ChatActivity.SAVE_EXIT_ON_SEND";

    ChatViewerAdapter chatViewerAdapter;
    ViewPager viewPager;

    private String extraText = null;

    private StatusBarPainter statusBarPainter;

    private boolean isVisible;

    private AccountJid account;
    private UserJid user;
    private int selectedPagePosition;
    private boolean exitOnSend;

    private Animation shakeAnimation = null;

    @Nullable
    private ContactVcardViewerFragment contactVcardViewerFragment;
    @Nullable
    private ChatFragment chatFragment;
    @Nullable
    private RecentChatFragment recentChatFragment;

    private Toolbar toolbar;
    private View contactTitleView;
    private View showcaseView;
    private Button btnShowcaseGotIt;

    boolean showArchived = false;

    public boolean isShowArchived() {
        return showArchived;
    }

    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Nullable
    private static AccountJid getAccount(Intent intent) {
        AccountJid value = EntityIntentBuilder.getAccount(intent);
        if (value != null)
            return value;
        // Backward compatibility.

        String stringExtra = intent.getStringExtra("com.xabber.android.data.account");
        if (stringExtra == null) {
            return null;
        }

        try {
            return AccountJid.from(stringExtra);
        } catch (XmppStringprepException e) {
            LogManager.exception(LOG_TAG, e);
            return null;
        }
    }

    @Nullable
    private static UserJid getUser(Intent intent) {
        UserJid value = EntityIntentBuilder.getUser(intent);
        if (value != null)
            return value;
        // Backward compatibility.

        String stringExtra = intent.getStringExtra("com.xabber.android.data.user");
        if (stringExtra == null) {
            return null;
        }

        try {
            return UserJid.from(stringExtra);
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(LOG_TAG, e);
            return null;
        }
    }

    private static boolean hasAttention(Intent intent) {
        return ACTION_ATTENTION.equals(intent.getAction());
    }

    public static Intent createSpecificChatIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, ChatActivity.class).setAccount(account).setUser(user).build();
        intent.setAction(ACTION_SPECIFIC_CHAT);
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        intent.putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived());
        return intent;
    }

    public static Intent createRecentChatsIntent(Context context) {
        Intent intent = new EntityIntentBuilder(context, ChatActivity.class).build();
        intent.setAction(ACTION_RECENT_CHATS);
        return intent;
    }

    public static Intent createClearTopIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = createSpecificChatIntent(context, account, user);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    /**
     * Create intent to send message.
     * <p/>
     * Contact list will not be shown on when chat will be closed.
     * @param text    if <code>null</code> then user will be able to send a number
     *                of messages. Else only one message can be send.
     */
    public static Intent createSendIntent(Context context, AccountJid account, UserJid user, String text) {
        Intent intent = ChatActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        intent.putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived());
        return intent;
    }

    public static Intent createSendUriIntent(Context context, AccountJid account,
                                             UserJid user, Uri uri) {
        Intent intent = ChatActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        return intent;
    }

    public static Intent createAttentionRequestIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = ChatActivity.createClearTopIntent(context, account, user);
        intent.setAction(ACTION_ATTENTION);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogManager.i(LOG_TAG, "onCreate " + savedInstanceState);

        setContentView(R.layout.activity_chat);
        getWindow().setBackgroundDrawable(null);

        contactTitleView = findViewById(R.id.contact_title);
        contactTitleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPage(2, true);
            }
        });
        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(ChatActivity.this);
            }
        });

        showcaseView = findViewById(R.id.showcaseView);

        statusBarPainter = new StatusBarPainter(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        getInitialChatFromIntent();
        getSelectedPageDataFromIntent();

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        initChats();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogManager.i(LOG_TAG, "onNewIntent");

        setIntent(intent);

        getSelectedPageDataFromIntent();
        getInitialChatFromIntent();
        if (account != null && user != null)
            selectChat(account, user);
    }


    @Override
    protected void onResume() {
        super.onResume();
        LogManager.i(LOG_TAG, "onResume");

        updateToolbar();

        isVisible = true;

        Application.getInstance().addUIListener(OnChatStateListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);

        this.showArchived = getIntent().getBooleanExtra(KEY_SHOW_ARCHIVED, false);

        selectPage();

        Intent intent = getIntent();

        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(account, user);
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())
                && intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

            Uri receivedUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            handleShareFileUri(receivedUri);

        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            extraText = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (extraText != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                exitOnSend = true;
            }
        }

        if (intent.getBooleanExtra(EXTRA_OTR_REQUEST, false) ||
                intent.getBooleanExtra(EXTRA_OTR_PROGRESS, false)) {
            handleOtrIntent(intent);
        }

        //showcase
        if (!SettingsManager.chatShowcaseSuggested()) {
            showShowcase(true);
        }
    }

    public void handleShareFileUri(Uri fileUri) {
        final String path = FileUtils.getPath(this, fileUri);

        LogManager.i(this, String.format("File uri: %s, path: %s", fileUri, path));

        if (path == null) {
            Toast.makeText(this, R.string.could_not_get_path_to_file, Toast.LENGTH_SHORT).show();
            return;
        }

        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
            HttpFileUploadManager.getInstance().uploadFile(account, user, path);
        }
    }

    private void handleOtrIntent(Intent intent) {
        String account = intent.getStringExtra(KEY_ACCOUNT);
        String user = intent.getStringExtra(KEY_USER);
        String question = intent.getStringExtra(KEY_QUESTION);

        if (account != null && user != null) {
            try {
                AccountJid accountJid = AccountJid.from(account);
                UserJid userJid = UserJid.from(user);
                AbstractChat chat = MessageManager.getInstance().getOrCreateChat(accountJid, userJid);

                if (chat != null && chat instanceof RegularChat) {
                    if (intent.getBooleanExtra(EXTRA_OTR_PROGRESS, false)) {
                        ((RegularChat) chat).setIntent(QuestionActivity.createCancelIntent(
                                Application.getInstance(), accountJid, userJid));
                    } else {
                        ((RegularChat)chat).setIntent(QuestionActivity.createIntent(
                                Application.getInstance(),
                                accountJid, userJid, question != null, true, question));
                    }
                }
            } catch (UserJid.UserJidCreateException | XmppStringprepException e) {
                e.printStackTrace();
            }
        }
        getIntent().removeExtra(EXTRA_OTR_REQUEST);
        getIntent().removeExtra(EXTRA_OTR_PROGRESS);
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
        super.onBackPressed();
    }

    private void initChats() {
        if (account != null && user != null) {
            chatViewerAdapter = new ChatViewerAdapter(getFragmentManager(), this, account, user);
        } else {
            chatViewerAdapter = new ChatViewerAdapter(getFragmentManager(), this);
        }

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(chatViewerAdapter);
        viewPager.addOnPageChangeListener(this);

    }

    private void getInitialChatFromIntent() {
        Intent intent = getIntent();
        AccountJid newAccount = getAccount(intent);
        UserJid newUser = getUser(intent);

        if (newAccount != null) {
            this.account = newAccount;
        }
        if (newUser != null) {
            this.user = newUser;
        }
        LogManager.i(LOG_TAG, "getInitialChatFromIntent " + this.user);
    }

    private void getSelectedPageDataFromIntent() {
        Intent intent = getIntent();

        if (intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_RECENT_CHATS:
                selectedPagePosition = PAGE_POSITION_RECENT_CHATS;
                break;

            case ACTION_SPECIFIC_CHAT:
            case ACTION_ATTENTION:
            case Intent.ACTION_SEND:
                selectedPagePosition = ChatViewerAdapter.PAGE_POSITION_CHAT;
                break;
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        account = savedInstanceState.getParcelable(SAVE_SELECTED_ACCOUNT);
        user = savedInstanceState.getParcelable(SAVE_SELECTED_USER);
        selectedPagePosition = savedInstanceState.getInt(SAVE_SELECTED_PAGE);
        exitOnSend = savedInstanceState.getBoolean(SAVE_EXIT_ON_SEND);
    }

    private void selectPage() {
        selectPage(selectedPagePosition, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_SELECTED_PAGE, selectedPagePosition);
        outState.putParcelable(SAVE_SELECTED_ACCOUNT, account);
        outState.putParcelable(SAVE_SELECTED_USER, user);
        outState.putBoolean(SAVE_EXIT_ON_SEND, exitOnSend);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnChatStateListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
        MessageManager.getInstance().removeVisibleChat();
        isVisible = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        ChatManager.getInstance().clearScrollStates();
    }

    private void selectChatPage(BaseEntity chat, boolean smoothScroll) {
        account = chat.getAccount();
        user = chat.getUser();

        chatViewerAdapter.selectChat(account, user);

        if (chatFragment != null) {
            chatFragment.saveInputState();
            chatFragment.setChat(chat.getAccount(), chat.getUser());
        }

        selectPage(ChatViewerAdapter.PAGE_POSITION_CHAT, smoothScroll);

        if (contactVcardViewerFragment != null) {
            contactVcardViewerFragment.updateContact(account, user);
            contactVcardViewerFragment.requestVCard();
        }
    }

    public void selectPage(int position, boolean smoothScroll) {
        onPageSelected(position);
        viewPager.setCurrentItem(position, smoothScroll);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
        updateRecentChats();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        updateToolbar();
        updateChat();
        updateRecentChats();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        updateToolbar();
        updateChat();
        updateRecentChats();
        updateStatusBar();
    }

    @Override
    public void onChatStateChanged(Collection<RosterContact> entities) {
        updateToolbar();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        hideKeyboard(this);
    }

    @Override
    public void onPageSelected(int position) {
        selectedPagePosition = position;

        if (selectedPagePosition == PAGE_POSITION_RECENT_CHATS) {
            MessageManager.getInstance().removeVisibleChat();
        } else {
            if (isVisible) {
                MessageManager.getInstance().setVisibleChat(MessageManager.getInstance().getOrCreateChat(account, user));
                NotificationManager.getInstance()
                        .removeMessageNotification(account, user);
            }
        }

        if (chatFragment != null) {
            if (selectedPagePosition == ChatViewerAdapter.PAGE_POSITION_CHAT)
                chatFragment.showPlaceholder(false);
            else chatFragment.showPlaceholder(true);
        }

        updateToolbar();
        updateStatusBar();
    }

    private void updateToolbar() {
        NewContactTitleInflater.updateTitle(contactTitleView, this,
                RosterManager.getInstance().getBestContact(account, user), getNotifMode());
        toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
        if (selectedPagePosition == 1)
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        else if (selectedPagePosition == 2)
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_settings_white_24dp));

        setUpOptionsMenu(toolbar.getMenu());
    }

    private void updateStatusBar() {
        statusBarPainter.updateWithAccountName(account);
    }

    private void updateChat() {
        if (chatFragment != null) {
            chatFragment.updateContact();
        }
    }

    private void updateRecentChats() {
        if (recentChatFragment != null) {
            recentChatFragment.updateChats();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onChatViewAdapterFinishUpdate() {
        insertExtraText();
    }

    private void insertExtraText() {
        if (extraText == null) {
            return;
        }

        if (chatFragment != null) {
            chatFragment.setInputText(extraText);
            extraText = null;
        }
    }

    @Override
    public void onChatSelected(AccountJid accountJid, UserJid userJid) {
        selectChat(accountJid, userJid);
    }

    @Override
    public boolean isCurrentChat(String account, String user) {
        return this.account.toString().equals(account) && this.user.toString().equals(user);
    }

    private void selectChat(AccountJid accountJid, UserJid userJid) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(accountJid, userJid);
        selectChatPage(chat, true);
    }

    @Override
    public void registerRecentChatFragment(RecentChatFragment recentChatFragment) {
        this.recentChatFragment = recentChatFragment;
    }

    @Override
    public void unregisterRecentChatFragment() {
        this.recentChatFragment = null;
    }

    @Override
    public void onCloseChat() {
        close();
    }

    @Override
    public void onMessageSent() {
        if (exitOnSend) {
            close();
        }
    }

    @Override
    public void registerChatFragment(ChatFragment chatFragment) {
        this.chatFragment = chatFragment;
    }

    @Override
    public void unregisterChatFragment() {
        this.chatFragment = null;
    }

    private void close() {
        finish();
        if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
            if (!ActivityManager.getInstance().hasContactList(this)) {
                startActivity(ContactListActivity.createIntent(this));
            }
        }
    }

    @Override
    public void onBlockedListChanged(AccountJid account) {
        // if chat of blocked contact is currently opened, it should be closed
        final Collection<UserJid> blockedContacts = BlockingManager.getInstance().getBlockedContacts(account);
        if (blockedContacts.contains(user)) {
            close();
        }
    }

    @Override
    public void onVCardReceived() {}

    @Override
    public void registerVCardFragment(ContactVcardViewerFragment fragment) {
        this.contactVcardViewerFragment = fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setUpOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onMenuItemClick(item);
    }

    private void setUpRecentChatsMenu(Menu menu, AbstractChat abstractChat) {

    }

    private void setUpRegularChatMenu(Menu menu, AbstractChat abstractChat) {

        // archive/unarchive chat
        menu.findItem(R.id.action_archive_chat).setVisible(!abstractChat.isArchived());
        menu.findItem(R.id.action_unarchive_chat).setVisible(abstractChat.isArchived());

        // mute chat
        menu.findItem(R.id.action_mute_chat).setVisible(abstractChat.notifyAboutMessage());
        menu.findItem(R.id.action_unmute_chat).setVisible(!abstractChat.notifyAboutMessage());
    }

    private void setUpMUCInfoMenu(Menu menu, AbstractChat abstractChat) {
        RoomState chatState = ((RoomChat) abstractChat).getState();
        if (chatState == RoomState.unavailable)
            menu.findItem(R.id.action_join_conference).setVisible(true);
        else {
            menu.findItem(R.id.action_invite_to_chat).setVisible(true);

            if (chatState != RoomState.error) {
                menu.findItem(R.id.action_leave_conference).setVisible(true);
            }
        }

        menu.findItem(R.id.action_remove_contact).setVisible(false);
        menu.findItem(R.id.action_delete_conference).setVisible(true);

        menu.findItem(R.id.action_send_contact).setVisible(false);
        menu.findItem(R.id.action_edit_alias).setVisible(false);
        menu.findItem(R.id.action_edit_groups).setVisible(false);
        menu.findItem(R.id.action_block_contact).setVisible(false);
    }

    private void setUpMUCMenu(Menu menu, AbstractChat abstractChat) {

        RoomState chatState = ((RoomChat) abstractChat).getState();
        if (chatState == RoomState.error) {
            menu.findItem(R.id.action_authorization_settings).setVisible(true);
        }

        setUpRegularChatMenu(menu, abstractChat);
    }

    private void setUpContactInfoMenu(Menu menu, AbstractChat abstractChat) {
        // request subscription
        AbstractContact abstractContact = RosterManager.getInstance()
                .getAbstractContact(abstractChat.getAccount(), abstractChat.getUser());
        menu.findItem(R.id.action_request_subscription).setVisible(!abstractContact.isSubscribed());
    }

    private void setUpOptionsMenu(Menu menu) {

        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);
        if (abstractChat != null) {
            menu.clear();
            MenuInflater inflater = getMenuInflater();
            if (selectedPagePosition == PAGE_POSITION_RECENT_CHATS) {
                inflater.inflate(R.menu.menu_chat_recent_list, menu);
                setUpRecentChatsMenu(menu, abstractChat);
                return;
            }
            if (selectedPagePosition == PAGE_POSITION_CHAT_INFO) {
                inflater.inflate(R.menu.toolbar_contact, menu);
                setUpContactInfoMenu(menu, abstractChat);
                if (abstractChat instanceof RoomChat)
                    setUpMUCInfoMenu(menu, abstractChat);
                return;
            }
            if (abstractChat instanceof RoomChat) {
                inflater.inflate(R.menu.menu_chat_muc, menu);
                setUpMUCMenu(menu, abstractChat);
                return;
            }
            if (abstractChat instanceof RegularChat) {
                inflater.inflate(R.menu.menu_chat_regular, menu);
                setUpRegularChatMenu(menu, abstractChat);
                return;
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);

        switch (item.getItemId()) {
            /* security menu */

            case R.id.action_start_encryption:
                if (chatFragment != null)
                    chatFragment.showResourceChoiceAlert(account, user, false);
                return true;

            case R.id.action_restart_encryption:
                if (chatFragment != null)
                    chatFragment.showResourceChoiceAlert(account, user, true);
                return true;

            case R.id.action_stop_encryption:
                if (chatFragment != null)
                    chatFragment.stopEncryption(account, user);
                return true;

            case R.id.action_verify_with_fingerprint:
                startActivity(FingerprintActivity.createIntent(this, account, user));
                return true;

            case R.id.action_verify_with_question:
                startActivity(QuestionActivity.createIntent(this, account, user, true, false, null));
                return true;

            case R.id.action_verify_with_shared_secret:
                startActivity(QuestionActivity.createIntent(this, account, user, false, false, null));
                return true;

            /* regular chat options menu */

            case R.id.action_send_contact:
                sendContact();
                return true;

            case R.id.action_view_contact:
                if (chatFragment != null)
                    chatFragment.showContactInfo();
                return true;

            case R.id.action_chat_settings:
                startActivity(ChatContactSettings.createIntent(this, account, user));
                return true;

            case R.id.action_authorization_settings:
                startActivity(ConferenceAddActivity.createIntent(this, account, user.getBareUserJid()));
                return true;

//            case R.id.action_close_chat:
//                if (chatFragment != null)
//                chatFragment.closeChat(account, user);
//                return true;

            case R.id.action_clear_history:
                if (chatFragment != null)
                    chatFragment.clearHistory(account, user);
                return true;

            case R.id.action_export_chat:
                if (chatFragment != null)
                    chatFragment.onExportChatClick();
                return true;

            case R.id.action_call_attention:
                if (chatFragment != null)
                    chatFragment.callAttention();
                return true;

            case R.id.action_block_contact:
                BlockContactDialog.newInstance(account, user).show(getFragmentManager(), BlockContactDialog.class.getName());
                return true;

            case R.id.action_request_subscription:
                try {
                    PresenceManager.getInstance().requestSubscription(account, user);
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
                return true;

            case R.id.action_archive_chat:
                if (abstractChat != null) abstractChat.setArchived(true, true);
                return true;

            case R.id.action_unarchive_chat:
                if (abstractChat != null) abstractChat.setArchived(false, true);
                return true;

            case R.id.action_mute_chat:
                if (abstractChat != null) abstractChat.setNotificationState(
                        new NotificationState(NotificationState.NotificationMode.disabled,
                                0), true);
                setUpOptionsMenu(toolbar.getMenu());
                return true;

            case R.id.action_unmute_chat:
                if (abstractChat != null) abstractChat.setNotificationState(
                        new NotificationState(NotificationState.NotificationMode.enabled,
                                0), true);
                setUpOptionsMenu(toolbar.getMenu());
                return true;

            /* conference specific options menu */

            case R.id.action_join_conference:
                onJoinConferenceClick();
                return true;

            case R.id.action_invite_to_chat:
                startActivity(ContactListActivity.createRoomInviteIntent(this, account, user.getBareUserJid()));
                return true;

            case R.id.action_leave_conference:
                if (chatFragment != null)
                    chatFragment.leaveConference(account, user);
                return true;

            case R.id.action_show_archived:
                this.showArchived = !this.showArchived;
                if (recentChatFragment != null) {
                    recentChatFragment.closeSnackbar();
                    recentChatFragment.updateChats();
                    Toast.makeText(this,
                            this.showArchived ? R.string.toast_archived_show
                                    : R.string.toast_archived_hide, Toast.LENGTH_SHORT).show();
                }
                return true;

            /* contact info menu */

            case R.id.action_edit_alias:
                editAlias();
                return true;

            case R.id.action_edit_groups:
                startActivity(GroupEditActivity.createIntent(this, account, user));
                return true;

            case R.id.action_remove_contact:
                ContactDeleteDialogFragment.newInstance(account, user)
                        .show(getFragmentManager(), "CONTACT_DELETE");
                return true;

            case R.id.action_delete_conference:
                ContactDeleteDialogFragment.newInstance(account, user)
                        .show(getFragmentManager(), "CONTACT_DELETE");
                return true;

            default:
                return false;
        }
    }

    @Override
    public void playIncomingAnimation() {
        if (shakeAnimation == null) {
            shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake);
        }
        toolbar.findViewById(R.id.name_holder).startAnimation(shakeAnimation);
    }

    private NotificationState.NotificationMode getNotifMode() {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        NotificationState.NotificationMode mode = NotificationState.NotificationMode.bydefault;
        if (chat != null) {
            boolean defaultValue = chat instanceof RegularChat ? SettingsManager.eventsOnChat() : SettingsManager.eventsOnMuc();
            if (chat.getNotificationState().getMode() == NotificationState.NotificationMode.enabled && !defaultValue)
                mode = NotificationState.NotificationMode.enabled;
            if (chat.getNotificationState().getMode() == NotificationState.NotificationMode.disabled && defaultValue)
                mode = NotificationState.NotificationMode.disabled;
        }
        return mode;
    }

    private void editAlias() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_alias);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, user);
        if (rosterContact != null)
            input.setText(rosterContact.getName());
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RosterManager.getInstance().setName(account, user, input.getText().toString());
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void sendContact() {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, user);
        String text = rosterContact != null ? rosterContact.getName() + "\nxmpp:" + user.toString() : "xmpp:" + user.toString();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
    }

    public void onJoinConferenceClick() {
        MUCManager.getInstance().joinRoom(account, user.getJid().asEntityBareJidIfPossible(), true);
    }

    public void showShowcase(boolean show) {
        showcaseView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            btnShowcaseGotIt = (Button) findViewById(R.id.btnGotIt);
            btnShowcaseGotIt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsManager.setChatShowcaseSuggested();
                    showShowcase(false);
                }
            });
        }
    }
}
