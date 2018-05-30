package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.database.messagerealm.Attachment;

import org.apache.commons.io.FileUtils;

import io.realm.RealmList;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

    private RealmList<Attachment> items;
    private FileListListener listener;

    interface FileListListener {
        void onFileClick(int position);
    }

    public FilesAdapter(RealmList<Attachment> items, FileListListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_message, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, final int position) {
        Attachment attachment = items.get(position);

        holder.tvFileName.setText(attachment.getTitle());
        Long size = attachment.getFileSize();
        holder.tvFileSize.setText(FileUtils.byteCountToDisplaySize(size != null ? size : 0));
        holder.ivFileIcon.setImageResource(attachment.getFilePath() != null ? R.drawable.ic_file : R.drawable.ic_download);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onFileClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {

        final TextView tvFileName;
        final TextView tvFileSize;
        final ImageView ivFileIcon;

        public FileViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
        }

    }

}
