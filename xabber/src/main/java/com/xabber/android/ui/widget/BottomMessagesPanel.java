package com.xabber.android.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.extension.groups.GroupMemberManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;

import java.util.HashSet;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class BottomMessagesPanel extends Fragment {

    private List<String> messagesIds;
    private TextView tvFrom;
    private TextView tvText;
    private ImageView ivCloseBottomMessagePanel;
    private OnCloseListener listener;
    private Purposes purpose;

    public interface OnCloseListener {
        void onClose();
    }

    public Purposes getPurpose() { return purpose; }

    public static BottomMessagesPanel newInstance(List<String> messagesIds, Purposes purpose) {
        BottomMessagesPanel panel = new BottomMessagesPanel();
        panel.messagesIds = messagesIds;
        panel.purpose = purpose;
        return panel;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnCloseListener) {
            listener = (OnCloseListener) getParentFragment();
        } else {
            throw new ClassCastException("must implement OnCloseListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_chat_bottom_message_panel, container, false);

        tvText = view.findViewById(R.id.tvText);
        tvFrom = view.findViewById(R.id.tvFrom);
        ivCloseBottomMessagePanel = view.findViewById(R.id.ivCloseBottomMessagePanel);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        if (messagesIds != null && messagesIds.size() > 0) {
            RealmResults<MessageRealmObject> messages = realm
                    .where(MessageRealmObject.class)
                    .in(MessageRealmObject.Fields.PRIMARY_KEY, messagesIds.toArray(new String[0]))
                    .findAll();

            tvFrom.setText(Html.fromHtml(getNames(messages)));

            String text = messages.get(0).getText();
            if (messages.size() > 1 || text.trim().isEmpty()) {
                Context context = getContext();
                if (context != null && purpose.equals(Purposes.FORWARDING)){
                    tvText.setText(context.getResources().getQuantityString(
                            R.plurals.forwarded_messages_count, messages.size(), messages.size()));
                }
            } else tvText.setText(text);
        }

        ivCloseBottomMessagePanel.setOnClickListener(v ->  {
                listener.onClose();
        });
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    private String getNames(RealmResults<MessageRealmObject> messages) {
        HashSet<String> names = new HashSet<>();
        for (MessageRealmObject message : messages) {
            AbstractChat chat = ChatManager.getInstance().getChat(message.getAccount(), message.getUser());
            String name = null;
            if (chat instanceof GroupChat) {
                if (message.getGroupchatUserId() != null
                        && GroupMemberManager.INSTANCE.getGroupMemberById(
                                message.getAccount(), message.getUser(), message.getGroupchatUserId()
                ) != null) {
                    name = GroupMemberManager.INSTANCE.getGroupMemberById(
                            message.getAccount(), message.getUser(), message.getGroupchatUserId()
                    ).getNickname();
                } else if (!message.isIncoming() && GroupMemberManager.INSTANCE.getMe((GroupChat) chat) != null) {
                    name = GroupMemberManager.INSTANCE.getMe((GroupChat) chat).getNickname();
                }
            } else if (message.isIncoming()) {
                name = RosterManager.getDisplayAuthorName(message);
            } else {
                name = AccountManager.getInstance().getNickName(message.getAccount());
            }
            if (name == null) name = "null";
            int color = ColorManager.changeColor(ColorGenerator.MATERIAL.getColor(name), 0.8f);
            names.add("<font color='#" + Integer.toHexString(color).substring(2) + "'>" + name + "</font>");
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String name : names) {
            stringBuilder.append(name);
            stringBuilder.append(", ");
        }

        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    public enum Purposes {
        FORWARDING,
        EDITING
    }
}
