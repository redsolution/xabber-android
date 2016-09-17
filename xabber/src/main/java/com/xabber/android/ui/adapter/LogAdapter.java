package com.xabber.android.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.database.realm.LogMessage;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;


public class LogAdapter extends RealmRecyclerViewAdapter<LogMessage, LogAdapter.LogHolder> {

    public LogAdapter(@NonNull Context context, @Nullable RealmResults<LogMessage> data, boolean autoUpdate) {
        super(context, data, autoUpdate);
    }

    @Override
    public LogHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LogHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.log_item, parent, false));
    }

    @Override
    public void onBindViewHolder(LogHolder holder, int position) {
        LogMessage logMessage = getItem(position);
        if (logMessage != null) {
            holder.logMessage.setText(logMessage.toString());
        } else {
            holder.logMessage.setText("");
        }
    }

    public static class LogHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.log_item_message)
        TextView logMessage;

        public LogHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
