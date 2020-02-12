package com.xabber.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.BlockedListActivity;
import com.xabber.android.ui.activity.BlockedListActivity.BlockedListState;
import com.xabber.android.ui.color.ColorManager;

import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlockedListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UpdatableAdapter {

    private static final int BLOCKED_CONTACT = 0;
    private static final int GROUP_INVITE = 1;

    private AccountJid account;
    @SuppressWarnings("WeakerAccess")
    List<UserJid> blockedContacts;

    @SuppressWarnings("WeakerAccess")
    List<UserJid> groupInvites;

    @SuppressWarnings("WeakerAccess")
    @Nullable OnBlockedContactClickListener listener;

    @SuppressWarnings("WeakerAccess")
    Set<UserJid> checkedContacts;

    @BlockedListState
    private int currentBlockListState;
    private boolean hasGroupInvites;

    public BlockedListAdapter(AccountJid account) {
        this.account = account;
        blockedContacts = new ArrayList<>();
        checkedContacts = new HashSet<>();
        groupInvites = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 1) {
            return new BlockListGroupInvitesVH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_block_group_invites, parent, false));
        } else {
            return new BlockListItemViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_block, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BlockListItemViewHolder) {
            final BlockListItemViewHolder viewHolder = (BlockListItemViewHolder) holder;
            final UserJid contact = blockedContacts.get(position);

            final AbstractContact rosterContact = RosterManager.getInstance().getBestContact(account, contact);
            final AbstractChat abstractChat = MessageManager.getInstance().getChat(account, contact);
            String name;

            if (viewHolder.avatar != null) {
                viewHolder.avatar.setImageDrawable(rosterContact.getAvatar());
            }

            if (abstractChat != null && abstractChat.isGroupchat()) {
                viewHolder.status.setImageResource(R.drawable.ic_groupchat_14_border);
                viewHolder.status.setVisibility(View.VISIBLE);
            } else {
                viewHolder.status.setVisibility(View.GONE);
            }
            if (rosterContact.getName().equals(contact.getBareJid().toString()) && currentBlockListState == BlockedListActivity.BLOCKED_LIST) {
                if (contact.getBareJid().isDomainBareJid()) {
                    name = Application.getInstance().getString(R.string.blocked_domain);
                } else {
                    if (abstractChat != null && abstractChat.isGroupchat()) {
                        name = Application.getInstance().getString(R.string.blocked_group);
                    } else {
                        name = Application.getInstance().getString(R.string.blocked_contact);
                    }
                }
            } else {
                name = rosterContact.getName();
            }
            viewHolder.name.setText(name);
            viewHolder.jid.setText(contact.getJid());

            viewHolder.checkBox.setChecked(checkedContacts.contains(contact));
        } else {
            BlockListGroupInvitesVH viewHolder = (BlockListGroupInvitesVH) holder;
            viewHolder.invite.setImageResource(R.drawable.ic_email_add_small);
            viewHolder.invite.setCircleBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
            viewHolder.groupInvitesCount.setText(String.valueOf(groupInvites.size()));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (hasGroupInvites && position == blockedContacts.size() && currentBlockListState == BlockedListActivity.BLOCKED_LIST) {
            return GROUP_INVITE;
        } else {
            return BLOCKED_CONTACT;
        }
    }

    @Override
    public int getItemCount() {
        return blockedContacts.size() + addToItemCountIfNeeded();
    }

    private int addToItemCountIfNeeded() {
        return currentBlockListState == BlockedListActivity.BLOCKED_LIST && hasGroupInvites ? 1 : 0;
    }

    @Override
    public void onChange() {
        blockedContacts.clear();
        groupInvites.clear();
        hasGroupInvites = false;
        final Collection<UserJid> blockedContacts = BlockingManager.getInstance().getCachedBlockedContacts(account);
        if (blockedContacts != null) {
            Collection<UserJid> filteredContacts = new ArrayList<>();
            Collection<UserJid> groupInvites = new ArrayList<>();
            for (UserJid user : blockedContacts) {
                AbstractChat chat = MessageManager.getInstance().getChat(account, user);
                if (chat != null && chat.isGroupchat()) {
                    if (user.getJid().getResourceOrEmpty().equals(Resourcepart.EMPTY)) {
                        filteredContacts.add(user);
                    } else {
                        groupInvites.add(user);
                        hasGroupInvites = true;
                    }
                } else {
                    filteredContacts.add(user);
                }
            }

            if (hasGroupInvites) {
                this.groupInvites.addAll(groupInvites);
            }

            if (currentBlockListState == BlockedListActivity.BLOCKED_LIST) {
                this.blockedContacts.addAll(filteredContacts);
            } else {
                this.blockedContacts.addAll(groupInvites);
            }
        }

        // remove checked contacts not containing in new blocked list
        final Iterator<UserJid> iterator = checkedContacts.iterator();
        while (iterator.hasNext()) {
            final UserJid next = iterator.next();
            if (!this.blockedContacts.contains(next)) {
                iterator.remove();
            }
        }

        notifyDataSetChanged();
    }

    public interface OnBlockedContactClickListener {
        void onBlockedContactClick();
        void onGroupInvitesClick();
    }

    public void setListener(@Nullable OnBlockedContactClickListener listener) {
        this.listener = listener;
    }

    private class BlockListItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private final String LOG_TAG = BlockListItemViewHolder.class.getSimpleName();
        @Nullable
        final ImageView avatar;
        final ImageView status;
        final TextView name;
        final TextView jid;
        final CheckBox checkBox;

        BlockListItemViewHolder(View view) {
            super(view);

            if (SettingsManager.contactsShowAvatars()) {
                avatar = (ImageView) view.findViewById(R.id.avatar);
                avatar.setVisibility(View.VISIBLE);
            } else {
                avatar = null;
            }

            status = (ImageView) view.findViewById(R.id.iv_status);
            name = (TextView) view.findViewById(R.id.contact_list_item_name);
            jid = (TextView) view.findViewById(R.id.contact_list_item_jid);
            checkBox = (CheckBox) view.findViewById(R.id.block_list_contact_checkbox);
            checkBox.setOnClickListener(this);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }

            UserJid userJid = blockedContacts.get(adapterPosition);

            if (checkedContacts.contains(userJid)) {
                checkedContacts.remove(userJid);
                checkBox.setChecked(false);
            } else {
                checkedContacts.add(userJid);
                checkBox.setChecked(true);
            }

            if (listener != null) {
                listener.onBlockedContactClick();
            }

        }
    }

    private class BlockListGroupInvitesVH extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private final String LOG_TAG = BlockListGroupInvitesVH.class.getSimpleName();

        private CircleImageView invite;
        private TextView groupInvitesCount;

        BlockListGroupInvitesVH(View view) {
            super(view);
            invite = view.findViewById(R.id.iv_group_invite);
            groupInvitesCount = view.findViewById(R.id.group_invites_count);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }
            if (listener != null) {
                listener.onGroupInvitesClick();
            }
        }
    }

    public void setBlockedListState(@BlockedListState int state) {
        this.currentBlockListState = state;
    }

    @BlockedListState
    public int getCurrentBlockListState() {
        return currentBlockListState;
    }

    public ArrayList<UserJid> getCheckedContacts() {
        return new ArrayList<>(checkedContacts);
    }

    public ArrayList<UserJid> getBlockedContacts() {
        return new ArrayList<>(blockedContacts);
    }

    public void setCheckedContacts(List<UserJid> checkedContacts) {
        this.checkedContacts.clear();
        this.checkedContacts.addAll(checkedContacts);
    }
}
