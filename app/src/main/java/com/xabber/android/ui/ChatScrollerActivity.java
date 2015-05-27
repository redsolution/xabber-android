package com.xabber.android.ui;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.helper.ChatScroller;
import com.xabber.android.ui.helper.ManagedActivity;

public abstract class ChatScrollerActivity extends ManagedActivity
        implements ChatScroller.ChatScrollerListener, ChatScroller.ChatScrollerProvider {

    ChatScroller chatScroller = new ChatScroller(this);


    @Override
    public ChatScroller getChatScroller() {
        return chatScroller;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Application.getInstance().addUIListener(OnChatChangedListener.class, chatScroller);
        Application.getInstance().addUIListener(OnContactChangedListener.class, chatScroller);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, chatScroller);

        chatScroller.update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnChatChangedListener.class, chatScroller);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, chatScroller);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, chatScroller);

        chatScroller.onHide();
    }
}
