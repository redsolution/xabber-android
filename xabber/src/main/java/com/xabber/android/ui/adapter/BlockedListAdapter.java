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
import com.xabber.android.data.entity.ContactJid;
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
    private static final int GROUP_INVITE_SUMMARY_FOOTER = 2;
    private AccountJid account;
    @SuppressWarnings("WeakerAccess")
    List<ContactJid> blockedContacts;

    @SuppressWarnings("WeakerAccess")
    List<ContactJid> groupInvites;

    @SuppressWarnings("WeakerAccess")
    @Nullable OnBlockedContactClickListener listener;

    @SuppressWarnings("WeakerAccess")
    Set<ContactJid> checkedContacts;

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
        } else if (viewType == 2) {
            return new BlockListGroupSummaryVH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_block_group_invites_summary, parent, false));
        } else {
            return new BlockListItemViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_block, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BlockListItemViewHolder) {
            final BlockListItemViewHolder viewHolder = (BlockListItemViewHolder) holder;
            final ContactJid contact = blockedContacts.get(position);

            final AbstractContact rosterContact = RosterManager.getInstance().getBestContact(account, contact);
            final AbstractChat abstractChat = MessageManager.getInstance().getChat(account, contact);
            String name;

            if (viewHolder.avatar != null) {
                viewHolder.avatar.setImageDrawable(rosterContact.getAvatar());
            }

            if (abstractChat != null && abstractChat.isGroupchat() || currentBlockListState == BlockedListActivity.GROUP_INVITES) {
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
        } else if (holder instanceof BlockListGroupInvitesVH) {
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
        } else if (currentBlockListState == BlockedListActivity.GROUP_INVITES && position == blockedContacts.size()) {
            return GROUP_INVITE_SUMMARY_FOOTER;
        } else {
            return BLOCKED_CONTACT;
        }
    }

    @Override
    public int getItemCount() {
        return blockedContacts.size() + addToItemCountIfNeeded();
    }

    private int addToItemCountIfNeeded() {
        return (currentBlockListState == BlockedListActivity.BLOCKED_LIST && hasGroupInvites)
                || currentBlockListState == BlockedListActivity.GROUP_INVITES ? 1 : 0;
    }

    @Override
    public void onChange() {
        blockedContacts.clear();
        groupInvites.clear();
        hasGroupInvites = false;
        final Collection<ContactJid> blockedContacts = BlockingManager.getInstance().getCachedBlockedContacts(account);
        if (blockedContacts != null) {
            // list of blocked contacts after filtering out group invites.
            Collection<ContactJid> newBlockedContacts = new ArrayList<>();
            // list of group invites
            Collection<ContactJid> groupInvites = new ArrayList<>();
            for (ContactJid user : blockedContacts) {
                if (user.getJid().getResourceOrEmpty().equals(Resourcepart.EMPTY)) {
                    // Any contact with a BareJid block address is considered as a
                    // normal blocked contact, since our clients normally block with BareJids
                    newBlockedContacts.add(user);
                } else {
                    // And contacts with resources are considered group invites, since we use
                    // the blocking mechanism to differentiate between new and old invitations
                    groupInvites.add(user);
                    hasGroupInvites = true;
                }
            }

            if (hasGroupInvites) {
                this.groupInvites.addAll(groupInvites);
            }

            if (currentBlockListState == BlockedListActivity.BLOCKED_LIST) {
                this.blockedContacts.addAll(newBlockedContacts);
            } else {
                this.blockedContacts.addAll(groupInvites);
            }
        }

        // remove checked contacts not containing in new blocked list
        final Iterator<ContactJid> iterator = checkedContacts.iterator();
        while (iterator.hasNext()) {
            final ContactJid next = iterator.next();
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

            ContactJid contactJid = blockedContacts.get(adapterPosition);

            if (checkedContacts.contains(contactJid)) {
                checkedContacts.remove(contactJid);
                checkBox.setChecked(false);
            } else {
                checkedContacts.add(contactJid);
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

    private class BlockListGroupSummaryVH extends RecyclerView.ViewHolder {
        BlockListGroupSummaryVH(View view) {
            super(view);
        }
    }

    public void setBlockedListState(@BlockedListState int state) {
        this.currentBlockListState = state;
    }

    @BlockedListState
    public int getCurrentBlockListState() {
        return currentBlockListState;
    }

    public ArrayList<ContactJid> getCheckedContacts() {
        return new ArrayList<>(checkedContacts);
    }

    public ArrayList<ContactJid> getBlockedContacts() {
        return new ArrayList<>(blockedContacts);
    }

    public void setCheckedContacts(List<ContactJid> checkedContacts) {
        this.checkedContacts.clear();
        this.checkedContacts.addAll(checkedContacts);
    }
}
