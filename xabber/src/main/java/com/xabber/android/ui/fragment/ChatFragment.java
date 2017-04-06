package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.messagerealm.SyncInfo;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.file.FileUtils;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.httpfileupload.HttpUploadListener;
import com.xabber.android.data.extension.mam.LastHistoryLoadFinishedEvent;
import com.xabber.android.data.extension.mam.LastHistoryLoadStartedEvent;
import com.xabber.android.data.extension.mam.LoadHistorySettings;
import com.xabber.android.data.extension.mam.MamManager;
import com.xabber.android.data.extension.mam.PreviousHistoryLoadFinishedEvent;
import com.xabber.android.data.extension.mam.PreviousHistoryLoadStartedEvent;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomState;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.NewIncomingMessageEvent;
import com.xabber.android.data.message.RegularChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ConferenceAddActivity;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.activity.FingerprintActivity;
import com.xabber.android.ui.activity.OccupantListActivity;
import com.xabber.android.ui.activity.QuestionActivity;
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.BlockContactDialog;
import com.xabber.android.ui.dialog.ChatExportDialogFragment;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.ui.preferences.ChatContactSettings;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jxmpp.jid.impl.JidCreate;

import java.util.Timer;
import java.util.TimerTask;

import github.ankushsachdeva.emojicon.EmojiconGridView;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.emoji.Emojicon;
import io.realm.RealmResults;
import io.realm.Sort;

