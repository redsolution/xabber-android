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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.ui.adapter.ChatViewerAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.fragment.ChatFragment;
import com.xabber.android.ui.fragment.RecentChatFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Collection;


/**
 * Chat activity.
 * <p/>
 *
 * @author alexander.ivanov
 */
public class ChatActivity extends ManagedActivity implements OnContactChangedListener,
        OnAccountChangedListener, ViewPager.OnPageChangeListener,
        ChatFragment.ChatViewerFragmentListener, OnBlockedListChangedListener,
        RecentChatFragment.Listener, ChatViewerAdapter.FinishUpdateListener {

    private static final String LOG_TAG = ChatActivity.class.getSimpleName();

    private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";
    private static final String ACTION_RECENT_CHATS = "com.xabber.android.data.RECENT_CHATS";
    private static final String ACTION_SPECIFIC_CHAT = "com.xabber.android.data.ACTION_SPECIFIC_CHAT";

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

    @Nullable
    private ChatFragment chatFragment;
    @Nullable
    private RecentChatFragment recentChatFragment;


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
        selectChat(account, user);
    }


    @Override
    protected void onResume() {
        super.onResume();
        LogManager.i(LOG_TAG, "onResume");

        isVisible = true;

        EventBus.getDefault().register(this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);

        selectPage();

        Intent intent = getIntent();

        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(chatFragment.getAccount(), chatFragment.getUser());
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            extraText = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (extraText != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                exitOnSend = true;
            }
        }
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

        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                viewPager.setBackgroundDrawable(getResources().getDrawable(R.drawable.chat_background_repeat_dark));
            } else {
                viewPager.setBackgroundDrawable(getResources().getDrawable(R.drawable.chat_background_repeat));
            }
        } else {
            viewPager.setBackgroundColor(ColorManager.getInstance().getChatBackgroundColor());
        }

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
                selectedPagePosition = ChatViewerAdapter.PAGE_POSITION_RECENT_CHATS;
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
        EventBus.getDefault().unregister(this);
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

        if (chatFragment == null) {
            chatViewerAdapter.selectChat(chat.getAccount(), chat.getUser());
        } else {
            chatFragment.saveInputState();
            chatFragment.setChat(chat.getAccount(), chat.getUser());
        }
        selectPage(ChatViewerAdapter.PAGE_POSITION_CHAT, smoothScroll);
    }

    private void selectPage(int position, boolean smoothScroll) {
        onPageSelected(position);
        viewPager.setCurrentItem(position, smoothScroll);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
        updateRecentChats();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        updateChat();
        updateRecentChats();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        updateChat();
        updateRecentChats();
        updateStatusBar();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        hideKeyboard(this);
    }

    @Override
    public void onPageSelected(int position) {
        selectedPagePosition = position;

        if (selectedPagePosition == ChatViewerAdapter.PAGE_POSITION_RECENT_CHATS) {
            MessageManager.getInstance().removeVisibleChat();
        } else {
            if (isVisible) {
                MessageManager.getInstance().setVisibleChat(MessageManager.getInstance().getOrCreateChat(account, user));
                NotificationManager.getInstance()
                        .removeMessageNotification(account, user);
            }
        }

        updateStatusBar();
    }

    private void updateStatusBar() {
        if (selectedPagePosition == ChatViewerAdapter.PAGE_POSITION_RECENT_CHATS || account == null) {
            statusBarPainter.updateWithDefaultColor();
        } else {
            statusBarPainter.updateWithAccountName(account);
        }
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
    public void onChatSelected(BaseEntity chat) {
        selectChat(chat.getAccount(), chat.getUser());
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
}
