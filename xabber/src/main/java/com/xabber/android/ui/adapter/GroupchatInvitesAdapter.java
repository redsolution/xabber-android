package com.xabber.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.fragment.GroupchatInfoFragment.GroupchatSelectorListItemActions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupchatInvitesAdapter extends RecyclerView.Adapter<GroupchatInvitesAdapter.GroupchatInviteViewHolder> {

    public List<String> invites;
    public Set<String> checkedInvites;

    GroupchatSelectorListItemActions listener;
    private boolean clicksAreDisabled = false;

    public GroupchatInvitesAdapter() {
        invites = new ArrayList<>();
        checkedInvites = new HashSet<>();
    }

    public void disableItemClicks(boolean disable) {
        clicksAreDisabled = disable;
    }

    public void setListener(GroupchatSelectorListItemActions listener) {
        this.listener = listener;
    }

    public void setInvites(List<String> invites) {
        this.invites.clear();
        this.invites.addAll(invites);
        notifyDataSetChanged();
    }

    public void setCheckedInvites(List<String> checkedInvites) {
        this.checkedInvites.clear();
        this.checkedInvites.addAll(checkedInvites);
        notifyDataSetChanged();
    }

    public void removeCheckedInvites(List<String> invalidInvites) {
        for (String invalidInvite : invalidInvites) {
            checkedInvites.remove(invalidInvite);
        }
        notifyDataSetChanged();
    }

    public List<String> getInvites() {
        return invites;
    }

    public Set<String> getCheckedInvites() {
        return checkedInvites;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        listener = null;
    }

    @NonNull
    @Override
    public GroupchatInviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupchatInviteViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_groupchat_invite, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GroupchatInviteViewHolder holder, int position) {
        String inviteJid = invites.get(position);

        try {
            holder.avatar.setImageDrawable(AvatarManager.getInstance()
                    .getUserAvatarForContactList(ContactJid.from(inviteJid), inviteJid));
        } catch (ContactJid.UserJidCreateException e) {
            e.printStackTrace();
        }

        holder.inviteJid.setText(inviteJid);

        holder.inviteCheckBox.setChecked(checkedInvites.contains(inviteJid));
    }

    @Override
    public int getItemCount() {
        return invites.size();
    }

    class GroupchatInviteViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final String LOG_TAG = GroupchatInviteViewHolder.class.getSimpleName();

        private ImageView avatar;
        private TextView inviteJid;
        private CheckBox inviteCheckBox;

        public GroupchatInviteViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            inviteJid = itemView.findViewById(R.id.tv_invite_jid);
            inviteCheckBox = itemView.findViewById(R.id.chk_invite);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (clicksAreDisabled) {
                return;
            }
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }

            String inviteJid = invites.get(adapterPosition);
            boolean actionIsSelect = false;
            if (checkedInvites.contains(inviteJid)) {
                checkedInvites.remove(inviteJid);
                inviteCheckBox.setChecked(false);
            } else {
                checkedInvites.add(inviteJid);
                inviteCheckBox.setChecked(true);
                actionIsSelect = true;
            }

            if (listener != null) {
                if (actionIsSelect) {
                    listener.onListItemSelected();
                } else {
                    listener.onListItemDeselected();
                }
            }
        }
    }
}
