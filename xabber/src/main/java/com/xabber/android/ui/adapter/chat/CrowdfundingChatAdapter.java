package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.R;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.ui.color.ColorManager;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class CrowdfundingChatAdapter extends RealmRecyclerViewAdapter<CrowdfundingMessage, CrowdfundingChatAdapter.CrowdMessageVH> {

    public CrowdfundingChatAdapter(Context context, RealmResults<CrowdfundingMessage> realmResults,
                                   boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
    }

    @Nullable
    public CrowdfundingMessage getMessage(int position) {
        if (position == RecyclerView.NO_POSITION) return null;

        if (position < realmResults.size())
            return realmResults.get(position);
        else return null;
    }

    @NonNull
    @Override
    public CrowdMessageVH onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        return new CrowdMessageVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_incoming, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CrowdMessageVH holder, int i) {
        CrowdfundingMessage message = getMessage(i);
        if (message == null) return;

        // text
        holder.messageText.setText(message.getMessageForCurrentLocale());

        // nickname
        String nick = null;
        if (nick != null) {
            holder.messageHeader.setText(nick);
            holder.messageHeader.setTextColor(ColorManager.changeColor(
                    ColorGenerator.MATERIAL.getColor(nick), 0.8f));
            holder.messageHeader.setVisibility(View.VISIBLE);
        } else holder.messageHeader.setVisibility(View.GONE);

        // time
        holder.messageTime.setText("12:22");

        // status
        holder.statusIcon.setVisibility(View.GONE);
        holder.ivEncrypted.setVisibility(View.GONE);

        // avatar
        String avatarUrl = null;
        if (avatarUrl != null) {
            holder.avatar.setVisibility(View.VISIBLE);
            holder.avatarBackground.setVisibility(View.VISIBLE);
        } else {
            holder.avatar.setVisibility(View.GONE);
            holder.avatarBackground.setVisibility(View.GONE);
        }

    }

    public class CrowdMessageVH extends RecyclerView.ViewHolder {

        TextView messageText;
        TextView messageHeader;
        TextView messageTime;
        ImageView statusIcon;
        ImageView ivEncrypted;
        public ImageView avatar;
        public ImageView avatarBackground;

        CrowdMessageVH(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text);
            messageHeader = itemView.findViewById(R.id.message_header);
            statusIcon = itemView.findViewById(R.id.message_status_icon);
            ivEncrypted = itemView.findViewById(R.id.message_encrypted_icon);
            messageTime = itemView.findViewById(R.id.message_time);
            avatar = itemView.findViewById(R.id.avatar);
            avatarBackground = itemView.findViewById(R.id.avatarBackground);

        }
    }

}
