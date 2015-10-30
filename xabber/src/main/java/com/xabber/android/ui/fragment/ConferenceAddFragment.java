package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomInvite;
import com.xabber.android.ui.activity.ChatViewer;

import org.jxmpp.util.XmppStringUtils;

public class ConferenceAddFragment extends Fragment {

    protected static final String ARG_ACCOUNT = "com.xabber.android.ui.fragment.ConferenceAddFragment.ARG_ACCOUNT";
    protected static final String ARG_CONFERENCE_JID = "com.xabber.android.ui.fragment.ConferenceAddFragment.ARG_CONFERENCE_NAME";

    private EditText nickView;
    private EditText passwordView;

    private String account = null;
    private String conferenceJid = null;

    public static ConferenceAddFragment newInstance(String account, String conference) {
        ConferenceAddFragment fragment = new ConferenceAddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT, account);
        args.putString(ARG_CONFERENCE_JID, conference);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            account = getArguments().getString(ARG_ACCOUNT);
            conferenceJid = getArguments().getString(ARG_CONFERENCE_JID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conference_add_fragment, container, false);

        ((TextView) view.findViewById(R.id.muc_conference_jid)).setText(conferenceJid);
        ((TextView) view.findViewById(R.id.muc_account_jid)).setText(XmppStringUtils.parseBareJid(account));

        Drawable accountAvatar = AvatarManager.getInstance().getAccountAvatar(account);
        int h = accountAvatar.getIntrinsicHeight();
        int w = accountAvatar.getIntrinsicWidth();
        accountAvatar.setBounds( 0, 0, w, h );
        ((TextView) view.findViewById(R.id.muc_account_jid)).setCompoundDrawables(accountAvatar, null, null, null);


        nickView = (EditText) view.findViewById(R.id.muc_nick);
        nickView.setText(MUCManager.getInstance().getNickname(account, conferenceJid));
        if ("".equals(nickView.getText().toString())) {
            nickView.setText(getNickname(account));
        }

        passwordView = (EditText) view.findViewById(R.id.muc_password);
        String password;
        RoomInvite roomInvite = MUCManager.getInstance().getInvite(account, conferenceJid);
        if (roomInvite != null) {
            password = roomInvite.getPassword();
        } else {
            password = MUCManager.getInstance().getPassword(account, conferenceJid);
        }
        passwordView.setText(password);

        MUCManager.getInstance().removeAuthorizationError(account, conferenceJid);

        setHasOptionsMenu(true);

        return view;
    }


    /**
     * @return Suggested nickname in the conferenceJid.
     */
    private String getNickname(String account) {
        if (account == null) {
            return "";
        }
        String nickname = AccountManager.getInstance().getNickName(account);
        String name = XmppStringUtils.parseLocalpart(nickname);
        if ("".equals(name)) {
            return nickname;
        } else {
            return name;
        }
    }

    public void addConference() {
        String nick = nickView.getText().toString();
        if ("".equals(nick)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_NICK_NAME), Toast.LENGTH_LONG).show();
            return;
        }
        String password = passwordView.getText().toString();
        final boolean join = true;
        MUCManager.getInstance().createRoom(account, conferenceJid, nick, password, join);
        startActivity(ChatViewer.createSpecificChatIntent(getActivity(), account, conferenceJid));
    }
}
