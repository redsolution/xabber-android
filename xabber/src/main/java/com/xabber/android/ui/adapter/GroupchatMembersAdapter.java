package com.xabber.android.ui.adapter;

import static com.xabber.android.ui.text.DatesUtilsKt.isCurrentYear;
import static com.xabber.android.ui.text.DatesUtilsKt.isToday;
import static com.xabber.android.ui.text.DatesUtilsKt.isYesterday;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.text.DatesUtilsKt;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GroupchatMembersAdapter extends RecyclerView.Adapter<GroupchatMembersAdapter.GroupchatMemberViewHolder>
        implements View.OnClickListener {

    private ArrayList<GroupMemberRealmObject> groupMembers;
    private final GroupChat chat;
    private final OnMemberClickListener listener;
    private RecyclerView recyclerView;

    public GroupchatMembersAdapter(ArrayList<GroupMemberRealmObject> groupMembers, GroupChat chat,
                                   OnMemberClickListener listener) {
        this.groupMembers = groupMembers;
        this.chat = chat;
        this.listener = listener;
    }

    public void setItems(ArrayList<GroupMemberRealmObject> groupMembers) {
        this.groupMembers = groupMembers;
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        listener.onMemberClick(groupMembers.get(recyclerView.getChildAdapterPosition(v)));
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public GroupchatMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupchatMemberViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_groupchat_member, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull GroupchatMemberViewHolder holder, int position) {
        GroupMemberRealmObject bindMember = groupMembers.get(position);

        holder.root.setOnClickListener(this);

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

        if (bindMember.getRole() != null) {
            holder.memberRole.setVisibility(View.VISIBLE);
            switch (bindMember.getRole()) {
                case owner:
                    holder.memberRole.setImageResource(R.drawable.ic_star_filled);
                    break;
                case admin:
                    holder.memberRole.setImageResource(R.drawable.ic_star_outline);
                    break;
                default:
                    holder.memberRole.setVisibility(View.GONE);
            }
        } else {
            holder.memberRole.setVisibility(View.GONE);
        }

        if (bindMember.isMe()) {
            holder.memberStatus.setText(holder.itemView.getContext().getText(R.string.groupchat_this_is_you));
            holder.memberStatus.setTextColor(
                    ColorManager.getInstance().getAccountPainter().getAccountColorWithTint(chat.getAccount(), 500)
            );
        } else {
            String memberStatus = getLastPresentString(bindMember.getLastSeen());
            holder.memberStatus.setText(memberStatus);
            if (memberStatus.equals(Application.getInstance().getString(R.string.account_state_connected))) {
                holder.memberStatus.setTextColor(Application.getInstance().getResources().getColor(R.color.green_800));
            } else {
                holder.memberStatus.setTextColor(Application.getInstance().getResources().getColor(R.color.grey_500));
            }
        }

        holder.avatar.setImageDrawable(AvatarManager.getInstance().getGroupMemberAvatar(bindMember, chat.getAccount()));
    }

    @Override
    public int getItemCount() {
        return groupMembers.size();
    }

    static class GroupchatMemberViewHolder extends RecyclerView.ViewHolder {

        private final View root;
        private final ImageView avatar;
        private final ImageView memberRole;
        private final TextView memberName;
        private final TextView memberStatus;
        private final TextView memberBadge;

        public GroupchatMemberViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.member_list_item_root);
            avatar = itemView.findViewById(R.id.ivMemberAvatar);
            memberRole = itemView.findViewById(R.id.ivMemberRole);
            memberName = itemView.findViewById(R.id.tvMemberName);
            memberStatus = itemView.findViewById(R.id.tvMemberStatus);
            memberBadge = itemView.findViewById(R.id.tvMemberBadge);
        }
    }

    public interface OnMemberClickListener{
        void onMemberClick(GroupMemberRealmObject groupMember);
    }

    @SuppressLint("StringFormatMatches")
    @NonNull
    private String getLastPresentString(String lastPresent) {
        final SimpleDateFormat groupchatMemberPresenceTimeFormat;
        groupchatMemberPresenceTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
        groupchatMemberPresenceTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (lastPresent != null && !lastPresent.isEmpty()) {
            try {
                Date lastPresentDate = groupchatMemberPresenceTimeFormat.parse(lastPresent);
                if (lastPresentDate == null) {
                    return Application.getInstance().getString(R.string.unavailable);
                }

                if (lastPresentDate.getTime() > 0) {
                    long timeAgo = (System.currentTimeMillis() - lastPresentDate.getTime()) / 1000;
                    Locale locale = Application.getInstance().getResources().getConfiguration().locale;

                    if (timeAgo < 60) {
                        return Application.getInstance().getString(R.string.last_seen_now);

                    } else if (timeAgo < 3600) {
                        return Application.getInstance().getString(
                                R.string.last_seen_minutes,
                                String.valueOf(
                                        TimeUnit.SECONDS.toMinutes(timeAgo)
                                )
                        );

                    } else if (timeAgo < 7200) {
                        return Application.getInstance().getString(R.string.last_seen_hours);

                    } else if (isToday(lastPresentDate)) {
                        return Application.getInstance().getString(
                                R.string.last_seen_today,
                                new SimpleDateFormat("HH:mm", locale).format(lastPresentDate)
                        );

                    } else if (isYesterday(lastPresentDate)) {
                        return Application.getInstance().getString(
                                R.string.last_seen_yesterday,
                                new SimpleDateFormat("HH:mm", locale).format(lastPresentDate)
                        );

                    } else if (timeAgo < TimeUnit.DAYS.toSeconds(7)) {
                        return Application.getInstance().getString(
                                R.string.last_seen_on_week,
                                DatesUtilsKt.getDayOfWeek(lastPresentDate, locale),
                                new SimpleDateFormat("HH:mm", locale).format(lastPresentDate)
                        );

                    } else if (isCurrentYear(lastPresentDate)) {
                        return Application.getInstance().getString(
                                R.string.last_seen_date,
                                new SimpleDateFormat("d MMMM", locale).format(lastPresentDate)
                        );

                    } else if (!isCurrentYear(lastPresentDate)) {
                        return Application.getInstance().getString(
                                R.string.last_seen_date,
                                new SimpleDateFormat("d MMMM yyyy", locale).format(lastPresentDate)
                        );
                    }
                }
            } catch (ParseException e) {
                LogManager.exception("StringUtils", e);
            }
            return Application.getInstance().getString(R.string.unavailable);
        } else {
            return Application.getInstance().getString(R.string.account_state_connected); // Online
        }
    }

}
