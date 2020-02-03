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
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.RosterManager.SubscriptionState;
import com.xabber.android.ui.activity.ContactEditActivity;
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
    private CheckBox chkSendPresence; //responsible for both sending presence and auto-approving requests.
    private CheckBox chkReceivePresence; //responsible for receiving presence and requesting subscription.
    private TextView tvSendPresence;
    private TextView tvReceivePresence;
    private ImageView avatar;

    private boolean stateRestored = false;
    private boolean sendChecked;
    private boolean receiveChecked;
    private RosterPacket.ItemType subType;
    private SubscriptionState subscriptionState;
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
        ((TextView) view.findViewById(R.id.tvSubInfo)).setTextColor(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(getAccount()));
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
    }

    private void updateContact() {
        getListView().setDivider(null);
        AbstractContact abstractContact = RosterManager.getInstance().getBestContact(getAccount(), getUser());

        avatar.setImageDrawable(abstractContact.getAvatar());
        userJid.setText(getUser().getBareJid().toString());
        contactEditNickname.setHint(abstractContact.getName());

        subscriptionState = RosterManager.getInstance().getSubscriptionState(getAccount(), getUser());

        setPresenceSettings();

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

    // Either set or restore checkbox values
    // and the text of TextViews paired with them.
    private void setPresenceSettings() {
        int pendingSubscription = subscriptionState.getPendingSubscription();
        boolean hasAutoAcceptSubscription = PresenceManager.getInstance().hasAutoAcceptSubscription(getAccount(), getUser());
        switch (subscriptionState.getSubscriptionType()) {
            case SubscriptionState.BOTH:
                setSendSubscriptionField(true, R.string.contact_subscription_send);
                setReceiveSubscriptionField(true, R.string.contact_subscription_receive);
                break;

            case SubscriptionState.TO:
                if (pendingSubscription == SubscriptionState.PENDING_IN) {
                    setSendSubscriptionField(false, R.string.contact_subscription_send);
                } else {
                    setSendSubscriptionField(hasAutoAcceptSubscription, R.string.contact_subscription_accept);
                }
                setReceiveSubscriptionField(true, R.string.contact_subscription_receive);
                break;

            case SubscriptionState.FROM:
                setSendSubscriptionField(true, R.string.contact_subscription_send);
                setReceiveSubscriptionField(pendingSubscription == SubscriptionState.PENDING_OUT,
                        R.string.contact_subscription_ask);
                break;

            case SubscriptionState.NONE:
                if (pendingSubscription == SubscriptionState.PENDING_IN_OUT
                        || pendingSubscription == SubscriptionState.PENDING_IN) {
                    setSendSubscriptionField(false, R.string.contact_subscription_send);
                } else {
                    setSendSubscriptionField(hasAutoAcceptSubscription, R.string.contact_subscription_accept);
                }
                setReceiveSubscriptionField(pendingSubscription == SubscriptionState.PENDING_IN_OUT
                        || pendingSubscription == SubscriptionState.PENDING_OUT, R.string.contact_subscription_ask);
        }
    }

    private boolean getRestoredCheckIfNeeded(boolean originalValue, boolean restoredValue) {
        return stateRestored ? restoredValue : originalValue;
    }

    private void setReceiveSubscriptionField(boolean originalValue, int stringId) {
        chkReceivePresence.setChecked(getRestoredCheckIfNeeded(originalValue, receiveChecked));
        tvReceivePresence.setText(stringId);
    }

    private void setSendSubscriptionField(boolean originalValue, int stringId) {
        chkSendPresence.setChecked(getRestoredCheckIfNeeded(originalValue, sendChecked));
        tvSendPresence.setText(stringId);
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        updateContact();
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
            ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
            return;
        }

        //Subscription settings
        boolean hasOutgoingSubscription = subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_OUT
                || subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_IN_OUT;
        boolean hasAutoAcceptSubscription = PresenceManager.getInstance().hasAutoAcceptSubscription(getAccount(), getUser());

        switch (subscriptionState.getSubscriptionType()) {
            case SubscriptionState.BOTH:
                if (!chkSendPresence.isChecked() || !chkReceivePresence.isChecked()) {

                    ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                    return;
                }
            case SubscriptionState.TO:
                if ((hasAutoAcceptSubscription != chkSendPresence.isChecked())
                        || !chkReceivePresence.isChecked()) {

                    ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                    return;
                }
            case SubscriptionState.FROM:
                if ((hasOutgoingSubscription != chkReceivePresence.isChecked())
                        || !chkSendPresence.isChecked()) {

                    ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                    return;
                }
            case SubscriptionState.NONE:
                if ((hasAutoAcceptSubscription != chkSendPresence.isChecked())
                        || (hasOutgoingSubscription != chkReceivePresence.isChecked())) {

                    ((ContactEditActivity) getActivity()).toolbarSetEnabled(true);
                    return;
                }
        }

        //Circles
        ArrayList<String> selectedGroups = getSelected();
        Collections.sort(contactGroups);
        Collections.sort(selectedGroups);

        if (contactGroups.size() != selectedGroups.size()) {
            ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
            return;
        }

        for (int i = 0; i < selectedGroups.size(); i++) {
            if (!selectedGroups.get(i).equals(contactGroups.get(i))) {
                ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                return;
            }
        }

        ((ContactEditActivity)getActivity()).toolbarSetEnabled(false);
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
        switch (subscriptionState.getSubscriptionType()) {
            case SubscriptionState.BOTH:
                if (chkSendPresence.isChecked() && chkReceivePresence.isChecked()) {
                    break;
                }
                try {
                    if (!chkSendPresence.isChecked()) {
                        PresenceManager.getInstance().discardSubscription(getAccount(), getUser());
                    }
                    if (!chkReceivePresence.isChecked()) {
                        PresenceManager.getInstance().unsubscribeFromPresence(getAccount(), getUser());
                        AbstractChat chat = MessageManager.getInstance().getChat(getAccount(), getUser());
                        if (chat != null) chat.setAddContactSuggested(true);
                    }
                } catch (NetworkException e) {
                    e.printStackTrace();
                }
                break;
            case SubscriptionState.TO:
                try {
                    if (chkSendPresence.isChecked()) {
                        PresenceManager.getInstance().addAutoAcceptSubscription(getAccount(), getUser());
                    } else {
                        PresenceManager.getInstance().removeAutoAcceptSubscription(getAccount(), getUser());
                    }
                    if (!chkReceivePresence.isChecked()) {
                        PresenceManager.getInstance().unsubscribeFromPresence(getAccount(), getUser());
                        AbstractChat chat = MessageManager.getInstance().getChat(getAccount(), getUser());
                        if (chat != null) chat.setAddContactSuggested(true);
                    }
                } catch (NetworkException e) {
                    e.printStackTrace();
                }
                break;
            case SubscriptionState.FROM:
                try {
                    if (!chkSendPresence.isChecked()) {
                        PresenceManager.getInstance().discardSubscription(getAccount(), getUser());
                    }
                    if (chkReceivePresence.isChecked()) {
                        if (subscriptionState.getPendingSubscription() != SubscriptionState.PENDING_OUT) {
                            PresenceManager.getInstance().subscribeForPresence(getAccount(), getUser());
                        }
                    } else {
                        if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_OUT) {
                            PresenceManager.getInstance().unsubscribeFromPresence(getAccount(), getUser());
                            AbstractChat chat = MessageManager.getInstance().getChat(getAccount(), getUser());
                            if (chat != null) chat.setAddContactSuggested(true);
                        }
                    }
                } catch (NetworkException e) {
                    e.printStackTrace();
                }
                break;
            case SubscriptionState.NONE:
                try {
                    if (chkSendPresence.isChecked()) {
                        PresenceManager.getInstance().addAutoAcceptSubscription(getAccount(), getUser());
                    } else {
                        PresenceManager.getInstance().removeAutoAcceptSubscription(getAccount(), getUser());
                    }
                    if (chkReceivePresence.isChecked()) {
                        if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_NONE
                                || subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_IN) {
                            PresenceManager.getInstance().subscribeForPresence(getAccount(), getUser());
                        }
                    } else {
                        if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_IN_OUT
                                || subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_OUT)  {
                            PresenceManager.getInstance().unsubscribeFromPresence(getAccount(), getUser());
                            AbstractChat chat = MessageManager.getInstance().getChat(getAccount(), getUser());
                            if (chat != null) chat.setAddContactSuggested(true);
                        }
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
