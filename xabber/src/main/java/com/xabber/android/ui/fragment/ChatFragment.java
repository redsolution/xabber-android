package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.messagerealm.SyncInfo;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.file.FileManager;
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
import com.xabber.android.data.extension.otr.AuthAskEvent;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.filedownload.DownloadManager;
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
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.activity.ImageViewerActivity;
import com.xabber.android.ui.activity.QuestionActivity;
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.android.ui.adapter.CustomMessageMenuAdapter;
import com.xabber.android.ui.adapter.ResourceAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.AttachDialog;
import com.xabber.android.ui.dialog.ChatExportDialogFragment;
import com.xabber.android.ui.dialog.ChatHistoryClearDialog;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.ui.widget.CustomMessageMenu;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import github.ankushsachdeva.emojicon.EmojiconGridView;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.emoji.Emojicon;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;

public class ChatFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        View.OnClickListener, Toolbar.OnMenuItemClickListener,
        ChatMessageAdapter.Message.MessageClickListener, HttpUploadListener,
        ChatMessageAdapter.Listener, AdapterView.OnItemClickListener, PopupWindow.OnDismissListener,
        AttachDialog.Listener {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";

    private static final String SAVE_ACCOUNT = "com.xabber.android.ui.fragment.ARGUMENT_ACCOUNT";
    private static final String SAVE_USER = "com.xabber.android.ui.fragment.ARGUMENT_USER";
    private static final String SAVE_CURRENT_PICTURE_PATH = "com.xabber.android.ui.fragment.ARGUMENT_CURRENT_PICTURE_PATH";
    private static final String LOG_TAG = ChatFragment.class.getSimpleName();

    private final long STOP_TYPING_DELAY = 4000; // in ms

    public static final int FILE_SELECT_ACTIVITY_REQUEST_CODE = 11;
    private static final int REQUEST_IMAGE_CAPTURE = 12;

    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 21;
    private static final int PERMISSIONS_REQUEST_EXPORT_CHAT = 22;
    private static final int PERMISSIONS_REQUEST_CAMERA = 23;
    private static final int PERMISSIONS_REQUEST_DOWNLOAD_FILE = 24;

    private AccountJid account;
    private UserJid user;

    private EditText inputView;
    private ImageButton sendButton;
    private ImageButton securityButton;
    private ImageButton attachButton;
    private View lastHistoryProgressBar;
    private View previousHistoryProgressBar;

    private ViewStub stubNotify;
    private RelativeLayout notifyLayout;
    private TextView tvNotifyTitle;
    private TextView tvNotifyAction;

    private RecyclerView realmRecyclerView;
    private ChatMessageAdapter chatMessageAdapter;
    private LinearLayoutManager layoutManager;
    private SwipeRefreshLayout swipeContainer;
    private View placeholder;
    private LinearLayout inputLayout;
    private ViewStub stubJoin;
    private LinearLayout joinLayout;
    private LinearLayout actionJoin;
    private RelativeLayout btnScrollDown;
    private TextView tvNewReceivedCount;

    boolean isInputEmpty = true;
    private boolean skipOnTextChanges = false;


    private ChatViewerFragmentListener listener;

    private MessageItem clickedMessageItem;

    private Timer stopTypingTimer = new Timer();

    private boolean isRemoteHistoryRequested = false;
    private int firstRemoteSyncedItemPosition = RecyclerView.NO_POSITION;
    private RealmResults<SyncInfo> syncInfoResults;
    private RealmResults<MessageItem> messageItems;
    private boolean toBeScrolled;

    private List<HashMap<String, String>> menuItems = null;

    private int checkedResource; // use only for alert dialog

    private Intent notifyIntent;
    private String currentPicturePath;

    private int clickedAttachmentPos;
    private int clickedMessagePos;

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
            currentPicturePath = savedInstanceState.getString(SAVE_CURRENT_PICTURE_PATH);
        }

        LogManager.i(this, "onCreate " + user);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        tvNewReceivedCount = view.findViewById(R.id.tvNewReceivedCount);
        btnScrollDown = view.findViewById(R.id.btnScrollDown);
        btnScrollDown.setOnClickListener(this);

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
        inputLayout = (LinearLayout) view.findViewById(R.id.input_layout);
        inputLayout.setBackgroundColor(ColorManager.getInstance().getChatInputBackgroundColor());

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

                showScrollDownButtonIfNeed();
                hideUnreadMessageCountIfNeed();
            }
        });

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeColors(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeContainer.setRefreshing(false);
                AbstractChat chat = getChat();
                if (chat != null) {
                    if (chat.isRemotePreviousHistoryCompletelyLoaded())
                        Toast.makeText(getActivity(), R.string.toast_no_history, Toast.LENGTH_SHORT).show();
                    else requestRemoteHistoryLoad();
                }
            }
        });

        stubNotify = (ViewStub) view.findViewById(R.id.stubNotify);
        stubJoin = (ViewStub) view.findViewById(R.id.stubJoin);

        setChat(account, user);

        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                view.setBackgroundResource(R.drawable.chat_background_repeat_dark);
            } else {
                view.setBackgroundResource(R.drawable.chat_background_repeat);
            }
        } else {
            view.setBackgroundColor(ColorManager.getInstance().getChatBackgroundColor());
        }

        placeholder = view.findViewById(R.id.placeholder);
        placeholder.setOnClickListener(this);

        return view;
    }

    public void setChat(AccountJid accountJid, UserJid userJid) {
        this.account = accountJid;
        this.user = userJid;

        AbstractChat abstractChat = getChat();

        if (!(abstractChat instanceof RegularChat)) {
            securityButton.setVisibility(View.GONE);
        }

        if (abstractChat != null) {
            messageItems = abstractChat.getMessages();
            syncInfoResults = abstractChat.getSyncInfo();
        }

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

        showHideNotifyIfNeed();

        showJoinButtonIfNeed();
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
        if (!TextUtils.isEmpty(currentPicturePath)) {
            outState.putString(SAVE_CURRENT_PICTURE_PATH, currentPicturePath);
        }
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
            swipeContainer.setRefreshing(true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PreviousHistoryLoadFinishedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            LogManager.i(this, "PreviousHistoryLoadFinishedEvent");
            isRemoteHistoryRequested = false;
            previousHistoryProgressBar.setVisibility(View.GONE);
            swipeContainer.setRefreshing(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageUpdateEvent event) {
        chatMessageAdapter.onChange();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NewIncomingMessageEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            listener.playIncomingAnimation();
            playIncomingSound();
            increaseUnreadMessageCountIfNeed();
            chatMessageAdapter.setUnreadCount(chatMessageAdapter.getUnreadCount() + 1);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AuthAskEvent event) {
        if (event.getAccount() == getAccount() && event.getUser() == getUser()) {
            showHideNotifyIfNeed();
        }
    }

    private void onAttachButtonPressed() {
        if (!HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
            // show notification
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.error_file_upload_not_support)
                    .setTitle(getString(R.string.error_sending_file, ""))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }

        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
            ((ChatActivity)getActivity()).showAttachDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ATTACH_FILE:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    ((ChatActivity)getActivity()).showAttachDialog();
                else onNoReadPermissionError();
                break;

            case PERMISSIONS_REQUEST_EXPORT_CHAT:
                if (PermissionsRequester.isPermissionGranted(grantResults)) showExportChatDialog();
                else onNoWritePermissionError();
                break;

            case PERMISSIONS_REQUEST_CAMERA:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    startCamera();
                else onNoCameraPermissionError();
                break;

            case PERMISSIONS_REQUEST_DOWNLOAD_FILE:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    openFileOrDownload(clickedMessagePos, clickedAttachmentPos);
                else onNoWritePermissionError();
                break;
        }
    }

    private void onNoWritePermissionError() {
        Toast.makeText(getActivity(), R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
    }

    private void onNoReadPermissionError() {
        Toast.makeText(getActivity(), R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
    }

    private void onNoCameraPermissionError() {
        Toast.makeText(getActivity(), R.string.no_permission_to_camera, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                addMediaToGallery(currentPicturePath);
                uploadFile(currentPicturePath);
                break;

            case FILE_SELECT_ACTIVITY_REQUEST_CODE:
                ClipData clipData = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    clipData = result.getClipData();
                }

                final List<String> paths = new ArrayList<>();
                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri uri = item.getUri();
                        paths.add(FileUtils.getPath(getActivity(), uri));
                    }
                } else {
                    Uri fileUri = result.getData();
                    String path = FileUtils.getPath(getActivity(), fileUri);
                    if (path != null) paths.add(path);
                }

                if (paths.size() == 0) {
                    Toast.makeText(getActivity(), R.string.could_not_get_path_to_file, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (paths.size() > 10) {
                    Toast.makeText(getActivity(), R.string.too_many_files_at_once, Toast.LENGTH_SHORT).show();
                    return;
                }

                uploadFiles(paths);
                break;
        }
    }

    @Override
    public void onRecentPhotosSend(List<String> paths) {
        uploadFiles(paths);
    }

    @Override
    public void onGalleryClick() {
        Intent intent = (new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE));
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onFilesClick() {
        Intent intent = (new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onCameraClick() {
        if (PermissionsRequester.requestCameraPermissionIfNeeded(this,
                PERMISSIONS_REQUEST_CAMERA)) startCamera();
    }

    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File image = generatePicturePath();
        if (image != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileManager.getFileUri(image));
            currentPicturePath = image.getAbsolutePath();
        }
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    private static File generatePicturePath() {
        try {
            File storageDir = getAlbumDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            return new File(storageDir, "IMG_" + timeStamp + ".jpg");
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
        return null;
    }

    private static File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    Application.getInstance().getString(R.string.application_title_short));
            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()){
                    LogManager.w(LOG_TAG, "failed to create directory");
                    return null;
                }
            }
        } else {
            LogManager.w(LOG_TAG, "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private static void addMediaToGallery(String fromPath) {
        if (fromPath == null) {
            return;
        }
        File f = new File(fromPath);
        Uri contentUri = Uri.fromFile(f);
        addMediaToGallery(contentUri);
    }

    private static void addMediaToGallery(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(uri);
            Application.getInstance().sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    private void uploadFile(String path) {
        List<String> paths = new ArrayList<>();
        paths.add(path);
        HttpFileUploadManager.getInstance().uploadFile(account, user, paths, getActivity());
    }

    private void uploadFiles(List<String> paths) {
        HttpFileUploadManager.getInstance().uploadFile(account, user, paths, getActivity());
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
            attachButton.setVisibility(View.VISIBLE);
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
        hideUnreadMessageBackground();
        scrollDown();
    }


    public void updateContact() {
        updateSecurityButton();
        updateSendButtonSecurityLevel();
    }

    private void scrollDown() {
        LogManager.i(this, "scrollDown");
        realmRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
    }

    private void scrollToFirstUnread(int unreadCount) {
        layoutManager.scrollToPositionWithOffset(
                chatMessageAdapter.getItemCount() - unreadCount, 200);
        showUnreadMessage(unreadCount);
    }

    private void updateSecurityButton() {
        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);
        if (securityButton != null) {
            // strange null ptr happens
            securityButton.setImageLevel(securityLevel.getImageLevel());
        }
    }

    private void updateSendButtonSecurityLevel() {
        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);
        if (sendButton != null)
            sendButton.setImageLevel(securityLevel.getImageLevel());
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

    public void setInputTextAtCursor(String additional) {
        skipOnTextChanges = true;
        String currentText = inputView.getText().toString();
        if (currentText.isEmpty()) {
            inputView.setText(additional);
            inputView.setSelection(additional.length());
        } else {
            int cursorPosition = inputView.getSelectionStart();
            String first = currentText.substring(0, cursorPosition);
            String second = currentText.substring(cursorPosition);
            inputView.setText(first.concat(additional).concat(second));
            inputView.setSelection(first.length() + additional.length());
        }
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
        return ((ChatActivity)getActivity()).onMenuItemClick(item);
    }

    public void onExportChatClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(this, PERMISSIONS_REQUEST_EXPORT_CHAT)) {
            showExportChatDialog();
        }

    }

    private void showExportChatDialog() {
        ChatExportDialogFragment.newInstance(account, user).show(getFragmentManager(), "CHAT_EXPORT");
    }

    public void stopEncryption(AccountJid account, UserJid user) {
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

    public void showResourceChoiceAlert(final AccountJid account, final UserJid user, final boolean restartSession) {
        final List<Presence> allPresences = RosterManager.getInstance().getPresences(account, user.getJid());

        final List<Map<String, String>> items = new ArrayList<>();
        for (Presence presence : allPresences) {
            Jid fromJid = presence.getFrom();
            ClientInfo clientInfo = CapabilitiesManager.getInstance().getCachedClientInfo(fromJid);

            String client = "";
            if (clientInfo == null) {
                CapabilitiesManager.getInstance().requestClientInfoByUser(account, fromJid);
            } else if (clientInfo == ClientInfo.INVALID_CLIENT_INFO) {
                client = getString(R.string.unknown);
            } else {
                String name = clientInfo.getName();
                if (name != null) {
                    client = name;
                }

                String type = clientInfo.getType();
                if (type != null) {
                    if (client.isEmpty()) {
                        client = type;
                    } else {
                        client = client + "/" + type;
                    }
                }
            }

            Map<String, String> map = new HashMap<>();
            if (!client.isEmpty()) {
                map.put(ResourceAdapter.KEY_CLIENT, client);
            }

            Resourcepart resourceOrNull = fromJid.getResourceOrNull();
            if (resourceOrNull != null) {
                map.put(ResourceAdapter.KEY_RESOURCE, resourceOrNull.toString());
                items.add(map);
            }
        }
        final ResourceAdapter adapter = new ResourceAdapter(getActivity(), items);
        adapter.setCheckedItem(checkedResource);

        if (items.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.otr_select_resource);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    checkedResource = adapter.getCheckedItem();
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    checkedResource = adapter.getCheckedItem();
                    try {
                        RegularChat chat = (RegularChat) getChat();
                        if (chat != null) {
                            chat.setOTRresource(Resourcepart.from(items.get(checkedResource).get(ResourceAdapter.KEY_RESOURCE)));
                            if (restartSession) restartEncryption(account, user);
                            else startEncryption(account, user);
                        } else {
                            Toast.makeText(getActivity(), R.string.otr_select_toast_error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (XmppStringprepException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), R.string.otr_select_toast_error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setSingleChoiceItems(adapter, checkedResource, null).show();
        } else {
            Toast.makeText(getActivity(), R.string.otr_select_toast_resources_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.avatar) {
            showContactInfo();
        }
        if (v.getId() == R.id.placeholder) {
            ((ChatActivity)getActivity()).selectPage(1, true);
        }
        if (v.getId() == R.id.actionJoin) {
            ((ChatActivity)getActivity()).onJoinConferenceClick();
            showJoinButtonIfNeed();
        }
        if (v.getId() == R.id.btnScrollDown) {
            AbstractChat chat = getChat();
            if (chat != null && chat.getUnreadMessageCount() > 0)
                scrollToFirstUnread(chat.getUnreadMessageCount());
            else scrollDown();
        }
    }

    public void showContactInfo() {
        Intent intent;
        if (MUCManager.getInstance().hasRoom(account, user)) {
            intent = ContactActivity.createIntent(getActivity(), account, user);
        } else {
            intent = ContactEditActivity.createIntent(getActivity(), account, user);
        }
        startActivity(intent);
    }

    public void closeChat(AccountJid account, UserJid user) {
        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);
        listener.onCloseChat();
    }

    public void clearHistory(AccountJid account, UserJid user) {
        ChatHistoryClearDialog.newInstance(account, user).show(getFragmentManager(), ChatHistoryClearDialog.class.getSimpleName());
    }

    public void leaveConference(AccountJid account, UserJid user) {
        MUCManager.getInstance().leaveRoom(account, user.getJid().asEntityBareJidIfPossible());
        closeChat(account, user);
    }

    public void callAttention() {
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
            if (clickedMessageItem == null) {
                LogManager.w(LOG_TAG, "onMessageClick null message item. Position: " + position);
                return;
            }
            showCustomMenu(caller);
        }
    }

    public void showCustomMenu(View anchor) {
        menuItems = new ArrayList<HashMap<String, String>>();

        if (clickedMessageItem.isError()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_repeat", getString(R.string.message_repeat));
        }

        if (clickedMessageItem.isIncoming() && MUCManager.getInstance()
                .hasRoom(account, user.getJid().asEntityBareJidIfPossible())) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_appeal", getString(R.string.message_appeal));
        }

        if (!MessageItem.isUploadFileMessage(clickedMessageItem)) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_quote", getString(R.string.message_quote));
            CustomMessageMenu.addMenuItem(menuItems, "action_message_copy", getString(R.string.message_copy));
            CustomMessageMenu.addMenuItem(menuItems, "action_message_remove", getString(R.string.message_remove));
        }

        if (clickedMessageItem.isIncoming() && MUCManager.getInstance()
                .hasRoom(account, user.getJid().asEntityBareJidIfPossible())) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_open_muc_private_chat", getString(R.string.message_open_private_chat));
        }

        if (OTRManager.getInstance().isEncrypted(clickedMessageItem.getText())) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_show_original_otr", getString(R.string.message_otr_show_original));
        }

        if (clickedMessageItem.isForwarded()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_FORWARDED);
        } else if (clickedMessageItem.isReceivedFromMessageArchive()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_SYNCED);
        } else if (clickedMessageItem.isError()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_ERROR);
        } else if (!clickedMessageItem.isSent()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_NOT_SEND);
        } else if (clickedMessageItem.isDelivered()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_DELIVERED);
        } else if (clickedMessageItem.isAcknowledged()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_ACK);
        }

        CustomMessageMenu.showMenu(getActivity(), anchor, menuItems, this, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (menuItems != null && menuItems.size() > position) {
            HashMap<String, String> menuItem = menuItems.get(position);

            switch (menuItem.get(CustomMessageMenuAdapter.KEY_ID)) {
                case "action_message_repeat":
                    if (MessageItem.isUploadFileMessage(clickedMessageItem)) {
                        uploadFile(clickedMessageItem.getFilePath());
                    } else {
                        sendMessage(clickedMessageItem.getText());
                    }
                    break;
                case "action_message_copy":
                    Spannable spannable = MessageItem.getSpannable(clickedMessageItem);
                    ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                            .setPrimaryClip(ClipData.newPlainText(spannable, spannable));
                    break;
                case "action_message_appeal":
                    setInputTextAtCursor(clickedMessageItem.getResource().toString() + ", ");
                    break;
                case "action_message_quote":
                    setInputTextAtCursor("> " + clickedMessageItem.getText() + "\n");
                    break;
                case "action_message_remove":
                    MessageManager.getInstance().removeMessage(clickedMessageItem.getUniqueId());
                    break;
                case "action_message_open_muc_private_chat":
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
                    break;
                case "action_message_show_original_otr":
                    chatMessageAdapter.addOrRemoveItemNeedOriginalText(clickedMessageItem.getUniqueId());
                    chatMessageAdapter.notifyDataSetChanged();
                    break;
                case "action_message_status":
                    if (clickedMessageItem.isError())
                        showError(clickedMessageItem.getErrorDescription());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDismiss() {
        menuItems = null;
    }

    private void showError(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.error_description_title)
                .setMessage(message)
                .setPositiveButton("Ok", null)
                .show();
    }

    @Override
    public void onDownloadCancel() {
        DownloadManager.getInstance().cancelDownload(getActivity());
    }

    @Override
    public void onUploadCancel() {
        HttpFileUploadManager.getInstance().cancelUpload(getActivity());
    }

    @Override
    public void onDownloadError(String error) {
        Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileLongClick(final int messagePosition, final int attachmentPosition, View caller) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), caller);
        popupMenu.inflate(R.menu.menu_file_attachment);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_copy_link:
                        onCopyFileLink(messagePosition, attachmentPosition);
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void onCopyFileLink(int messagePosition, int attachmentPosition) {
        MessageItem messageItem = chatMessageAdapter.getMessageItem(messagePosition);
        if (messageItem == null) return;

        RealmList<Attachment> fileAttachments = new RealmList<>();
        for (Attachment attachment : messageItem.getAttachments()) {
            if (!attachment.isImage()) fileAttachments.add(attachment);
        }

        Attachment attachment = fileAttachments.get(attachmentPosition);
        if (attachment == null) return;
        String url = attachment.getFileUrl();

        ClipboardManager clipboardManager = ((ClipboardManager)
                getActivity().getSystemService(Context.CLIPBOARD_SERVICE));
        if (clipboardManager != null)
            clipboardManager.setPrimaryClip(ClipData.newPlainText(url, url));
        Toast.makeText(getActivity(), R.string.toast_link_copied, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileClick(int messagePosition, int attachmentPosition) {
        clickedAttachmentPos = attachmentPosition;
        clickedMessagePos = messagePosition;
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(
                this, PERMISSIONS_REQUEST_DOWNLOAD_FILE))
            openFileOrDownload(messagePosition, attachmentPosition);
    }

    private void openFileOrDownload(int messagePosition, int attachmentPosition) {
        MessageItem messageItem = chatMessageAdapter.getMessageItem(messagePosition);
        if (messageItem == null) {
            LogManager.w(LOG_TAG, "onMessageFileClick: null message item. Position: " + messagePosition);
            return;
        }

        if (messageItem.haveAttachments()) {
            RealmList<Attachment> fileAttachments = new RealmList<>();
            for (Attachment attachment : messageItem.getAttachments()) {
                if (!attachment.isImage()) fileAttachments.add(attachment);
            }

            Attachment attachment = fileAttachments.get(attachmentPosition);
            if (attachment == null) return;

            if (attachment.getFilePath() != null) {
                File file = new File(attachment.getFilePath());
                if (!file.exists()) {
                    MessageManager.setAttachmentLocalPathToNull(attachment.getUniqueId());
                    return;
                }

                Intent i = new Intent(Intent.ACTION_VIEW);
                String path = attachment.getFilePath();
                i.setDataAndType(FileProvider.getUriForFile(getActivity(),
                        getActivity().getApplicationContext().getPackageName()
                                + ".provider", new File(path)), attachment.getMimeType());
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    LogManager.exception(LOG_TAG, e);
                    Toast.makeText(getActivity(), R.string.toast_could_not_open_file, Toast.LENGTH_SHORT).show();
                }

            } else DownloadManager.getInstance().downloadFile(attachment, getAccount(), getActivity());
        }
    }

    @Override
    public void onImageClick(int messagePosition, int attachmentPosition) {
        MessageItem messageItem = chatMessageAdapter.getMessageItem(messagePosition);
        if (messageItem == null) {
            LogManager.w(LOG_TAG, "onMessageFileClick: null message item. Position: " + messagePosition);
            return;
        }

        if (messageItem.haveAttachments()) {
            try {
                startActivity(ImageViewerActivity.createIntent(getActivity(),
                        messageItem.getUniqueId(), attachmentPosition));
                // possible if image was not sent and don't have URL yet.
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else {
            try {
                startActivity(ImageViewerActivity.createIntent(getActivity(),
                        messageItem.getUniqueId(), messageItem.getText()));
                // possible if image was not sent and don't have URL yet.
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
    }

    public void playIncomingSound() {
        if (SettingsManager.eventsInChatSounds()) {
            final MediaPlayer mp = MediaPlayer.create(getActivity(), SettingsManager.eventsSound());
            mp.start();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mp.release();
                }
            });
        }
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

    public void saveScrollState() {
        int position = layoutManager.findLastCompletelyVisibleItemPosition();
        AbstractChat chat = getChat();

        if (position == -1) return;
        if (position == chatMessageAdapter.getItemCount() - 1) position = 0;
        if (chat != null) chat.saveLastPosition(position);
    }

    public void restoreScrollState() {
        AbstractChat chat = getChat();
        int position;
        int unread;
        if (chat != null) {
            position = chat.getLastPosition();
            unread = chat.getUnreadMessageCount();
            if (position == 0 && unread > 0)
                scrollToFirstUnread(unread);
            else if (position > 0) {
                layoutManager.scrollToPosition(position);
                showUnreadMessage(unread);
                updateNewReceivedMessageCounter(unread);
            }
        }
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

        void playIncomingAnimation();
    }

    public void showHideNotifyIfNeed() {
        AbstractChat chat = getChat();
        if (chat != null && chat instanceof RegularChat) {
            notifyIntent = ((RegularChat)chat).getIntent();
            if (notifyIntent != null) {
                setupNotifyLayout(notifyIntent);
            } else if (notifyLayout != null)
                notifyLayout.setVisibility(View.GONE);
        }
    }

    public void showPlaceholder(boolean show) {
        if (show) placeholder.setVisibility(View.VISIBLE);
        else placeholder.setVisibility(View.GONE);
    }

    private void inflateJoinLayout() {
        View view = stubJoin.inflate();
        joinLayout = (LinearLayout) view.findViewById(R.id.joinLayout);
        actionJoin = (LinearLayout) view.findViewById(R.id.actionJoin);
        actionJoin.setOnClickListener(this);
    }

    public void showJoinButtonIfNeed() {
        AbstractChat chat = getChat();
        if (chat != null && chat instanceof RoomChat) {
            RoomState chatState = ((RoomChat) chat).getState();
            if (chatState == RoomState.unavailable) {
                if (joinLayout == null)
                    inflateJoinLayout();
                joinLayout.setVisibility(View.VISIBLE);
                inputView.setVisibility(View.GONE);
            } else {
                if (joinLayout != null)
                    joinLayout.setVisibility(View.GONE);
                inputView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupNotifyLayout(Intent notifyIntent) {
        if (notifyLayout == null || tvNotifyTitle == null || tvNotifyAction == null) {
            inflateNotifyLayout();
        }

        if (notifyIntent.getBooleanExtra(QuestionActivity.EXTRA_FIELD_CANCEL, false)) {
            tvNotifyTitle.setText(R.string.otr_verification_progress_title);
            tvNotifyAction.setText(R.string.otr_verification_notify_button_cancel);
        } else {
            tvNotifyTitle.setText(R.string.otr_verification_notify_title);
            tvNotifyAction.setText(R.string.otr_verification_notify_button);
        }
        notifyLayout.setVisibility(View.VISIBLE);
    }

    private void inflateNotifyLayout() {
        View view = stubNotify.inflate();
        tvNotifyTitle = (TextView) view.findViewById(R.id.tvNotifyTitle);
        tvNotifyAction = (TextView) view.findViewById(R.id.tvNotifyAction);
        notifyLayout = (RelativeLayout) view.findViewById(R.id.notifyLayout);
        notifyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (notifyIntent != null) startActivity(notifyIntent);
                notifyLayout.setVisibility(View.GONE);
            }
        });
    }

    private void showScrollDownButtonIfNeed() {
        int pastVisibleItems = layoutManager.findLastVisibleItemPosition();
        boolean isBottom = pastVisibleItems >= chatMessageAdapter.getItemCount() - 1;

        if (isBottom) {
            btnScrollDown.setVisibility(View.GONE);
            hideUnreadMessageBackground();
        } else btnScrollDown.setVisibility(View.VISIBLE);
    }

    private void hideUnreadMessageCountIfNeed() {
        AbstractChat chat = getChat();
        if (chat == null) return;
        int pastVisibleItems = layoutManager.findLastVisibleItemPosition();
        if (pastVisibleItems >= chatMessageAdapter.getItemCount() - chat.getUnreadMessageCount()) {
            resetUnreadMessageCount();
        }
    }

    private void increaseUnreadMessageCountIfNeed() {
        AbstractChat chat = getChat();
        if (btnScrollDown.getVisibility() == View.VISIBLE && chat != null) {
            chat.increaseUnreadMessageCount();
            updateNewReceivedMessageCounter(chat.getUnreadMessageCount());
        }
    }

    private void resetUnreadMessageCount() {
        AbstractChat chat = getChat();
        if (chat != null) {
            chat.resetUnreadMessageCount();
            updateNewReceivedMessageCounter(0);
            ((ChatActivity)getActivity()).updateRecentChats();
        }
    }

    private void updateNewReceivedMessageCounter(int count) {
        tvNewReceivedCount.setText(String.valueOf(count));
        if (count > 0)
            tvNewReceivedCount.setVisibility(View.VISIBLE);
        else tvNewReceivedCount.setVisibility(View.GONE);
    }

    private void showUnreadMessage(int count) {
        chatMessageAdapter.setUnreadCount(count);
        chatMessageAdapter.notifyDataSetChanged();
    }

    private void hideUnreadMessageBackground() {
        if (chatMessageAdapter.setUnreadCount(0)) {
            realmRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    chatMessageAdapter.notifyDataSetChanged();
                }
            });
        }
    }
}
