package com.xabber.android.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsHideKeyboard;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
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
import com.xabber.android.ui.widget.PageSwitcher;
import com.xabber.androiddev.R;

public class ChatViewerFragment implements OnCreateContextMenuListener {

	/**
	 * Minimum number of new messages to be requested from the server side
	 * archive.
	 */
	private static final int MINIMUM_MESSAGES_TO_LOAD = 10;

	/**
	 * Delay before hide pages.
	 */
	private static final long PAGES_HIDDER_DELAY = 1000;

	private AbstractAvatarInflaterHelper avatarInflaterHelper;

	private boolean skipOnTextChanges;

	private TextView pageView;
	private View titleView;
	private EditText inputView;
	private ListView listView;
	private ChatMessageAdapter chatMessageAdapter;

	/**
	 * Whether pages are shown.
	 */
	private boolean pagesShown;

	/**
	 * Animation used to hide pages.
	 */
	private Animation pagesHideAnimation;

	/**
	 * Animation used for incoming message notification.
	 */
	private Animation shakeAnimation;

	private Handler handler;

	/**
	 * Runnable called to hide pages.
	 */
	private final Runnable pagesHideRunnable = new Runnable() {
		@Override
		public void run() {
			handler.removeCallbacks(this);
			pageView.startAnimation(pagesHideAnimation);
		}
	};

	private final FragmentActivity activity;

	private final View view;

	public ChatViewerFragment(FragmentActivity activity) {
		this.activity = activity;
		onCreate(null);
		view = onCreateView(activity.getLayoutInflater(), null, null);
	}

	private FragmentActivity getActivity() {
		return activity;
	}

	private String getString(int resId, Object... formatArgs) {
		return activity.getString(resId, formatArgs);
	}

	private void registerForContextMenu(View view) {
		view.setOnCreateContextMenuListener(this);
	}

	public View getView() {
		return view;
	}

