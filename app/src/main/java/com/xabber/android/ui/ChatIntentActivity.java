package com.xabber.android.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.ui.preferences.AboutViewer;
import com.xabber.android.ui.preferences.AccountEditor;
import com.xabber.android.ui.preferences.PreferenceEditor;

public abstract class ChatIntentActivity extends ChatScrollerActivity implements ContactListDrawerFragment.ContactListDrawerListener {

    protected static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";
    protected static final String ACTION_SPECIFIC_CHAT = "com.xabber.android.data.ACTION_SPECIFIC_CHAT";
    protected static final String ACTION_SHORTCUT = "com.xabber.android.data.ACTION_SHORTCUT";

    protected ActionBarDrawerToggle drawerToggle;
    protected DrawerLayout drawerLayout;
    protected Toolbar toolbar;

    public static Intent createSpecificChatIntent(Context context, String account, String user) {
        Intent intent = new EntityIntentBuilder(context, ContactList.class).setAccount(account).setUser(user).build();
        intent.setAction(ACTION_SPECIFIC_CHAT);
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

    public static Intent createSendIntent(Context context, String account, String user, String text) {
        Intent intent = createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    public static Intent createAttentionRequestIntent(Context context, String account, String user) {
        Intent intent = createClearTopIntent(context, account, user);
        intent.setAction(ACTION_ATTENTION);
        return intent;
    }

    private static boolean hasAttention(Intent intent) {
        return ACTION_ATTENTION.equals(intent.getAction());
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contact_list);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            BaseEntity chatFromIntent = getChatFromIntent(intent);
            if (chatFromIntent != null) {
                ChatManager.getInstance().setInitialChat(chatFromIntent);
                ChatManager.getInstance().setSelectedChat(chatFromIntent);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        BaseEntity chatFromIntent = getChatFromIntent(intent);

        ChatManager.getInstance().setSelectedChat(chatFromIntent);

        if (ACTION_SHORTCUT.equals(intent.getAction())) {
            ChatManager.getInstance().setInitialChat(chatFromIntent);
        }
    }

    private BaseEntity getChatFromIntent(Intent intent) {
        String account = getAccount(intent);
        String user = getUser(intent);
        if (account != null && user != null) {
            return new BaseEntity(account, user);
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(ChatManager.getInstance().getSelectedChat());
        }
    }

    @Override
    public void onContactListDrawerListener(int viewId) {
        drawerLayout.closeDrawers();
        switch (viewId) {
            case R.id.drawer_action_settings:
                startActivity(PreferenceEditor.createIntent(this));
                break;
            case R.id.drawer_action_about:
                startActivity(AboutViewer.createIntent(this));
                break;
            case R.id.drawer_action_exit:
                exit();
                break;

        }
    }

    abstract void exit();

    @Override
    public void onAccountSelected(String account) {
        drawerLayout.closeDrawers();
        startActivity(AccountEditor.createIntent(this, account));
    }
}
