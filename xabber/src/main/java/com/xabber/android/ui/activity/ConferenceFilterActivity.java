package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.adapter.HostedConferencesAdapter;

import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.List;

public class ConferenceFilterActivity extends ManagedActivity implements TextWatcher, View.OnClickListener,
        AdapterView.OnItemClickListener, TextView.OnEditorActionListener {

    private static final String LOG_TAG = ConferenceFilterActivity.class.getSimpleName();

    public static final String ARG_CONFERENCE_NAME = "com.xabber.android.ui.activity.ConferenceFilterActivity.ARG_CONFERENCE_NAME";
    public static final String ARG_CONFERENCE_LIST_NAMES = "com.xabber.android.ui.activity.ConferenceFilterActivity.ARG_CONFERENCE_LIST_NAMES";
    public static final String ARG_CONFERENCE_LIST_JIDS = "com.xabber.android.ui.activity.ConferenceFilterActivity.ARG_CONFERENCE_LIST_JIDS";
    public static final String ARG_CONFERENCE_LIST = "com.xabber.android.ui.activity.ConferenceFilterActivity.ARG_CONFERENCE_LIST";


    public static final int REQUEST_CODE_FILTER_ROOMS = 1;

    private EditText conferenceNameEditText;
    private ImageButton roomClearButton;

    private AccountJid account;
    private HostedConferencesAdapter hostedConferencesAdapter;


    public static Intent createIntent(Context context, AccountJid account) {
        return new EntityIntentBuilder(context, ConferenceFilterActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            returnResult();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void returnResult() {
        Intent data = new Intent();
        data.putExtra(ARG_CONFERENCE_NAME, conferenceNameEditText.getText().toString());

        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conferences_filter);


        roomClearButton = (ImageButton)findViewById(R.id.room_clear_button);
        roomClearButton.setOnClickListener(this);
        conferenceNameEditText = (EditText)findViewById(R.id.room_name_edit_text);

        setRoomClearButtonVisibility();

        Intent intent = getIntent();


        account = getAccount(intent);

        hostedConferencesAdapter = new HostedConferencesAdapter(this);

        ListView listView = (ListView) findViewById(R.id.hosted_rooms_list_view);
        listView.setAdapter(hostedConferencesAdapter);
        listView.setOnItemClickListener(this);

        Bundle bundleExtra = intent.getBundleExtra(ARG_CONFERENCE_LIST);

        hostedConferencesAdapter.addAll(restoreConferenceList(bundleExtra));

        String room = intent.getStringExtra(ARG_CONFERENCE_NAME);
        if (room != null) {
            conferenceNameEditText.setText(room);
            conferenceNameEditText.setSelection(room.length());
        }

        conferenceNameEditText.addTextChangedListener(this);
        conferenceNameEditText.setOnEditorActionListener(this);
    }

    public static List<HostedRoom> restoreConferenceList(Bundle bundleExtra) {
        List<String> conferencesNames = bundleExtra.getStringArrayList(ARG_CONFERENCE_LIST_NAMES);
        List<String> conferencesJids = bundleExtra.getStringArrayList(ARG_CONFERENCE_LIST_JIDS);

        List<HostedRoom> conferences = new ArrayList<>();

        if (conferencesNames != null && conferencesJids != null && conferencesNames.size() == conferencesJids.size()) {
            for (int i = 0; i < conferencesNames.size(); i++) {
                try {
                    DiscoverItems.Item item = new DiscoverItems.Item(JidCreate.from(conferencesJids.get(i)));
                    item.setName(conferencesNames.get(i));
                    conferences.add(new HostedRoom(item));
                } catch (XmppStringprepException e) {
                    LogManager.exception(LOG_TAG, e);
                }
            }
        }
        return conferences;
    }

    private void setRoomClearButtonVisibility() {
        if (conferenceNameEditText.getText().toString().trim().isEmpty()) {
            roomClearButton.setVisibility(View.GONE);
        } else {
            roomClearButton.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        setRoomClearButtonVisibility();
        hostedConferencesAdapter.getFilter().filter(s);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.room_clear_button) {
            conferenceNameEditText.getText().clear();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            startActivity(ConferenceAddActivity.createIntent(this, account,
                    UserJid.from(hostedConferencesAdapter.getItem(position).getJid())));
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
            returnResult();
            return true;
        }
        else {
            return false;
        }
    }
}
