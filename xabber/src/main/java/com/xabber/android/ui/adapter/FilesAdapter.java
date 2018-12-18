package com.xabber.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.data.filedownload.FileCategory;

import org.apache.commons.io.FileUtils;

import io.realm.RealmList;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

    private RealmList<Attachment> items;
    private FileListListener listener;

    public interface FileListListener {
        void onFileClick(int position);
        void onFileLongClick(Attachment attachment, View caller);
        void onDownloadCancel();
        void onDownloadError(String error);
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
    public void onBindViewHolder(final FileViewHolder holder, final int position) {
        Attachment attachment = items.get(position);

        holder.attachmentId = attachment.getUniqueId();

        // set file icon
        holder.tvFileName.setText(attachment.getTitle());
        Long size = attachment.getFileSize();
        holder.tvFileSize.setText(FileUtils.byteCountToDisplaySize(size != null ? size : 0));
        holder.ivFileIcon.setImageResource(attachment.getFilePath() != null
                ? getFileIconByCategory(FileCategory.determineFileCategory(attachment.getMimeType()))
                : R.drawable.ic_download);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onFileClick(position);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (items.size() > position)
                    listener.onFileLongClick(items.get(position), v);
                return true;
            }
        });

        holder.ivCancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDownloadCancel();
            }
        });

        holder.itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                holder.subscribeForDownloadProgress();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                holder.unsubscribeAll();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int getFileIconByCategory(FileCategory category) {
        switch (category) {
            case image:
                return R.drawable.ic_image;
            case audio:
                return R.drawable.ic_audio;
            case video:
                return R.drawable.ic_video;
            case document:
                return R.drawable.ic_document;
            case pdf:
                return R.drawable.ic_pdf;
            case table:
                return R.drawable.ic_table;
            case presentation:
                return R.drawable.ic_presentation;
            case archive:
                return R.drawable.ic_archive;
            default:
                return R.drawable.ic_file;
        }
    }

    class FileViewHolder extends RecyclerView.ViewHolder {

        private CompositeSubscription subscriptions = new CompositeSubscription();
        String attachmentId;

        final TextView tvFileName;
        final TextView tvFileSize;
        final ImageView ivFileIcon;
        final ProgressBar progressBar;
        final ImageButton ivCancelDownload;

        public FileViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            progressBar = itemView.findViewById(R.id.progressBar);
            ivCancelDownload = itemView.findViewById(R.id.ivCancelDownload);
        }

        public void unsubscribeAll() {
            subscriptions.clear();
        }

        public void subscribeForDownloadProgress() {
            subscriptions.add(DownloadManager.getInstance().subscribeForProgress()
                .doOnNext(new Action1<DownloadManager.ProgressData>() {
                    @Override
                    public void call(DownloadManager.ProgressData progressData) {
                        setUpProgress(progressData);
                    }
                }).subscribe());
        }

        private void setUpProgress(DownloadManager.ProgressData progressData) {
            if (progressData != null && progressData.getAttachmentId().equals(attachmentId)) {
                if (progressData.isCompleted()) {
                    showProgress(false);
                } else if (progressData.getError() != null) {
                    showProgress(false);
                    listener.onDownloadError(progressData.getError());
                } else {
                    progressBar.setProgress(progressData.getProgress());
                    showProgress(true);
                }
            } else showProgress(false);
        }

        private void showProgress(boolean show) {
            if (show) {
                progressBar.setVisibility(View.VISIBLE);
                ivCancelDownload.setVisibility(View.VISIBLE);
                ivFileIcon.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
                ivCancelDownload.setVisibility(View.GONE);
                ivFileIcon.setVisibility(View.VISIBLE);
            }
        }

    }

}
