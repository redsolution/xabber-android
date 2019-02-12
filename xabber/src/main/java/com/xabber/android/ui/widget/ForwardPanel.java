package com.xabber.android.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.R;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;

import java.util.HashSet;
import java.util.List;

import io.realm.RealmResults;

public class ForwardPanel extends Fragment {

    private List<String> forwardedIds;
    private TextView tvForwardedFrom;
    private TextView tvForwardedText;
    private ImageView ivCloseForwardPanel;
    private OnCloseListener listener;

    public interface OnCloseListener {
        void onClose();
    }

    public static ForwardPanel newInstance(List<String> forwardedIds) {
        ForwardPanel panel = new ForwardPanel();
        panel.forwardedIds = forwardedIds;
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
        View view = inflater.inflate(R.layout.view_chat_forward, container, false);

        tvForwardedText = view.findViewById(R.id.tvForwardedText);
        tvForwardedFrom = view.findViewById(R.id.tvForwardedFrom);
        ivCloseForwardPanel = view.findViewById(R.id.ivCloseForwardPanel);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (forwardedIds != null && forwardedIds.size() > 0) {
            RealmResults<MessageItem> forwardedMessages =
                    MessageDatabaseManager.getInstance().getRealmUiThread().where(MessageItem.class)
                            .in(MessageItem.Fields.UNIQUE_ID, forwardedIds.toArray(new String[0])).findAll();

            tvForwardedFrom.setText(Html.fromHtml(getNames(forwardedMessages)));

            String forwardedText = forwardedMessages.get(0).getText();
            if (forwardedMessages.size() > 1 || forwardedText.trim().isEmpty()) {
                Context context = getContext();
                if (context != null)
                    tvForwardedText.setText(String.format(context.getResources()
                        .getString(R.string.forwarded_messages_count), forwardedMessages.size()));
            } else tvForwardedText.setText(forwardedText);
        }

        ivCloseForwardPanel.setOnClickListener(new View.OnClickListener() {
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
}
