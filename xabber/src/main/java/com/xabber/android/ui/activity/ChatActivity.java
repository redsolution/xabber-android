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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomState;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.CrowdfundingChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.message.RegularChat;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnChatStateListener;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.mvp.contactlist.UpdateBackpressure;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;
import com.xabber.android.ui.dialog.AttachDialog;
import com.xabber.android.ui.dialog.BlockContactDialog;
import com.xabber.android.ui.dialog.ContactDeleteDialogFragment;
import com.xabber.android.ui.dialog.SnoozeDialog;
import com.xabber.android.ui.fragment.ChatFragment;
import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
import com.xabber.android.ui.fragment.OccupantListFragment;
import com.xabber.android.ui.helper.NewContactTitleInflater;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.ui.preferences.CustomNotifySettings;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Chat activity.
 * <p/>
 *
 * @author alexander.ivanov
 */
public class ChatActivity extends ManagedActivity implements OnContactChangedListener,
        OnAccountChangedListener, OnChatStateListener, ChatFragment.ChatViewerFragmentListener, OnBlockedListChangedListener,
        ContactVcardViewerFragment.Listener, Toolbar.OnMenuItemClickListener,
        UpdateBackpressure.UpdatableObject, OccupantListFragment.Listener, SnoozeDialog.OnSnoozeListener {

    private static final String LOG_TAG = ChatActivity.class.getSimpleName();
    private static final String CHAT_FRAGMENT_TAG = "CHAT_FRAGMENT_TAG";
    private static final String CONTACT_INFO_FRAGMENT_TAG = "CONTACT_INFO_FRAGMENT_TAG";

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector gestureDetector;

    private String currentFragment;

    private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";
    private static final String ACTION_RECENT_CHATS = "com.xabber.android.data.RECENT_CHATS";
    private static final String ACTION_SPECIFIC_CHAT = "com.xabber.android.data.ACTION_SPECIFIC_CHAT";
    public static final String ACTION_FORWARD = "com.xabber.android.data.ACTION_FORWARD";

    public static final String EXTRA_NEED_SCROLL_TO_UNREAD = "com.xabber.android.data.EXTRA_NEED_SCROLL_TO_UNREAD";
    public static final String EXTRA_OTR_REQUEST = "com.xabber.android.data.EXTRA_OTR_REQUEST";
    public static final String EXTRA_OTR_PROGRESS = "com.xabber.android.data.EXTRA_OTR_PROGRESS";
//    public static final String ACTION_OTR_REQUEST = "com.xabber.android.data.ACTION_OTR_REQUEST";
//    public static final String ACTION_OTR_PROGRESS = "com.xabber.android.data.ACTION_OTR_PROGRESS";
    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 24;
    public final static String KEY_ACCOUNT = "KEY_ACCOUNT";
    public final static String KEY_USER = "KEY_USER";
    public final static String KEY_QUESTION = "KEY_QUESTION";
    public final static String KEY_SHOW_ARCHIVED = "KEY_SHOW_ARCHIVED";
    public final static String KEY_MESSAGES_ID = "KEY_MESSAGES_ID";

    private static final String SAVE_SELECTED_PAGE = "com.xabber.android.ui.activity.ChatActivity.SAVE_SELECTED_PAGE";
    private static final String SAVE_SELECTED_ACCOUNT = "com.xabber.android.ui.activity.ChatActivity.SAVE_SELECTED_ACCOUNT";
    private static final String SAVE_SELECTED_USER = "com.xabber.android.ui.activity.ChatActivity.SAVE_SELECTED_USER";
    private static final String SAVE_EXIT_ON_SEND = "com.xabber.android.ui.activity.ChatActivity.SAVE_EXIT_ON_SEND";

    private UpdateBackpressure updateBackpressure;
    private String extraText = null;
    private ArrayList<String> forwardsIds;

    private StatusBarPainter statusBarPainter;

    private boolean isVisible;

    private AccountJid account;
    private UserJid user;
    private int selectedPagePosition;
    private boolean exitOnSend;

    private Animation shakeAnimation = null;

    @Nullable
    private ContactVcardViewerFragment contactVcardViewerFragment;
    @Nullable
    private ChatFragment chatFragment;

    private Toolbar toolbar;
    private View contactTitleView;
    private View showcaseView;
    private Button btnShowcaseGotIt;

    boolean showArchived = false;
    private boolean needScrollToUnread = false;

    public boolean isShowArchived() {
        return showArchived;
    }

    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public String getCurrentFragment(){ return currentFragment;}

    @Nullable
    private static AccountJid getAccount(Intent intent) {
        AccountJid value = EntityIntentBuilder.getAccount(intent);
        if (value != null)
            return value;
        // Backward compatibility.

        String stringExtra = intent.getStringExtra("com.xabber.android.data.account");
        if (stringExtra == null) {
            return null;
        }

        try {
            return AccountJid.from(stringExtra);
        } catch (XmppStringprepException e) {
            LogManager.exception(LOG_TAG, e);
            return null;
        }
    }

    @Nullable
    private static UserJid getUser(Intent intent) {
        UserJid value = EntityIntentBuilder.getUser(intent);
        if (value != null)
            return value;
        // Backward compatibility.

        String stringExtra = intent.getStringExtra("com.xabber.android.data.user");
        if (stringExtra == null) {
            return null;
        }

        try {
            return UserJid.from(stringExtra);
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(LOG_TAG, e);
            return null;
        }
    }

    private static boolean hasAttention(Intent intent) {
        return ACTION_ATTENTION.equals(intent.getAction());
    }

    public static Intent createSpecificChatIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = new EntityIntentBuilder(context, ChatActivity.class).setAccount(account).setUser(user).build();
        intent.setAction(ACTION_SPECIFIC_CHAT);
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        intent.putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived());
        return intent;
    }

    public static Intent createClearTopIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = createSpecificChatIntent(context, account, user);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    public static Intent createForwardIntent(Context context, AccountJid account, UserJid user,
                                             ArrayList<String> messagesIds) {
        Intent intent = new EntityIntentBuilder(context, ChatActivity.class).setAccount(account).setUser(user).build();
        intent.setAction(ACTION_FORWARD);
        intent.putStringArrayListExtra(KEY_MESSAGES_ID, messagesIds);
        return intent;
    }

    /**
     * Create intent to send message.
     * <p/>
     * Contact list will not be shown on when chat will be closed.
     * @param text    if <code>null</code> then user will be able to send a number
     *                of messages. Else only one message can be send.
     */
    public static Intent createSendIntent(Context context, AccountJid account, UserJid user, String text) {
        Intent intent = ChatActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        intent.putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived());
        //LogManager.i(LOG_TAG, "Intent created:" + intent.c);
        return intent;
    }

    public static Intent createSendUriIntent(Context context, AccountJid account,
                                             UserJid user, Uri uri) {
        Intent intent = ChatActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        return intent;
    }

    public static Intent createSendUrisIntent(Context context, AccountJid account,
                                             UserJid user, ArrayList<Uri> uris) {
        Intent intent = ChatActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        return intent;
    }

    public static Intent createAttentionRequestIntent(Context context, AccountJid account, UserJid user) {
        Intent intent = ChatActivity.createClearTopIntent(context, account, user);
        intent.setAction(ACTION_ATTENTION);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogManager.i(LOG_TAG, "onCreate " + savedInstanceState);

        setContentView(R.layout.activity_chat);
        getWindow().setBackgroundDrawable(null);

        updateBackpressure = new UpdateBackpressure(this);

        contactTitleView = findViewById(R.id.contact_title);
        contactTitleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CrowdfundingChat.USER.equals(user.getBareJid().toString())){
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.chat_container,
                            ContactVcardViewerFragment.newInstance(account, user), CONTACT_INFO_FRAGMENT_TAG);
                    fragmentTransaction.commit();
                    updateToolbar();
                    //updateToolbarMenuIcon();
                }
            }
        });

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFragment.equals(CONTACT_INFO_FRAGMENT_TAG)){
                    initChats(false);
                    updateToolbar();
                    //updateToolbarMenuIcon();
                } else close();
            }
        });

        showcaseView = findViewById(R.id.showcaseView);

        statusBarPainter = new StatusBarPainter(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Intent intent = getIntent();
        account = getAccount(intent);
        user = getUser(intent);

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
        gestureDetector = new GestureDetector(new SwipeDetector());
        initChats(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogManager.i(LOG_TAG, "onNewIntent");
        setIntent(intent);
        //getSelectedPageDataFromIntent();
        getInitialChatFromIntent();
        if (account != null && user != null)
            selectChat(account, user);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogManager.i(LOG_TAG, "onResume");

        updateToolbar();
        updateStatusBar();

        isVisible = true;

        Application.getInstance().addUIListener(OnChatStateListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);

        this.showArchived = getIntent().getBooleanExtra(KEY_SHOW_ARCHIVED, false);

        Intent intent = getIntent();

        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(account, user);
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())
                && intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {

            Uri receivedUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            intent.removeExtra(Intent.EXTRA_STREAM);
            handleShareFileUri(receivedUri);

        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {

            extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (extraText != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                exitOnSend = false;
            }

        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                intent.removeExtra(Intent.EXTRA_STREAM);
                handleShareFileUris(uris);
            }
        }

        needScrollToUnread = intent.getBooleanExtra(EXTRA_NEED_SCROLL_TO_UNREAD, false);

        if (intent.getBooleanExtra(EXTRA_OTR_REQUEST, false) ||
                intent.getBooleanExtra(EXTRA_OTR_PROGRESS, false)) {
            handleOtrIntent(intent);
        }

        //showcase
//        if (!SettingsManager.chatShowcaseSuggested()) {
//            showShowcase(true);
//        }

        // forward
        if (ACTION_FORWARD.equals(intent.getAction())) {
            List<String> messages = intent.getStringArrayListExtra(KEY_MESSAGES_ID);
            forwardsIds = (ArrayList<String>) messages;
            intent.removeExtra(KEY_MESSAGES_ID);
            exitOnSend = false;
        }

        insertExtraText();
        setForwardMessages();
    }

    public void handleShareFileUri(Uri fileUri) {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
            List<Uri> uris = new ArrayList<>();
            uris.add(fileUri);
            HttpFileUploadManager.getInstance().uploadFileViaUri(account, user, uris, this);
        }
    }

    public void handleShareFileUris(ArrayList<Uri> uris) {
        if (uris.size() == 0) {
            Toast.makeText(this, R.string.could_not_get_path_to_file, Toast.LENGTH_SHORT).show();
            return;
        }

        if (uris.size() > 10) {
            Toast.makeText(this, R.string.too_many_files_at_once, Toast.LENGTH_SHORT).show();
            return;
        }

        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
            HttpFileUploadManager.getInstance().uploadFileViaUri(account, user, uris, this);
        }
    }

    private void handleOtrIntent(Intent intent) {
        String account = intent.getStringExtra(KEY_ACCOUNT);
        String user = intent.getStringExtra(KEY_USER);
        String question = intent.getStringExtra(KEY_QUESTION);

        if (account != null && user != null) {
            try {
                AccountJid accountJid = AccountJid.from(account);
                UserJid userJid = UserJid.from(user);
                AbstractChat chat = MessageManager.getInstance().getOrCreateChat(accountJid, userJid);

                if (chat instanceof RegularChat) {
                    if (intent.getBooleanExtra(EXTRA_OTR_PROGRESS, false)) {
                        ((RegularChat) chat).setIntent(QuestionActivity.createCancelIntent(
                                Application.getInstance(), accountJid, userJid));
                    } else {
                        ((RegularChat)chat).setIntent(QuestionActivity.createIntent(
                                Application.getInstance(),
                                accountJid, userJid, question != null, true, question));
                    }
                }
            } catch (UserJid.UserJidCreateException | XmppStringprepException e) {
                e.printStackTrace();
            }
        }
        getIntent().removeExtra(EXTRA_OTR_REQUEST);
        getIntent().removeExtra(EXTRA_OTR_PROGRESS);
    }

    @Override
    public void onBackPressed() {
        close();
        super.onBackPressed();
    }

    private void initChats(boolean animated) {
        Fragment fragment;
//        if (CrowdfundingChat.USER.equals(user.getBareJid().toString()))
//            fragment = CrowdfundingChatFragment.newInstance();
//        else
        fragment = ChatFragment.newInstance(account, user);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (animated) fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        fragmentTransaction.replace(R.id.chat_container, fragment, CHAT_FRAGMENT_TAG);
        fragmentTransaction.commit();
    }

    private void getInitialChatFromIntent() {
        Intent intent = getIntent();
        AccountJid newAccount = getAccount(intent);
        UserJid newUser = getUser(intent);

        if (newAccount != null) {
            this.account = newAccount;
        }
        if (newUser != null) {
            this.user = newUser;
        }
        initChats(false);
        LogManager.i(LOG_TAG, "getInitialChatFromIntent " + this.user);
    }

