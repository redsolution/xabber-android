package com.xabber.android.ui;

import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.RegularChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.androiddev.R;

public class ChatViewerFragment extends Fragment implements AdapterView.OnItemClickListener, PopupMenu.OnMenuItemClickListener {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";

    private EditText inputView;
    private ListView listView;
    private ChatMessageAdapter chatMessageAdapter;

    private boolean skipOnTextChanges;

    private String account;
    private String user;

    boolean isInputEmpty = true;
    private ImageButton sendButton;
    private ImageButton securityButton;

    public static ChatViewerFragment newInstance(String account, String user) {
        ChatViewerFragment fragment = new ChatViewerFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
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

        View view = inflater.inflate(R.layout.chat_viewer_item, container, false);

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

        chatMessageAdapter = new ChatMessageAdapter(getActivity(), account, user);

        listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(chatMessageAdapter);
        listView.setOnItemClickListener(this);


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
        ((ChatViewer)getActivity()).registerChat(this);

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
        ((ChatViewer)getActivity()).unregisterChat(this);
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

        ((ChatViewer) getActivity()).onSent();

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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        ChatMessageAdapter chatMessageAdapter = (ChatMessageAdapter) listView.getAdapter();

        int itemViewType = chatMessageAdapter.getItemViewType(info.position);

        if (itemViewType == ChatMessageAdapter.VIEW_TYPE_INCOMING_MESSAGE
                || itemViewType == ChatMessageAdapter.VIEW_TYPE_OUTGOING_MESSAGE) {

            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.chat_context_menu, menu);

            if (((MessageItem) chatMessageAdapter.getItem(info.position)).isError()) {
                menu.findItem(R.id.action_message_repeat).setVisible(true);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final MessageItem message = (MessageItem) listView.getAdapter().getItem(info.position);

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

    public void clearInputText() {
        skipOnTextChanges = true;
        inputView.getText().clear();
        skipOnTextChanges = false;
    }

    public void scrollChat() {
        int size = listView.getCount();
        if (size > 0) {
            listView.setSelection(size - 1);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        registerForContextMenu(listView);
        listView.showContextMenuForChild(view);
        unregisterForContextMenu(listView);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
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

            default:
                return false;
        }
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
}
