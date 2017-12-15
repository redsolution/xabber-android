package com.xabber.android.ui.adapter.contactlist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.contactlist.viewobjects.BaseRosterItemVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ChatVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ContactVO;

import java.util.ArrayList;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ContactListItemViewHolder.ContactClickListener {

    private List<ContactVO> contacts;
    private final ContactItemChatInflater inflater;
    private final Listener listener;

    public interface Listener {
        void onRecentChatClick(AbstractContact contact);
        boolean isCurrentChat(String account, String user);
    }

    public ChatListAdapter(Context context, Listener listener) {
        this.listener = listener;
        contacts = new ArrayList<>();
        inflater = new ContactItemChatInflater(context);
    }

    public void updateContacts(List<AbstractContact> contacts) {
        this.contacts = ChatVO.convert(contacts);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RosterChatViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_in_contact_list, parent, false), this);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatVO chat = (ChatVO) contacts.get(position);
        boolean isCurrent = listener.isCurrentChat(chat.getAccountJid().toString(), chat.getUserJid().toString());
        inflater.bindViewHolder((RosterChatViewHolder) holder, chat, isCurrent);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public void onContactClick(int adapterPosition) {
        AccountJid accountJid = (contacts.get(adapterPosition)).getAccountJid();
        UserJid userJid = (contacts.get(adapterPosition)).getUserJid();
        listener.onRecentChatClick(RosterManager.getInstance().getAbstractContact(accountJid, userJid));
    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        inflater.onAvatarClick(contacts.get(adapterPosition));
    }

    @Override
    public void onContactButtonClick(int adapterPosition) {}

    @Override
    public void onContactCreateContextMenu(int adapterPosition, ContextMenu menu) {}

    public Object getItem(int position) {
        return contacts.get(position);
    }

    public void removeItem(int position) {
        contacts.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    public void restoreItem(BaseRosterItemVO item, int position) {
        contacts.add(position, (ContactVO) item);
        // notify item added by position
        notifyItemInserted(position);
    }
}
