package com.xabber.android.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.RosterManager.SubscriptionState;
import com.xabber.android.ui.OnContactChangedListener;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.color.ColorManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ContactEditFragment extends CircleEditorFragment implements OnContactChangedListener {

    private static final String SAVED_CONTACT_NICKNAME = "com.xabber.android.ui.fragment.CircleEditorFragment.SAVED_CONTACT_NICKNAME";
    private static final String SAVED_SEND_PRESENCE = "com.xabber.android.ui.fragment.CircleEditorFragment.SAVED_SEND_PRESENCE";
    private static final String SAVED_RECEIVE_PRESENCE = "com.xabber.android.ui.fragment.CircleEditorFragment.SAVED_RECEIVE_PRESENCE";

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
    private boolean saveNickname;
    private SubscriptionState subscriptionState;
    private ArrayList<String> contactCircles = new ArrayList<>();

    public static ContactEditFragment newInstance(AccountJid account, ContactJid user) {
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
        int accountColor = ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(getAccount());
        ((TextView) view.findViewById(R.id.tvNickname)).setTextColor(accountColor);
        ((TextView) view.findViewById(R.id.tvSubInfo)).setTextColor(accountColor);
        ((TextView) view.findViewById(R.id.select_circles_text_view)).setTextColor(accountColor);
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
        AbstractContact abstractContact = RosterManager.getInstance().getBestContact(getAccount(), getContactJid());

        avatar.setImageDrawable(abstractContact.getAvatar());
        userJid.setText(getContactJid().getBareJid().toString());
        contactEditNickname.setHint(abstractContact.getName());
        if (abstractContact instanceof RosterContact) {
            contactEditNickname.setText(((RosterContact) abstractContact).getNickname());
        }

        subscriptionState = RosterManager.getInstance().getSubscriptionState(getAccount(), getContactJid());

        setPresenceSettings();

        chkSendPresence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sendChecked = isChecked;
            stateRestored = false;
            enableSaveIfNeeded();
        });

        chkReceivePresence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            receiveChecked = isChecked;
            stateRestored = false;
            enableSaveIfNeeded();
        });

        contactCircles = new ArrayList<>(RosterManager.getInstance().getCircles(getAccount(), getContactJid()));
    }

    // Either set or restore checkbox values
    // and the text of TextViews paired with them.
    private void setPresenceSettings() {
        boolean hasAutoAcceptSubscription = PresenceManager.INSTANCE.hasAutoAcceptSubscription(getAccount(), getContactJid());
        switch (subscriptionState.getSubscriptionType()) {
            case SubscriptionState.BOTH:
                setSendSubscriptionField(true, R.string.contact_subscription_send);
                setReceiveSubscriptionField(true, R.string.contact_subscription_receive);
                break;

            case SubscriptionState.TO:
                if (subscriptionState.hasIncomingSubscription()) {
                    setSendSubscriptionField(false, R.string.contact_subscription_send);
                } else {
                    setSendSubscriptionField(hasAutoAcceptSubscription, R.string.contact_subscription_accept);
                }
                setReceiveSubscriptionField(true, R.string.contact_subscription_receive);
                break;

            case SubscriptionState.FROM:
                setSendSubscriptionField(true, R.string.contact_subscription_send);
                setReceiveSubscriptionField(subscriptionState.hasOutgoingSubscription(), R.string.contact_subscription_ask);
                break;

            case SubscriptionState.NONE:
                if (subscriptionState.hasIncomingSubscription()) {
                    setSendSubscriptionField(false, R.string.contact_subscription_send);
                } else {
                    setSendSubscriptionField(hasAutoAcceptSubscription, R.string.contact_subscription_accept);
                }
                setReceiveSubscriptionField(subscriptionState.hasOutgoingSubscription(), R.string.contact_subscription_ask);
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
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_CONTACT_NICKNAME, contactEditNickname.getText().toString());
        outState.putBoolean(SAVED_RECEIVE_PRESENCE, chkReceivePresence.isChecked());
        outState.putBoolean(SAVED_SEND_PRESENCE, chkSendPresence.isChecked());
    }

    private void enableSaveIfNeeded() {
        // Nickname
        String name = RosterManager.getInstance().getName(getAccount(), getContactJid());
        String nickname = RosterManager.getInstance().getNickname(getAccount(), getContactJid());
        String setName = contactEditNickname.getText().toString().trim();

        // Set name must be different from the general name
        // (could be a nickname from the Roster if not empty/from the vCard)
        if (!setName.equals(name)) {
            // And must be different from the nickname itself, since it could be empty.
            if (!setName.equals(nickname)) {
                // This will allow us to save when we actually need to modify
                // the nickname or when we want to remove the nickname completely
                ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                saveNickname = true;
                return;
            }
        }
        saveNickname = false;

        // Subscription settings
        if (subscriptionState == null) {
            return;
        }
        boolean hasOutgoingSubscription = subscriptionState.hasOutgoingSubscription();
        boolean hasAutoAcceptSubscription = PresenceManager.INSTANCE.hasAutoAcceptSubscription(getAccount(), getContactJid());

        switch (subscriptionState.getSubscriptionType()) {
            case SubscriptionState.BOTH:
                if (!chkSendPresence.isChecked() || !chkReceivePresence.isChecked()) {

                    ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                    return;
                }
                break;
            case SubscriptionState.TO:
                if ((hasAutoAcceptSubscription != chkSendPresence.isChecked())
                        || !chkReceivePresence.isChecked()) {

                    ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                    return;
                }
                break;
            case SubscriptionState.FROM:
                if ((hasOutgoingSubscription != chkReceivePresence.isChecked())
                        || !chkSendPresence.isChecked()) {

                    ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                    return;
                }
                break;
            case SubscriptionState.NONE:
                if ((hasAutoAcceptSubscription != chkSendPresence.isChecked())
                        || (hasOutgoingSubscription != chkReceivePresence.isChecked())) {

                    ((ContactEditActivity) getActivity()).toolbarSetEnabled(true);
                    return;
                }
                break;
        }

        // Circles
        ArrayList<String> selectedCircles = getSelected();
        Collections.sort(contactCircles);
        Collections.sort(selectedCircles);

        if (contactCircles.size() != selectedCircles.size()) {
            ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
            return;
        }

        for (int i = 0; i < selectedCircles.size(); i++) {
            if (!selectedCircles.get(i).equals(contactCircles.get(i))) {
                ((ContactEditActivity)getActivity()).toolbarSetEnabled(true);
                return;
            }
        }

        ((ContactEditActivity)getActivity()).toolbarSetEnabled(false);
    }

    public void saveChanges() {
        if (saveNickname) {
            RosterManager.getInstance().setName(getAccount(), getContactJid(), contactEditNickname.getText().toString());
        }
        saveSubscriptionSettings();
        saveCircles();
    }

    private void saveSubscriptionSettings() {
        switch (subscriptionState.getSubscriptionType()) {
            case SubscriptionState.BOTH:
                if (chkSendPresence.isChecked() && chkReceivePresence.isChecked()) {
                    break;
                }
                try {
                    if (!chkSendPresence.isChecked()) {
                        PresenceManager.INSTANCE.discardSubscription(getAccount(), getContactJid());
                    }
                    if (!chkReceivePresence.isChecked()) {
                        PresenceManager.INSTANCE.unsubscribeFromPresence(getAccount(), getContactJid());
                        AbstractChat chat = ChatManager.getInstance().getChat(getAccount(), getContactJid());
                        if (chat != null) chat.setAddContactSuggested(true);
                    }
                } catch (NetworkException e) {
                    LogManager.exception(getClass().getSimpleName(), e);
                }
                break;
            case SubscriptionState.TO:
                try {
                    if (chkSendPresence.isChecked()) {
                        PresenceManager.INSTANCE.addAutoAcceptSubscription(getAccount(), getContactJid());
                    } else {
                        PresenceManager.INSTANCE.removeAutoAcceptSubscription(getAccount(), getContactJid());
                    }
                    if (!chkReceivePresence.isChecked()) {
                        PresenceManager.INSTANCE.unsubscribeFromPresence(getAccount(), getContactJid());
                        AbstractChat chat = ChatManager.getInstance().getChat(getAccount(), getContactJid());
                        if (chat != null) chat.setAddContactSuggested(true);
                    }
                } catch (NetworkException e) {
                    LogManager.exception(getClass().getSimpleName(), e);
                }
                break;
            case SubscriptionState.FROM:
                try {
                    if (!chkSendPresence.isChecked()) {
                        PresenceManager.INSTANCE.discardSubscription(getAccount(), getContactJid());
                    }
                    if (chkReceivePresence.isChecked()) {
                        if (subscriptionState.getPendingSubscription() != SubscriptionState.PENDING_OUT) {
                            PresenceManager.INSTANCE.subscribeForPresence(getAccount(), getContactJid());
                        }
                    } else {
                        if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_OUT) {
                            PresenceManager.INSTANCE.unsubscribeFromPresence(getAccount(), getContactJid());
                            AbstractChat chat = ChatManager.getInstance().getChat(getAccount(), getContactJid());
                            if (chat != null) chat.setAddContactSuggested(true);
                        }
                    }
                } catch (NetworkException e) {
                    LogManager.exception(getClass().getSimpleName(), e);
                }
                break;
            case SubscriptionState.NONE:
                try {
                    if (chkSendPresence.isChecked()) {
                        PresenceManager.INSTANCE.addAutoAcceptSubscription(getAccount(), getContactJid());
                    } else {
                        PresenceManager.INSTANCE.removeAutoAcceptSubscription(getAccount(), getContactJid());
                    }
                    if (chkReceivePresence.isChecked()) {
                        if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_NONE
                                || subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_IN) {
                            PresenceManager.INSTANCE.subscribeForPresence(getAccount(), getContactJid());
                        }
                    } else {
                        if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_IN_OUT
                                || subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_OUT)  {
                            PresenceManager.INSTANCE.unsubscribeFromPresence(getAccount(), getContactJid());
                            AbstractChat chat = ChatManager.getInstance().getChat(getAccount(), getContactJid());
                            if (chat != null) chat.setAddContactSuggested(true);
                        }
                    }
                } catch (NetworkException e) {
                    LogManager.exception(getClass().getSimpleName(), e);
                }
                break;
        }
    }

    @Override
    public void onCircleAdded() {
        super.onCircleAdded();
        enableSaveIfNeeded();
    }

    @Override
    public void onCircleToggled() {
        super.onCircleToggled();
        enableSaveIfNeeded();
    }

    @Override
    public void onContactsChanged(@NotNull Collection<? extends RosterContact> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(getAccount(), getContactJid())) {
                Application.getInstance().runOnUiThread(this::updateContact);
            }
        }
    }

}
