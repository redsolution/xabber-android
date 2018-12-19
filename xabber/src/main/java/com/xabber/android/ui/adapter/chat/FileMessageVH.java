package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.FilesAdapter;
import com.xabber.android.ui.widget.ImageGridBuilder;

import io.realm.Realm;
import io.realm.RealmList;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class FileMessageVH extends MessageVH
        implements FilesAdapter.FileListListener, View.OnClickListener {

    private static final String LOG_TAG = FileMessageVH.class.getSimpleName();

    private CompositeSubscription subscriptions = new CompositeSubscription();
    private FileListener listener;

    final TextView messageFileInfo;
    final ProgressBar progressBar;
    final ImageView messageImage;
    final View fileLayout;
    final RecyclerView rvFileList;
    final FrameLayout imageGridContainer;
    final ProgressBar uploadProgressBar;
    final ImageButton ivCancelUpload;

    public interface FileListener {
        void onImageClick(int messagePosition, int attachmentPosition, String messageUID);
        void onFileClick(int messagePosition, int attachmentPosition, String messageUID);
        void onFileLongClick(Attachment attachment, View caller);
        void onDownloadCancel();
        void onUploadCancel();
        void onDownloadError(String error);
    }

    public FileMessageVH(View itemView, MessageClickListener messageListener,
            MessageLongClickListener longClickListener,
            FileListener listener, int appearance) {
        super(itemView, messageListener, longClickListener, appearance);
        this.listener = listener;

        messageImage = itemView.findViewById(R.id.message_image);
        fileLayout = itemView.findViewById(R.id.fileLayout);
        rvFileList = itemView.findViewById(R.id.rvFileList);
        imageGridContainer = itemView.findViewById(R.id.imageGridContainer);
        uploadProgressBar = itemView.findViewById(R.id.uploadProgressBar);
        ivCancelUpload = itemView.findViewById(R.id.ivCancelUpload);
        progressBar = itemView.findViewById(R.id.message_progress_bar);
        messageFileInfo = itemView.findViewById(R.id.message_file_info);

        if (ivCancelUpload != null) ivCancelUpload.setOnClickListener(this);
        if (messageImage != null) messageImage.setOnClickListener(this);
    }

    public void bind(MessageItem messageItem, MessagesAdapter.MessageExtraData extraData) {
        super.bind(messageItem, extraData);
        setupImageOrFile(messageItem, extraData.getContext());
    }

    protected void setupImageOrFile(MessageItem messageItem, Context context) {
        fileLayout.setVisibility(View.GONE);
        messageImage.setVisibility(View.GONE);
        imageGridContainer.removeAllViews();
        imageGridContainer.setVisibility(View.GONE);
        messageText.setVisibility(View.VISIBLE);

        if (messageItem.haveAttachments()) {
            setUpImage(messageItem.getAttachments());
            setUpFile(messageItem.getAttachments(), context);
        } else if (messageItem.isImage()) {
            prepareImage(messageItem, context);
        }
    }

    private void prepareImage(MessageItem messageItem, Context context) {
        String filePath = messageItem.getFilePath();
        Integer imageWidth = messageItem.getImageWidth();
        Integer imageHeight = messageItem.getImageHeight();
        String imageUrl = messageItem.getText();
        final String uniqueId = messageItem.getUniqueId();
        setUpImage(filePath, imageUrl, uniqueId, imageWidth, imageHeight, context);
    }

    private void setUpImage(RealmList<Attachment> attachments) {
        final ImageGridBuilder gridBuilder = new ImageGridBuilder();

        if (!SettingsManager.connectionLoadImages()) return;

        RealmList<Attachment> imageAttachments = new RealmList<>();
        for (Attachment attachment : attachments) {
            if (attachment.isImage()) imageAttachments.add(attachment);
        }

        if (imageAttachments.size() > 0) {
            View imageGridView = gridBuilder.inflateView(imageGridContainer, imageAttachments.size());
            gridBuilder.bindView(imageGridView, imageAttachments, this);

            imageGridContainer.addView(imageGridView);
            imageGridContainer.setVisibility(View.VISIBLE);
            messageText.setVisibility(View.GONE);
        }
    }

    private void setUpFile(RealmList<Attachment> attachments, Context context) {
        RealmList<Attachment> fileAttachments = new RealmList<>();
        for (Attachment attachment : attachments) {
            if (!attachment.isImage()) fileAttachments.add(attachment);
        }

        if (fileAttachments.size() > 0) {
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
            rvFileList.setLayoutManager(layoutManager);
            FilesAdapter adapter = new FilesAdapter(fileAttachments, this);
            rvFileList.setAdapter(adapter);
            messageText.setVisibility(View.GONE);
            fileLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setUpImage(String imagePath, String imageUrl, final String uniqueId, Integer imageWidth,
                            Integer imageHeight, Context context) {

        if (!SettingsManager.connectionLoadImages()) return;

        if (imagePath != null) {
            boolean result = FileManager.loadImageFromFile(context, imagePath, messageImage);

            if (result) {
                messageImage.setVisibility(View.VISIBLE);
                messageText.setVisibility(View.GONE);
            } else {
                final Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
                realm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        MessageItem first = realm.where(MessageItem.class)
                                .equalTo(MessageItem.Fields.UNIQUE_ID, uniqueId)
                                .findFirst();
                        if (first != null) {
                            first.setFilePath(null);
                        }
                    }
                });
            }
        } else {
            final ViewGroup.LayoutParams layoutParams = messageImage.getLayoutParams();

            if (imageWidth != null && imageHeight != null) {
                FileManager.scaleImage(layoutParams, imageHeight, imageWidth);
                Glide.with(context)
                        .load(imageUrl)
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                messageImage.setVisibility(View.GONE);
                                messageText.setVisibility(View.VISIBLE);
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(messageImage);

                messageImage.setVisibility(View.VISIBLE);
                messageText.setVisibility(View.GONE);
            } else {

                Glide.with(context)
                        .load(imageUrl)
                        .asBitmap()
                        .placeholder(R.drawable.ic_recent_image_placeholder)
                        .error(R.drawable.ic_recent_image_placeholder)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onLoadStarted(Drawable placeholder) {
                                super.onLoadStarted(placeholder);
                                messageImage.setImageDrawable(placeholder);
                                messageImage.setVisibility(View.VISIBLE);
                                messageText.setVisibility(View.GONE);
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                                messageImage.setImageDrawable(errorDrawable);
                                messageImage.setVisibility(View.VISIBLE);
                                messageText.setVisibility(View.GONE);
                            }

                            @Override
                            public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                final int width = resource.getWidth();
                                final int height = resource.getHeight();

                                if (width <= 0 || height <= 0) {
                                    messageImage.setVisibility(View.GONE);
                                    messageText.setVisibility(View.VISIBLE);
                                    return;
                                }

                                final Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
                                realm.executeTransactionAsync(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        MessageItem first = realm.where(MessageItem.class)
                                                .equalTo(MessageItem.Fields.UNIQUE_ID, uniqueId)
                                                .findFirst();
                                        if (first != null) {
                                            first.setImageWidth(width);
                                            first.setImageHeight(height);
                                        }
                                    }
                                });


                                FileManager.scaleImage(layoutParams, height, width);

                                messageImage.setImageBitmap(resource);

                                messageImage.setVisibility(View.VISIBLE);
                                messageText.setVisibility(View.GONE);
                            }
                        });
            }
        }
    }

    /** File list Listener */

    @Override
    public void onFileClick(int attachmentPosition) {
        int messagePosition = getAdapterPosition();
        if (messagePosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return;
        }
        listener.onFileClick(messagePosition, attachmentPosition, messageId);
    }

    @Override
    public void onFileLongClick(Attachment attachment, View caller) {
        listener.onFileLongClick(attachment, caller);
    }

    @Override
    public void onDownloadCancel() {
        listener.onDownloadCancel();
    }

    @Override
    public void onDownloadError(String error) {
        listener.onDownloadError(error);
    }

    @Override
    public void onClick(View v) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return;
        }

        switch (v.getId()) {
            case R.id.ivImage0:
                listener.onImageClick(adapterPosition, 0, messageId);
                break;
            case R.id.ivImage1:
                listener.onImageClick(adapterPosition, 1, messageId);
                break;
            case R.id.ivImage2:
                listener.onImageClick(adapterPosition, 2, messageId);
                break;
            case R.id.ivImage3:
                listener.onImageClick(adapterPosition, 3, messageId);
                break;
            case R.id.ivImage4:
                listener.onImageClick(adapterPosition, 4, messageId);
                break;
            case R.id.ivImage5:
                listener.onImageClick(adapterPosition, 5, messageId);
                break;
            case R.id.message_image:
                listener.onImageClick(adapterPosition, 0, messageId);
                break;
            case R.id.ivCancelUpload:
                listener.onUploadCancel();
                break;
            default:
                super.onClick(v);
        }
    }

    /** Upload progress subscription */

    protected void subscribeForUploadProgress(final Context context) {
        subscriptions.add(HttpFileUploadManager.getInstance().subscribeForProgress()
                .doOnNext(new Action1<HttpFileUploadManager.ProgressData>() {
                    @Override
                    public void call(HttpFileUploadManager.ProgressData progressData) {
                        setUpProgress(context, progressData);
                    }
                }).subscribe());
    }

    protected void unsubscribeAll() {
        subscriptions.clear();
    }

    private void setUpProgress(Context context, HttpFileUploadManager.ProgressData progressData) {
        if (progressData != null && messageId.equals(progressData.getMessageId())) {
            if (progressData.isCompleted()) {
                showProgress(false);
            } else if (progressData.getError() != null) {
                showProgress(false);
                listener.onDownloadError(progressData.getError());
            } else {
                if (uploadProgressBar != null) uploadProgressBar.setProgress(progressData.getProgress());
                if (messageFileInfo != null)
                    messageFileInfo.setText(context.getString(R.string.uploaded_files_count,
                            progressData.getProgress() + "/" + progressData.getFileCount()));
                showProgress(true);
            }
        } else showProgress(false);
    }

    private void showProgress(boolean show) {
        if (uploadProgressBar != null) uploadProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (ivCancelUpload != null) ivCancelUpload.setVisibility(show ? View.VISIBLE : View.GONE);
        if (messageFileInfo != null) messageFileInfo.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
