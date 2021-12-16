package com.xabber.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.devices.SessionVO;
import com.xabber.android.data.roster.PresenceManager;

import java.util.ArrayList;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionHolder> {

    private List<SessionVO> items;
    private final Listener listener;
    private final AccountJid accountJid;

    public interface Listener {
        void onItemClick(SessionVO token);
    }

    public SessionAdapter(Listener listener) {
        this.items = new ArrayList<>();
        this.listener = listener;
        this.accountJid = null;
    }

    public SessionAdapter(AccountJid accountJid, Listener listener) {
        this.items = new ArrayList<>();
        this.listener = listener;
        this.accountJid = accountJid;
    }

    public void setItems(List<SessionVO> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public List<String> getItemsIDs() {
        List<String> ids = new ArrayList<>();
        for (SessionVO session : items) {
            ids.add(session.getUid());
        }
        return ids;
    }

    public void remove(SessionVO sessionVO) {
        int position = getPosition(sessionVO);
        items.remove(position);
        notifyItemRemoved(position);
    }

    public boolean isListEmpty() {
        return items.isEmpty();
    }

    public int getPosition(SessionVO sessionVO) {
        return items.indexOf(sessionVO);
    }

    @NonNull
    @Override
    public SessionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SessionHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SessionHolder holder, int position) {
        final SessionVO session = items.get(position);
        if (session.getDescription() != null && !session.getDescription().isEmpty()) {
            holder.tvClient.setText(session.getDescription());
        } else {
            holder.tvClient.setText(session.getClient());
        }
        holder.tvDevice.setText(session.getClient() + ", " + session.getDevice());
        holder.tvIPAddress.setText(session.getIp());

        if (accountJid != null) {
            holder.tvDate.setText(
                    session.createSmartLastSeen(accountJid, PresenceManager.INSTANCE).toLowerCase()
            );
        } else {
            holder.tvDate.setText(session.getLastAuth());
        }

        holder.itemView.setOnClickListener(view -> listener.onItemClick(session));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class SessionHolder extends RecyclerView.ViewHolder {
        TextView tvClient;
        TextView tvDevice;
        TextView tvIPAddress;
        TextView tvDate;

        SessionHolder(View itemView) {
            super(itemView);

            tvClient = (TextView) itemView.findViewById(R.id.tvClient);
            tvDevice = (TextView) itemView.findViewById(R.id.tvDevice);
            tvIPAddress = (TextView) itemView.findViewById(R.id.tvIPAddress);
            tvDate = (TextView) itemView.findViewById(R.id.tvDate);
        }
    }

}
