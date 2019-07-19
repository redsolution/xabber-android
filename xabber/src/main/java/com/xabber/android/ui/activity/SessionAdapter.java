package com.xabber.android.ui.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.extension.xtoken.SessionVO;

import java.util.ArrayList;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionHolder> {

    private List<SessionVO> items;

    public SessionAdapter() {
        this.items = new ArrayList<>();
    }

    public void setItems(List<SessionVO> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SessionHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SessionHolder holder, int position) {
        SessionVO session = items.get(position);
        holder.tvClient.setText(session.getClient());
        holder.tvDevice.setText(session.getDevice());
        holder.tvIPAddress.setText(session.getIp());
        holder.tvDate.setText(session.getLastAuth());
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
