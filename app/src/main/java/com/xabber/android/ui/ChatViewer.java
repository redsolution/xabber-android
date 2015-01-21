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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;

import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.adapter.ChatViewerAdapter;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.ui.widget.PageSwitcher;
import com.xabber.android.ui.widget.PageSwitcher.OnSelectListener;
import com.xabber.androiddev.R;

import java.util.Collection;

/**
 * Chat activity.
 * 
 * Warning: {@link PageSwitcher} is to be removed and related implementation is
 * to be fixed.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatViewer extends ManagedActivity implements OnSelectListener,
    OnChatChangedListener, OnContactChangedListener,
    OnAccountChangedListener {

    /**
     * Attention request.
     */
    private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";

    private static final String SAVED_ACCOUNT = "com.xabber.android.ui.ChatViewer.SAVED_ACCOUNT";
    private static final String SAVED_USER = "com.xabber.android.ui.ChatViewer.SAVED_USER";
    private static final String SAVED_EXIT_ON_SEND = "com.xabber.android.ui.ChatViewer.EXIT_ON_SEND";

    private ChatViewerAdapter chatViewerAdapter;
    private PageSwitcher pageSwitcher;

    private String actionWithAccount;
    private String actionWithUser;
    private View actionWithView;

    private boolean exitOnSend;

    private boolean isVisible;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        Intent intent = getIntent();
        String account = getAccount(intent);
        String user = getUser(intent);
        if (PageSwitcher.LOG)
            LogManager.i(this, "Intent: " + account + ":" + user);
        if (account == null || user == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }
        if (hasAttention(intent))
            AttentionManager.getInstance().removeAccountNotifications(account,
                    user);
        actionWithAccount = null;
        actionWithUser = null;
        actionWithView = null;

        setContentView(R.layout.chat_viewer);
        chatViewerAdapter = new ChatViewerAdapter(this, account, user);

        pageSwitcher = (PageSwitcher) findViewById(R.id.switcher);
        pageSwitcher.setAdapter(chatViewerAdapter);
        pageSwitcher.setOnSelectListener(this);

        if (savedInstanceState != null) {
            actionWithAccount = savedInstanceState.getString(SAVED_ACCOUNT);
            actionWithUser = savedInstanceState.getString(SAVED_USER);
            exitOnSend = savedInstanceState.getBoolean(SAVED_EXIT_ON_SEND);
        }
        if (actionWithAccount == null)
            actionWithAccount = account;
        if (actionWithUser == null)
            actionWithUser = user;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        selectChat(actionWithAccount, actionWithUser);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnChatChangedListener.class,
                this);
        Application.getInstance().addUIListener(OnContactChangedListener.class,
                this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class,
                this);
        chatViewerAdapter.onChange();
        if (actionWithView != null)
            chatViewerAdapter.onChatChange(actionWithView, false);
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String additional = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (additional != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                exitOnSend = true;
                if (actionWithView != null)
                    chatViewerAdapter.insertText(actionWithView, additional);
            }
        }
        isVisible = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (PageSwitcher.LOG)
            LogManager.i(this, "onSave: " + actionWithAccount + ":"
                    + actionWithUser);
        outState.putString(SAVED_ACCOUNT, actionWithAccount);
        outState.putString(SAVED_USER, actionWithUser);
        outState.putBoolean(SAVED_EXIT_ON_SEND, exitOnSend);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnChatChangedListener.class,
                this);
        Application.getInstance().removeUIListener(
                OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(
                OnAccountChangedListener.class, this);
        MessageManager.getInstance().removeVisibleChat();
        pageSwitcher.saveState();
        isVisible = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (isFinishing())
            return;

        String account = getAccount(intent);
        String user = getUser(intent);
        if (account == null || user == null) {
            return;
        }
        if (hasAttention(intent))
            AttentionManager.getInstance().removeAccountNotifications(account,
                    user);

        chatViewerAdapter.onChange();
        if (!selectChat(account, user))
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        chatViewerAdapter.onPrepareOptionsMenu(actionWithView, menu);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            close();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    void close() {
        finish();
        if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
            if (!ActivityManager.getInstance().hasContactList(this))
                startActivity(ContactList.createIntent(this));
        }
    }

    void onSent() {
        if (exitOnSend)
            close();
    }

    @Override
    public void onSelect() {
        BaseEntity contactItem = (BaseEntity) pageSwitcher.getSelectedItem();
        actionWithAccount = contactItem.getAccount();
        actionWithUser = contactItem.getUser();
        if (PageSwitcher.LOG)
            LogManager.i(this, "onSelect: " + actionWithAccount + ":"
                    + actionWithUser);
        actionWithView = pageSwitcher.getSelectedView();
        if (isVisible)
            MessageManager.getInstance().setVisibleChat(actionWithAccount,
                    actionWithUser);
        MessageArchiveManager.getInstance().requestHistory(
                actionWithAccount,
                actionWithUser,
                0,
                MessageManager.getInstance()
                        .getChat(actionWithAccount, actionWithUser)
                        .getRequiredMessageCount());
        NotificationManager.getInstance().removeMessageNotification(
                actionWithAccount, actionWithUser);
    }

    @Override
    public void onUnselect() {
        actionWithAccount = null;
        actionWithUser = null;
        actionWithView = null;
        if (PageSwitcher.LOG)
            LogManager.i(this, "onUnselect");
    }

    @Override
    public void onChatChanged(final String account, final String user,
            final boolean incoming) {
        BaseEntity baseEntity;
        baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
        if (baseEntity != null && baseEntity.equals(account, user)) {
            chatViewerAdapter.onChatChange(pageSwitcher.getSelectedView(),
                    incoming);
            return;
        }
        baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
        if (baseEntity != null && baseEntity.equals(account, user)) {
            chatViewerAdapter.onChatChange(pageSwitcher.getVisibleView(),
                    incoming);
            return;
        }
        // Search for chat in adapter.
        final int count = chatViewerAdapter.getCount();
        for (int index = 0; index < count; index++)
            if (((BaseEntity) chatViewerAdapter.getItem(index)).equals(account,
                    user))
                return;
        // New chat.
        chatViewerAdapter.onChange();
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        BaseEntity baseEntity;
        baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
        if (baseEntity != null && entities.contains(baseEntity)) {
            chatViewerAdapter.onChange();
            return;
        }
        baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
        if (baseEntity != null && entities.contains(baseEntity)) {
            chatViewerAdapter.onChange();
            return;
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        BaseEntity baseEntity;
        baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
        if (baseEntity != null && accounts.contains(baseEntity.getAccount())) {
            chatViewerAdapter.onChange();
            return;
        }
        baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
        if (baseEntity != null && accounts.contains(baseEntity.getAccount())) {
            chatViewerAdapter.onChange();
            return;
        }
    }

    private boolean selectChat(String account, String user) {
        for (int position = 0; position < chatViewerAdapter.getCount(); position++)
            if (((BaseEntity) chatViewerAdapter.getItem(position)).equals(
                    account, user)) {
                if (PageSwitcher.LOG)
                    LogManager.i(this, "setSelection: " + position + ", "
                            + account + ":" + user);
                pageSwitcher.setSelection(position);
                return true;
            }
        if (PageSwitcher.LOG)
            LogManager.i(this, "setSelection: not found, " + account + ":"
                    + user);
        return false;
    }

    public int getChatCount() {
        return chatViewerAdapter.getCount();
    }

    public int getChatPosition(String account, String user) {
        return chatViewerAdapter.getPosition(account, user);
    }

    public static Intent createIntent(Context context, String account,
            String user) {
        return new EntityIntentBuilder(context, ChatViewer.class)
                .setAccount(account).setUser(user).build();
    }

    public static Intent createClearTopIntent(Context context, String account,
            String user) {
        Intent intent = createIntent(context, account, user);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    /**
     * Create intent to send message.
     *
     * Contact list will not be shown on when chat will be closed.
     *
     * @param context
     * @param account
     * @param user
     * @param text
     *            if <code>null</code> then user will be able to send a number
     *            of messages. Else only one message can be send.
     * @return
     */
    public static Intent createSendIntent(Context context, String account,
            String user, String text) {
        Intent intent = ChatViewer.createIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    public static Intent createAttentionRequestIntent(Context context,
            String account, String user) {
        Intent intent = ChatViewer.createClearTopIntent(context, account, user);
        intent.setAction(ACTION_ATTENTION);
        return intent;
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

}