//    private void getSelectedPageDataFromIntent() {
//        Intent intent = getIntent();
//
//        if (intent.getAction() == null) {
//            return;
//        }
//
//        switch (intent.getAction()) {
//            case ACTION_RECENT_CHATS:
//                selectedPagePosition = PAGE_POSITION_RECENT_CHATS;
//                break;
//
//            case ACTION_SPECIFIC_CHAT:
//            case ACTION_ATTENTION:
//            case Intent.ACTION_SEND:
//                selectedPagePosition = ChatViewerAdapter.PAGE_POSITION_CHAT;
//                break;
//            case Intent.ACTION_SEND_MULTIPLE:
//                selectedPagePosition = ChatViewerAdapter.PAGE_POSITION_CHAT;
//                break;
//            case ACTION_FORWARD:
//                selectedPagePosition = ChatViewerAdapter.PAGE_POSITION_CHAT;
//                break;
//        }
//    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        account = savedInstanceState.getParcelable(SAVE_SELECTED_ACCOUNT);
        user = savedInstanceState.getParcelable(SAVE_SELECTED_USER);
        //selectedPagePosition = savedInstanceState.getInt(SAVE_SELECTED_PAGE);
        exitOnSend = savedInstanceState.getBoolean(SAVE_EXIT_ON_SEND);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putInt(SAVE_SELECTED_PAGE, selectedPagePosition);
        outState.putParcelable(SAVE_SELECTED_ACCOUNT, account);
        outState.putParcelable(SAVE_SELECTED_USER, user);
        outState.putBoolean(SAVE_EXIT_ON_SEND, exitOnSend);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateBackpressure.removeRefreshRequests();
        Application.getInstance().removeUIListener(OnChatStateListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
        MessageManager.getInstance().removeVisibleChat();
        isVisible = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

//    private void selectChatPage(BaseEntity chat, boolean smoothScroll) {
//        account = chat.getAccount();
//        user = chat.getUser();
//
//        if (chatFragment != null) {
//            chatFragment.saveInputState();
//            chatFragment.setChat(chat.getAccount(), chat.getUser());
//        }
//
//        chatViewerAdapter.selectChat(account, user);
//
//        //selectPage(ChatViewerAdapter.PAGE_POSITION_CHAT, smoothScroll);
//
//        if (contactVcardViewerFragment != null) {
//            contactVcardViewerFragment.updateContact(account, user);
//            contactVcardViewerFragment.requestVCard();
//        }
//    }

//    public void selectPage(int position, boolean smoothScroll) {
//        onPageSelected(position);
//        //viewPager.setCurrentItem(position, smoothScroll);
//    }

    @Override
    public void update() {
        updateToolbar();
        updateChat();
        updateStatusBar();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
        updateBackpressure.refreshRequest();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageUpdateEvent event) {
        updateBackpressure.refreshRequest();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        updateBackpressure.refreshRequest();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        updateBackpressure.refreshRequest();
    }

    @Override
    public void onChatStateChanged(Collection<RosterContact> entities) {
        updateToolbar();
    }

    private void updateToolbar() {
        if (CrowdfundingChat.USER.equals(user.getBareJid().toString())) setCrowdfundingToolbar();
        else {
            NewContactTitleInflater.updateTitle(contactTitleView, this,
                    RosterManager.getInstance().getBestContact(account, user), getNotifMode());
            toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
            updateToolbarMenuIcon();
        }
        setUpOptionsMenu(toolbar.getMenu());
    }

    private void updateToolbarMenuIcon(){
        if (currentFragment.equals(CHAT_FRAGMENT_TAG))
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        else if (currentFragment.equals(CONTACT_INFO_FRAGMENT_TAG))
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_settings_white_24dp));
    }

    private void setCrowdfundingToolbar() {
        toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        final TextView nameView = (TextView) contactTitleView.findViewById(R.id.name);
        final ImageView avatarView = (ImageView) contactTitleView.findViewById(R.id.ivAvatar);
        final TextView statusTextView = (TextView) contactTitleView.findViewById(R.id.status_text);
        final ImageView statusModeView = (ImageView) contactTitleView.findViewById(R.id.ivStatus);

        nameView.setText(R.string.xabber_chat_title);
        statusTextView.setText(R.string.xabber_chat_description);
        avatarView.setImageDrawable(getResources().getDrawable(R.drawable.xabber_logo_80dp));
        statusModeView.setVisibility(View.GONE);
    }

    private void updateStatusBar() {
        if (CrowdfundingChat.USER.equals(user.getBareJid().toString()))
            statusBarPainter.updateWithDefaultColor();
        else statusBarPainter.updateWithAccountName(account);
    }

    private void updateChat() {
        if (chatFragment != null) {
            chatFragment.updateContact();
        }
    }

