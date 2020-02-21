package com.xabber.android.ui.widget;

import android.content.Context;
import android.os.Bundle;
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
import com.xabber.android.data.database.realmobjects.MessageItem;
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

        if (messagesIds != null && messagesIds.size() > 0) {
            RealmResults<MessageItem> messages = Realm
                    .getDefaultInstance().where(MessageItem.class)
                    .in(MessageItem.Fields.UNIQUE_ID, messagesIds.toArray(new String[0]))
                    .findAll();

            tvFrom.setText(Html.fromHtml(getNames(messages)));

            String text = messages.get(0).getText();
            if (messages.size() > 1 || text.trim().isEmpty()) {
                Context context = getContext();
                if (context != null && purpose.equals(Purposes.FORWARDING))
                    tvText.setText(String.format(context.getResources()
                        .getString(R.string.forwarded_messages_count), messages.size()));
            } else tvText.setText(text);
        }

        ivCloseBottomMessagePanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClose();
            }
        });
    }

    private String getNames(RealmResults<MessageItem> messages) {
        HashSet<String> names = new HashSet<>();
        for (MessageItem message : messages) {
            String name = RosterManager.getDisplayAuthorName(message);
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
