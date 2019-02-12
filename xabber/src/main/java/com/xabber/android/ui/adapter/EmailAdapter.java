package com.xabber.android.ui.adapter;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.xaccount.EmailDTO;

import java.util.ArrayList;
import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private List<EmailDTO> emails = new ArrayList();
    private Listener listener;

    public interface Listener {
        void onEmailVerifyClick(String email);
        void onEmailDeleteClick(int id);
    }

    public EmailAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<EmailDTO> emails) {
        this.emails.clear();
        this.emails.addAll(emails);
    }

    @Override
    public EmailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_email, parent, false);
        return new EmailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EmailViewHolder holder, final int position) {
        final EmailDTO emailDTO = emails.get(position);

        Resources res = Application.getInstance().getResources();
        holder.tvEmail.setText(emailDTO.getEmail());
        holder.tvStatus.setText(emailDTO.isVerified() ? R.string.title_verified_email : R.string.title_unverified_email);
        holder.tvStatus.setTextColor(emailDTO.isVerified() ? res.getColor(R.color.grey_500) : res.getColor(R.color.red_500));
        holder.tvAction.setVisibility(emailDTO.isVerified() ? View.GONE : View.VISIBLE);
        holder.ivEmail.setImageResource(emailDTO.isVerified() ? R.drawable.ic_confirmed_email_circle : R.drawable.ic_email_circle);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailDTO.isVerified())
                    listener.onEmailDeleteClick(emailDTO.getId());
                else listener.onEmailVerifyClick(emailDTO.getEmail());
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.onEmailDeleteClick(emailDTO.getId());
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    class EmailViewHolder extends RecyclerView.ViewHolder {

        final TextView tvEmail;
        final TextView tvStatus;
        final TextView tvAction;
        final ImageView ivEmail;

        public EmailViewHolder(View itemView) {
            super(itemView);
            this.tvEmail = itemView.findViewById(R.id.tvEmail);
            this.tvStatus = itemView.findViewById(R.id.tvStatus);
            this.tvAction = itemView.findViewById(R.id.tvAction);
            this.ivEmail = itemView.findViewById(R.id.ivEmail);
        }
    }

}
