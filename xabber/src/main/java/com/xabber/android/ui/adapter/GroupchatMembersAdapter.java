package com.xabber.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.message.chat.groupchat.GroupChat;
import com.xabber.android.data.message.chat.groupchat.GroupchatMember;
import com.xabber.android.utils.StringUtils;

import java.util.ArrayList;

public class GroupchatMembersAdapter extends RecyclerView.Adapter<GroupchatMembersAdapter.GroupchatMemberViewHolder> {

    private ArrayList<GroupchatMember> groupchatMembers;
    private GroupChat chat;

    public GroupchatMembersAdapter(ArrayList<GroupchatMember> groupchatMembers, GroupChat chat) {
        this.groupchatMembers = groupchatMembers;
        this.chat = chat;
    }

    public void setItems(ArrayList<GroupchatMember> groupchatMembers) {
        this.groupchatMembers = groupchatMembers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupchatMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupchatMemberViewHolder(LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_groupchat_member, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GroupchatMemberViewHolder holder, int position) {
        GroupchatMember bindMember = groupchatMembers.get(position);

        if (bindMember.getNickname() != null && !bindMember.getNickname().isEmpty()) {
            holder.memberName.setText(bindMember.getNickname());
        } else if (bindMember.getJid() != null) {
            holder.memberName.setText(bindMember.getJid());
        }

        if (bindMember.getBadge() != null && !bindMember.getBadge().isEmpty()) {
            holder.memberBadge.setVisibility(View.VISIBLE);
            holder.memberBadge.setText(bindMember.getBadge());
        } else {
            holder.memberBadge.setVisibility(View.GONE);
        }

        if (bindMember.getRole() != null && !bindMember.getBadge().isEmpty()) {
            holder.memberRole.setVisibility(View.VISIBLE);
            if (bindMember.getRole().equals("owner")) {
                holder.memberRole.setImageResource(R.drawable.ic_star_filled);
            } else if (bindMember.getRole().equals("admin")) {
                holder.memberRole.setImageResource(R.drawable.ic_star_outline);
            } else {
                holder.memberRole.setVisibility(View.GONE);
            }
        } else {
            holder.memberRole.setVisibility(View.GONE);
        }

        holder.memberStatus.setText(StringUtils.getLastPresentString(bindMember.getLastPresent()));

        holder.avatar.setImageDrawable(AvatarManager.getInstance().getGroupchatMemberAvatar(bindMember, chat.getAccount()));
    }

    @Override
    public int getItemCount() {
        return groupchatMembers.size();
    }

    static class GroupchatMemberViewHolder extends RecyclerView.ViewHolder {

        private ImageView avatar;
        private ImageView memberRole;
        private TextView memberName;
        private TextView memberStatus;
        private TextView memberBadge;

        public GroupchatMemberViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.ivMemberAvatar);
            memberRole = itemView.findViewById(R.id.ivMemberRole);
            memberName = itemView.findViewById(R.id.tvMemberName);
            memberStatus = itemView.findViewById(R.id.tvMemberStatus);
            memberBadge = itemView.findViewById(R.id.tvMemberBadge);
        }
    }
}
