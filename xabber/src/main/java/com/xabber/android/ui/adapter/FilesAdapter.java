package com.xabber.android.ui.adapter;

import static com.xabber.android.ui.helper.AndroidUtilsKt.dipToPx;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager;
import com.xabber.android.data.extension.references.mutable.voice.VoiceMessagePresenterManager;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.data.filedownload.FileCategory;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.text.DatesUtilsKt;
import com.xabber.android.ui.widget.PlayerVisualizerView;

import org.apache.commons.io.FileUtils;

import java.util.Locale;

import io.realm.RealmList;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

    private RealmList<AttachmentRealmObject> items;
    private FileListListener listener;
    private Long timestamp;

    public interface FileListListener {
        void onFileClick(int position);
        void onVoiceClick(int position, String attachmentId, boolean saved, Long timestamp);
        void onVoiceProgressClick(int position, String attachmentId, Long timestamp, int current, int max);
        void onFileLongClick(AttachmentRealmObject attachmentRealmObject, View caller);
        void onDownloadCancel();
        void onDownloadError(String error);
    }

    public FilesAdapter(RealmList<AttachmentRealmObject> items, Long timestamp, FileListListener listener) {
        this.items = items;
        this.timestamp = timestamp;
        this.listener = listener;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_message, parent, false);
        return new FileViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(final FileViewHolder holder, final int position) {
        final AttachmentRealmObject attachmentRealmObject = items.get(position);

        holder.attachmentId = attachmentRealmObject.getUniqueId();

        if (attachmentRealmObject.isVoice()) {
            holder.voiceMessage = true;
            holder.subscribeForAudioProgress();

            StringBuilder voiceText = new StringBuilder();
            voiceText.append(Application.getInstance().getResources().getString(R.string.voice_message));
            if (attachmentRealmObject.getDuration() != null && attachmentRealmObject.getDuration() != 0) {
                voiceText.append(
                        String.format(
                                Locale.getDefault(),
                                ", %s",
                                DatesUtilsKt.getDurationStringForVoiceMessage(null, attachmentRealmObject.getDuration())
                        )
                );
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.fileInfoLayout.getLayoutParams();
                int width = dipToPx(140, holder.fileInfoLayout.getContext());
                if (attachmentRealmObject.getDuration() < 10) {
                    lp.width = width + dipToPx(6 * attachmentRealmObject.getDuration(), holder.fileInfoLayout.getContext());
                } else {
                    lp.width = width + dipToPx(60, holder.fileInfoLayout.getContext());
                }
                holder.fileInfoLayout.setLayoutParams(lp);
            }
            holder.tvFileName.setText(voiceText);

            Long size = attachmentRealmObject.getFileSize();

            if (attachmentRealmObject.getFilePath() != null) {
                holder.tvFileName.setVisibility(View.GONE);
                holder.tvFileSize.setText(
                        (attachmentRealmObject.getDuration()!= null && attachmentRealmObject.getDuration() != 0) ?
                                DatesUtilsKt.getDurationStringForVoiceMessage(
                                        0L, attachmentRealmObject.getDuration()
                                ) : FileUtils.byteCountToDisplaySize(size != null ? size : 0)
                );
                VoiceMessagePresenterManager.getInstance().sendWaveDataIfSaved(attachmentRealmObject.getFilePath(), holder.audioVisualizer);
                holder.audioVisualizer.setVisibility(View.VISIBLE);
                holder.audioVisualizer.setOnTouchListener(new PlayerVisualizerView.onProgressTouch() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_MOVE:
                                if (VoiceManager.getInstance().playbackInProgress(holder.attachmentId, timestamp)) {
                                    LogManager.d("TOUCH", "down/move super");
                                    return super.onTouch(view, motionEvent);
                                }
                                else {
                                    LogManager.d("TOUCH", "down/move");
                                    ((PlayerVisualizerView)view).updatePlayerPercent(0, true);
                                    return true;
                                }
                            case MotionEvent.ACTION_UP:
                                if (VoiceManager.getInstance().playbackInProgress(holder.attachmentId, timestamp))
                                    listener.onVoiceProgressClick(holder.getAdapterPosition(), holder.attachmentId, timestamp, (int)motionEvent.getX(), view.getWidth());
                                LogManager.d("TOUCH", "up super");
                                return super.onTouch(view, motionEvent);
                        }
                        LogManager.d("TOUCH", "empty");
                        return super.onTouch(view, motionEvent);
                    }
                });
            } else {
                holder.tvFileSize.setText(FileUtils.byteCountToDisplaySize(size != null ? size : 0));
                if (SettingsManager.chatsAutoDownloadVoiceMessage())
                    listener.onFileClick(position);
            }
            holder.ivFileIcon.setImageResource(R.drawable.ic_play);
        } else {
            // set file icon
            holder.voiceMessage = false;
            holder.tvFileName.setText(attachmentRealmObject.getTitle());
            Long size = attachmentRealmObject.getFileSize();
            holder.tvFileSize.setText(FileUtils.byteCountToDisplaySize(size != null ? size : 0));
            holder.ivFileIcon.setImageResource(attachmentRealmObject.getFilePath() != null
                    ? getFileIconByCategory(FileCategory.determineFileCategory(attachmentRealmObject.getMimeType()))
                    : R.drawable.ic_download);
        }
        holder.ivFileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.voiceMessage)
                    listener.onVoiceClick(holder.getAdapterPosition(), holder.attachmentId, attachmentRealmObject.getFilePath()!=null, timestamp);
                else
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
                holder.subscribeForAudioProgress();
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
        boolean voiceMessage;

        final View itemView;
        final LinearLayout fileInfoLayout;
        final TextView tvFileName;
        final TextView tvFileSize;
        final ImageView ivFileIcon;
        final ProgressBar progressBar;
        final SeekBar audioProgress;
        final PlayerVisualizerView audioVisualizer;
        final ImageButton ivCancelDownload;

        public FileViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView.findViewById(R.id.file_message);
            fileInfoLayout = itemView.findViewById(R.id.fileInfoLayout);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            progressBar = itemView.findViewById(R.id.progressBar);
            audioProgress = itemView.findViewById(R.id.audioProgress);
            /*audioProgress.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return true;
                }
            });*/
            audioVisualizer = itemView.findViewById(R.id.audioVisualizer);
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

        //public void subscribeForAudioProgress() {
        //    subscriptions.add(FileInteractionFragment.PublishAudioProgress.getInstance().subscribeForProgress()
        //            .doOnNext(new Action1<FileInteractionFragment.PublishAudioProgress.AudioInfo>() {
        //                @Override
        //                public void call(FileInteractionFragment.PublishAudioProgress.AudioInfo info) {
        //                    setUpAudioProgress(info);
        //                }
        //            }).subscribe());
        //}

        public void subscribeForAudioProgress() {
            subscriptions.add(VoiceManager.PublishAudioProgress.getInstance().subscribeForProgress()
                    .doOnNext(new Action1<VoiceManager.PublishAudioProgress.AudioInfo>() {
                        @Override
                        public void call(VoiceManager.PublishAudioProgress.AudioInfo info) {
                            setUpAudioProgress(info);
                        }
                    }).subscribe());
        }

        private void setUpAudioProgress(VoiceManager.PublishAudioProgress.AudioInfo info) {
            if(info != null && info.getAttachmentIdHash() == attachmentId.hashCode()) {
                if (info.getTimestamp() != null && info.getTimestamp().equals(timestamp)) {
                    if (info.getDuration() != 0) {
                        if (info.getDuration() > 1000) {
                            audioVisualizer.updatePlayerPercent(((float) info.getCurrentPosition() / (float) info.getDuration()), false);
                            audioProgress.setMax(info.getDuration());
                            audioProgress.setProgress(info.getCurrentPosition());
                        } else {
                            audioVisualizer.updatePlayerPercent(((float) info.getCurrentPosition() / ((float) info.getDuration() * 1000)), false);
                            audioProgress.setMax(info.getDuration() * 1000);
                            audioProgress.setProgress(info.getCurrentPosition());
                        }
                        if (info.getResultCode() == VoiceManager.COMPLETED_AUDIO_PROGRESS) {
                            ivFileIcon.setImageResource(R.drawable.ic_play);
                            showProgress(false);
                            tvFileSize.setText(
                                    DatesUtilsKt.getDurationStringForVoiceMessage(
                                            0L,
                                    info.getDuration() > 1000 ? (info.getDuration() / 1000) : info.getDuration()
                                    )
                            );
                        } else if (info.getResultCode() == VoiceManager.PAUSED_AUDIO_PROGRESS) {
                            ivFileIcon.setImageResource(R.drawable.ic_play);
                            showProgress(false);
                            tvFileSize.setText(
                                    DatesUtilsKt.getDurationStringForVoiceMessage(
                                            (long) info.getCurrentPosition() / 1000,
                                            info.getDuration() > 1000 ? (info.getDuration() / 1000) : info.getDuration()
                                    )
                            );
                        } else {
                            ivFileIcon.setImageResource(R.drawable.ic_pause);
                            showProgress(false);
                            tvFileSize.setText(
                                    DatesUtilsKt.getDurationStringForVoiceMessage(
                                            (long) info.getCurrentPosition() / 1000,
                                            info.getDuration() > 1000 ? (info.getDuration() / 1000) : info.getDuration()
                                    )
                            );
                        }
                    }
                }
            }
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
