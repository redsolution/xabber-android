package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.fragment.app.FragmentTransaction;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.ui.contactlist.ChatListFragment;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.AccountChooseDialogFragment;
import com.xabber.android.ui.dialog.ContactSubscriptionDialog;
import com.xabber.android.ui.dialog.MucInviteDialog;
import com.xabber.android.ui.dialog.MucPrivateChatInvitationDialog;
import com.xabber.android.ui.widget.ShortcutBuilder;
import com.xabber.xmpp.uri.XMPPUri;

import java.util.ArrayList;
import java.util.Collection;

public class SearchActivity extends ManagedActivity implements View.OnClickListener, ChatListFragment.ChatListFragmentListener {

    /* Constants for Chat List Fragment */
    private static final String CHAT_LIST_TAG = "CHAT_LIST";

    /* Constants for savind state budle*/
    private static final String SAVED_ACTION = "com.xabber.android.ui.activity.SearchActivity.SAVED_ACTION";
    private static final String SAVED_SEND_TEXT = "com.xabber.android.ui.activity.SearchActivity.SAVED_SEND_TEXT";

    /* Constants for in app Intents */
    private static final String ACTION_CLEAR_STACK = "com.xabber.android.ui.activity.SearchActivity.ACTION_CLEAR_STACK";
    private static final String ACTION_ROOM_INVITE = "com.xabber.android.ui.activity.SearchActivity.ACTION_ROOM_INVITE";
    private static final String ACTION_MUC_PRIVATE_CHAT_INVITE = "com.xabber.android.ui.activity.SearchActivity.ACTION_MUC_PRIVATE_CHAT_INVITE";
    private static final String ACTION_CONTACT_SUBSCRIPTION = "com.xabber.android.ui.activity.SearchActivity.ACTION_CONTACT_SUBSCRIPTION";
    private static final String ACTION_INCOMING_MUC_INVITE = "com.xabber.android.ui.activity.SearchActivity.ACTION_INCOMING_MUC_INVITE";
    private static final String ACTION_SEARCH = "com.xabber.android.ui.activity.SearchActivity.ACTION_SEARCH";

    /* Toolbar variables */
    private RelativeLayout toolbarLayout;           //Toolbar layout
    private ImageView toolbarBackIv;                //Back arrow always active
    private RelativeLayout toolbarGreetingsLayout;  //Contains toolbarGreetingsSearchIv and "Choose recipient" TextView
    private ImageView toolbarGreetingsSearchIv;     //Belongs to toolbarGreetingLayout
    private RelativeLayout toolbarSearchlayout;     //Contains toolbar toolbarSearchEt, toolbarSearchClearIv
    private EditText toolbarSearchEt;               //Belongs to toolbarSearchLayout
    private ImageView toolbarSearchClearIv;         //belongs to toolbarSearchLayout

    /* InputMethodManager for keyboard management variable */
    private InputMethodManager inputMethodManager;

    /* Variables for intents */
    private String action;
    private String sendText;

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
        toolbarLayout = findViewById(R.id.search_toolbar_layout);
        toolbarBackIv = findViewById(R.id.toolbar_search_back_button);
        toolbarGreetingsLayout = findViewById(R.id.search_toolbar_greetings_view);
        toolbarGreetingsSearchIv = findViewById(R.id.search_toolbar_search_button);
        toolbarSearchlayout = findViewById(R.id.search_toolbar_search_view);
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
                if (s.length() != 0){
                    toolbarSearchClearIv.setVisibility(View.VISIBLE);
                    getChatListFragment().search(s.toString());
                } else {
                    getChatListFragment().search(null);
                    toolbarSearchClearIv.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        showChatListFragment();

        toolbarGreetingsLayout.setVisibility(View.VISIBLE);

        /*
        Update background color via current main user;
         */
        TypedValue typedValue = new TypedValue();
        TypedArray a = this.obtainStyledAttributes(typedValue.data, new int[] {R.attr.contact_list_account_group_background});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        final int[] accountGroupColors = this.getResources().getIntArray(accountGroupColorsResourceId);
        final int level = AccountManager.getInstance().getColorLevel(AccountPainter.getFirstAccount());
        toolbarLayout.setBackgroundColor(accountGroupColors[level]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* Setup StatusBarColor */   //TODO Doesn't working think about it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        }

        /* Handle intents if not null */
        action = getIntent().getAction();
        getIntent().setAction(null);
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())
                || Intent.ACTION_SEND.equals(getIntent().getAction())
                || Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())
                || Intent.ACTION_SENDTO.equals(getIntent().getAction())
                || Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            ActivityManager.getInstance().startNewTask(this);
        }

        if (isFinishing()) {
            return;
        }
            //TODO THINK ABOUT IT
