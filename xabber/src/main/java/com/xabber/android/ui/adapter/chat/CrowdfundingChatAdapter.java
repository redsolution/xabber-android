package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.xabber.android.R;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.android.utils.Utils;

import java.util.Date;

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
        String nick = message.getNameForCurrentLocale();
        if (nick != null) {
            holder.messageHeader.setText(nick);
            holder.messageHeader.setTextColor(ColorManager.changeColor(
                    ColorGenerator.MATERIAL.getColor(nick), 0.8f));
            holder.messageHeader.setVisibility(View.VISIBLE);
        } else holder.messageHeader.setVisibility(View.GONE);

        // time
        String time = StringUtils.getTimeText(new Date((long)message.getTimestamp()*1000));
        holder.messageTime.setText(time);

        // status
        holder.statusIcon.setVisibility(View.GONE);
        holder.ivEncrypted.setVisibility(View.GONE);

        // avatar
        String avatarUrl = message.getAuthorAvatar();
        if (avatarUrl != null) {
            setupAvatar(holder.avatar, avatarUrl);
            holder.avatar.setVisibility(View.VISIBLE);
            holder.avatarBackground.setVisibility(View.VISIBLE);
        } else {
            holder.avatar.setVisibility(View.GONE);
            holder.avatarBackground.setVisibility(View.GONE);
        }

        // setup BALLOON margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.setMargins(
                Utils.dipToPx(2f, context),
                Utils.dipToPx(2f, context),
                Utils.dipToPx(0f, context),
                Utils.dipToPx(2f, context));
        holder.messageShadow.setLayoutParams(layoutParams);

    }

    private void setupAvatar(ImageView view, String url) {
        Glide.with(context).load(url).into(view);
    }

    public class CrowdMessageVH extends RecyclerView.ViewHolder {

        TextView messageText;
        TextView messageHeader;
        TextView messageTime;
        ImageView statusIcon;
        ImageView ivEncrypted;
        ImageView avatar;
        ImageView avatarBackground;
        View messageShadow;

        CrowdMessageVH(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text);
            messageHeader = itemView.findViewById(R.id.message_header);
            statusIcon = itemView.findViewById(R.id.message_status_icon);
            ivEncrypted = itemView.findViewById(R.id.message_encrypted_icon);
            messageTime = itemView.findViewById(R.id.message_time);
            avatar = itemView.findViewById(R.id.avatar);
            avatarBackground = itemView.findViewById(R.id.avatarBackground);
            messageShadow = itemView.findViewById(R.id.message_shadow);

        }
    }

}