	public void onCreate(Bundle savedInstanceState) {
		// super.onCreate(savedInstanceState);
		avatarInflaterHelper = AbstractAvatarInflaterHelper
				.createAbstractContactInflaterHelper();
		handler = new Handler();
		pagesShown = false;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
				R.anim.shake);
		pagesHideAnimation = AnimationUtils.loadAnimation(getActivity(),
				R.anim.chat_page_out);
		pagesHideAnimation.setAnimationListener(new AnimationListener() {

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
		View view = inflater.inflate(R.layout.chat_viewer_item, container,
				false);
		chatMessageAdapter = new ChatMessageAdapter(getActivity());
		pageView = (TextView) view.findViewById(R.id.chat_page);
		titleView = view.findViewById(R.id.title);
		inputView = (EditText) view.findViewById(R.id.chat_input);
		listView = (ListView) view.findViewById(android.R.id.list);

		listView.setAdapter(chatMessageAdapter);
		view.findViewById(R.id.chat_send).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						sendMessage();
					}

				});
		titleView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int size = listView.getCount();
				if (size > 0)
					listView.setSelection(size - 1);
			}

		});
		inputView.setOnKeyListener(new OnKeyListener() {

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
		inputView.setOnEditorActionListener(new OnEditorActionListener() {

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
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
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
		registerForContextMenu(listView);
		return view;
	}

    public boolean onPrepareOptionsMenu(Menu menu) {
        final String account = chatMessageAdapter.getAccount();
        final String user = chatMessageAdapter.getUser();
        AbstractChat abstractChat = MessageManager.getInstance().getChat(account, user);

        getActivity().getMenuInflater().inflate(R.menu.chat, menu);

        if (abstractChat != null && abstractChat instanceof RoomChat) {
            if (((RoomChat) abstractChat).getState() == RoomState.unavailable) {

                MenuItem item = menu.findItem(R.id.action_join_conference);
                item.setVisible(true);
                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MUCManager.getInstance().joinRoom(account, user, true);
                        return true;
                    }});
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
                        onChatChange(false);
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
                        onChatChange(false);
                        return false;
                    }
                });

        menu.findItem(R.id.action_export_chat).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ChatExportDialogFragment
                                .newInstance(account, user)
                                .show(getActivity().getSupportFragmentManager(),
                                        "CHAT_EXPORT");
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
                        .setEnabled(SettingsManager.securityOtrMode() != SecurityOtrMode.disabled)
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
        return true;
    }



	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		// super.onCreateContextMenu(menu, view, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		final MessageItem message = (MessageItem) listView.getAdapter()
				.getItem(info.position);
		if (message != null && message.getAction() != null)
			return;
		if (message.isError()) {
			menu.add(R.string.message_repeat).setOnMenuItemClickListener(
					new MenuItem.OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							sendMessage(message.getText());
							return true;
						}

					});
		}
		menu.add(R.string.message_quote).setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						insertText("> " + message.getText() + "\n");
						return true;
					}

				});
		menu.add(R.string.message_copy).setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						((ClipboardManager) getActivity().getSystemService(
								Context.CLIPBOARD_SERVICE)).setText(message
								.getSpannable());
						return true;
					}

				});
		menu.add(R.string.message_remove).setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						MessageManager.getInstance().removeMessage(message);
						onChatChange(false);
						return true;
					}

				});
	}

	public void setChat(AbstractChat chat) {
		final String account = chat.getAccount();
		final String user = chat.getUser();
		final AbstractContact abstractContact = RosterManager.getInstance()
				.getBestContact(account, user);
		if (chat.equals(chatMessageAdapter.getAccount(),
				chatMessageAdapter.getUser())) {
			chatMessageAdapter.updateInfo();
		} else {
			if (chatMessageAdapter.getAccount() != null
					&& chatMessageAdapter.getUser() != null)
				saveState();
			if (PageSwitcher.LOG)
				LogManager.i(this, "Load  " + chatMessageAdapter.getUser()
						+ " in " + chatMessageAdapter.getAccount());
			skipOnTextChanges = true;
			inputView.setText(ChatManager.getInstance().getTypedMessage(
					account, user));
			inputView.setSelection(
					ChatManager.getInstance().getSelectionStart(account, user),
					ChatManager.getInstance().getSelectionEnd(account, user));
			skipOnTextChanges = false;
			chatMessageAdapter.setChat(account, user);
			listView.setAdapter(listView.getAdapter());
		}

		pageView.setText(getString(
				R.string.chat_page,
				((ChatViewer) getActivity()).getChatPosition(account, user) + 1,
				((ChatViewer) getActivity()).getChatCount()));
		ContactTitleInflater.updateTitle(titleView, getActivity(),
				abstractContact);
		avatarInflaterHelper.updateAvatar(
				(ImageView) titleView.findViewById(R.id.avatar),
				abstractContact);
		SecurityLevel securityLevel = OTRManager.getInstance()
				.getSecurityLevel(chat.getAccount(), chat.getUser());
		SecurityOtrMode securityOtrMode = SettingsManager.securityOtrMode();
		ImageView securityView = (ImageView) titleView
				.findViewById(R.id.security);
		if (securityLevel == SecurityLevel.plain
				&& (securityOtrMode == SecurityOtrMode.disabled || securityOtrMode == SecurityOtrMode.manual)) {
			securityView.setVisibility(View.GONE);
		} else {
			securityView.setVisibility(View.VISIBLE);
			securityView.setImageLevel(securityLevel.getImageLevel());
		}
	}

	public void saveState() {
		if (PageSwitcher.LOG)
			LogManager.i(this, "Save " + chatMessageAdapter.getUser() + " in "
					+ chatMessageAdapter.getAccount());
		ChatManager.getInstance().setTyped(chatMessageAdapter.getAccount(),
				chatMessageAdapter.getUser(), inputView.getText().toString(),
				inputView.getSelectionStart(), inputView.getSelectionEnd());
	}

	public void onChatChange(boolean incomingMessage) {
		if (incomingMessage)
			titleView.findViewById(R.id.name_holder).startAnimation(
					shakeAnimation);
		chatMessageAdapter.onChange();
	}

	/**
	 * Show pages.
	 */
	public void showPages() {
		if (pagesShown)
			return;
		pagesShown = true;
		handler.removeCallbacks(pagesHideRunnable);
		pageView.clearAnimation();
		pageView.setVisibility(View.VISIBLE);
	}

	/**
	 * Requests pages to be hiden in future.
	 */
	public void hidePages() {
		if (!pagesShown)
			return;
		pagesShown = false;
		handler.postDelayed(pagesHideRunnable, PAGES_HIDDER_DELAY);
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

	private void sendMessage() {
		String text = inputView.getText().toString();
		int start = 0;
		int end = text.length();
		while (start < end
				&& (text.charAt(start) == ' ' || text.charAt(start) == '\n'))
			start += 1;
		while (start < end
				&& (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\n'))
			end -= 1;
		text = text.substring(start, end);
		if ("".equals(text))
			return;
		skipOnTextChanges = true;
		inputView.setText("");
		skipOnTextChanges = false;
		sendMessage(text);
		((ChatViewer) getActivity()).onSent();
		if (SettingsManager.chatsHideKeyboard() == ChatsHideKeyboard.always
				|| (getActivity().getResources().getBoolean(R.bool.landscape) && SettingsManager
						.chatsHideKeyboard() == ChatsHideKeyboard.landscape)) {
			InputMethodManager imm = (InputMethodManager) getActivity()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(inputView.getWindowToken(), 0);
		}
	}

	private void sendMessage(String text) {
		final String account = chatMessageAdapter.getAccount();
		final String user = chatMessageAdapter.getUser();
		MessageManager.getInstance().sendMessage(account, user, text);
		onChatChange(false);
	}

}
