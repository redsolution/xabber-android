package com.xabber.android.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.GroupEditActivity;
import com.xabber.android.ui.color.ColorManager;

import org.jivesoftware.smack.roster.packet.RosterPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ContactEditFragment extends GroupEditorFragment implements OnContactChangedListener {

    private static final String SAVED_CONTACT_NICKNAME = "com.xabber.android.ui.fragment.GroupEditorFragment.SAVED_CONTACT_NICKNAME";
    private static final String SAVED_SEND_PRESENCE = "com.xabber.android.ui.fragment.GroupEditorFragment.SAVED_SEND_PRESENCE";
    private static final String SAVED_RECEIVE_PRESENCE = "com.xabber.android.ui.fragment.GroupEditorFragment.SAVED_RECEIVE_PRESENCE";

    private EditText contactEditNickname;
    private TextView userJid;
    private CheckBox chkSendPresence;
    private CheckBox chkReceivePresence;
    private TextView tvSendPresence;
    private TextView tvReceivePresence;
    private ImageView avatar;

    private boolean stateRestored = false;
    private boolean sendChecked;
    private boolean receiveChecked;
    private RosterPacket.ItemType subType;
    private ArrayList<String> contactGroups = new ArrayList<>();

    public static ContactEditFragment newInstance(AccountJid account, UserJid user) {
        ContactEditFragment fragment = new ContactEditFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ACCOUNT, account);
        args.putParcelable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_edit_layout, container, false);
        contactEditNickname = view.findViewById(R.id.nickname);
        userJid = view.findViewById(R.id.tvBareUserJid);
        chkSendPresence = view.findViewById(R.id.chkSendPresence);
        chkReceivePresence = view.findViewById(R.id.chkReceivePresence);
        avatar = view.findViewById(R.id.contact_avatar);
        tvSendPresence = view.findViewById(R.id.tvSendPresence);
        tvReceivePresence = view.findViewById(R.id.tvReceivePresence);
        ((TextView) view.findViewById(R.id.tvNickname)).setTextColor(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(getAccount()));
        ((TextView) view.findViewById(R.id.subInfo)).setTextColor(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(getAccount()));
        ((TextView) view.findViewById(R.id.tvCircles)).setTextColor(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(getAccount()));
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        contactEditNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                enableSaveIfNeeded();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        if (savedInstanceState != null) {
            contactEditNickname.setText(savedInstanceState.getString(SAVED_CONTACT_NICKNAME));
            sendChecked = savedInstanceState.getBoolean(SAVED_SEND_PRESENCE);
            receiveChecked = savedInstanceState.getBoolean(SAVED_RECEIVE_PRESENCE);
            stateRestored = true;
        }
        updateContact();
    }

    private void updateContact() {
        getListView().setDivider(null);
        AbstractContact abstractContact = RosterManager.getInstance().getBestContact(getAccount(), getUser());

        avatar.setImageDrawable(abstractContact.getAvatar());
        userJid.setText(getUser().getBareJid().toString());
        contactEditNickname.setHint(abstractContact.getName());

        subType = RosterManager.getInstance().getSubscriptionType(getAccount(), getUser());
        if (subType == null) {
            getActivity().finish();
        }
        switch (subType) {
            case both:
                //both see each other's presence
                chkSendPresence.setChecked(!stateRestored || sendChecked);
                tvSendPresence.setText(R.string.contact_subscription_send);

                chkReceivePresence.setChecked(!stateRestored || receiveChecked);
                tvReceivePresence.setText(R.string.contact_subscription_receive);
                break;
            case to:
                //account can see contact's presence
                chkSendPresence.setChecked(stateRestored ? sendChecked : PresenceManager.getInstance().hasAutoAcceptSubscription(getAccount(), getUser()));
                tvSendPresence.setText(R.string.contact_subscription_accept);

                chkReceivePresence.setChecked(!stateRestored || receiveChecked);
                tvReceivePresence.setText(R.string.contact_subscription_receive);
                break;
            case from:
                //contact can see account's presence
                chkSendPresence.setChecked(!stateRestored || sendChecked);
                tvSendPresence.setText(R.string.contact_subscription_send);

                chkReceivePresence.setChecked(stateRestored && receiveChecked);
                tvReceivePresence.setText(R.string.contact_subscription_ask);
                break;
            case none:
                //no presence sharing
                chkSendPresence.setChecked(stateRestored ? sendChecked : PresenceManager.getInstance().hasAutoAcceptSubscription(getAccount(), getUser()));
                tvSendPresence.setText(R.string.contact_subscription_accept);

                chkReceivePresence.setChecked(stateRestored && receiveChecked);
                tvReceivePresence.setText(R.string.contact_subscription_ask);
                break;
        }

        chkSendPresence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sendChecked = isChecked;
                stateRestored = false;
                enableSaveIfNeeded();
            }
        });

        chkReceivePresence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                receiveChecked = isChecked;
                stateRestored = false;
                enableSaveIfNeeded();
            }
        });

        contactGroups = new ArrayList<String>(RosterManager.getInstance().getGroups(getAccount(), getUser()));
    }


    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_CONTACT_NICKNAME, contactEditNickname.getText().toString());
        outState.putBoolean(SAVED_RECEIVE_PRESENCE, chkReceivePresence.isChecked());
        outState.putBoolean(SAVED_SEND_PRESENCE, chkSendPresence.isChecked());
    }

    private void enableSaveIfNeeded() {
        //Nickname
        String name = RosterManager.getInstance().getName(getAccount(), getUser());
        if (!name.equals(contactEditNickname.getText().toString()) && !contactEditNickname.getText().toString().isEmpty()) {
            ((GroupEditActivity)getActivity()).toolbarSetEnabled(true);
            return;
        }

        //Subscription settings
        if (chkSendPresence.isChecked() && chkReceivePresence.isChecked()) {
            if (subType != RosterPacket.ItemType.both) {
                ((GroupEditActivity)getActivity()).toolbarSetEnabled(true);
                return;
            }
        }

        if (chkSendPresence.isChecked() && !chkReceivePresence.isChecked()) {
            if (subType != RosterPacket.ItemType.from) {
                ((GroupEditActivity)getActivity()).toolbarSetEnabled(true);
                return;
            }
        }

        if (!chkSendPresence.isChecked() && chkReceivePresence.isChecked()) {
            if (subType != RosterPacket.ItemType.to) {
                ((GroupEditActivity)getActivity()).toolbarSetEnabled(true);
                return;
            }
        }

        if (!chkSendPresence.isChecked() && !chkReceivePresence.isChecked()) {
            if (subType != RosterPacket.ItemType.none) {
                ((GroupEditActivity)getActivity()).toolbarSetEnabled(true);
                return;
            }
        }

        //Circles
        ArrayList<String> selectedGroups = getSelected();
        Collections.sort(contactGroups);
        Collections.sort(selectedGroups);

        if (contactGroups.size() != selectedGroups.size()) {
            ((GroupEditActivity)getActivity()).toolbarSetEnabled(true);
            return;
        }

        for (int i = 0; i < selectedGroups.size(); i++) {
            if (!selectedGroups.get(i).equals(contactGroups.get(i))) {
                ((GroupEditActivity)getActivity()).toolbarSetEnabled(true);
                return;
            }
        }

        ((GroupEditActivity)getActivity()).toolbarSetEnabled(false);
    }

    public void saveChanges() {
        if (!contactEditNickname.getText().toString().isEmpty()) {
            RosterManager.getInstance().setName(getAccount(), getUser(), contactEditNickname.getText().toString());
        }
        saveSubscriptionSettings();
        saveGroups();
        updateContact();
    }

    private void saveSubscriptionSettings() {
        switch (subType) {
            case both:
                if (chkSendPresence.isChecked() && chkReceivePresence.isChecked()) {
                    break;
                }
                try {
                    if (!chkSendPresence.isChecked()) {
                        PresenceManager.getInstance().discardSubscription(getAccount(), getUser());
                    }
                    if (!chkReceivePresence.isChecked()) {
                        PresenceManager.getInstance().unsubscribeFromPresence(getAccount(), getUser());
                    }
                } catch (NetworkException e) {
                    e.printStackTrace();
                }
                break;
            case to:
                //if (!chkSendPresence.isChecked() && chkReceivePresence.isChecked()) {
                //    break;
                //}
                try {
                    if (chkSendPresence.isChecked()) {
                        PresenceManager.getInstance().addAutoAcceptSubscription(getAccount(), getUser());
                    } else {
                        PresenceManager.getInstance().removeAutoAcceptSubscription(getAccount(), getUser());
                    }
                    if (!chkReceivePresence.isChecked()) {
                        PresenceManager.getInstance().unsubscribeFromPresence(getAccount(), getUser());
                    }
                } catch (NetworkException e) {
                    e.printStackTrace();
                }
                break;
            case from:
                if (chkSendPresence.isChecked() && !chkReceivePresence.isChecked()) {
                    break;
                }
                try {
                    if (!chkSendPresence.isChecked()) {
                        PresenceManager.getInstance().discardSubscription(getAccount(), getUser());
                    }
                    if (chkReceivePresence.isChecked()) {
                        PresenceManager.getInstance().subscribeForPresence(getAccount(), getUser());
                    }
                } catch (NetworkException e) {
                    e.printStackTrace();
                }
                break;
            case none:
                //if (!chkSendPresence.isChecked() && !chkReceivePresence.isChecked()) {
                //    break;
                //}
                try {
                    if (chkSendPresence.isChecked()) {
                        PresenceManager.getInstance().addAutoAcceptSubscription(getAccount(), getUser());
                    } else {
                        PresenceManager.getInstance().removeAutoAcceptSubscription(getAccount(), getUser());
                    }
                    if (chkReceivePresence.isChecked()) {
                        PresenceManager.getInstance().subscribeForPresence(getAccount(), getUser());
                    }
                } catch (NetworkException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        enableSaveIfNeeded();
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        enableSaveIfNeeded();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(getAccount(), getUser())) {
                updateContact();
            }
        }
    }
}
