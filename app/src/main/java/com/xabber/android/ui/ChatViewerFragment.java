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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
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
import com.xabber.android.ui.helper.AbstractAvatarInflaterHelper;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.preferences.ChatEditor;
import com.xabber.androiddev.R;

public class ChatViewerFragment extends Fragment implements ChatViewer.CurrentUpdatableChat {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";

    private static final int MINIMUM_MESSAGES_TO_LOAD = 10;

    private TextView pageView;
    private View titleView;
    private EditText inputView;
    private ListView listView;
    private ChatMessageAdapter chatMessageAdapter;

    private boolean skipOnTextChanges;

    /**
     * Animation used for incoming message notification.
     */
    private Animation shakeAnimation;

    private AbstractAvatarInflaterHelper avatarInflaterHelper;

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

        avatarInflaterHelper = AbstractAvatarInflaterHelper.createAbstractContactInflaterHelper();

        Bundle args = getArguments();
        account = args.getString(ARGUMENT_ACCOUNT, null);
        user = args.getString(ARGUMENT_USER, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);

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
        titleView = view.findViewById(R.id.title);
        inputView = (EditText) view.findViewById(R.id.chat_input);

        view.findViewById(R.id.chat_send).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        sendMessage();
                    }

                });
        titleView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int size = listView.getCount();
                if (size > 0)
                    listView.setSelection(size - 1);
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

        setHasOptionsMenu(true);

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
        inputView.setText("");
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
        updateChat(false);
    }


    private void updateMessages() {
        chatMessageAdapter.onChange();
    }

    private void updateView() {
        final AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);

        ContactTitleInflater.updateTitle(titleView, getActivity(), abstractContact);

        avatarInflaterHelper.updateAvatar((ImageView) titleView.findViewById(R.id.avatar), abstractContact);

        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);
        SettingsManager.SecurityOtrMode securityOtrMode = SettingsManager.securityOtrMode();
        ImageView securityView = (ImageView) titleView.findViewById(R.id.security);

        if (securityLevel == SecurityLevel.plain
                && (securityOtrMode == SettingsManager.SecurityOtrMode.disabled
                || securityOtrMode == SettingsManager.SecurityOtrMode.manual)) {
            securityView.setVisibility(View.GONE);
        } else {
            securityView.setVisibility(View.VISIBLE);
            securityView.setImageLevel(securityLevel.getImageLevel());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        final String account = chatMessageAdapter.getAccount();
        final String user = chatMessageAdapter.getUser();
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);

        inflater.inflate(R.menu.chat, menu);

        if (abstractChat != null && abstractChat instanceof RoomChat) {
            if (((RoomChat) abstractChat).getState() == RoomState.unavailable) {

                MenuItem item = menu.findItem(R.id.action_join_conference);
                item.setVisible(true);
                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MUCManager.getInstance().joinRoom(account, user, true);
                        return true;
                    }
                });
            } else {
                menu.findItem(R.id.action_invite_to_chat).setVisible(true)
                        .setIntent(ContactList.createRoomInviteIntent(getActivity(), account, user));
            }
        } else {
            menu.findItem(R.id.action_edit_contact).setVisible(true)
                    .setIntent(ContactEditor.createIntent(getActivity(), account, user));
        }
        menu.findItem(R.id.action_chat_list).setIntent(ChatList.createIntent(getActivity()));

        menu.findItem(R.id.action_chat_settings)
                .setIntent(ChatEditor.createIntent(getActivity(), account, user));

        menu.findItem(R.id.action_show_history).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MessageManager.getInstance().requestToLoadLocalHistory(account, user);
                        MessageArchiveManager.getInstance()
                                .requestHistory(account, user, MINIMUM_MESSAGES_TO_LOAD, 0);
                        updateChat(false);
                        return true;
                    }
                });

        if (abstractChat != null && abstractChat instanceof RoomChat
                && ((RoomChat) abstractChat).getState() != RoomState.unavailable) {
            if (((RoomChat) abstractChat).getState() == RoomState.error) {
                menu.findItem(R.id.action_authorization_settings).setVisible(true)
                        .setIntent(MUCEditor.createIntent(getActivity(), account, user));
            } else {
                menu.findItem(R.id.action_leave_conference).setVisible(true).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                MUCManager.getInstance().leaveRoom(account, user);
                                MessageManager.getInstance().closeChat(account, user);
                                NotificationManager.getInstance()
                                        .removeMessageNotification(account, user);
                                ((ChatViewer) getActivity()).close();
                                return true;
                            }
                        });
            }
        } else {
            menu.findItem(R.id.action_close_chat).setVisible(true)
                    .setOnMenuItemClickListener(
                            new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    MessageManager.getInstance().closeChat(account, user);
                                    NotificationManager.getInstance()
                                            .removeMessageNotification(account, user);
                                    ((ChatViewer) getActivity()).close();
                                    return true;
                                }
                            });
        }

        menu.findItem(R.id.action_clear_text).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        inputView.setText("");
                        return true;
                    }
                });

        menu.findItem(R.id.action_clear_history).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MessageManager.getInstance()
                                .clearHistory(account, user);
                        updateChat(false);
                        return false;
                    }
                });

        menu.findItem(R.id.action_export_chat).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ChatExportDialogFragment.newInstance(account, user).show(getActivity().getFragmentManager(), "CHAT_EXPORT");
                        return true;
                    }

                });
        if (abstractChat != null && abstractChat instanceof RegularChat) {
            menu.findItem(R.id.action_call_attention).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                AttentionManager.getInstance().sendAttention(
                                        account, user);
                            } catch (NetworkException e) {
                                Application.getInstance().onError(e);
                            }
                            return true;
                        }
                    });
            SecurityLevel securityLevel = OTRManager.getInstance()
                    .getSecurityLevel(abstractChat.getAccount(),
                            abstractChat.getUser());
            if (securityLevel == SecurityLevel.plain) {
                menu.findItem(R.id.action_start_encryption).setVisible(true)
                        .setEnabled(SettingsManager.securityOtrMode() != SettingsManager.SecurityOtrMode.disabled)
                        .setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        try {
                                            OTRManager.getInstance().startSession(account, user);
                                        } catch (NetworkException e) {
                                            Application.getInstance().onError(e);
                                        }
                                        return true;
                                    }
                                });
            } else {
                menu.findItem(R.id.action_restart_encryption).setVisible(true)
                        .setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        try {
                                            OTRManager.getInstance().refreshSession(
                                                    account, user);
                                        } catch (NetworkException e) {
                                            Application.getInstance().onError(e);
                                        }
                                        return true;
                                    }

                                });
            }
            menu.findItem(R.id.action_stop_encryption)
                    .setEnabled(securityLevel != SecurityLevel.plain)
                    .setOnMenuItemClickListener(
                            new MenuItem.OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    try {
                                        OTRManager.getInstance().endSession(account, user);
                                    } catch (NetworkException e) {
                                        Application.getInstance().onError(e);
                                    }
                                    return true;
                                }

                            });

            menu.findItem(R.id.action_verify_with_fingerprint)
                    .setEnabled(securityLevel != SecurityLevel.plain)
                    .setIntent(FingerprintViewer.createIntent(getActivity(), account, user));

            menu.findItem(R.id.action_verify_with_question)
                    .setEnabled(securityLevel != SecurityLevel.plain)
                    .setIntent(QuestionViewer
                            .createIntent(getActivity(), account, user, true, false, null));

            menu.findItem(R.id.action_verify_with_shared_secret)
                    .setEnabled(securityLevel != SecurityLevel.plain)
                    .setIntent(QuestionViewer
                            .createIntent(getActivity(), account, user, false, false, null));
        }
        if (abstractChat != null && abstractChat instanceof RoomChat
                && ((RoomChat) abstractChat).getState() == RoomState.available)
            menu.findItem(R.id.action_list_of_occupants).setVisible(true).setIntent(
                    OccupantList.createIntent(getActivity(), account, user));
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
                updateChat(false);
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
    public void insertText(String additional) {
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

    @Override
    public void updateChat(boolean incomingMessage) {
        if (incomingMessage) {
            titleView.findViewById(R.id.name_holder).startAnimation(shakeAnimation);
        }

        updateMessages();
        updateView();
    }

    @Override
    public boolean isEqual(String account, String user) {
        return this.account.equals(account) && this.user.equals(user);
    }
}
