package com.xabber.android.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
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

	private AbstractAvatarInflaterHelper avatarInflaterHelper;

	private Animation shake;

	private boolean skipOnTextChanges;

	private ChatViewHolder chatViewHolder;

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
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
		View view = inflater.inflate(R.layout.chat_viewer_item, container,
				false);
		ChatMessageAdapter chatMessageAdapter = new ChatMessageAdapter(
				getActivity());
		chatViewHolder = new ChatViewHolder(view, chatMessageAdapter);
		chatViewHolder.list.setAdapter(chatViewHolder.chatMessageAdapter);
		chatViewHolder.send.setOnClickListener((OnClickListener) getActivity());
		chatViewHolder.title
				.setOnClickListener((OnClickListener) getActivity());
		chatViewHolder.input.setOnKeyListener((OnKeyListener) getActivity());
		chatViewHolder.input
				.setOnEditorActionListener((OnEditorActionListener) getActivity());
		chatViewHolder.input.addTextChangedListener(new TextWatcher() {

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
						chatViewHolder.input, s);
			}

		});
		chatViewHolder.list.setOnCreateContextMenuListener(getActivity());
		return view;
	}

	public void setChat(AbstractChat chat) {
		final String account = chat.getAccount();
		final String user = chat.getUser();
		final AbstractContact abstractContact = RosterManager.getInstance()
				.getBestContact(account, user);
		if (chat.equals(chatViewHolder.chatMessageAdapter.getAccount(),
				chatViewHolder.chatMessageAdapter.getUser())) {
			chatViewHolder.chatMessageAdapter.updateInfo();
		} else {
			if (chatViewHolder.chatMessageAdapter.getAccount() != null
					&& chatViewHolder.chatMessageAdapter.getUser() != null)
				saveState();
			if (PageSwitcher.LOG)
				LogManager.i(
						this,
						"Load  "
								+ chatViewHolder.chatMessageAdapter.getUser()
								+ " in "
								+ chatViewHolder.chatMessageAdapter
										.getAccount());
			skipOnTextChanges = true;
			chatViewHolder.input.setText(ChatManager.getInstance()
					.getTypedMessage(account, user));
			chatViewHolder.input.setSelection(ChatManager.getInstance()
					.getSelectionStart(account, user), ChatManager
					.getInstance().getSelectionEnd(account, user));
			skipOnTextChanges = false;
			chatViewHolder.chatMessageAdapter.setChat(account, user);
			chatViewHolder.list.setAdapter(chatViewHolder.list.getAdapter());
		}

		chatViewHolder.page
				.setText(getString(R.string.chat_page,
						((ChatViewer) getActivity()).getChatPosition(account,
								user) + 1, ((ChatViewer) getActivity())
								.getChatCount()));
		ContactTitleInflater.updateTitle(chatViewHolder.title, getActivity(),
				abstractContact);
		avatarInflaterHelper.updateAvatar(chatViewHolder.avatar,
				abstractContact);
		SecurityLevel securityLevel = OTRManager.getInstance()
				.getSecurityLevel(chat.getAccount(), chat.getUser());
		SecurityOtrMode securityOtrMode = SettingsManager.securityOtrMode();
		if (securityLevel == SecurityLevel.plain
				&& (securityOtrMode == SecurityOtrMode.disabled || securityOtrMode == SecurityOtrMode.manual)) {
			chatViewHolder.security.setVisibility(View.GONE);
		} else {
			chatViewHolder.security.setVisibility(View.VISIBLE);
			chatViewHolder.security
					.setImageLevel(securityLevel.getImageLevel());
		}
	}

	public void saveState() {
		if (PageSwitcher.LOG)
			LogManager.i(
					this,
					"Save " + chatViewHolder.chatMessageAdapter.getUser()
							+ " in "
							+ chatViewHolder.chatMessageAdapter.getAccount());
		ChatManager.getInstance().setTyped(
				chatViewHolder.chatMessageAdapter.getAccount(),
				chatViewHolder.chatMessageAdapter.getUser(),
				chatViewHolder.input.getText().toString(),
				chatViewHolder.input.getSelectionStart(),
				chatViewHolder.input.getSelectionEnd());
	}

	public void onChatChange(boolean incomingMessage) {
		if (incomingMessage)
			chatViewHolder.nameHolder.startAnimation(shake);
		chatViewHolder.chatMessageAdapter.onChange();
	}

	private static class ChatViewHolder {

		final TextView page;
		final View title;
		final View nameHolder;
		final ImageView avatar;
		final ImageView security;
		final View send;
		final EditText input;
		final ListView list;
		final ChatMessageAdapter chatMessageAdapter;

		public ChatViewHolder(View view, ChatMessageAdapter chatMessageAdapter) {
			page = (TextView) view.findViewById(R.id.chat_page);
			title = view.findViewById(R.id.title);
			nameHolder = title.findViewById(R.id.name_holder);
			avatar = (ImageView) title.findViewById(R.id.avatar);
			security = (ImageView) title.findViewById(R.id.security);
			send = view.findViewById(R.id.chat_send);
			input = (EditText) view.findViewById(R.id.chat_input);
			list = (ListView) view.findViewById(android.R.id.list);
			this.chatMessageAdapter = chatMessageAdapter;
		}

	}

}
