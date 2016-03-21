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
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.extension.blocking.PrivateMucChatBlockingManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.adapter.ChatScrollIndicatorAdapter;
import com.xabber.android.ui.adapter.ChatViewerAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.fragment.ChatViewerFragment;
import com.xabber.android.ui.fragment.RecentChatFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collection;
import java.util.HashSet;


/**
 * Chat activity.
 * <p/>
 *
 * @author alexander.ivanov
 */
public class ChatViewer extends ManagedActivity implements OnContactChangedListener,
        OnAccountChangedListener, ViewPager.OnPageChangeListener,
        ChatViewerAdapter.FinishUpdateListener, RecentChatFragment.RecentChatFragmentInteractionListener,
        ChatViewerFragment.ChatViewerFragmentListener, OnBlockedListChangedListener {

    /**
     * Attention request.
     */
    private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";
    private static final String ACTION_RECENT_CHATS = "com.xabber.android.data.RECENT_CHATS";
    private static final String ACTION_SPECIFIC_CHAT = "com.xabber.android.data.ACTION_SPECIFIC_CHAT";
    private static final String ACTION_SHORTCUT = "com.xabber.android.data.ACTION_SHORTCUT";

    private static final String SAVED_INITIAL_ACCOUNT = "com.xabber.android.ui.activity.ChatViewer.SAVED_INITIAL_ACCOUNT";
    private static final String SAVED_INITIAL_USER = "com.xabber.android.ui.activity.ChatViewer.SAVED_INITIAL_USER";

    private static final String SAVED_IS_RECENT_CHATS_SELECTED = "com.xabber.android.ui.activity.ChatViewer.SAVED_IS_RECENT_CHATS_SELECTED";
    private static final String SAVED_SELECTED_ACCOUNT = "com.xabber.android.ui.activity.ChatViewer.SAVED_SELECTED_ACCOUNT";
    private static final String SAVED_SELECTED_USER = "com.xabber.android.ui.activity.ChatViewer.SAVED_SELECTED_USER";

    private static final String SAVED_EXIT_ON_SEND = "com.xabber.android.ui.activity.ChatViewer.EXIT_ON_SEND";
    ChatScrollIndicatorAdapter chatScrollIndicatorAdapter;
    ChatViewerAdapter chatViewerAdapter;
    ViewPager viewPager;
    Collection<ChatViewerFragment> registeredChats = new HashSet<>();
    Collection<RecentChatFragment> recentChatFragments = new HashSet<>();
    private boolean exitOnSend;
    private String extraText = null;
    private BaseEntity initialChat = null;
    private BaseEntity selectedChat = null;

    private StatusBarPainter statusBarPainter;

    private boolean isRecentChatsSelected;

    private boolean isVisible;

    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private static String getAccount(Intent intent) {
        String value = EntityIntentBuilder.getAccount(intent);
        if (value != null)
            return value;
        // Backward compatibility.
        return intent.getStringExtra("com.xabber.android.data.account");
    }

    private static String getUser(Intent intent) {
        String value = EntityIntentBuilder.getUser(intent);
        if (value != null)
            return value;
        // Backward compatibility.
        return intent.getStringExtra("com.xabber.android.data.user");
    }

    private static boolean hasAttention(Intent intent) {
        return ACTION_ATTENTION.equals(intent.getAction());
    }

    public static Intent createSpecificChatIntent(Context context, String account, String user) {
        Intent intent = new EntityIntentBuilder(context, ChatViewer.class).setAccount(account).setUser(user).build();
        intent.setAction(ACTION_SPECIFIC_CHAT);
        return intent;
    }

    public static Intent createRecentChatsIntent(Context context) {
        Intent intent = new EntityIntentBuilder(context, ChatViewer.class).build();
        intent.setAction(ACTION_RECENT_CHATS);
        return intent;
    }

    public static Intent createClearTopIntent(Context context, String account, String user) {
        Intent intent = createSpecificChatIntent(context, account, user);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    public static Intent createShortCutIntent(Context context, String account, String user) {
        Intent intent = createClearTopIntent(context, account, user);
        intent.setAction(ACTION_SHORTCUT);
        return intent;
    }

    /**
     * Create intent to send message.
     * <p/>
     * Contact list will not be shown on when chat will be closed.
     *
     * @param context
     * @param account
     * @param user
     * @param text    if <code>null</code> then user will be able to send a number
     *                of messages. Else only one message can be send.
     * @return
     */
    public static Intent createSendIntent(Context context, String account, String user, String text) {
        Intent intent = ChatViewer.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    public static Intent createAttentionRequestIntent(Context context, String account, String user) {
        Intent intent = ChatViewer.createClearTopIntent(context, account, user);
        intent.setAction(ACTION_ATTENTION);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.chat_viewer);
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
    protected void onStop() {
        super.onStop();
        ChatManager.getInstance().clearScrollStates();
    }


    private void initChats() {
        if (initialChat != null) {
            chatViewerAdapter = new ChatViewerAdapter(getFragmentManager(), initialChat, this);
        } else {
            chatViewerAdapter = new ChatViewerAdapter(getFragmentManager(), this);
        }

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(chatViewerAdapter);
        viewPager.setOnPageChangeListener(this);

        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                viewPager.setBackgroundDrawable(getResources().getDrawable(R.drawable.chat_background_repeat_dark));
            } else {
                viewPager.setBackgroundDrawable(getResources().getDrawable(R.drawable.chat_background_repeat));
            }
        } else {
            viewPager.setBackgroundColor(ColorManager.getInstance().getChatBackgroundColor());
        }

        chatScrollIndicatorAdapter = new ChatScrollIndicatorAdapter(this,
                (LinearLayout)findViewById(R.id.chat_scroll_indicator));
        chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
    }

    private void getInitialChatFromIntent() {
        Intent intent = getIntent();
        String account = getAccount(intent);
        String user = getUser(intent);
        if (account != null && user != null) {
            initialChat = new BaseEntity(account, user);
        }
    }

    private void getSelectedPageDataFromIntent() {
        Intent intent = getIntent();

        if (intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_RECENT_CHATS:
                isRecentChatsSelected = true;
                selectedChat = null;
                break;

            case ACTION_SPECIFIC_CHAT:
            case ACTION_ATTENTION:
            case Intent.ACTION_SEND:
            case ACTION_SHORTCUT:
                isRecentChatsSelected = false;
                selectedChat = new BaseEntity(getAccount(intent), getUser(intent));
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (isFinishing()) {
            return;
        }

        setIntent(intent);

        getSelectedPageDataFromIntent();

        if (intent.getAction() != null && intent.getAction().equals(ACTION_SHORTCUT)) {
            getInitialChatFromIntent();
            initChats();
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        isRecentChatsSelected = savedInstanceState.getBoolean(SAVED_IS_RECENT_CHATS_SELECTED);
        if (isRecentChatsSelected) {
            selectedChat = null;
        } else {
            selectedChat = new BaseEntity(savedInstanceState.getString(SAVED_SELECTED_ACCOUNT),
                    savedInstanceState.getString(SAVED_SELECTED_USER));
        }
        exitOnSend = savedInstanceState.getBoolean(SAVED_EXIT_ON_SEND);

        String initialAccount = savedInstanceState.getString(SAVED_INITIAL_ACCOUNT);
        String initialUser = savedInstanceState.getString(SAVED_INITIAL_USER);

        if (initialAccount != null && initialUser != null) {
            initialChat = new BaseEntity(initialAccount, initialUser);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;

        EventBus.getDefault().register(this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);

        chatViewerAdapter.updateChats();
        chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
        selectPage();

        Intent intent = getIntent();

        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(selectedChat);
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            extraText = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (extraText != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                exitOnSend = true;
            }
        }
    }

    private void selectPage() {
        if (isRecentChatsSelected) {
            selectRecentChatsPage();
        } else {
            selectChatPage(selectedChat, false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_IS_RECENT_CHATS_SELECTED, isRecentChatsSelected);
        if (!isRecentChatsSelected && selectedChat != null) {
            outState.putString(SAVED_SELECTED_ACCOUNT, selectedChat.getAccount());
            outState.putString(SAVED_SELECTED_USER, selectedChat.getUser());
        }
        outState.putBoolean(SAVED_EXIT_ON_SEND, exitOnSend);

        if (initialChat != null) {
            outState.putString(SAVED_INITIAL_ACCOUNT, initialChat.getAccount());
            outState.putString(SAVED_INITIAL_USER, initialChat.getUser());
        }
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

    private void selectChatPage(BaseEntity chat, boolean smoothScroll) {
        selectPage(chatViewerAdapter.getPageNumber(chat), smoothScroll);
    }

    private void selectRecentChatsPage() {
        selectPage(chatViewerAdapter.getPageNumber(null), false);
    }

    private void selectPage(int position, boolean smoothScroll) {
        onPageSelected(position);
        viewPager.setCurrentItem(position, smoothScroll);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
        if (chatViewerAdapter.updateChats()) {
            chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
            selectPage();
        } else {
            updateRegisteredRecentChatsFragments();
            updateStatusBar();
        }
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        for (BaseEntity contact : entities) {
            for (ChatViewerFragment chat : registeredChats) {
                if (chat.isEqual(contact)) {
                    chat.updateContact();
                }
            }
        }

        updateRegisteredRecentChatsFragments();
        updateStatusBar();
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        updateRegisteredChats();
        updateRegisteredRecentChatsFragments();
        updateStatusBar();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        hideKeyboard(this);
        chatScrollIndicatorAdapter.select(chatViewerAdapter.getRealPagePosition(position));

        selectedChat = chatViewerAdapter.getChatByPageNumber(position);
        isRecentChatsSelected = selectedChat == null;

        if (isRecentChatsSelected) {
            MessageManager.getInstance().removeVisibleChat();
        } else {
            if (isVisible) {
                MessageManager.getInstance().setVisibleChat(selectedChat);
            }

            NotificationManager.getInstance().removeMessageNotification(selectedChat.getAccount(), selectedChat.getUser());
        }

        updateStatusBar();
    }

    private void updateStatusBar() {
        if (isRecentChatsSelected) {
            statusBarPainter.updateWithDefaultColor();
        } else {
            statusBarPainter.updateWithAccountName(selectedChat.getAccount());
        }
    }

    private void updateRegisteredChats() {
        for (ChatViewerFragment chat : registeredChats) {
            chat.updateContact();
        }
    }

    private void updateRegisteredRecentChatsFragments() {
        for (RecentChatFragment recentChatFragment : recentChatFragments) {
            recentChatFragment.updateChats(chatViewerAdapter.getActiveChats());
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    public void registerRecentChatsList(RecentChatFragment recentChatFragment) {
        recentChatFragments.add(recentChatFragment);
    }

    public void unregisterRecentChatsList(RecentChatFragment recentChatFragment) {
        recentChatFragments.remove(recentChatFragment);
    }

    @Override
    public void onChatViewAdapterFinishUpdate() {
        insertExtraText();
    }

    private void insertExtraText() {

        if (extraText == null) {
            return;
        }

        boolean isExtraTextInserted = false;

        for (ChatViewerFragment chat : registeredChats) {
            if (chat.isEqual(selectedChat)) {
                chat.setInputText(extraText);
                isExtraTextInserted = true;
            }
        }

        if (isExtraTextInserted) {
            extraText = null;
        }
    }

    @Override
    public void onChatSelected(AbstractChat chat) {
        selectChatPage(chat, true);
    }

    public ChatViewerAdapter getChatViewerAdapter() {
        return chatViewerAdapter;
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
    public void registerChat(ChatViewerFragment chat) {
        registeredChats.add(chat);
    }

    @Override
    public void unregisterChat(ChatViewerFragment chat) {
        registeredChats.remove(chat);
    }

    private void close() {
        finish();
        if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
            if (!ActivityManager.getInstance().hasContactList(this)) {
                startActivity(ContactList.createIntent(this));
            }
        }
    }

    @Override
    public void onBlockedListChanged(String account) {
        // if chat of blocked contact is currently opened, it should be closed
        if (selectedChat != null) {
            final Collection<String> blockedContacts = BlockingManager.getInstance().getBlockedContacts(account);
            if (blockedContacts.contains(selectedChat.getUser())) {
                close();
            }

            final Collection<String> blockedMucContacts = PrivateMucChatBlockingManager.getInstance().getBlockedContacts(account);
            if (blockedMucContacts.contains(selectedChat.getUser())) {
                close();
            }
        }
    }
}
