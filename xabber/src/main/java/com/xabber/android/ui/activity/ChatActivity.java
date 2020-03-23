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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
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
import com.xabber.android.ui.dialog.ChatDeleteDialog;
import com.xabber.android.ui.dialog.ContactDeleteDialog;
import com.xabber.android.ui.dialog.SnoozeDialog;
import com.xabber.android.ui.fragment.ChatFragment;
import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
import com.xabber.android.ui.helper.NewContactTitleInflater;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.ui.preferences.CustomNotifySettings;
import com.xabber.android.ui.widget.BottomMessagesPanel;

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
        UpdateBackpressure.UpdatableObject, SnoozeDialog.OnSnoozeListener, SensorEventListener {

    private static final String LOG_TAG = ChatActivity.class.getSimpleName();
    private static final String CHAT_FRAGMENT_TAG = "CHAT_FRAGMENT_TAG";
    private static final String CONTACT_INFO_FRAGMENT_TAG = "CONTACT_INFO_FRAGMENT_TAG";

    private static final int SWIPE_MIN_DISTANCE = 300;
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
    private Uri attachmentUri;
    private ArrayList<Uri> attachmentUris;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private AccountJid account;
    private ContactJid user;
    private boolean exitOnSend = false;

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
    private static ContactJid getUser(Intent intent) {
        ContactJid value = EntityIntentBuilder.getUser(intent);
        if (value != null)
            return value;
        // Backward compatibility.

        String stringExtra = intent.getStringExtra("com.xabber.android.data.user");
        if (stringExtra == null) {
            return null;
        }

        try {
            return ContactJid.from(stringExtra);
        } catch (ContactJid.UserJidCreateException e) {
            LogManager.exception(LOG_TAG, e);
            return null;
        }
    }

    private static boolean hasAttention(Intent intent) {
        return ACTION_ATTENTION.equals(intent.getAction());
    }

    public static Intent createSpecificChatIntent(Context context, AccountJid account, ContactJid user) {
        Intent intent = new EntityIntentBuilder(context, ChatActivity.class).setAccount(account).setUser(user).build();
        intent.setAction(ACTION_SPECIFIC_CHAT);
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        intent.putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived());
        return intent;
    }

    public static Intent createClearTopIntent(Context context, AccountJid account, ContactJid user) {
        Intent intent = createSpecificChatIntent(context, account, user);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    public static Intent createForwardIntent(Context context, AccountJid account, ContactJid user,
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
    public static Intent createSendIntent(Context context, AccountJid account, ContactJid user, String text) {
        Intent intent = ChatActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        intent.putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived());
        //LogManager.i(LOG_TAG, "Intent created:" + intent.c);
        return intent;
    }

    public static Intent createSendUriIntent(Context context, AccountJid account,
                                             ContactJid user, Uri uri) {
        Intent intent = ChatActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        return intent;
    }

    public static Intent createSendUrisIntent(Context context, AccountJid account,
                                              ContactJid user, ArrayList<Uri> uris) {
        Intent intent = ChatActivity.createSpecificChatIntent(context, account, user);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        return intent;
    }

    public static Intent createAttentionRequestIntent(Context context, AccountJid account, ContactJid user) {
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
        contactTitleView.setOnClickListener(v ->
                startActivity(ContactViewerActivity.createIntent(ChatActivity.this, account, user)));

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_grey_24dp));
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        }
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFragment.equals(CONTACT_INFO_FRAGMENT_TAG)){
                    initChats(false);
                    updateToolbar();
                } else close();
            }
        });

        showcaseView = findViewById(R.id.showcaseView);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        Intent intent = getIntent();
        account = getAccount(intent);
        user = getUser(intent);

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        } else initChats(false);
        gestureDetector = new GestureDetector(new SwipeDetector());
        //initChats(false);
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

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        updateToolbar();
        updateStatusBar();

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

        // forward
        if (ACTION_FORWARD.equals(intent.getAction())) {
            List<String> messages = intent.getStringArrayListExtra(KEY_MESSAGES_ID);
            forwardsIds = (ArrayList<String>) messages;
            intent.removeExtra(KEY_MESSAGES_ID);
        }

        insertExtraText();
        setForwardMessages();
    }

    public void handleShareFileUri(Uri fileUri) {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
            if (HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
                List<Uri> uris = new ArrayList<>();
                uris.add(fileUri);
                HttpFileUploadManager.getInstance().uploadFileViaUri(account, user, uris, this);
            } else {
                showUploadNotSupportedDialog();
            }
        } else {
            attachmentUri = fileUri;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_ATTACH_FILE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (getIntent().getAction() != null) {
                switch (getIntent().getAction()) {
                    case Intent.ACTION_SEND:
                        handleShareFileUri(attachmentUri);
                        break;
                    case Intent.ACTION_SEND_MULTIPLE:
                        handleShareFileUris(attachmentUris);
                        break;
                }
            }
        } else if (requestCode == PERMISSIONS_REQUEST_ATTACH_FILE && grantResults[0] == PackageManager.PERMISSION_DENIED){
            Toast.makeText(this, R.string.no_permission_storage, Toast.LENGTH_LONG).show();
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
            if (HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
                HttpFileUploadManager.getInstance().uploadFileViaUri(account, user, uris, this);
            } else {
                showUploadNotSupportedDialog();
            }
        } else {
            attachmentUris = uris;
        }
    }

    private void handleOtrIntent(Intent intent) {
        String account = intent.getStringExtra(KEY_ACCOUNT);
        String user = intent.getStringExtra(KEY_USER);
        String question = intent.getStringExtra(KEY_QUESTION);

        if (account != null && user != null) {
            try {
                AccountJid accountJid = AccountJid.from(account);
                ContactJid contactJid = ContactJid.from(user);
                AbstractChat chat = MessageManager.getInstance().getOrCreateChat(accountJid, contactJid);

                if (chat instanceof RegularChat) {
                    if (intent.getBooleanExtra(EXTRA_OTR_PROGRESS, false)) {
                        ((RegularChat) chat).setIntent(QuestionActivity.createCancelIntent(
                                Application.getInstance(), accountJid, contactJid));
                    } else {
                        ((RegularChat)chat).setIntent(QuestionActivity.createIntent(
                                Application.getInstance(),
                                accountJid, contactJid, question != null, true, question));
                    }
                }
            } catch (ContactJid.UserJidCreateException | XmppStringprepException e) {
                e.printStackTrace();
            }
        }
        getIntent().removeExtra(EXTRA_OTR_REQUEST);
        getIntent().removeExtra(EXTRA_OTR_PROGRESS);
    }

    private void showUploadNotSupportedDialog() {
        String serverName = account.getFullJid().getDomain().toString();
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setMessage(this.getResources().getString(R.string.error_file_upload_not_support, serverName))
                .setTitle(getString(R.string.error_sending_file, ""))
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        close();
        super.onBackPressed();
    }

    private void initChats(boolean animated) {
        Fragment fragment;
        Fragment oldFragment = getSupportFragmentManager().findFragmentByTag(CHAT_FRAGMENT_TAG);

        fragment = ChatFragment.newInstance(account, user);

        if (oldFragment != null) {
            FragmentTransaction fragmentTransactionOld = getSupportFragmentManager().beginTransaction();
            fragmentTransactionOld.remove(oldFragment);
            fragmentTransactionOld.commit();
        }
        FragmentTransaction fragmentTransactionNew = getSupportFragmentManager().beginTransaction();
        if (animated) fragmentTransactionNew.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        fragmentTransactionNew.add(R.id.chat_container, fragment, CHAT_FRAGMENT_TAG);
        fragmentTransactionNew.commit();
    }

    private void getInitialChatFromIntent() {
        Intent intent = getIntent();
        AccountJid newAccount = getAccount(intent);
        ContactJid newUser = getUser(intent);

        if (newAccount != null) {
            this.account = newAccount;
        }
        if (newUser != null) {
            this.user = newUser;
        }
        initChats(false);
        LogManager.i(LOG_TAG, "getInitialChatFromIntent " + this.user);
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        account = savedInstanceState.getParcelable(SAVE_SELECTED_ACCOUNT);
        user = savedInstanceState.getParcelable(SAVE_SELECTED_USER);
        exitOnSend = savedInstanceState.getBoolean(SAVE_EXIT_ON_SEND);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVE_SELECTED_ACCOUNT, account);
        outState.putParcelable(SAVE_SELECTED_USER, user);
        outState.putBoolean(SAVE_EXIT_ON_SEND, exitOnSend);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateBackpressure.removeRefreshRequests();
        mSensorManager.unregisterListener(this);
        Application.getInstance().removeUIListener(OnChatStateListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
        MessageManager.getInstance().removeVisibleChat();
        if (exitOnSend) ActivityManager.getInstance().cancelTask(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

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
        for (BaseEntity entity : entities) {
            if (entity.equals(account, user)) {
                updateBackpressure.refreshRequest();
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            updateBackpressure.refreshRequest();
        }
    }

    @Override
    public void onChatStateChanged(Collection<RosterContact> entities) {
        for (RosterContact contact : entities) {
            if (contact.getUser().getBareJid().equals(user.getBareJid())) {
                updateToolbar();
                return;
            }
        }
    }

    private void updateToolbar() {
        NewContactTitleInflater.updateTitle(contactTitleView, this,
                RosterManager.getInstance().getBestContact(account, user), getNotifMode());
//        updateToolbarMenuIcon();
        setUpOptionsMenu(toolbar.getMenu());

        /* Update background color via current main user and theme; */
        TypedValue typedValue = new TypedValue();
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
            toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountRippleColor(account));
        } else {
            this.getTheme().resolveAttribute(R.attr.bars_color, typedValue, true);
            toolbar.setBackgroundColor(typedValue.data);
        }

    }

    public Toolbar getToolbar() {
        return toolbar;
    }


    private void updateStatusBar() {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            StatusBarPainter.instanceUpdateWithAccountName(this, account);
        else {
            TypedValue typedValue = new TypedValue();
            this.getTheme().resolveAttribute(R.attr.bars_color, typedValue, true);
            StatusBarPainter.instanceUpdateWIthColor(this, typedValue.data);
        }
    }

    private void updateChat() {
        if (chatFragment != null) {
            chatFragment.updateContact();
        }
    }

    private void setForwardMessages() {
        if (forwardsIds == null || forwardsIds.isEmpty() || chatFragment == null) return;
        chatFragment.setBottomPanelMessagesIds(forwardsIds, BottomMessagesPanel.Purposes.FORWARDING);
        forwardsIds = null;
    }

    public void hideForwardPanel() {
        if (chatFragment == null) return;
        chatFragment.hideBottomMessagePanel();
    }

    public void setUpVoiceMessagePresenter(String tempPath) {
        if (chatFragment == null) return;
        chatFragment.setVoicePresenterData(tempPath);
    }

    public void finishVoiceRecordLayout() {
        if (chatFragment == null) return;
        chatFragment.clearVoiceMessage();
        chatFragment.finishVoiceRecordLayout();
    }

    private void insertExtraText() {
        if (extraText == null || extraText.equals("")) {
            return;
        }
        if (chatFragment != null) {
            chatFragment.setInputText(extraText);
            extraText = null;
            exitOnSend = true;
        }
    }

    private void selectChat(AccountJid accountJid, ContactJid contactJid) {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(accountJid, contactJid);
        //selectChatPage(chat, true);
    }

    @Override
    public void onCloseChat() {
        close();
    }

    @Override
    public void onMessageSent() {

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
        if (chatFragment != null) {
            chatFragment.cleanUpVoice(true);
        }
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
        final Collection<ContactJid> blockedContacts = BlockingManager.getInstance().getCachedBlockedContacts(account);
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

    private void setUpContactInfoMenu(Menu menu, AbstractChat abstractChat) {
        // request subscription
        AbstractContact abstractContact = RosterManager.getInstance()
                .getAbstractContact(abstractChat.getAccount(), abstractChat.getUser());
        menu.findItem(R.id.action_request_subscription).setVisible(!abstractContact.isSubscribed() && !RosterManager.getInstance().hasSubscriptionPending(account, user));
    }

    private void setUpOptionsMenu(Menu menu) {
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);
        if (abstractChat != null) {
            menu.clear();
            MenuInflater inflater = getMenuInflater();
            if (currentFragment.equals(CONTACT_INFO_FRAGMENT_TAG)) {
                inflater.inflate(R.menu.toolbar_contact, menu);
                setUpContactInfoMenu(menu, abstractChat);
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
                BlockContactDialog.newInstance(account, user)
                        .show(getSupportFragmentManager(), BlockContactDialog.class.getName());
                return true;

            case R.id.action_request_subscription:
                try {
                    PresenceManager.getInstance().requestSubscription(account, user);
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
                return true;

            case R.id.action_archive_chat:
                if (abstractChat != null) {
                    abstractChat.setArchived(true, true);
                    setUpOptionsMenu(toolbar.getMenu());
                }
                return true;

            case R.id.action_unarchive_chat:
                if (abstractChat != null) {
                    abstractChat.setArchived(false, true);
                    setUpOptionsMenu(toolbar.getMenu());
                }
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

            case R.id.action_invite_to_chat:
                startActivity(SearchActivity.createRoomInviteIntent(this, account, user.getBareUserJid()));
                return true;

            /* contact info menu */
            case R.id.action_edit_contact:
                startActivity(ContactEditActivity.createIntent(this, account, user));
                return true;

            case R.id.action_remove_contact:
                ContactDeleteDialog.newInstance(account, user)
                        .show(getSupportFragmentManager(), ContactDeleteDialog.class.getName());
                return true;

            case R.id.action_delete_chat:
                ChatDeleteDialog.newInstance(account, user)
                        .show(getSupportFragmentManager(), ChatDeleteDialog.class.getName());
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

    private NotificationState.NotificationMode getNotifMode() {
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, user);
        if (chat != null)
            return chat.getNotificationState().determineModeByGlobalSettings();
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
        //finish();
        startActivity(sendIntent);
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (sensorEvent.values[0] < sensorEvent.sensor.getMaximumRange()) {
                //near
            } else {
                //far
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
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