public class ChatFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        View.OnClickListener, Toolbar.OnMenuItemClickListener,
        ChatMessageAdapter.Message.MessageClickListener, HttpUploadListener,
        ChatMessageAdapter.Listener {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";

    private static final String SAVE_ACCOUNT = "com.xabber.android.ui.fragment.ARGUMENT_ACCOUNT";
    private static final String SAVE_USER = "com.xabber.android.ui.fragment.ARGUMENT_USER";

    private final long STOP_TYPING_DELAY = 4000; // in ms

    public static final int FILE_SELECT_ACTIVITY_REQUEST_CODE = 23;
    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 24;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 26;
    private static final int PERMISSIONS_REQUEST_EXPORT_CHAT = 27;

    private AccountJid account;
    private UserJid user;

    private Toolbar toolbar;
    private View contactTitleView;
    private EditText inputView;
    private ImageButton sendButton;
    private ImageButton securityButton;
    private ImageButton attachButton;
    private View lastHistoryProgressBar;
    private View previousHistoryProgressBar;

    private RecyclerView realmRecyclerView;
    private ChatMessageAdapter chatMessageAdapter;
    private LinearLayoutManager layoutManager;

    boolean isInputEmpty = true;
    private boolean skipOnTextChanges = false;


    private ChatViewerFragmentListener listener;
    private Animation shakeAnimation = null;

    private MessageItem clickedMessageItem;

    private Timer stopTypingTimer = new Timer();

    private boolean isRemoteHistoryRequested = false;
    private int firstRemoteSyncedItemPosition = RecyclerView.NO_POSITION;
    private RealmResults<SyncInfo> syncInfoResults;
    private RealmResults<MessageItem> messageItems;
    private boolean toBeScrolled;

    public static ChatFragment newInstance(AccountJid account, UserJid user) {
        ChatFragment fragment = new ChatFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (ChatViewerFragmentListener) activity;
            listener.registerChatFragment(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ChatViewerFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);

        if (savedInstanceState != null) {
            account = savedInstanceState.getParcelable(SAVE_ACCOUNT);
            user = savedInstanceState.getParcelable(SAVE_USER);
        }

        LogManager.i(this, "onCreate " + user);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        contactTitleView = view.findViewById(R.id.contact_title);
        contactTitleView.findViewById(R.id.avatar).setOnClickListener(this);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar_default);
        toolbar.inflateMenu(R.menu.toolbar_chat);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(getActivity());
            }
        });

        setHasOptionsMenu(true);

        sendButton = (ImageButton) view.findViewById(R.id.button_send_message);
        sendButton.setColorFilter(ColorManager.getInstance().getAccountPainter().getGreyMain());

        attachButton = (ImageButton) view.findViewById(R.id.button_attach);
        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAttachButtonPressed();
            }
        });


        lastHistoryProgressBar = view.findViewById(R.id.chat_last_history_progress_bar);
        previousHistoryProgressBar = view.findViewById(R.id.chat_previous_history_progress_bar);


        securityButton = (ImageButton) view.findViewById(R.id.button_security);
        securityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSecurityMenu();
            }
        });

        // to avoid strange bug on some 4.x androids
        view.findViewById(R.id.input_layout).setBackgroundColor(ColorManager.getInstance().getChatInputBackgroundColor());

        view.findViewById(R.id.button_send_message).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMessage();
                    }
                }
        );

        setUpInputView(view);
        setUpEmoji(view);

        realmRecyclerView = (RecyclerView) view.findViewById(R.id.chat_messages_recycler_view);

        layoutManager = new LinearLayoutManager(getActivity());
        realmRecyclerView.setLayoutManager(layoutManager);

        layoutManager.setStackFromEnd(true);


        realmRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy < 0) {
                    loadHistoryIfNeeded();
                }

                if (dy >= 0) {
                    toBeScrolled = false;
                }
            }
        });

        setChat(account, user);

        return view;
    }

    public void setChat(AccountJid accountJid, UserJid userJid) {
        this.account = accountJid;
        this.user = userJid;

        AbstractChat abstractChat = getChat();

        if (!(abstractChat instanceof RegularChat)) {
            securityButton.setVisibility(View.GONE);
        }

        messageItems = abstractChat.getMessages();
        syncInfoResults = abstractChat.getSyncInfo();

        chatMessageAdapter = new ChatMessageAdapter(getActivity(), messageItems, abstractChat, this);
        realmRecyclerView.setAdapter(chatMessageAdapter);

        restoreInputState();

        updateContact();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);
        if (accountItem != null) {
            LoadHistorySettings loadHistorySettings = accountItem.getLoadHistorySettings();

            if (loadHistorySettings == LoadHistorySettings.all || loadHistorySettings == LoadHistorySettings.current) {
                if (!isRemoteHistoryRequested) {
                    MamManager.getInstance().requestLastHistoryByUser(getChat());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        LogManager.i(this, "onResume");

        updateContact();
        restoreInputState();
        restoreScrollState();
    }

    @Override
    public void onPause() {
        super.onPause();

        ChatStateManager.getInstance().onPaused(account, user);

        saveInputState();
        saveScrollState();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(SAVE_ACCOUNT, account);
        outState.putParcelable(SAVE_USER, user);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        chatMessageAdapter.release();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (listener != null) {
            listener.unregisterChatFragment();
            listener = null;
        }
    }



    private void setUpInputView(View view) {
        inputView = (EditText) view.findViewById(R.id.chat_input);

        inputView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (SettingsManager.chatsSendByEnter()
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        inputView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!skipOnTextChanges && stopTypingTimer != null) {
                    stopTypingTimer.cancel();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                ChatFragment.this.afterTextChanged(text);
            }

        });
    }

    private void afterTextChanged(Editable text) {
        setUpInputViewButtons();

        if (skipOnTextChanges) {
            return;
        }

        ChatStateManager.getInstance().onComposing(account, user, text);

        stopTypingTimer = new Timer();
        stopTypingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ChatStateManager.getInstance().onPaused(account, user);
                    }
                });
            }

        }, STOP_TYPING_DELAY);
    }

    private void setUpEmoji(View view) {
        final ImageButton emojiButton = (ImageButton) view.findViewById(R.id.button_emoticon);
        final View rootView = view.findViewById(R.id.root_view);


        // Give the topmost view of your activity layout hierarchy. This will be used to measure soft keyboard height
        final EmojiconsPopup popup = new EmojiconsPopup(rootView, getActivity());

        //Will automatically set size according to the soft keyboard size
        popup.setSizeForSoftKeyboard();

        //If the emoji popup is dismissed, change emojiButton to smiley icon
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_mood_black_24dp);
            }
        });

        //If the text keyboard closes, also dismiss the emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {

            }

            @Override
            public void onKeyboardClose() {
                if (popup.isShowing())
                    popup.dismiss();
            }
        });

        //On emoji clicked, add it to edittext
        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {
                if (inputView == null || emojicon == null) {
                    return;
                }

                int start = inputView.getSelectionStart();
                int end = inputView.getSelectionEnd();
                if (start < 0) {
                    inputView.append(emojicon.getEmoji());
                } else {
                    inputView.getText().replace(Math.min(start, end),
                            Math.max(start, end), emojicon.getEmoji(), 0,
                            emojicon.getEmoji().length());
                }
            }
        });

        //On backspace clicked, emulate the KEYCODE_DEL key event
        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {
                KeyEvent event = new KeyEvent(
                        0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                inputView.dispatchKeyEvent(event);
            }
        });

        // To toggle between text keyboard and emoji keyboard keyboard(Popup)
        emojiButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //If popup is not showing => emoji keyboard is not visible, we need to show it
                if(!popup.isShowing()){

                    //If keyboard is visible, simply show the emoji popup
                    if(popup.isKeyBoardOpen()){
                        popup.showAtBottom();
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_keyboard_black_24dp);
                    }

                    //else, open the text keyboard first and immediately after that show the emoji popup
                    else{
                        inputView.setFocusableInTouchMode(true);
                        inputView.requestFocus();
                        popup.showAtBottomPending();
                        final InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(inputView, InputMethodManager.SHOW_IMPLICIT);
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_keyboard_black_24dp);
                    }
                }

                //If popup is showing, simply dismiss it to show the undelying text keyboard
                else{
                    popup.dismiss();
                }
            }
        });
    }

    public void saveScrollState() {
        ChatManager.getInstance().setScrollState(account, user, layoutManager.onSaveInstanceState());
    }

    private void loadHistoryIfNeeded() {
        AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);
        if (accountItem == null) {
            return;
        }

        LoadHistorySettings loadHistorySettings = accountItem.getLoadHistorySettings();

        if (loadHistorySettings != LoadHistorySettings.current
                && loadHistorySettings != LoadHistorySettings.all) {
            return;
        }

        if (isRemoteHistoryRequested) {
            return;
        }

        int visibleItemCount = layoutManager.getChildCount();

        if (visibleItemCount == 0) {
            return;
        }

        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

        if (firstVisibleItemPosition / visibleItemCount <= 2) {
            requestRemoteHistoryLoad();
            return;
        }

        if (firstVisibleItemPosition < firstRemoteSyncedItemPosition) {
            requestRemoteHistoryLoad();
            return;
        }

        if (firstVisibleItemPosition - firstRemoteSyncedItemPosition < visibleItemCount * 2) {
            requestRemoteHistoryLoad();
            return;
        }
    }

    private void requestRemoteHistoryLoad() {
        if (!isRemoteHistoryRequested) {
            AbstractChat chat = getChat();
            if (chat != null) {
                MamManager.getInstance().requestPreviousHistory(chat);
            }

        }
    }

    @Nullable
    private AbstractChat getChat() {
        return MessageManager.getInstance().getOrCreateChat(account, user);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LastHistoryLoadStartedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            lastHistoryProgressBar.setVisibility(View.VISIBLE);
            isRemoteHistoryRequested = true;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LastHistoryLoadFinishedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            lastHistoryProgressBar.setVisibility(View.GONE);
            isRemoteHistoryRequested = false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PreviousHistoryLoadStartedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            LogManager.i(this, "PreviousHistoryLoadStartedEvent");
            previousHistoryProgressBar.setVisibility(View.VISIBLE);
            isRemoteHistoryRequested = true;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PreviousHistoryLoadFinishedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            LogManager.i(this, "PreviousHistoryLoadFinishedEvent");
            isRemoteHistoryRequested = false;
            previousHistoryProgressBar.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageUpdateEvent event) {
        chatMessageAdapter.onChange();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NewIncomingMessageEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            playIncomingAnimation();
        }
    }

    private void onAttachButtonPressed() {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
            startFileSelection();
        }

    }

    private void startFileSelection() {
        Intent intent = (new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
        startActivityForResult(intent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ATTACH_FILE :
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    startFileSelection();
                } else {
                    onNoReadPermissionError();
                }
                break;
            case PERMISSIONS_REQUEST_EXPORT_CHAT :
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    showExportChatDialog();
                } else {
                    onNoWritePermissionError();
                }
                break;
        }
    }

    private void onNoWritePermissionError() {
        Toast.makeText(getActivity(), R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
    }

    private void onNoReadPermissionError() {
        Toast.makeText(getActivity(), R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode != FILE_SELECT_ACTIVITY_REQUEST_CODE) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        final Uri fileUri = result.getData();
        final String path = FileUtils.getPath(getActivity(), fileUri);

        LogManager.i(this, String.format("File uri: %s, path: %s", fileUri, path));

        if (path == null) {
            Toast.makeText(getActivity(), R.string.could_not_get_path_to_file, Toast.LENGTH_SHORT).show();
            return;
        }

        uploadFile(path);
    }

    private void uploadFile(String path) {
        HttpFileUploadManager.getInstance().uploadFile(account, user, path);
    }

    private void changeEmojiKeyboardIcon(ImageView iconToBeChanged, int drawableResourceId){
        iconToBeChanged.setImageResource(drawableResourceId);
    }

    private void showSecurityMenu() {
        PopupMenu popup = new PopupMenu(getActivity(), securityButton);
        popup.inflate(R.menu.security);
        popup.setOnMenuItemClickListener(this);

        Menu menu = popup.getMenu();

        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);

        if (securityLevel == SecurityLevel.plain) {
            menu.findItem(R.id.action_start_encryption).setVisible(true)
                    .setEnabled(SettingsManager.securityOtrMode() != SettingsManager.SecurityOtrMode.disabled);
        } else {
            menu.findItem(R.id.action_restart_encryption).setVisible(true);
        }

        boolean isEncrypted = securityLevel != SecurityLevel.plain;

        menu.findItem(R.id.action_stop_encryption).setEnabled(isEncrypted);
        menu.findItem(R.id.action_verify_with_fingerprint).setEnabled(isEncrypted);
        menu.findItem(R.id.action_verify_with_question).setEnabled(isEncrypted);
        menu.findItem(R.id.action_verify_with_shared_secret).setEnabled(isEncrypted);

        popup.show();
    }

    private void setUpInputViewButtons() {
        boolean empty = inputView.getText().toString().trim().isEmpty();

        if (empty != isInputEmpty) {
            isInputEmpty = empty;
        }

        if (isInputEmpty) {
            sendButton.setColorFilter(ColorManager.getInstance().getAccountPainter().getGreyMain());
            sendButton.setEnabled(false);
            securityButton.setVisibility(View.VISIBLE);
            if (HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
                attachButton.setVisibility(View.VISIBLE);
            } else {
                attachButton.setVisibility(View.GONE);
            }
        } else {
            sendButton.setEnabled(true);
            sendButton.setColorFilter(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(account));
            securityButton.setVisibility(View.GONE);
            attachButton.setVisibility(View.GONE);
        }
    }



    public void restoreInputState() {
        skipOnTextChanges = true;

        inputView.setText(ChatManager.getInstance().getTypedMessage(account, user));
        inputView.setSelection(ChatManager.getInstance().getSelectionStart(account, user),
                ChatManager.getInstance().getSelectionEnd(account, user));

        skipOnTextChanges = false;

        if (!inputView.getText().toString().isEmpty()) {
            inputView.requestFocus();
        }
    }

    public void saveInputState() {
        ChatManager.getInstance().setTyped(account, user, inputView.getText().toString(),
                inputView.getSelectionStart(), inputView.getSelectionEnd());
    }

    private void sendMessage() {
        String text = inputView.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        clearInputText();

        sendMessage(text);

        listener.onMessageSent();

        if (SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.always
                || (getActivity().getResources().getBoolean(R.bool.landscape)
                && SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.landscape)) {
            ChatActivity.hideKeyboard(getActivity());
        }
    }

    private void sendMessage(String text) {
        MessageManager.getInstance().sendMessage(account, user, text);
        scrollDown();
    }

    /**
     * This method used for hardware menu button
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_chat, menu);
        setUpOptionsMenu(menu);
    }

    /**
     * This method used for hardware menu button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onMenuItemClick(item);
    }

    private void setUpOptionsMenu(Menu menu) {
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);

        if (abstractChat instanceof RoomChat) {
            RoomState chatState = ((RoomChat) abstractChat).getState();

            if (chatState == RoomState.available) {
                menu.findItem(R.id.action_list_of_occupants).setVisible(true);
            }

            if (chatState == RoomState.unavailable) {
                menu.findItem(R.id.action_join_conference).setVisible(true);

            } else {
                menu.findItem(R.id.action_invite_to_chat).setVisible(true);

                if (chatState == RoomState.error) {
                    menu.findItem(R.id.action_authorization_settings).setVisible(true);
                } else {
                    menu.findItem(R.id.action_leave_conference).setVisible(true);
                }
            }
        }

        if (abstractChat instanceof RegularChat) {
            menu.findItem(R.id.action_view_contact).setVisible(true);
            menu.findItem(R.id.action_close_chat).setVisible(true);
            menu.findItem(R.id.action_block_contact).setVisible(true);
        }
    }

    public void updateContact() {
        ContactTitleInflater.updateTitle(contactTitleView, getActivity(),
                RosterManager.getInstance().getBestContact(account, user));
        toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
        setUpOptionsMenu(toolbar.getMenu());
        updateSecurityButton();
    }

    private void scrollDown() {
        LogManager.i(this, "scrollDown");
        realmRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
    }

    private void updateSecurityButton() {
        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);
        if (securityButton != null) {
            // strange null ptr happens
            securityButton.setImageLevel(securityLevel.getImageLevel());
        }
    }

    public boolean isEqual(BaseEntity chat) {
        return chat != null && this.account.equals(chat.getAccount()) && this.user.equals(chat.getUser());
    }

    public void setInputText(String additional) {
        skipOnTextChanges = true;
        inputView.setText(additional);
        inputView.setSelection(additional.length());
        skipOnTextChanges = false;
    }

    public AccountJid getAccount() {
        return account;
    }

    public UserJid getUser() {
        return user;
    }

    private void clearInputText() {
        skipOnTextChanges = true;
        inputView.getText().clear();
        skipOnTextChanges = false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            /* security menu */

            case R.id.action_start_encryption:
                startEncryption(account, user);
                return true;

            case R.id.action_restart_encryption:
                restartEncryption(account, user);
                return true;

            case R.id.action_stop_encryption:
                stopEncryption(account, user);
                return true;

            case R.id.action_verify_with_fingerprint:
                startActivity(FingerprintActivity.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_verify_with_question:
                startActivity(QuestionActivity.createIntent(getActivity(), account, user, true, false, null));
                return true;

            case R.id.action_verify_with_shared_secret:
                startActivity(QuestionActivity.createIntent(getActivity(), account, user, false, false, null));
                return true;

            /* regular chat options menu */

            case R.id.action_view_contact:
                showContactInfo();
                return true;

            case R.id.action_chat_settings:
                startActivity(ChatContactSettings.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_authorization_settings:
                startActivity(ConferenceAddActivity.createIntent(getActivity(), account, user.getBareUserJid()));
                return true;

            case R.id.action_close_chat:
                closeChat(account, user);
                return true;

            case R.id.action_clear_history:
                clearHistory(account, user);
                return true;

            case R.id.action_export_chat:
                onExportChatClick();
                return true;

            case R.id.action_call_attention:
                callAttention();
                return true;

            case R.id.action_block_contact:
                BlockContactDialog.newInstance(account, user).show(getFragmentManager(), BlockContactDialog.class.getName());
                return true;

            /* conference specific options menu */

            case R.id.action_join_conference:
                MUCManager.getInstance().joinRoom(account, user.getJid().asEntityBareJidIfPossible(), true);
                return true;

            case R.id.action_invite_to_chat:
                startActivity(ContactListActivity.createRoomInviteIntent(getActivity(), account, user.getBareUserJid()));
                return true;

            case R.id.action_leave_conference:
                leaveConference(account, user);
                return true;

            case R.id.action_list_of_occupants:
                startActivity(OccupantListActivity.createIntent(getActivity(), account, user));
                return true;

            /* message popup menu */

            case R.id.action_message_repeat:
                if (MessageItem.isUploadFileMessage(clickedMessageItem)) {
                    uploadFile(clickedMessageItem.getFilePath());
                } else {
                    sendMessage(clickedMessageItem.getText());
                }
                return true;

            case R.id.action_message_copy:
                Spannable spannable = MessageItem.getSpannable(clickedMessageItem);
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText(spannable, spannable));
                return true;

            case R.id.action_message_quote:
                setInputText("> " + clickedMessageItem.getText() + "\n");
                return true;

            case R.id.action_message_remove:
                MessageManager.getInstance().removeMessage(clickedMessageItem.getUniqueId());
                return true;

            case R.id.action_message_open_muc_private_chat:
                UserJid occupantFullJid = null;
                try {
                    occupantFullJid = UserJid.from(
                            JidCreate.domainFullFrom(user.getJid().asDomainBareJid(),
                                    clickedMessageItem.getResource()));
                    MessageManager.getInstance().openChat(account, occupantFullJid);
                    startActivity(ChatActivity.createSpecificChatIntent(getActivity(), account, occupantFullJid));

                } catch (UserJid.UserJidCreateException e) {
                    LogManager.exception(this, e);
                }

                return true;

            default:
                return false;
        }
    }

    private void onExportChatClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(this, PERMISSIONS_REQUEST_EXPORT_CHAT)) {
            showExportChatDialog();
        }

    }

    private void showExportChatDialog() {
        ChatExportDialogFragment.newInstance(account, user).show(getFragmentManager(), "CHAT_EXPORT");
    }

    private void stopEncryption(AccountJid account, UserJid user) {
        try {
            OTRManager.getInstance().endSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void restartEncryption(AccountJid account, UserJid user) {
        try {
            OTRManager.getInstance().refreshSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void startEncryption(AccountJid account, UserJid user) {
        try {
            OTRManager.getInstance().startSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.avatar) {
            showContactInfo();
        }
    }

    private void showContactInfo() {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(account, user)) {
            intent = ContactActivity.createIntent(getActivity(), account, user);
        } else {
            intent = ContactEditActivity.createIntent(getActivity(), account, user);
        }
        startActivity(intent);
    }

    private void closeChat(AccountJid account, UserJid user) {
        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);
        listener.onCloseChat();
    }

    private void clearHistory(AccountJid account, UserJid user) {
        MessageManager.getInstance().clearHistory(account, user);
    }

    private void leaveConference(AccountJid account, UserJid user) {
        MUCManager.getInstance().leaveRoom(account, user.getJid().asEntityBareJidIfPossible());
        closeChat(account, user);
    }

    private void callAttention() {
        try {
            AttentionManager.getInstance().sendAttention(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    @Override
    public void onMessageClick(View caller, int position) {
        int itemViewType = chatMessageAdapter.getItemViewType(position);

        if (itemViewType == ChatMessageAdapter.VIEW_TYPE_INCOMING_MESSAGE
                || itemViewType == ChatMessageAdapter.VIEW_TYPE_OUTGOING_MESSAGE) {

            clickedMessageItem = chatMessageAdapter.getMessageItem(position);

            PopupMenu popup = new PopupMenu(getActivity(), caller);
            popup.inflate(R.menu.item_message);
            popup.setOnMenuItemClickListener(this);

            final Menu menu = popup.getMenu();

            if (clickedMessageItem.isError()) {
                menu.findItem(R.id.action_message_repeat).setVisible(true);
            }

            if (MessageItem.isUploadFileMessage(clickedMessageItem)) {
                menu.findItem(R.id.action_message_copy).setVisible(false);
                menu.findItem(R.id.action_message_quote).setVisible(false);
                menu.findItem(R.id.action_message_remove).setVisible(false);
            }

            if (clickedMessageItem.isIncoming() && MUCManager.getInstance()
                    .hasRoom(account, user.getJid().asEntityBareJidIfPossible())) {
                menu.findItem(R.id.action_message_open_muc_private_chat).setVisible(true);
            }

            popup.show();
        }
    }

    @Override
    public void onMessageImageClick(View caller, int position) {
        MessageItem messageItem = chatMessageAdapter.getMessageItem(position);

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(messageItem.getText()));
        startActivity(i);
    }

    public void playIncomingAnimation() {
        if (shakeAnimation == null) {
            shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
        }
        toolbar.findViewById(R.id.name_holder).startAnimation(shakeAnimation);
    }

    @Override
    public void onSuccessfullUpload(String getUrl) {
    }

    @Override
    public void onMessageNumberChanged(int prevItemCount) {
        int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

        if (toBeScrolled || lastVisibleItemPosition == -1 || lastVisibleItemPosition == (prevItemCount - 1)) {
            toBeScrolled = true;
            scrollDown();
        }
    }

    public void restoreScrollState() {
        layoutManager.onRestoreInstanceState(ChatManager.getInstance().getScrollState(account, user));
    }

    @Override
    public void onMessagesUpdated() {
        updateFirstRemoteSyncedItemPosition();
        loadHistoryIfNeeded();
    }

    private void updateFirstRemoteSyncedItemPosition() {
        if (!syncInfoResults.isLoaded() || !messageItems.isLoaded() || syncInfoResults.isEmpty()) {
            return;
        }

        SyncInfo syncInfo = syncInfoResults.first();

        String firstMamMessageStanzaId = syncInfo.getFirstMamMessageStanzaId();
        if (firstMamMessageStanzaId == null) {
            return;
        }

        RealmResults<MessageItem> allSorted = messageItems.where()
                .equalTo(MessageItem.Fields.STANZA_ID, firstMamMessageStanzaId)
                .findAllSorted(MessageItem.Fields.TIMESTAMP, Sort.ASCENDING);
        if (allSorted.isEmpty()) {
            return;
        }

        String firstRemotelySyncedMessageUniqueId = allSorted.last().getUniqueId();

        firstRemoteSyncedItemPosition = chatMessageAdapter.findMessagePosition(firstRemotelySyncedMessageUniqueId);
    }

    public interface ChatViewerFragmentListener {
        void onCloseChat();
        void onMessageSent();

        void registerChatFragment(ChatFragment chatFragment);

        void unregisterChatFragment();
    }
}