//        if (!isTaskRoot() && !ACTION_ROOM_INVITE.equals(action)
//                && !Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
//            finish();
//            return;
//        }
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
                    //Toast.makeText(this, getString(R.string.select_contact), Toast.LENGTH_LONG).show();
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

                            UserJid user = null;
                            try {
                                user = UserJid.from(xmppUri.getPath());
                            } catch (UserJid.UserJidCreateException e) {
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
                                UserJid user = UserJid.from(path.substring(1));
                                openChat(user, null);
                            } catch (UserJid.UserJidCreateException e) {
                                LogManager.exception(this, e);
                            }
                        }
                    }
                    break;
                }
                case ACTION_MUC_PRIVATE_CHAT_INVITE:
                    action = null;
                    showMucPrivateChatDialog();
                    break;

                case ACTION_CONTACT_SUBSCRIPTION:
                    action = null;
                    showContactSubscriptionDialog();
                    break;

                case ACTION_INCOMING_MUC_INVITE:
                    action = null;
                    showMucInviteDialog();
                    break;
                case ACTION_SEARCH:
                    toolbarGreetingsLayout.setVisibility(View.GONE);
                    toolbarSearchlayout.setVisibility(View.VISIBLE);
                    toolbarSearchEt.requestFocus();
                    inputMethodManager.showSoftInput(toolbarSearchEt, InputMethodManager.SHOW_IMPLICIT);
            }
        }
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
        switch (v.getId()){
            case R.id.toolbar_search_back_button:
                finish();
                break;
            case R.id.search_toolbar_search_button:
                toolbarSearchlayout.setVisibility(View.VISIBLE);
                toolbarGreetingsLayout.setVisibility(View.GONE);
                toolbarSearchEt.requestFocus();
                inputMethodManager.showSoftInput(toolbarSearchEt, InputMethodManager.SHOW_IMPLICIT);
                break;
            case R.id.search_toolbar_clear_button:
                if (toolbarSearchEt.getText().toString().isEmpty() || toolbarSearchEt.getText() == null){
                    inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                    toolbarSearchlayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setVisibility(View.VISIBLE);
                    //TODO clearing search string
                } else {
                    toolbarSearchEt.setText("");
                }
                break;
            default:
                inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                if (toolbarSearchEt.getText().toString().isEmpty() || toolbarSearchEt.getText() == null){
                    inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                    toolbarSearchlayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setVisibility(View.VISIBLE);
                    //TODO implement clearing search string and probably change view id
                }
                break;
        }
    }

    @Override
    public void onManageAccountsClick() {
        finish();
    }

    /**
     * @return existing or make new ChatListFragment
     */
    private ChatListFragment getChatListFragment(){
        if ((ChatListFragment) getSupportFragmentManager().findFragmentByTag(CHAT_LIST_TAG) != null){
            return (ChatListFragment) getSupportFragmentManager().findFragmentByTag(CHAT_LIST_TAG);
        } else return ChatListFragment.newInstance(null);
    }

    /**
     * Shows existing or make new ChatListFragment
     */
    private void showChatListFragment() {
        if (!isFinishing()) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            fTrans.replace(R.id.search_activity_container, getChatListFragment(), CHAT_LIST_TAG);
            fTrans.commit();
        }
    }

    private void showMucInviteDialog() {
        Intent intent = getIntent();
        AccountJid account = getRoomInviteAccount(intent);
        UserJid user = getRoomInviteUser(intent);
        if (account != null && user != null) {
            MucInviteDialog.newInstance(account, user).show(getFragmentManager(), MucInviteDialog.class.getName());
        }
    }

    private void showContactSubscriptionDialog() {
        Intent intent = getIntent();
        AccountJid account = getRoomInviteAccount(intent);
        UserJid user = getRoomInviteUser(intent);
        if (account != null && user != null) {
            ContactSubscriptionDialog.newInstance(account, user).show(getFragmentManager(), ContactSubscriptionDialog.class.getName());
        }
    }

    private void showMucPrivateChatDialog() {
        Intent intent = getIntent();
        AccountJid account = getRoomInviteAccount(intent);
        UserJid user = getRoomInviteUser(intent);
        if (account != null && user != null) {
            MucPrivateChatInvitationDialog.newInstance(account, user).show(getFragmentManager(), MucPrivateChatInvitationDialog.class.getName());
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
    private void openChat(UserJid user, String text) {
        UserJid bareAddress = user.getBareUserJid();
        ArrayList<BaseEntity> entities = new ArrayList<>();
        for (AbstractChat check : MessageManager.getInstance().getChats()) {
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
            openChat(rosterManager.getBestContact(enabledAccounts.iterator().next(), bareAddress), text);
            return;
        }
        AccountChooseDialogFragment.newInstance(bareAddress, text)
                .show(getFragmentManager(), "OPEN_WITH_ACCOUNT");
    }

    private static AccountJid getRoomInviteAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static UserJid getRoomInviteUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    /**
     * Open chat with specified contact and enter text to be sent.
     *
     * @param text       can be <code>null</code>.
     */
    private void openChat(AccountJid account, UserJid user, String text) {
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
    public void onChatClick(AbstractContact abstractContact) {

        if (action == null) {
            startActivityForResult(ChatActivity.createSendIntent(this, abstractContact.getAccount(),
                    abstractContact.getUser(), null), 301);
            return;
        }
        switch (action) {
            case ACTION_ROOM_INVITE: {
                action = null;
                Intent intent = getIntent();
                AccountJid account = getRoomInviteAccount(intent);
                UserJid user = getRoomInviteUser(intent);
                if (account != null && user != null) {
                    try {
                        MUCManager.getInstance().invite(account, user.getJid().asEntityBareJidIfPossible(), abstractContact.getUser());
                    } catch (NetworkException e) {
                        Application.getInstance().onError(e);
                    }
                }
                finish();
                break;
            }
            case Intent.ACTION_SEND:
                if (!isSharedText(getIntent().getType())) {
                    // share file
                    if (getIntent().getExtras() != null) {
                        action = null;
                        startActivity(ChatActivity.createSendUriIntent(this,
                                abstractContact.getAccount(), abstractContact.getUser(),
                                (Uri)getIntent().getParcelableExtra(Intent.EXTRA_STREAM)));
                        finish();
                    }
                } else {
                    action = null;
                    startActivity(ChatActivity.createSendIntent(this,
                            abstractContact.getAccount(), abstractContact.getUser(), sendText));
                    finish();
                }
                break;
            case Intent.ACTION_SEND_MULTIPLE:
                if (getIntent().getExtras() != null) {
                    action = null;
                    startActivity(ChatActivity.createSendUrisIntent(this,
                            abstractContact.getAccount(), abstractContact.getUser(),
                            getIntent().<Uri>getParcelableArrayListExtra(Intent.EXTRA_STREAM)));
                    finish();
                }
                break;
            case Intent.ACTION_CREATE_SHORTCUT: {
                createShortcut(abstractContact);
                finish();
                break;
            }
            case ChatActivity.ACTION_FORWARD: {
                forwardMessages(abstractContact, getIntent());
                break;
            }
            default:
                startActivityForResult(ChatActivity.createSpecificChatIntent(this, abstractContact.getAccount(),
                        abstractContact.getUser()), 301);
                break;
        }
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SearchActivity.class);
    }

    public static Intent createSearchIntent(Context context){
        Intent intent = new Intent(context, SearchActivity.class);
        intent.setAction(ACTION_SEARCH);
        return intent;
    }

    public static Intent createRoomInviteIntent(Context context, AccountJid account, UserJid room) {
        Intent intent = new EntityIntentBuilder(context, SearchActivity.class)
                .setAccount(account).setUser(room).build();
        intent.setAction(ACTION_ROOM_INVITE);
        return intent;
    }

    public static Intent createContactSubscriptionIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, SearchActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setAction(ACTION_CONTACT_SUBSCRIPTION);
        return intent;
    }

    public static Intent createMucPrivateChatInviteIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, SearchActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setAction(ACTION_MUC_PRIVATE_CHAT_INVITE);
        return intent;
    }

    public static Intent createMucInviteIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, SearchActivity.class)
                .setAccount(account).setUser(user).build();
        intent.setAction(ACTION_INCOMING_MUC_INVITE);
        return intent;
    }

    public static Intent createClearStackIntent(Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.setAction(ACTION_CLEAR_STACK);
        return intent;
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

    @Override
    public void onUnreadChanged(int unread) {
    }

    private boolean isSharedText(String type) {
        return type.contains("text/plain");
    }
}