//    @Override
//    public void onPageScrollStateChanged(int state) {
//    }

//    @Override
//    public void onChatViewAdapterFinishUpdate() {
//        insertExtraText();
//        setForwardMessages();
//    }

    private void setForwardMessages() {
        if (forwardsIds == null || chatFragment == null) return;
        chatFragment.setForwardIds(forwardsIds);
        forwardsIds = null;
    }

    private void insertExtraText() {
        if (extraText == null || extraText.equals("")) {
            return;
        }

        if (chatFragment != null) {
            chatFragment.setInputText(extraText);
            extraText = null;
        }
    }

    private void selectChat(AccountJid accountJid, UserJid userJid) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(accountJid, userJid);
        //selectChatPage(chat, true);
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
    public void registerChatFragment(ChatFragment chatFragment) {
        this.chatFragment = chatFragment;
    }

    @Override
    public void unregisterChatFragment() {
        this.chatFragment = null;
    }

    private void close() {
        update();
        finish();
        ActivityManager.getInstance().clearStack(false);
        //if (!ActivityManager.getInstance().hasContactList(this)) {
        startActivity(ContactListActivity.createIntent(this));
        //}

    }

    @Override
    public void onBlockedListChanged(AccountJid account) {
        // if chat of blocked contact is currently opened, it should be closed
        final Collection<UserJid> blockedContacts = BlockingManager.getInstance().getCachedBlockedContacts(account);
        if (blockedContacts.contains(user)) {
            close();
        }
    }

    @Override
    public void onVCardReceived() {}

    @Override
    public void registerVCardFragment(ContactVcardViewerFragment fragment) {
        this.contactVcardViewerFragment = fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setUpOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onMenuItemClick(item);
    }

    private void setUpRegularChatMenu(Menu menu, AbstractChat abstractChat) {

        // archive/unarchive chat
        menu.findItem(R.id.action_archive_chat).setVisible(!abstractChat.isArchived());
        menu.findItem(R.id.action_unarchive_chat).setVisible(abstractChat.isArchived());

        // mute chat
        menu.findItem(R.id.action_mute_chat).setVisible(abstractChat.notifyAboutMessage());
        menu.findItem(R.id.action_unmute_chat).setVisible(!abstractChat.notifyAboutMessage());
    }

    private void setUpMUCInfoMenu(Menu menu, AbstractChat abstractChat) {
        RoomState chatState = ((RoomChat) abstractChat).getState();
        if (chatState == RoomState.unavailable)
            menu.findItem(R.id.action_join_conference).setVisible(true);
        else {
            menu.findItem(R.id.action_invite_to_chat).setVisible(true);

            if (chatState != RoomState.error) {
                menu.findItem(R.id.action_leave_conference).setVisible(true);
            }
        }

        menu.findItem(R.id.action_remove_contact).setVisible(false);
        menu.findItem(R.id.action_delete_conference).setVisible(true);

        menu.findItem(R.id.action_generate_qrcode).setVisible(false);
        menu.findItem(R.id.action_send_contact).setVisible(false);
        menu.findItem(R.id.action_edit_alias).setVisible(false);
        menu.findItem(R.id.action_edit_groups).setVisible(false);
        menu.findItem(R.id.action_block_contact).setVisible(false);
    }

    private void setUpMUCMenu(Menu menu, AbstractChat abstractChat) {

        RoomState chatState = ((RoomChat) abstractChat).getState();
        if (chatState == RoomState.error) {
            menu.findItem(R.id.action_authorization_settings).setVisible(true);
        }

        setUpRegularChatMenu(menu, abstractChat);
    }

    private void setUpContactInfoMenu(Menu menu, AbstractChat abstractChat) {
        // request subscription
        AbstractContact abstractContact = RosterManager.getInstance()
                .getAbstractContact(abstractChat.getAccount(), abstractChat.getUser());
        menu.findItem(R.id.action_request_subscription).setVisible(!abstractContact.isSubscribed());
    }

    private void setUpOptionsMenu(Menu menu) {
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);
        if (abstractChat != null) {
            menu.clear();
            MenuInflater inflater = getMenuInflater();
            if (CrowdfundingChat.USER.equals(user.getBareJid().toString())) {
                menu.clear();
                return;
            }
            if (currentFragment.equals(CONTACT_INFO_FRAGMENT_TAG)) {
                inflater.inflate(R.menu.toolbar_contact, menu);
                setUpContactInfoMenu(menu, abstractChat);
                if (abstractChat instanceof RoomChat)
                    setUpMUCInfoMenu(menu, abstractChat);
                return;
            }
            if (abstractChat instanceof RoomChat) {
                inflater.inflate(R.menu.menu_chat_muc, menu);
                setUpMUCMenu(menu, abstractChat);
                return;
            }
            if (abstractChat instanceof RegularChat) {
                inflater.inflate(R.menu.menu_chat_regular, menu);
                setUpRegularChatMenu(menu, abstractChat);
                return;
            }
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        currentFragment = fragment.getTag();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);

        switch (item.getItemId()) {
            /* security menu */

            case R.id.action_start_encryption:
                if (chatFragment != null)
                    chatFragment.showResourceChoiceAlert(account, user, false);
                return true;

            case R.id.action_restart_encryption:
                if (chatFragment != null)
                    chatFragment.showResourceChoiceAlert(account, user, true);
                return true;

            case R.id.action_stop_encryption:
                if (chatFragment != null)
                    chatFragment.stopEncryption(account, user);
                return true;

            case R.id.action_verify_with_fingerprint:
                startActivity(FingerprintActivity.createIntent(this, account, user));
                return true;

            case R.id.action_verify_with_question:
                startActivity(QuestionActivity.createIntent(this, account, user, true, false, null));
                return true;

            case R.id.action_verify_with_shared_secret:
                startActivity(QuestionActivity.createIntent(this, account, user, false, false, null));
                return true;

            /* regular chat options menu */

            case R.id.action_send_contact:
                sendContact();
                return true;

            case R.id.action_generate_qrcode:
                generateQR();
                return true;

            case R.id.action_view_contact:
                if (chatFragment != null)
                    chatFragment.showContactInfo();
                return true;

            case R.id.action_configure_notifications:
                startActivity(CustomNotifySettings.createIntent(this, account, user));
                return true;

            case R.id.action_authorization_settings:
                startActivity(ConferenceAddActivity.createIntent(this, account, user.getBareUserJid()));
                return true;

            case R.id.action_clear_history:
                if (chatFragment != null)
                    chatFragment.clearHistory(account, user);
                return true;

            case R.id.action_export_chat:
                if (chatFragment != null)
                    chatFragment.onExportChatClick();
                return true;

            case R.id.action_call_attention:
                if (chatFragment != null)
                    chatFragment.callAttention();
                return true;

            case R.id.action_block_contact:
                BlockContactDialog.newInstance(account, user).show(getFragmentManager(), BlockContactDialog.class.getName());
                return true;

            case R.id.action_request_subscription:
                try {
                    PresenceManager.getInstance().requestSubscription(account, user);
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
                return true;

            case R.id.action_archive_chat:
                if (abstractChat != null) abstractChat.setArchived(true, true);
                return true;

            case R.id.action_unarchive_chat:
                if (abstractChat != null) abstractChat.setArchived(false, true);
                return true;

            case R.id.action_mute_chat:
                showSnoozeDialog(abstractChat);
                return true;

            case R.id.action_unmute_chat:
                if (abstractChat != null) abstractChat.setNotificationStateOrDefault(
                        new NotificationState(NotificationState.NotificationMode.enabled,
                                0), true);
                onSnoozed();
                return true;

            /* conference specific options menu */
            case R.id.action_join_conference:
                onJoinConferenceClick();
                return true;

            case R.id.action_invite_to_chat:
                startActivity(SearchActivity.createRoomInviteIntent(this, account, user.getBareUserJid()));
                return true;

            case R.id.action_leave_conference:
                if (chatFragment != null)
                    chatFragment.leaveConference(account, user);
                return true;

            /* contact info menu */
            case R.id.action_edit_alias:
                editAlias();
                return true;

            case R.id.action_edit_groups:
                startActivity(GroupEditActivity.createIntent(this, account, user));
                return true;

            case R.id.action_remove_contact:
                ContactDeleteDialogFragment.newInstance(account, user)
                        .show(getFragmentManager(), "CONTACT_DELETE");
                return true;

            case R.id.action_delete_conference:
                ContactDeleteDialogFragment.newInstance(account, user)
                        .show(getFragmentManager(), "CONTACT_DELETE");
                return true;

            default:
                return false;
        }
    }

    @Override
    public void playIncomingAnimation() {
        if (shakeAnimation == null) {
            shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake);
        }
        toolbar.findViewById(R.id.name_holder).startAnimation(shakeAnimation);
    }

    @Override
    public void onOccupantClick(String username) {
        if (chatFragment != null) chatFragment.mentionUser(username);
    }

    private NotificationState.NotificationMode getNotifMode() {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        if (chat != null)
            return chat.getNotificationState().determineModeByGlobalSettings(chat instanceof RoomChat);
        else return NotificationState.NotificationMode.bydefault;
    }

    private void editAlias() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_alias);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, user);
        if (rosterContact != null)
            input.setText(rosterContact.getName());
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RosterManager.getInstance().setName(account, user, input.getText().toString());
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void sendContact() {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, user);
        String text = rosterContact != null ? rosterContact.getName() + "\nxmpp:" + user.toString() : "xmpp:" + user.toString();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
    }

    public void forwardMessages(ArrayList<String> messagesIds) {
        Intent sendIntent = SearchActivity.createIntent(this);
        sendIntent.setAction(ACTION_FORWARD);
        sendIntent.putStringArrayListExtra(KEY_MESSAGES_ID, messagesIds);
        finish();
        startActivity(sendIntent);
    }

    public void onJoinConferenceClick() {
        MUCManager.getInstance().joinRoom(account, user.getJid().asEntityBareJidIfPossible(), true);
    }

    public void showShowcase(boolean show) {
        showcaseView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            btnShowcaseGotIt = (Button) findViewById(R.id.btnGotIt);
            btnShowcaseGotIt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsManager.setChatShowcaseSuggested();
                    showShowcase(false);
                }
            });
        }
    }

    public void showAttachDialog() {
        if (chatFragment != null) {
            AttachDialog dialog = AttachDialog.newInstance(chatFragment);
            dialog.show(getSupportFragmentManager(), "attach_fragment");
        }
    }

    public void showSnoozeDialog(AbstractChat chat) {
        SnoozeDialog dialog = SnoozeDialog.newInstance(chat, this);
        dialog.show(getSupportFragmentManager(), "snooze_fragment");
    }

    @Override
    public void onSnoozed() {
        setUpOptionsMenu(toolbar.getMenu());
        updateToolbar();
    }

    public boolean needScrollToUnread() {
        if (needScrollToUnread) {
            needScrollToUnread = false;
            return true;
        } else return false;
    }

    private void  generateQR(){
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, user);
        Intent intent = QRCodeActivity.createIntent(ChatActivity.this, account);
        String textName = rosterContact != null ? rosterContact.getName() : "";
        intent.putExtra("account_name", textName);
        String textAddress = user.toString();
        intent.putExtra("account_address", textAddress);
        intent.putExtra("caller", "ChatActivity");
        startActivity(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            if (gestureDetector.onTouchEvent(ev))
                return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;

            if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                if (ChatActivity.this.getCurrentFragment().equals(CONTACT_INFO_FRAGMENT_TAG)){
                    //ChatActivity.this.initChats(true);
                }
                else{
                    startActivity(new Intent(ChatActivity.this, ContactListActivity.class));
                    ChatActivity.this.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                }
                return true;
            }

            return false;
        }
    }
}
