package com.xabber.android.ui.helper;


import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.widget.LinearLayout;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.ChatViewer;
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
        ChatViewerAdapter.FinishUpdateListener,
        RecentChatFragment.RecentChatFragmentInteractionListener,
        ChatViewerFragment.ChatViewerFragmentListener {

    ViewPager viewPager;
    ChatViewerAdapter chatViewerAdapter;
    ChatScrollIndicatorAdapter chatScrollIndicatorAdapter;
    Collection<ChatViewerFragment> registeredChats = new HashSet<>();
    Collection<RecentChatFragment> recentChatFragments = new HashSet<>();
    ChatScrollerListener listener;
    Activity activity;
    private boolean isRecentChatsSelected;
    private BaseEntity selectedChat = null;
    private boolean isVisible;
    private String extraText = null;
    private boolean exitOnSend = false;

    public ChatScroller(Activity activity, ViewPager viewPager, LinearLayout chatScrollIndicatorLayout) {
        this.viewPager = viewPager;
        this.activity = activity;
        this.listener = (ChatScrollerListener) activity;
        this.chatScrollIndicatorAdapter = new ChatScrollIndicatorAdapter(activity, chatScrollIndicatorLayout);
    }

    public BaseEntity getSelectedChat() {
        return selectedChat;
    }

    public void setSelectedChat(BaseEntity selectedChat) {
        this.selectedChat = selectedChat;
        isRecentChatsSelected = selectedChat == null;
    }

    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
        if (!isVisible) {
            MessageManager.getInstance().removeVisibleChat();
        }
    }

    public boolean isExitOnSend() {
        return exitOnSend;
    }

    public void setExitOnSend(boolean exitOnSend) {
        this.exitOnSend = exitOnSend;
    }

    public void setExtraText(String extraText) {
        this.extraText = extraText;
    }

    public void initChats(BaseEntity initialChat) {
        if (initialChat != null) {
            chatViewerAdapter = new ChatViewerAdapter(activity.getFragmentManager(), initialChat, this);
        } else {
            chatViewerAdapter = new ChatViewerAdapter(activity.getFragmentManager(), this);
        }


        viewPager.setAdapter(chatViewerAdapter);
        viewPager.setOnPageChangeListener(this);

        if (SettingsManager.chatsShowBackground()) {
            viewPager.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.chat_background_repeat));
        }

        chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
    }

    public void update() {
        chatViewerAdapter.updateChats();
        chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
        selectPage();
    }

    private void selectPage() {
        if (isRecentChatsSelected) {
            selectRecentChatsPage();
        } else {
            selectChatPage(selectedChat, false);
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
        if (isRecentChatsSelected) {
            listener.onAccountSelected(null);
        } else {
            listener.onAccountSelected(selectedChat.getAccount());
        }
    }

    /**
     * OnChatChangedListener
     */

    @Override
    public void onChatChanged(final String account, final String user, final boolean incoming) {
        if (chatViewerAdapter.updateChats()) {
            chatScrollIndicatorAdapter.update(chatViewerAdapter.getActiveChats());
            selectPage();
        } else {
            updateRegisteredChats();
            updateRegisteredRecentChatsFragments();
            updateStatusBar();

            for (ChatViewerFragment chat : registeredChats) {
                if (chat.isEqual(selectedChat) && incoming) {
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
        ChatViewer.hideKeyboard(activity);
        chatScrollIndicatorAdapter.select(chatViewerAdapter.getRealPagePosition(position));

        setSelectedChat(chatViewerAdapter.getChatByPageNumber(position));

        if (isRecentChatsSelected) {
            MessageManager.getInstance().removeVisibleChat();
        } else {
            if (isVisible) {
                MessageManager.getInstance().setVisibleChat(selectedChat);
            }

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
    public void onCloseChat() {
        listener.onClose();
    }

    @Override
    public void onMessageSent() {
        if (exitOnSend) {
            listener.onClose();
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

    /**
     * FinishUpdateListener
     */

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

    public interface ChatScrollerListener {
        void onAccountSelected(String account);

        void onClose();
    }

    public interface ChatScrollerProvider {
        ChatScroller getChatScroller();
    }

}
