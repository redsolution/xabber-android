package com.xabber.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomState;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.RegularChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.android.ui.adapter.OnTextChangedListener;
import com.xabber.android.ui.dialog.ChatExportDialogFragment;
import com.xabber.android.ui.helper.AbstractAvatarInflaterHelper;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.widget.PageSwitcher;
import com.xabber.androiddev.R;

public class ChatViewerFragment {

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
				(OnClickListener) getActivity());
		titleView.setOnClickListener((OnClickListener) getActivity());
		inputView.setOnKeyListener((OnKeyListener) getActivity());
		inputView
				.setOnEditorActionListener((OnEditorActionListener) getActivity());
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
			public void afterTextChanged(Editable s) {
				if (skipOnTextChanges)
					return;
				((OnTextChangedListener) getActivity()).onTextChanged(
						inputView, s);
			}

		});
		listView.setOnCreateContextMenuListener(getActivity());
		return view;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		final String account = chatMessageAdapter.getAccount();
		final String user = chatMessageAdapter.getUser();
		AbstractChat abstractChat = MessageManager.getInstance().getChat(
				account, user);
		if (abstractChat != null && abstractChat instanceof RoomChat) {
			if (((RoomChat) abstractChat).getState() == RoomState.unavailable)
				menu.add(R.string.muc_join)
						.setIcon(android.R.drawable.ic_menu_add)
						.setOnMenuItemClickListener(
								new MenuItem.OnMenuItemClickListener() {

									@Override
									public boolean onMenuItemClick(MenuItem item) {
										MUCManager.getInstance().joinRoom(
												account, user, true);
										return true;
									}

								});
			else
				menu.add(R.string.muc_invite)
						.setIcon(android.R.drawable.ic_menu_add)
						.setIntent(
								ContactList.createRoomInviteIntent(
										getActivity(), account, user));
		} else {
			menu.add(R.string.contact_editor)
					.setIcon(android.R.drawable.ic_menu_edit)
					.setIntent(
							ContactEditor.createIntent(getActivity(), account,
									user));
		}
		menu.add(R.string.chat_list).setIcon(R.drawable.ic_menu_friendslist)
				.setIntent(ChatList.createIntent(getActivity()));
		menu.add(R.string.chat_settings)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setIntent(
						ChatEditor.createIntent(getActivity(), account, user));
		menu.add(R.string.show_history)
				.setIcon(R.drawable.ic_menu_archive)
				.setOnMenuItemClickListener(
						new MenuItem.OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item) {
								MessageManager.getInstance()
										.requestToLoadLocalHistory(account,
												user);
								MessageArchiveManager.getInstance()
										.requestHistory(account, user,
												MINIMUM_MESSAGES_TO_LOAD, 0);
								onChatChange(false);
								return true;
							}

						});
		if (abstractChat != null
				&& abstractChat instanceof RoomChat
				&& ((RoomChat) abstractChat).getState() != RoomState.unavailable) {
			if (((RoomChat) abstractChat).getState() == RoomState.error)
				menu.add(R.string.muc_edit)
						.setIcon(android.R.drawable.ic_menu_edit)
						.setIntent(
								MUCEditor.createIntent(getActivity(), account,
										user));
			else
				menu.add(R.string.muc_leave)
						.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
						.setOnMenuItemClickListener(
								new MenuItem.OnMenuItemClickListener() {

									@Override
									public boolean onMenuItemClick(MenuItem item) {
										MUCManager.getInstance().leaveRoom(
												account, user);
										MessageManager.getInstance().closeChat(
												account, user);
										NotificationManager.getInstance()
												.removeMessageNotification(
														account, user);
										((ChatViewer) getActivity()).close();
										return true;
									}

								});
		} else {
			menu.add(R.string.close_chat)
					.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
					.setOnMenuItemClickListener(
							new MenuItem.OnMenuItemClickListener() {

								@Override
								public boolean onMenuItemClick(MenuItem item) {
									MessageManager.getInstance().closeChat(
											account, user);
									NotificationManager.getInstance()
											.removeMessageNotification(account,
													user);
									((ChatViewer) getActivity()).close();
									return true;
								}

							});
		}
		menu.add(R.string.clear_message)
				.setIcon(R.drawable.ic_menu_stop)
				.setOnMenuItemClickListener(
						new MenuItem.OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item) {
								inputView.setText("");
								return true;
							}

						});
		menu.add(R.string.clear_history).setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						MessageManager.getInstance()
								.clearHistory(account, user);
						onChatChange(false);
						return false;
					}

				});
		menu.add(R.string.export_chat).setOnMenuItemClickListener(
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
			menu.add(R.string.call_attention).setOnMenuItemClickListener(
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
			SubMenu otrMenu = menu.addSubMenu(R.string.otr_encryption);
			otrMenu.setHeaderTitle(R.string.otr_encryption);
			if (securityLevel == SecurityLevel.plain)
				otrMenu.add(R.string.otr_start)
						.setEnabled(
								SettingsManager.securityOtrMode() != SecurityOtrMode.disabled)
						.setOnMenuItemClickListener(
								new MenuItem.OnMenuItemClickListener() {

									@Override
									public boolean onMenuItemClick(MenuItem item) {
										try {
											OTRManager
													.getInstance()
													.startSession(account, user);
										} catch (NetworkException e) {
											Application.getInstance()
													.onError(e);
										}
										return true;
									}

								});
			else
				otrMenu.add(R.string.otr_refresh).setOnMenuItemClickListener(
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
			otrMenu.add(R.string.otr_end)
					.setEnabled(securityLevel != SecurityLevel.plain)
					.setOnMenuItemClickListener(
							new MenuItem.OnMenuItemClickListener() {

								@Override
								public boolean onMenuItemClick(MenuItem item) {
									try {
										OTRManager.getInstance().endSession(
												account, user);
									} catch (NetworkException e) {
										Application.getInstance().onError(e);
									}
									return true;
								}

							});
			otrMenu.add(R.string.otr_verify_fingerprint)
					.setEnabled(securityLevel != SecurityLevel.plain)
					.setIntent(
							FingerprintViewer.createIntent(getActivity(),
									account, user));
			otrMenu.add(R.string.otr_verify_question)
					.setEnabled(securityLevel != SecurityLevel.plain)
					.setIntent(
							QuestionViewer.createIntent(getActivity(), account,
									user, true, false, null));
			otrMenu.add(R.string.otr_verify_secret)
					.setEnabled(securityLevel != SecurityLevel.plain)
					.setIntent(
							QuestionViewer.createIntent(getActivity(), account,
									user, false, false, null));
		}
		if (abstractChat != null && abstractChat instanceof RoomChat
				&& ((RoomChat) abstractChat).getState() == RoomState.available)
			menu.add(R.string.occupant_list).setIntent(
					OccupantList.createIntent(getActivity(), account, user));
		return true;
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

}
