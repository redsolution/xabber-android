package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
    private Context context;

    public SessionAdapter(Context context) {
        this.items = new ArrayList<>();
        this.context = context;
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
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTerminateSessionDialog();
            }
        });
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

    private void showTerminateSessionDialog() {
        new AlertDialog.Builder(context)
            .setMessage(R.string.terminate_session_title)
            .setPositiveButton(R.string.button_terminate, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            })
            .create().show();
    }

}
