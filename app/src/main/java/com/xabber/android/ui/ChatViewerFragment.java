package com.xabber.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
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
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.android.ui.dialog.ChatExportDialogFragment;
import com.xabber.android.ui.helper.ActionBarPainter;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.preferences.ChatEditor;
import com.xabber.androiddev.R;

public class ChatViewerFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        View.OnClickListener, Toolbar.OnMenuItemClickListener, ChatMessageAdapter.Message.MessageClickListener {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";

    private static final int MINIMUM_MESSAGES_TO_LOAD = 10;

    private EditText inputView;

    private ChatMessageAdapter chatMessageAdapter;

    private boolean skipOnTextChanges;

    private String account;
    private String user;

    boolean isInputEmpty = true;
    private ImageButton sendButton;
    private ImageButton securityButton;
    private Toolbar toolbar;

    private ChatViewerFragmentListener listener;
    private Animation shakeAnimation = null;
    private int contextMenuItemPosition;
    private RecyclerView recyclerView;

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

        View view = inflater.inflate(R.layout.chat_fragment, container, false);


        View contactTitle = view.findViewById(R.id.contact_title);

        final AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);
        ContactTitleInflater.updateTitle(contactTitle, getActivity(), abstractContact);
        contactTitle.findViewById(R.id.avatar).setOnClickListener(this);

        ActionBarPainter actionBarPainter = new ActionBarPainter((ActionBarActivity) getActivity());

        toolbar = (Toolbar) view.findViewById(R.id.toolbar_default);
        toolbar.setBackgroundColor(actionBarPainter.getAccountColor(account));
        toolbar.inflateMenu(R.menu.chat);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(getActivity());
            }
        });

        sendButton = (ImageButton) view.findViewById(R.id.button_send_message);
        sendButton.setImageResource(R.drawable.ic_button_send_inactive_24dp);

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

        chatMessageAdapter = new ChatMessageAdapter(getActivity(), account, user, this);

        recyclerView = (RecyclerView) view.findViewById(R.id.chat_messages_recycler_view);
        recyclerView.setAdapter(chatMessageAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

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

        inputView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    ChatStateManager.getInstance().onPaused(account, user);
                }
            }
        });

        inputView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                LogManager.i(this, "afterTextChanged");

                setSendButtonColor();

                if (!skipOnTextChanges) {
                    ChatStateManager.getInstance().onComposing(account, user, text);
                }
             }

        });

        updateChat();
        return view;
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

    private void setSendButtonColor() {
        boolean empty = inputView.getText().toString().isEmpty();

        if (empty != isInputEmpty) {
            isInputEmpty = empty;

            if (isInputEmpty) {
                sendButton.setImageResource(R.drawable.ic_button_send_inactive_24dp);
            } else {
                sendButton.setImageResource(R.drawable.ic_button_send);
                sendButton.setImageLevel(AccountManager.getInstance().getColorLevel(account));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        listener.registerChat(this);

        updateChat();

        restoreInputState();

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
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        int itemViewType = chatMessageAdapter.getItemViewType(contextMenuItemPosition);

        if (itemViewType == ChatMessageAdapter.VIEW_TYPE_INCOMING_MESSAGE
                || itemViewType == ChatMessageAdapter.VIEW_TYPE_OUTGOING_MESSAGE) {

            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.chat_context_menu, menu);

            if (chatMessageAdapter.getMessageItem(contextMenuItemPosition).isError()) {
                menu.findItem(R.id.action_message_repeat).setVisible(true);

            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final MessageItem message = chatMessageAdapter.getMessageItem(contextMenuItemPosition);

        switch (item.getItemId()) {
            case R.id.action_message_repeat:
                sendMessage(message.getText());
                return true;

            case R.id.action_message_copy:
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText(message.getSpannable(), message.getSpannable()));
                return true;

            case R.id.action_message_quote:
                setInputText("> " + message.getText() + "\n");
                return true;

            case R.id.action_message_remove:
                MessageManager.getInstance().removeMessage(message);
                updateChat();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    public void updateChat() {
        chatMessageAdapter.onChange();
        recyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);

        setUpOptionsMenu(toolbar.getMenu());
        updateSecurityButton();
    }

    private void updateSecurityButton() {
        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);
        securityButton.setImageLevel(securityLevel.getImageLevel());
    }

    public boolean isEqual(String account, String user) {
        return this.account.equals(account) && this.user.equals(user);
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
                startActivity(ChatEditor.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_show_history:
                showHistory(account, user);
                return true;

            case R.id.action_authorization_settings:
                startActivity(MUCEditor.createIntent(getActivity(), account, user));
                return true;

            case R.id.action_close_chat:
                closeChat(account, user);
                return true;

            case R.id.action_clear_history:
                clearHistory(account, user);
                return true;

            case R.id.action_export_chat:
                ChatExportDialogFragment.newInstance(account, user).show(getFragmentManager(), "CHAT_EXPORT");
                return true;

            case R.id.action_call_attention:
                callAttention();
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


            default:
                return false;
        }
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
        if (MUCManager.getInstance().hasRoom(account, user)) {
            startActivity(ContactViewer.createIntent(getActivity(), account, user));
        } else {
            startActivity(ContactEditor.createIntent(getActivity(), account, user));
        }
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
        registerForContextMenu(caller);
        this.contextMenuItemPosition = position;
        caller.setOnCreateContextMenuListener(this);
        caller.showContextMenu();
        unregisterForContextMenu(caller);
    }

    public interface ChatViewerFragmentListener {
        void onCloseChat();
        void onMessageSent();
        void registerChat(ChatViewerFragment chat);
        void unregisterChat(ChatViewerFragment chat);
    }

    public void playIncomingAnimation() {
        if (shakeAnimation == null) {
            shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
        }
        toolbar.findViewById(R.id.name_holder).startAnimation(shakeAnimation);
    }
}
