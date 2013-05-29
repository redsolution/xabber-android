package com.xabber.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
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

import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.android.ui.adapter.OnTextChangedListener;
import com.xabber.android.ui.helper.AbstractAvatarInflaterHelper;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.widget.PageSwitcher;
import com.xabber.androiddev.R;

public class ChatViewerFragment {

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
