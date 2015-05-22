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
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.helper.ChatScroller;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.ui.helper.StatusBarPainter;

/**
 * Chat activity.
 * <p/>
 *
 * @author alexander.ivanov
 */
public class ChatViewer extends ManagedActivity implements ChatScroller.ChatScrollerListener, ChatScroller.ChatScrollerProvider {

    /**
     * Attention request.
     */
    private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";
    private static final String ACTION_RECENT_CHATS = "com.xabber.android.data.RECENT_CHATS";
    private static final String ACTION_SPECIFIC_CHAT = "com.xabber.android.data.ACTION_SPECIFIC_CHAT";
    private static final String ACTION_SHORTCUT = "com.xabber.android.data.ACTION_SHORTCUT";

    private static final String SAVED_INITIAL_ACCOUNT = "com.xabber.android.ui.ChatViewer.SAVED_INITIAL_ACCOUNT";
    private static final String SAVED_INITIAL_USER = "com.xabber.android.ui.ChatViewer.SAVED_INITIAL_USER";

    private static final String SAVED_IS_RECENT_CHATS_SELECTED = "com.xabber.android.ui.ChatViewer.SAVED_IS_RECENT_CHATS_SELECTED";
    private static final String SAVED_SELECTED_ACCOUNT = "com.xabber.android.ui.ChatViewer.SAVED_SELECTED_ACCOUNT";
    private static final String SAVED_SELECTED_USER = "com.xabber.android.ui.ChatViewer.SAVED_SELECTED_USER";

    private static final String SAVED_EXIT_ON_SEND = "com.xabber.android.ui.ChatViewer.EXIT_ON_SEND";

    private ChatScroller chatScroller;

    private StatusBarPainter statusBarPainter;

    private BaseEntity initialChat = null;

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

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        LinearLayout chatScrollIndicatorLayout = (LinearLayout) findViewById(R.id.chat_scroll_indicator);

        chatScroller = new ChatScroller(this, viewPager, chatScrollIndicatorLayout);

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

    private void initChats() {
        chatScroller.initChats(initialChat);
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
                chatScroller.setSelectedChat(null);
                break;

            case ACTION_SPECIFIC_CHAT:
            case ACTION_ATTENTION:
            case Intent.ACTION_SEND:
            case ACTION_SHORTCUT:
                chatScroller.setSelectedChat(new BaseEntity(getAccount(intent), getUser(intent)));
                break;
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        boolean isRecentChatSelected = savedInstanceState.getBoolean(SAVED_IS_RECENT_CHATS_SELECTED);
        if (isRecentChatSelected) {
            chatScroller.setSelectedChat(null);
        } else {
            chatScroller.setSelectedChat(new BaseEntity(savedInstanceState.getString(SAVED_SELECTED_ACCOUNT),
                    savedInstanceState.getString(SAVED_SELECTED_USER)));
        }
        chatScroller.setExitOnSend(savedInstanceState.getBoolean(SAVED_EXIT_ON_SEND));

        String initialAccount = savedInstanceState.getString(SAVED_INITIAL_ACCOUNT);
        String initialUser = savedInstanceState.getString(SAVED_INITIAL_USER);

        if (initialAccount != null && initialUser != null) {
            initialChat = new BaseEntity(initialAccount, initialUser);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatScroller.setIsVisible(true);

        Application.getInstance().addUIListener(OnChatChangedListener.class, chatScroller);
        Application.getInstance().addUIListener(OnContactChangedListener.class, chatScroller);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, chatScroller);

        chatScroller.update();

        Intent intent = getIntent();

        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(chatScroller.getSelectedChat());
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (extraText != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                chatScroller.setExtraText(extraText);
                chatScroller.setExitOnSend(true);
            }
        }
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        BaseEntity selectedChat = chatScroller.getSelectedChat();

        outState.putBoolean(SAVED_IS_RECENT_CHATS_SELECTED, selectedChat == null);

        if (selectedChat != null) {
            outState.putString(SAVED_SELECTED_ACCOUNT, selectedChat.getAccount());
            outState.putString(SAVED_SELECTED_USER, selectedChat.getUser());
        }

        outState.putBoolean(SAVED_EXIT_ON_SEND, chatScroller.isExitOnSend());

        outState.putString(SAVED_INITIAL_ACCOUNT, initialChat.getAccount());
        outState.putString(SAVED_INITIAL_USER, initialChat.getUser());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnChatChangedListener.class, chatScroller);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, chatScroller);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, chatScroller);
        chatScroller.setIsVisible(false);
    }

    @Override
    public void onAccountSelected(String account) {
        if (account == null) {
            statusBarPainter.updateWithDefaultColor();
        } else {
            statusBarPainter.updateWithAccountName(account);
        }
    }

    @Override
    public void onClose() {
        finish();
        if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
            if (!ActivityManager.getInstance().hasContactList(this)) {
                startActivity(ContactList.createIntent(this));
            }
        }
    }

    @Override
    public ChatScroller getChatScroller() {
        return chatScroller;
    }
}
