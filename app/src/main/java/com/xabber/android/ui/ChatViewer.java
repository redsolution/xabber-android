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
import android.support.v4.view.ViewPager;

import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.ChatViewerAdapter;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

import java.util.Collection;
import java.util.HashSet;

/**
 * Chat activity.
 * <p/>
 *
 * @author alexander.ivanov
 */
public class ChatViewer extends ManagedActivity implements OnChatChangedListener,
        OnContactChangedListener, OnAccountChangedListener, ViewPager.OnPageChangeListener {

    /**
     * Attention request.
     */
    private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";

    private static final String SAVED_ACCOUNT = "com.xabber.android.ui.ChatViewer.SAVED_ACCOUNT";
    private static final String SAVED_USER = "com.xabber.android.ui.ChatViewer.SAVED_USER";
    private static final String SAVED_EXIT_ON_SEND = "com.xabber.android.ui.ChatViewer.EXIT_ON_SEND";

    private boolean exitOnSend;

    ChatViewerAdapter chatViewerAdapter;

    ViewPager viewPager;

    Collection<CurrentUpdatableChat> registeredChats = new HashSet<>();

    private String actionWithAccount = null;
    private String actionWithUser = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        Intent intent = getIntent();
        String account = getAccount(intent);
        String user = getUser(intent);
        LogManager.i(this, "onCreate account: " + account + ", user: " + user);

        if (account == null || user == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }
        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(account, user);
        }
        actionWithAccount = null;
        actionWithUser = null;

        if (savedInstanceState != null) {
            actionWithAccount = savedInstanceState.getString(SAVED_ACCOUNT);
            actionWithUser = savedInstanceState.getString(SAVED_USER);
            exitOnSend = savedInstanceState.getBoolean(SAVED_EXIT_ON_SEND);
        }
        if (actionWithAccount == null) {
            actionWithAccount = account;
        }
        if (actionWithUser == null) {
            actionWithUser = user;
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_chat_viewer);

        chatViewerAdapter = new ChatViewerAdapter(getFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(chatViewerAdapter);
        viewPager.setOnPageChangeListener(this);


        selectPage(actionWithAccount, actionWithUser);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnChatChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String additional = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (additional != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                exitOnSend = true;
            }
        }

//        selectPage(actionWithAccount, actionWithUser);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        LogManager.i(this, "onSave: " + actionWithAccount + ":" + actionWithUser);
        outState.putString(SAVED_ACCOUNT, actionWithAccount);
        outState.putString(SAVED_USER, actionWithUser);
        outState.putBoolean(SAVED_EXIT_ON_SEND, exitOnSend);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnChatChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        MessageManager.getInstance().removeVisibleChat();
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

        LogManager.i(this, "onNewIntent account: " + account + ", user: " + user);

        selectPage(account, user);



    }

    private void selectPage(String account, String user) {
        LogManager.i(this, "selectPage: account: " + account + ", user: " + user);

        int position = chatViewerAdapter.getPosition(account, user);
        viewPager.setCurrentItem(position);

        onChatSelected(account, user);
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

    public static Intent createIntent(Context context, String account,
                                      String user) {
        return new EntityIntentBuilder(context, ChatViewer.class).setAccount(account).setUser(user).build();
    }

    public static Intent createClearTopIntent(Context context, String account,
                                              String user) {
        Intent intent = createIntent(context, account, user);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

    @Override
    public void onChatChanged(final String account, final String user,
                              final boolean incoming) {
        LogManager.i(this, "updateChat account: " + account + ", user: " + user);


        chatViewerAdapter.onChange();


        for (CurrentUpdatableChat chat : registeredChats) {
            if (chat.isEqual(account, user)) {
                chat.updateChat(incoming);
            }
        }
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        chatViewerAdapter.onChange();

        for (CurrentUpdatableChat chat : registeredChats) {
                chat.updateChat(false);
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        chatViewerAdapter.onChange();

        for (CurrentUpdatableChat chat : registeredChats) {
                chat.updateChat(false);
        }
    }

    void onSent() {
        if (exitOnSend) {
            close();
        }
    }

    void close() {
        finish();
        if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
            if (!ActivityManager.getInstance().hasContactList(this)) {
                startActivity(ContactList.createIntent(this));
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        LogManager.i(this, "onPageSelected: " + position);

        AbstractChat chat = chatViewerAdapter.getChat(position);
        String account = chat.getAccount();
        String user = chat.getUser();

        actionWithAccount = account;
        actionWithUser = user;

        onChatSelected(account, user);
    }

    private void onChatSelected(String account, String user) {
        LogManager.i(this, "onChatSelected. account: " + account + "; user: " + user);
        MessageManager.getInstance().setVisibleChat(account, user);

        MessageArchiveManager.getInstance().requestHistory(
                account, user, 0,
                MessageManager.getInstance().getChat(account, user).getRequiredMessageCount());

        NotificationManager.getInstance().removeMessageNotification(account, user);

        final AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);

        setTitle(abstractContact.getName());
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    public void registerChat(CurrentUpdatableChat chat) {
        registeredChats.add(chat);
    }

    public void unregisterChat(CurrentUpdatableChat chat) {
        registeredChats.remove(chat);
    }

    public interface CurrentUpdatableChat {
        public void updateChat(final boolean incoming);
        public boolean isEqual(String account, String user);
    }
}
