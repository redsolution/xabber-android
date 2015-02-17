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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.androiddev.R;

public class ChatViewerFragment extends Fragment {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";

    private TextView pageView;
    private EditText inputView;
    private ListView listView;
    private ChatMessageAdapter chatMessageAdapter;

    private boolean skipOnTextChanges;

    private String account;
    private String user;

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

        /*
        Animation used to hide pages.
        */
        Animation pagesHideAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.chat_page_out);
        pagesHideAnimation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                pageView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

        });

        View view = inflater.inflate(R.layout.chat_viewer_item, container, false);

        chatMessageAdapter = new ChatMessageAdapter(getActivity());
        chatMessageAdapter.setChat(account, user);

        listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(chatMessageAdapter);

        pageView = (TextView) view.findViewById(R.id.chat_page);
        inputView = (EditText) view.findViewById(R.id.chat_input);

        view.findViewById(R.id.chat_send).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        sendMessage();
                    }

                });

        inputView.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER
                        && SettingsManager.chatsSendByEnter()) {
                    sendMessage();
                    return true;
                }
                return false;
            }

        });
        inputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView view, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                }
                return false;
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
                if (skipOnTextChanges)
                    return;
                String account = chatMessageAdapter.getAccount();
                String user = chatMessageAdapter.getUser();
                ChatStateManager.getInstance().onComposing(account, user, text);
            }

        });

        updateView();

        chatMessageAdapter.onChange();

        return view;

    }

    @Override
    public void onResume() {
        super.onResume();

        ((ChatViewer)getActivity()).registerChat(this);

        registerForContextMenu(listView);

        restoreInputState();
    }

    private void restoreInputState() {
        skipOnTextChanges = true;

        inputView.setText(ChatManager.getInstance().getTypedMessage(account, user));
        inputView.setSelection(ChatManager.getInstance().getSelectionStart(account, user),
                ChatManager.getInstance().getSelectionEnd(account, user));

        skipOnTextChanges = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        saveInputState();

        ((ChatViewer)getActivity()).unregisterChat(this);

        unregisterForContextMenu(listView);
    }

    public void saveInputState() {
        ChatManager.getInstance().setTyped(account, user, inputView.getText().toString(),
                inputView.getSelectionStart(), inputView.getSelectionEnd());
    }

    private void sendMessage() {
        String text = inputView.getText().toString();
        int start = 0;
        int end = text.length();

        while (start < end && (text.charAt(start) == ' ' || text.charAt(start) == '\n')) {
            start += 1;
        }
        while (start < end && (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\n')) {
            end -= 1;
        }
        text = text.substring(start, end);

        if ("".equals(text)) {
            return;
        }

        skipOnTextChanges = true;
        inputView.getText().clear();
        skipOnTextChanges = false;

        sendMessage(text);

        ((ChatViewer) getActivity()).onSent();

        if (SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.always
                || (getActivity().getResources().getBoolean(R.bool.landscape)
                && SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.landscape)) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(inputView.getWindowToken(), 0);
        }
    }

    private void sendMessage(String text) {
        MessageManager.getInstance().sendMessage(account, user, text);
        updateChat();
    }

    private void updateView() {
        chatMessageAdapter.onChange();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        final MessageItem message = (MessageItem) listView.getAdapter().getItem(info.position);

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.chat_context_menu, menu);

        if (message.isError()) {
            menu.findItem(R.id.action_message_repeat).setVisible(true);
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
                insertText("> " + message.getText() + "\n");
                return true;

            case R.id.action_message_remove:
                MessageManager.getInstance().removeMessage(message);
                updateChat();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Insert additional text to the input.
     *
     * @param additional
     */
    private void insertText(String additional) {
        String source = inputView.getText().toString();
        int selection = inputView.getSelectionEnd();
        if (selection == -1)
            selection = source.length();
        else if (selection > source.length())
            selection = source.length();
        String before = source.substring(0, selection);
        String after = source.substring(selection);
        if (before.length() > 0 && !before.endsWith("\n"))
            additional = "\n" + additional;
        inputView.setText(before + additional + after);
        inputView.setSelection(selection + additional.length());
    }

    public void updateChat() {
        updateView();
    }

    public boolean isEqual(String account, String user) {
        return this.account.equals(account) && this.user.equals(user);
    }

    public void setInputText(String text) {
        insertText(text);
    }

    public String getAccount() {
        return account;
    }

    public String getUser() {
        return user;
    }

    public void clearInputView() {
        inputView.getText().clear();
    }

    public void scrollChat() {
        int size = listView.getCount();
        if (size > 0) {
            listView.setSelection(size - 1);
        }
    }
}
