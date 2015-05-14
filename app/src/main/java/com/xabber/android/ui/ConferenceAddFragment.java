package com.xabber.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomInvite;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.ui.adapter.AccountChooseAdapter;

import org.jivesoftware.smack.util.StringUtils;

public class ConferenceAddFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    protected static final String ARG_ACCOUNT = "com.xabber.android.ui.ConferenceAddFragment.ARG_ACCOUNT";
    protected static final String ARG_ROOM = "com.xabber.android.ui.ConferenceAddFragment.ARG_ROOM";

    private Spinner accountView;
    private EditText serverView;
    private EditText roomView;
    private EditText nickView;
    private EditText passwordView;
    private CheckBox joinCheckBox;

    private int selectedAccount;

    private String account = null;
    private String room = null;

    private Listener listener;

    public static ConferenceAddFragment newInstance(String account, String room) {
        ConferenceAddFragment fragment = new ConferenceAddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT, account);
        args.putString(ARG_ROOM, room);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        listener = (Listener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            account = getArguments().getString(ARG_ACCOUNT);
            room = getArguments().getString(ARG_ROOM);
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conference_add_fragment, container, false);

        accountView = (Spinner) view.findViewById(R.id.contact_account);
        serverView = (EditText) view.findViewById(R.id.muc_server);
        roomView = (EditText) view.findViewById(R.id.muc_room);
        nickView = (EditText) view.findViewById(R.id.muc_nick);
        passwordView = (EditText) view.findViewById(R.id.muc_password);
        joinCheckBox = (CheckBox) view.findViewById(R.id.muc_join);

        accountView.setAdapter(new AccountChooseAdapter(getActivity()));
        accountView.setOnItemSelectedListener(this);

        if (room != null) {
            serverView.setText(StringUtils.parseServer(room));
            roomView.setText(StringUtils.parseName(room));
        }

        if (account != null && room != null) {
            MUCManager.getInstance().removeAuthorizationError(account, room);
            nickView.setText(MUCManager.getInstance().getNickname(account, room));
            String password;
            RoomInvite roomInvite = MUCManager.getInstance().getInvite(account, room);
            if (roomInvite != null) {
                password = roomInvite.getPassword();
            } else {
                password = MUCManager.getInstance().getPassword(account, room);
            }
            passwordView.setText(password);
        }

        if (account != null) {
            for (int position = 0; position < accountView.getCount(); position++) {
                if (account.equals(accountView.getItemAtPosition(position))) {
                    accountView.setSelection(position);
                    break;
                }
            }
        }
        if ("".equals(nickView.getText().toString())) {
            nickView.setText(getNickname(((String) accountView.getSelectedItem())));
        }

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        selectedAccount = accountView.getSelectedItemPosition();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String current = nickView.getText().toString();
        String previous;
        if (selectedAccount == AdapterView.INVALID_POSITION) {
            previous = "";
        } else {
            previous = getNickname((String) accountView.getAdapter().getItem(selectedAccount));
        }
        if (current.equals(previous)) {
            nickView.setText(getNickname((String) accountView.getSelectedItem()));
        }
        selectedAccount = accountView.getSelectedItemPosition();

        listener.onAccountSelected((String) accountView.getSelectedItem());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        selectedAccount = accountView.getSelectedItemPosition();
    }

    /**
     * @param account
     * @return Suggested nickname in the room.
     */
    private String getNickname(String account) {
        if (account == null) {
            return "";
        }
        String nickname = AccountManager.getInstance().getNickName(account);
        String name = StringUtils.parseName(nickname);
        if ("".equals(name)) {
            return nickname;
        } else {
            return name;
        }
    }

    private void addConference() {
        String account = (String) accountView.getSelectedItem();
        if (account == null) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ACCOUNT), Toast.LENGTH_LONG).show();
            return;
        }
        String server = serverView.getText().toString();
        if ("".equals(server)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_SERVER_NAME), Toast.LENGTH_LONG).show();
            return;
        }
        String room = roomView.getText().toString();
        if ("".equals(room)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ROOM_NAME), Toast.LENGTH_LONG).show();
            return;
        }
        String nick = nickView.getText().toString();
        if ("".equals(nick)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_NICK_NAME), Toast.LENGTH_LONG).show();
            return;
        }
        String password = passwordView.getText().toString();
        boolean join = joinCheckBox.isChecked();
        room = room + "@" + server;
        if (this.account != null && this.room != null) {
            if (!account.equals(this.account) || !room.equals(this.room)) {
                MUCManager.getInstance().removeRoom(this.account, this.room);
                MessageManager.getInstance().closeChat(this.account, this.room);
                NotificationManager.getInstance().removeMessageNotification(this.account, this.room);
            }
        }
        MUCManager.getInstance().createRoom(account, room, nick, password, join);
        getActivity().finish();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.add_conference, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_conference:
                addConference();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    interface Listener {
        void onAccountSelected(String account);
    }
}
