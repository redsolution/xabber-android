package com.xabber.android.ui.helper;


import android.app.Activity;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.ChatViewerFragment;
import com.xabber.android.ui.RecentChatFragment;
import com.xabber.android.ui.adapter.ChatScrollIndicatorAdapter;
import com.xabber.android.ui.adapter.ChatViewerAdapter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class ChatScroller implements
        OnChatChangedListener,
        OnContactChangedListener,
        OnAccountChangedListener,
        ViewPager.OnPageChangeListener,
        RecentChatFragment.RecentChatFragmentInteractionListener,
        ChatViewerFragment.ChatViewerFragmentListener {

    private final ChatManager chatManager;
    Activity activity;
    ViewPager viewPager;
    ChatViewerAdapter chatViewerAdapter;
    ChatScrollIndicatorAdapter chatScrollIndicatorAdapter;
    Collection<ChatViewerFragment> registeredChats = new HashSet<>();
    Collection<RecentChatFragment> recentChatFragments = new HashSet<>();
    ChatScrollerListener listener;
    public ChatScroller(Activity activity) {
        chatManager = ChatManager.getInstance();
        this.activity = activity;
        this.listener = (ChatScrollerListener) activity;

    }

    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void createView(ViewPager viewPager, LinearLayout chatScrollIndicatorLayout) {
        this.viewPager = viewPager;
        this.chatScrollIndicatorAdapter = new ChatScrollIndicatorAdapter(activity, chatScrollIndicatorLayout);

        chatViewerAdapter = new ChatViewerAdapter(activity.getFragmentManager());
        viewPager.setAdapter(chatViewerAdapter);
        viewPager.setOnPageChangeListener(this);

        if (SettingsManager.chatsShowBackground()) {
            viewPager.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.chat_background_repeat));
        }

        chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
    }

    public void onHide() {
        MessageManager.getInstance().removeVisibleChat();
    }

    public void update() {
        if (!isInitialized()) {
            return;
        }

        chatViewerAdapter.updateChats();
        chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
        selectPage();
    }

    public boolean isInitialized() {
        return chatViewerAdapter != null;
    }

    private void selectPage() {
        if (chatManager.getSelectedChat() == null) {
            selectRecentChatsPage();
        } else {
            selectChatPage(chatManager.getSelectedChat(), false);
        }
    }

    private void selectRecentChatsPage() {
        selectPage(chatViewerAdapter.getPageNumber(null), false);
    }

    private void selectChatPage(BaseEntity chat, boolean smoothScroll) {
        selectPage(chatViewerAdapter.getPageNumber(chat), smoothScroll);
    }

    private void selectPage(int position, boolean smoothScroll) {
        onPageSelected(position);
        viewPager.setCurrentItem(position, smoothScroll);
    }

    private void updateRegisteredChats() {
        for (ChatViewerFragment chat : registeredChats) {
            chat.updateChat();
        }
    }

    private void updateRegisteredRecentChatsFragments() {
        for (RecentChatFragment recentChatFragment : recentChatFragments) {
            recentChatFragment.updateChats();
        }
    }

    private void updateStatusBar() {
        BaseEntity selectedChat = chatManager.getSelectedChat();
        if (selectedChat == null) {
            listener.onStatusBarNeedPaint(null);
        } else {
            listener.onStatusBarNeedPaint(selectedChat.getAccount());
        }
    }

    /**
     * OnChatChangedListener
     */

    @Override
    public void onChatChanged(final String account, final String user, final boolean incoming) {
        if (!isInitialized()) {
            return;
        }

        if (chatViewerAdapter.updateChats()) {
            chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
            selectPage();
        } else {
            updateRegisteredChats();
            updateRegisteredRecentChatsFragments();
            updateStatusBar();

            for (ChatViewerFragment chat : registeredChats) {
                if (chat.isEqual(new BaseEntity(account, user)) && incoming) {
                    chat.playIncomingAnimation();
                }
            }
        }
    }

    /**
     * OnContactChangedListener
     */

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        updateRegisteredChats();
        updateRegisteredRecentChatsFragments();
        updateStatusBar();
    }

    /**
     * OnAccountChangedListener
     */

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        updateRegisteredChats();
        updateRegisteredRecentChatsFragments();
        updateStatusBar();
    }

    /**
     * OnPageChangeListener
     */

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        hideKeyboard(activity);
        chatScrollIndicatorAdapter.select(chatViewerAdapter.getRealPagePosition(position));

        chatManager.setSelectedChat(chatViewerAdapter.getChatByPageNumber(position));

        if (chatManager.getSelectedChat() == null) {
            MessageManager.getInstance().removeVisibleChat();
        } else {
            BaseEntity selectedChat = chatManager.getSelectedChat();

            MessageManager.getInstance().setVisibleChat(selectedChat);
            MessageArchiveManager.getInstance().requestHistory(selectedChat.getAccount(), selectedChat.getUser(), 0,
                    MessageManager.getInstance().getChat(selectedChat.getAccount(), selectedChat.getUser()).getRequiredMessageCount());

            NotificationManager.getInstance().removeMessageNotification(selectedChat.getAccount(), selectedChat.getUser());
        }

        updateStatusBar();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * RecentChatFragmentInteractionListener
     */

    @Override
    public void onChatSelected(AbstractChat chat) {
        selectChatPage(chat, true);
    }

    @Override
    public void registerRecentChatsList(RecentChatFragment recentChatFragment) {
        recentChatFragments.add(recentChatFragment);
    }

    @Override
    public void unregisterRecentChatsList(RecentChatFragment recentChatFragment) {
        recentChatFragments.remove(recentChatFragment);
    }

    @Override
    public List<AbstractChat> getActiveChats() {
        return chatViewerAdapter.getActiveChats();
    }

    /**
     * ChatViewerFragmentListener
     */

    @Override
    public void onCloseChat(BaseEntity chat) {
        update();
        listener.onClose(chat);
    }

    @Override
    public void onMessageSent() {
    }

    @Override
    public void registerChat(ChatViewerFragment chat) {
        registeredChats.add(chat);
    }

    @Override
    public void unregisterChat(ChatViewerFragment chat) {
        registeredChats.remove(chat);
    }

    public interface ChatScrollerListener {
        void onStatusBarNeedPaint(String account);
        void onClose(BaseEntity chat);
    }

    public interface ChatScrollerProvider {
        ChatScroller getChatScroller();
    }
}
