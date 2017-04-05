package com.xabber.android.ui.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xabber.android.R;

import java.util.ArrayList;
import java.util.List;

public class ServerInfoAdapter extends RecyclerView.Adapter {

    private List<String> serverInfoList;

    public ServerInfoAdapter() {
        this.serverInfoList = new ArrayList<>();
    }

    public void setServerInfoList(List<String> serverInfoList) {
        this.serverInfoList = serverInfoList;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ServerInfoHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_server_info, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ServerInfoHolder serverInfoHolder = (ServerInfoHolder) holder;
        String serverInfo = serverInfoList.get(position);
        serverInfoHolder.text.setText(serverInfo);
    }

    @Override
    public int getItemCount() {
        return serverInfoList.size();
    }

    private static class ServerInfoHolder extends RecyclerView.ViewHolder {

        TextView text;

        ServerInfoHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(R.id.item_server_info_text);
        }
    }
}
