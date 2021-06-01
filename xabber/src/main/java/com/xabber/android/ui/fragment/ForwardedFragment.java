package com.xabber.android.ui.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.groupchat.GroupchatUser;
import com.xabber.android.data.groupchat.GroupchatUserManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ForwardedActivity;
import com.xabber.android.ui.adapter.chat.ForwardedAdapter;
import com.xabber.android.ui.adapter.chat.MessagesAdapter;
import com.xabber.android.ui.color.ColorManager;

import io.realm.Realm;
import io.realm.RealmResults;

public class ForwardedFragment extends FileInteractionFragment {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";
    private final static String KEY_MESSAGE_ID = "messageId";

    private String userName;
    private int accountMainColor;
    private int mentionColor;
    private ColorStateList colorStateList;
    private String messageId;

    private RecyclerView recyclerView;
    private View backgroundView;

    public static ForwardedFragment newInstance(AccountJid account, ContactJid user, String messageId) {
        ForwardedFragment fragment = new ForwardedFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        arguments.putString(KEY_MESSAGE_ID, messageId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            account = args.getParcelable(ARGUMENT_ACCOUNT);
            user = args.getParcelable(ARGUMENT_USER);
            messageId = args.getString(KEY_MESSAGE_ID);

            userName = RosterManager.getInstance().getName(account, user);
            accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
            mentionColor = ColorManager.getInstance().getAccountPainter().getAccountIndicatorBackColor(account);
            colorStateList = ColorManager.getInstance().getChatIncomingBalloonColorsStateList(account);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forwarded, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        backgroundView = view.findViewById(R.id.backgroundView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // background
        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                backgroundView.setBackgroundResource(R.color.black);
            } else {
                backgroundView.setBackgroundResource(R.drawable.chat_background_repeat);
            }
        } else {
            backgroundView.setBackgroundColor(ColorManager.getInstance().getChatBackgroundColor());
        }

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();

        // messages adapter
        MessageRealmObject messageRealmObject = realm
                .where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageId)
                .findFirst();

        RealmResults<MessageRealmObject> forwardedMessages = realm
                .where(MessageRealmObject.class)
                .in(MessageRealmObject.Fields.UNIQUE_ID, messageRealmObject.getForwardedIdsAsArray())
                .findAll();

        // groupchat user
        GroupchatUser groupchatUser = GroupchatUserManager.getInstance().getGroupchatUser(messageRealmObject.getGroupchatUserId());

        MessagesAdapter.MessageExtraData extraData = new MessagesAdapter.MessageExtraData(this,
                this, getActivity(), userName, colorStateList, groupchatUser,
                accountMainColor, mentionColor, null, false,
                false, false, false, false);

        if (forwardedMessages.size() > 0) {
            ForwardedAdapter adapter = new ForwardedAdapter(forwardedMessages, extraData);
            recyclerView.setLayoutManager(new LinearLayoutManager(extraData.getContext()));
            recyclerView.setAdapter(adapter);
            ((ForwardedActivity)getActivity()).setToolbar(forwardedMessages.size());
        }

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }
}
