package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.EmailDTO;

import java.util.ArrayList;
import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private List<EmailDTO> emails = new ArrayList();
    private Listener listener;

    public interface Listener {
        void onEmailClick(String email);
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

        holder.tvEmail.setText(emailDTO.getEmail());
        holder.ivEmail.setImageResource(emailDTO.isVerified() ? R.drawable.ic_email_confirmed :
                R.drawable.ic_email);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEmailClick(emailDTO.getEmail());
            }
        });
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    class EmailViewHolder extends RecyclerView.ViewHolder {

        final TextView tvEmail;
        final ImageView ivEmail;

        public EmailViewHolder(View itemView) {
            super(itemView);
            this.tvEmail = itemView.findViewById(R.id.tvEmail);
            this.ivEmail = itemView.findViewById(R.id.ivEmail);
        }
    }

}
