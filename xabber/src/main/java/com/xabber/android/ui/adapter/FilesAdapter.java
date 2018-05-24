package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.database.messagerealm.Attachment;

import org.apache.commons.io.FileUtils;

import io.realm.RealmList;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

    RealmList<Attachment> items;

    public FilesAdapter(RealmList<Attachment> items) {
        this.items = items;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_message, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int position) {
        Attachment attachment = items.get(position);

        holder.tvFileName.setText(attachment.getTitle());
        Long size = attachment.getFileSize();
        holder.tvFileSize.setText(FileUtils.byteCountToDisplaySize(size != null ? size : 0));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {

        final TextView tvFileName;
        final TextView tvFileSize;

        public FileViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
        }

    }

}
