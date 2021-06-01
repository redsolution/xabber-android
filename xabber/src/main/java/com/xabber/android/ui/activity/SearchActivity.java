package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentTransaction;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.SearchContactsListItemAdapter;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment;
import com.xabber.android.ui.dialog.ContactSubscriptionDialog;
import com.xabber.android.ui.fragment.SearchFragment;
import com.xabber.android.ui.fragment.chatListFragment.ChatListFragment;
import com.xabber.android.ui.widget.ShortcutBuilder;
import com.xabber.xmpp.uri.XMPPUri;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class SearchActivity extends ManagedActivity implements View.OnClickListener,
        ChatListFragment.ChatListFragmentListener,
        SearchContactsListItemAdapter.SearchContactsListItemListener {

    /* Constants for Chat List Fragment */
    private static final String SEARCH_FRAGMENT = "SEARCH_FRAGMENT";

    /* Constants for saving state bundle*/
    private static final String SAVED_ACTION = "com.xabber.android.ui.activity.SearchActivity.SAVED_ACTION";
    private static final String SAVED_SEND_TEXT = "com.xabber.android.ui.activity.SearchActivity.SAVED_SEND_TEXT";

    /* Constants for in app Intents */
    private static final String ACTION_CLEAR_STACK = "com.xabber.android.ui.activity.SearchActivity.ACTION_CLEAR_STACK";
    private static final String ACTION_ROOM_INVITE = "com.xabber.android.ui.activity.SearchActivity.ACTION_ROOM_INVITE";
    private static final String ACTION_CONTACT_SUBSCRIPTION = "com.xabber.android.ui.activity.SearchActivity.ACTION_CONTACT_SUBSCRIPTION";
    private static final String ACTION_SEARCH = "com.xabber.android.ui.activity.SearchActivity.ACTION_SEARCH";

    private ImageView toolbarBackIv;                //Back arrow always active
    private RelativeLayout toolbarGreetingsLayout;  //Contains toolbarGreetingsSearchIv and "Choose recipient" TextView
    private TextView toolbarGreetingsSearchTitle;   //Belongs to toolbarGreetingLayout
    private RelativeLayout toolbarSearchLayout;     //Contains toolbar toolbarSearchEt, toolbarSearchClearIv
    private EditText toolbarSearchEt;               //Belongs to toolbarSearchLayout
    private ImageView toolbarSearchClearIv;         //belongs to toolbarSearchLayout

    /* InputMethodManager for keyboard management variable */
    private InputMethodManager inputMethodManager;

    /* Variables for intents */
    private String action;
    private String sendText;

    private static AccountJid getRoomInviteAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static ContactJid getRoomInviteUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SearchActivity.class);
    }

    public static Intent createSearchIntent(Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.setAction(ACTION_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    public static Intent createRoomInviteIntent(Context context, AccountJid account,
                                                ContactJid room) {

        Intent intent = new EntityIntentBuilder(context, SearchActivity.class)
                .setAccount(account).setUser(room).build();
        intent.setAction(ACTION_ROOM_INVITE);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setContentView(R.layout.activity_search);

        /* Check Saved State and restore if not empty*/
        if (savedInstanceState != null) {
            sendText = savedInstanceState.getString(SAVED_SEND_TEXT);
            action = savedInstanceState.getString(SAVED_ACTION);
        } else {
            sendText = null;
            action = getIntent().getAction();
        }

        /* Initialize InputMethodManager */
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        /* Initialize and setup toolbar */
        /* Toolbar variables */
        //Toolbar layout
        RelativeLayout toolbarLayout = findViewById(R.id.search_toolbar_layout);
        toolbarBackIv = findViewById(R.id.toolbar_search_back_button);
        toolbarGreetingsLayout = findViewById(R.id.search_toolbar_greetings_view);
        //Belongs to toolbarGreetingLayout
        ImageView toolbarGreetingsSearchIv = findViewById(R.id.search_toolbar_search_button);
        toolbarGreetingsSearchTitle = findViewById(R.id.search_toolbar_title);
        toolbarSearchLayout = findViewById(R.id.search_toolbar_search_view);
        toolbarSearchEt = findViewById(R.id.search_toolbar_edittext);
        toolbarSearchClearIv = findViewById(R.id.search_toolbar_clear_button);
        toolbarSearchClearIv.setOnClickListener(this);
        toolbarSearchEt.setOnClickListener(this);
        toolbarGreetingsSearchIv.setOnClickListener(this);
        toolbarBackIv.setOnClickListener(this);
        findViewById(R.id.search_activity_container).setOnClickListener(this);
        toolbarSearchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    toolbarSearchClearIv.setVisibility(View.VISIBLE);
                    getSearchFragment().buildChatsListWithFilter(s.toString().toLowerCase());
                } else {
                    getSearchFragment().buildChatsListWithFilter(null);
                    toolbarSearchClearIv.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        showSearchFragment();

        toolbarGreetingsLayout.setVisibility(View.VISIBLE);

        /*
        Update toolbar and statusbar background color via current main user and theme;
         */
        TypedValue typedValue = new TypedValue();
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            TypedArray a = this.obtainStyledAttributes(typedValue.data,
                    new int[]{R.attr.contact_list_account_group_background});
            final int accountGroupColorsResourceId = a.getResourceId(0, 0);
            a.recycle();
            final int[] accountGroupColors = this.getResources()
                    .getIntArray(accountGroupColorsResourceId);

            int level = 0;
            if (AccountManager.getInstance().getFirstAccount() != null)
                level = AccountManager.getInstance()
                        .getColorLevel(AccountManager.getInstance().getFirstAccount());

            toolbarLayout.setBackgroundColor(accountGroupColors[level]);
            StatusBarPainter.instanceUpdateWithDefaultColor(this);
        } else {
            this.getTheme().resolveAttribute(R.attr.bars_color, typedValue, true);
            toolbarLayout.setBackgroundColor(typedValue.data);
            StatusBarPainter.instanceUpdateWIthColor(this, typedValue.data);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        /* Handle intents if not null */
        action = getIntent().getAction();
        getIntent().setAction(null);
        if (Intent.ACTION_VIEW.equals(action)
                || Intent.ACTION_SEND.equals(action)
                || Intent.ACTION_SEND_MULTIPLE.equals(action)
                || Intent.ACTION_SENDTO.equals(action)
                || Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            ActivityManager.getInstance().startNewTask(this);
        }

        if (isFinishing()) {
            return;
        }

        if (ACTION_CLEAR_STACK.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
        }

        if (action != null) {
            switch (action) {
                case ACTION_ROOM_INVITE:
                case Intent.ACTION_SEND:
                case Intent.ACTION_SEND_MULTIPLE:
                case ChatActivity.ACTION_FORWARD:
                case Intent.ACTION_CREATE_SHORTCUT:
                    if (Intent.ACTION_SEND.equals(action)) {
                        sendText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                    }
                    break;
                case Intent.ACTION_VIEW: {
                    action = null;
                    Uri data = getIntent().getData();
                    if (data != null && "xmpp".equals(data.getScheme())) {
                        XMPPUri xmppUri;
                        try {
                            xmppUri = XMPPUri.parse(data);
                        } catch (IllegalArgumentException e) {
                            xmppUri = null;
                        }
                        if (xmppUri != null && "message".equals(xmppUri.getQueryType())) {
                            ArrayList<String> texts = xmppUri.getValues("body");
                            String text = null;
                            if (texts != null && !texts.isEmpty()) {
                                text = texts.get(0);
                            }

                            ContactJid user = null;
                            try {
                                user = ContactJid.from(xmppUri.getPath());
                            } catch (ContactJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }

                            if (user != null) {
                                openChat(user, text);
                            }
                        }
                    }
                    break;
                }
                case Intent.ACTION_SENDTO: {
                    action = null;
                    Uri data = getIntent().getData();
                    if (data != null) {
                        String path = data.getPath();
                        if (path != null && path.startsWith("/")) {
                            try {
                                ContactJid user = ContactJid.from(path.substring(1));
                                openChat(user, null);
                            } catch (ContactJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }
                        }
                    }
                    break;
                }

                case ACTION_CONTACT_SUBSCRIPTION:
                    action = null;
                    showContactSubscriptionDialog();
                    break;

                case ACTION_SEARCH:
                    toolbarGreetingsSearchTitle
                            .setText(getApplicationContext().getString(R.string.search));

                    toolbarGreetingsLayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setOnClickListener(this);
                    toolbarSearchLayout.setVisibility(View.VISIBLE);
                    toolbarSearchEt.requestFocus();
                    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        }
    }

    @Override
    protected void onPause() {
        inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        action = getIntent().getAction();
        getIntent().setAction(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        /* Save Intent action and extra if not empty */
        outState.putString(SAVED_ACTION, action);
        outState.putString(SAVED_SEND_TEXT, sendText);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_search_back_button:
                if (action != null && !action.equals(ACTION_SEARCH) && toolbarSearchEt.hasFocus()) {
                    inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                    toolbarSearchLayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setVisibility(View.VISIBLE);
                    toolbarSearchEt.clearFocus();
                    toolbarSearchEt.setText("");
                } else {
                    finish();
                }
                break;
            case R.id.search_toolbar_greetings_view:
            case R.id.search_toolbar_search_button:
                toolbarSearchLayout.setVisibility(View.VISIBLE);
                toolbarGreetingsLayout.setVisibility(View.GONE);
                toolbarSearchEt.requestFocus();
                inputMethodManager.showSoftInput(toolbarSearchEt, InputMethodManager.SHOW_IMPLICIT);
                break;
            case R.id.search_toolbar_clear_button:
                if (toolbarSearchEt.getText().toString().isEmpty()
                        || toolbarSearchEt.getText() == null) {

                    inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                    toolbarSearchLayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setVisibility(View.VISIBLE);
                } else {
                    toolbarSearchEt.setText("");
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        onClick(toolbarBackIv);
    }

    /**
     * @return existing or make new ChatListFragment
     */
    private SearchFragment getSearchFragment() {
        if (getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT) != null) {
            return (SearchFragment) getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT);
        } else return SearchFragment.newInstance();
    }

    /**
     * Shows existing or make new ChatListFragment
     */
    private void showSearchFragment() {
        if (!isFinishing()) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.search_activity_container, getSearchFragment(), SEARCH_FRAGMENT);
            fTrans.commit();
        }
    }

    private void showContactSubscriptionDialog() {
        Intent intent = getIntent();
        AccountJid account = getRoomInviteAccount(intent);
        ContactJid user = getRoomInviteUser(intent);
        if (account != null && user != null) {
            ContactSubscriptionDialog.newInstance(account, user)
                    .show(getFragmentManager(), ContactSubscriptionDialog.class.getName());
        }
    }

    /**
     * Open chat with specified contact.
     * <p/>
     * Show dialog to choose account if necessary.
     *
     * @param user
     * @param text can be <code>null</code>.
     */
    private void openChat(ContactJid user, String text) {
        ContactJid bareAddress = user.getBareUserJid();
        ArrayList<BaseEntity> entities = new ArrayList<>();
        for (AbstractChat check : ChatManager.getInstance().getChats()) {
            if (check.isActive() && check.getUser().equals(bareAddress)) {
                entities.add(check);
            }
        }
        if (entities.size() == 1) {
            openChat(entities.get(0), text);
            return;
        }
        entities.clear();

        Collection<AccountJid> enabledAccounts = AccountManager.getInstance().getEnabledAccounts();
        RosterManager rosterManager = RosterManager.getInstance();

        for (AccountJid accountJid : enabledAccounts) {
            RosterContact rosterContact = rosterManager.getRosterContact(accountJid, user);
            if (rosterContact != null && rosterContact.isEnabled()) {
                entities.add(rosterContact);
            }
        }

        if (entities.size() == 1) {
            openChat(entities.get(0), text);
            return;
        }

        if (enabledAccounts.isEmpty()) {
            return;
        }
        if (enabledAccounts.size() == 1) {

            openChat(rosterManager.getBestContact(enabledAccounts.iterator().next(), bareAddress),
                    text);

            return;
        }
        AccountChooseDialogFragment.newInstance(bareAddress, text)
                .show(getFragmentManager(), "OPEN_WITH_ACCOUNT");
    }

    /**
     * Open chat with specified contact and enter text to be sent.
     *
     * @param text can be <code>null</code>.
     */
    private void openChat(AccountJid account, ContactJid user, String text) {
        if (text == null) {
            startActivity(ChatActivity.createSendIntent(this, account, user, null));
        } else {
            startActivity(ChatActivity.createSendIntent(this, account, user, text));
        }
        finish();
    }

    private void openChat(BaseEntity entity, String text) {
        openChat(entity.getAccount(), entity.getUser(), text);
    }

    @Override
    public void onContactListItemClick(@NotNull AbstractChat contact) {
        onChatClick(RosterManager.getInstance()
                .getAbstractContact(contact.getAccount(), contact.getUser()));
    }

    @Override
    public void onChatClick(AbstractContact abstractContact) {

        if (action == null) {
            startActivityForResult(ChatActivity.createSendIntent(this,
                    abstractContact.getAccount(), abstractContact.getUser(), null), 301);
            return;
        }
        switch (action) {
            case Intent.ACTION_SEND:
                if (!isSharedText(getIntent().getType())) {
                    // share file
                    if (getIntent().getExtras() != null) {
                        action = null;
                        startActivity(ChatActivity.createSendUriIntent(this,
                                abstractContact.getAccount(), abstractContact.getUser(),
                                getIntent().getParcelableExtra(Intent.EXTRA_STREAM)));
                    }
                } else {
                    action = null;
                    startActivity(ChatActivity.createSendIntent(this,
                            abstractContact.getAccount(), abstractContact.getUser(), sendText));
                }
                finish();
                break;
            case Intent.ACTION_SEND_MULTIPLE:
                if (getIntent().getExtras() != null) {
                    action = null;
                    startActivity(ChatActivity.createSendUrisIntent(this,
                            abstractContact.getAccount(), abstractContact.getUser(),
                            getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM)));
                }
                finish();
                break;
            case Intent.ACTION_CREATE_SHORTCUT: {
                createShortcut(abstractContact);
                finish();
                break;
            }
            case ChatActivity.ACTION_FORWARD: {
                forwardMessages(abstractContact, getIntent());
                finish();
                break;
            }
            default:
                startActivityForResult(ChatActivity.createSpecificChatIntent(this,
                        abstractContact.getAccount(), abstractContact.getUser()), 301);
                break;
        }
    }

    private void forwardMessages(AbstractContact abstractContact, Intent intent) {
        ArrayList<String> messages = intent.getStringArrayListExtra(ChatActivity.KEY_MESSAGES_ID);
        if (messages != null)
            startActivity(ChatActivity.createForwardIntent(this,
                    abstractContact.getAccount(), abstractContact.getUser(), messages));
    }

    private void createShortcut(AbstractContact abstractContact) {
        Intent intent = ShortcutBuilder.createPinnedShortcut(this, abstractContact);
        if (intent != null) setResult(RESULT_OK, intent);
    }

    @Override
    public void onChatListStateChanged(ChatListFragment.ChatListState chatListState) {
    }

    private boolean isSharedText(String type) {
        return type.contains("text/plain");
    }
}
