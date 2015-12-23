package com.xabber.android.ui.fragment;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
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
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.file.FileUtils;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.httpfileupload.HttpUploadListener;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomState;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.RegularChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ChatViewer;
import com.xabber.android.ui.activity.ConferenceAdd;
import com.xabber.android.ui.activity.ContactEditor;
import com.xabber.android.ui.activity.ContactList;
import com.xabber.android.ui.activity.ContactViewer;
import com.xabber.android.ui.activity.FingerprintViewer;
import com.xabber.android.ui.activity.OccupantList;
import com.xabber.android.ui.activity.QuestionViewer;
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.BlockContactDialog;
import com.xabber.android.ui.dialog.ChatExportDialogFragment;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.ui.preferences.ChatContactSettings;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import github.ankushsachdeva.emojicon.EmojiconGridView;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.emoji.Emojicon;

public class ChatViewerFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        View.OnClickListener, Toolbar.OnMenuItemClickListener,
        ChatMessageAdapter.Message.MessageClickListener, HttpUploadListener, ChatMessageAdapter.Listener {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";

    private static final int MINIMUM_MESSAGES_TO_LOAD = 10;
    public static final int FILE_SELECT_ACTIVITY_REQUEST_CODE = 23;
    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 24;
    private static final int PERMISSIONS_REQUEST_SAVE_TO_DOWNLOADS = 25;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 26;
    private static final int PERMISSIONS_REQUEST_EXPORT_CHAT = 27;
    boolean isInputEmpty = true;
    private EditText inputView;
    private ChatMessageAdapter chatMessageAdapter;
    private boolean skipOnTextChanges = false;
    private String account;
    private String user;
    private ImageButton sendButton;
    private ImageButton securityButton;
    private Toolbar toolbar;

    private ChatViewerFragmentListener listener;
    private Animation shakeAnimation = null;
    private RecyclerView recyclerView;
    private View contactTitleView;
    private AbstractContact abstractContact;
    private LinearLayoutManager layoutManager;
    private MessageItem clickedMessageItem;

    private Timer stopTypingTimer = new Timer();
    private final long STOP_TYPING_DELAY = 4000; // in ms
    private ImageButton attachButton;

    public static ChatViewerFragment newInstance(String account, String user) {
        ChatViewerFragment fragment = new ChatViewerFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (ChatViewerFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ChatViewerFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        account = args.getString(ARGUMENT_ACCOUNT, null);
        user = args.getString(ARGUMENT_USER, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.chat_viewer_fragment, container, false);


        contactTitleView = view.findViewById(R.id.contact_title);

        abstractContact = RosterManager.getInstance().getBestContact(account, user);
        contactTitleView.findViewById(R.id.avatar).setOnClickListener(this);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar_default);
        toolbar.inflateMenu(R.menu.chat);
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

        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);

        securityButton = (ImageButton) view.findViewById(R.id.button_security);

        if (abstractChat instanceof RegularChat) {
            securityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSecurityMenu();
                }
            });
        } else {
            securityButton.setVisibility(View.GONE);
        }

        chatMessageAdapter = new ChatMessageAdapter(getActivity(), account, user, this, this);

        recyclerView = (RecyclerView) view.findViewById(R.id.chat_messages_recycler_view);
        recyclerView.setAdapter(chatMessageAdapter);

        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        // to avoid strange bug on some 4.x androids
        view.findViewById(R.id.input_layout).setBackgroundColor(ColorManager.getInstance().getChatInputBackgroundColor());

        inputView = (EditText) view.findViewById(R.id.chat_input);

        view.findViewById(R.id.button_send_message).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        sendMessage();
                    }

                });

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

        });


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

        attachButton = (ImageButton) view.findViewById(R.id.button_attach);
        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAttachButtonPressed();
            }
        });

        return view;
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
            case PERMISSIONS_REQUEST_SAVE_TO_DOWNLOADS :
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    saveFileToDownloads();
                } else {
                    onNoWritePermissionError();
                }
                break;
            case PERMISSIONS_REQUEST_EXPORT_CHAT :
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    showExportChatDialog();
                } else {
                    onNoWritePermissionError();
                }
                break;
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE :
                if (!PermissionsRequester.isPermissionGranted(grantResults)) {
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

    @Override
    public void onResume() {
        super.onResume();
        listener.registerChat(this);
        updateChat();
        restoreInputState();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
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

    @Override
    public void onPause() {
        super.onPause();

        ChatStateManager.getInstance().onPaused(account, user);

        saveInputState();
        listener.unregisterChat(this);
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
            ChatViewer.hideKeyboard(getActivity());
        }
    }

    private void sendMessage(String text) {
        MessageManager.getInstance().sendMessage(account, user, text);
        updateChat();
    }

    /**
     * This method used for hardware menu button
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chat, menu);
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

    public void updateChat() {
        ContactTitleInflater.updateTitle(contactTitleView, getActivity(), abstractContact);
        toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
        int itemCountBeforeUpdate = chatMessageAdapter.getItemCount();
        chatMessageAdapter.onChange();
        scrollChat(itemCountBeforeUpdate);
        setUpOptionsMenu(toolbar.getMenu());
        updateSecurityButton();
    }

    private void scrollChat(int itemCountBeforeUpdate) {
        int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        if (lastVisibleItemPosition == -1 || lastVisibleItemPosition == (itemCountBeforeUpdate - 1)) {
            scrollDown();
        }
    }

    private void scrollDown() {
        recyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
    }

    private void updateSecurityButton() {
        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);
        securityButton.setImageLevel(securityLevel.getImageLevel());
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

    public String getAccount() {
        return account;
    }

    public String getUser() {
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
                startActivity(FingerprintViewer.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_verify_with_question:
                startActivity(QuestionViewer.createIntent(getActivity(), account, user, true, false, null));
                return true;

            case R.id.action_verify_with_shared_secret:
                startActivity(QuestionViewer.createIntent(getActivity(), account, user, false, false, null));
                return true;

            /* regular chat options menu */

            case R.id.action_view_contact:
                showContactInfo();
                return true;

            case R.id.action_chat_settings:
                startActivity(ChatContactSettings.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_show_history:
                showHistory(account, user);
                return true;

            case R.id.action_authorization_settings:
                startActivity(ConferenceAdd.createIntent(getActivity(), account, user));
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
                MUCManager.getInstance().joinRoom(account, user, true);
                return true;

            case R.id.action_invite_to_chat:
                startActivity(ContactList.createRoomInviteIntent(getActivity(), account, user));
                return true;

            case R.id.action_leave_conference:
                leaveConference(account, user);
                return true;

            case R.id.action_list_of_occupants:
                startActivity(OccupantList.createIntent(getActivity(), account, user));
                return true;

            /* message popup menu */

            case R.id.action_message_repeat:
                if (clickedMessageItem.isUploadFileMessage()) {
                    uploadFile(clickedMessageItem.getFile().getPath());
                } else {
                    sendMessage(clickedMessageItem.getText());
                }
                return true;

            case R.id.action_message_copy:
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText(clickedMessageItem.getSpannable(), clickedMessageItem.getSpannable()));
                return true;

            case R.id.action_message_quote:
                setInputText("> " + clickedMessageItem.getText() + "\n");
                return true;

            case R.id.action_message_remove:
                MessageManager.getInstance().removeMessage(clickedMessageItem);
                updateChat();
                return true;

            case R.id.action_message_open_file:
                FileManager.openFile(getActivity(), clickedMessageItem.getFile());
                return true;

            case R.id.action_message_save_file:
                OnSaveFileToDownloadsClick();
                return true;

            case R.id.action_message_open_muc_private_chat:
                String occupantFullJid = user + "/" + clickedMessageItem.getResource();
                MessageManager.getInstance().openChat(account, occupantFullJid);
                startActivity(ChatViewer.createSpecificChatIntent(getActivity(), account, occupantFullJid));
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

    private void OnSaveFileToDownloadsClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(this, PERMISSIONS_REQUEST_SAVE_TO_DOWNLOADS)) {
            saveFileToDownloads();
        }
    }

    private void saveFileToDownloads() {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    FileManager.saveFileToDownloads(clickedMessageItem.getFile());
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), R.string.file_saved_successfully, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), R.string.could_not_save_file, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void showHistory(String account, String user) {
        MessageManager.getInstance().requestToLoadLocalHistory(account, user);
        MessageArchiveManager.getInstance().requestHistory(account, user, MINIMUM_MESSAGES_TO_LOAD, 0);
    }

    private void stopEncryption(String account, String user) {
        try {
            OTRManager.getInstance().endSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void restartEncryption(String account, String user) {
        try {
            OTRManager.getInstance().refreshSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void startEncryption(String account, String user) {
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
            intent = ContactViewer.createIntent(getActivity(), account, user);
        } else {
            intent = ContactEditor.createIntent(getActivity(), account, user);
        }
        startActivity(intent);
    }

    private void closeChat(String account, String user) {
        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);
        listener.onCloseChat();
    }

    private void clearHistory(String account, String user) {
        MessageManager.getInstance().clearHistory(account, user);
    }

    private void leaveConference(String account, String user) {
        MUCManager.getInstance().leaveRoom(account, user);
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
            popup.inflate(R.menu.chat_context_menu);
            popup.setOnMenuItemClickListener(this);

            final Menu menu = popup.getMenu();

            if (clickedMessageItem.isError()) {
                menu.findItem(R.id.action_message_repeat).setVisible(true);
            }

            if (clickedMessageItem.isUploadFileMessage()) {
                menu.findItem(R.id.action_message_copy).setVisible(false);
                menu.findItem(R.id.action_message_quote).setVisible(false);
                menu.findItem(R.id.action_message_remove).setVisible(false);
            }

            final File file = clickedMessageItem.getFile();

            if (file != null && file.exists()) {
                menu.findItem(R.id.action_message_open_file).setVisible(true);
                menu.findItem(R.id.action_message_save_file).setVisible(true);
            }

            if (clickedMessageItem.isIncoming() && MUCManager.getInstance().hasRoom(account, user)) {
                menu.findItem(R.id.action_message_open_muc_private_chat).setVisible(true);
            }

            popup.show();
        }
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

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onNoDownloadFilePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    public interface ChatViewerFragmentListener {
        void onCloseChat();

        void onMessageSent();

        void registerChat(ChatViewerFragment chat);

        void unregisterChat(ChatViewerFragment chat);
    }
}
