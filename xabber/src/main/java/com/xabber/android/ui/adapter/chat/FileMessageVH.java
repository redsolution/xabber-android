package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.ui.adapter.FilesAdapter;
import com.xabber.android.ui.widget.ImageGridBuilder;

import io.realm.Realm;
import io.realm.RealmList;

public class FileMessageVH extends MessageVH {

    public FileMessageVH(View itemView, MessageClickListener onClickListener, int appearance) {
        super(itemView, onClickListener, appearance);
    }

    public void bind(MessageItem messageItem, boolean isMUC, boolean showOriginalOTR,
                     Context context, boolean unread) {
        super.bind(messageItem, isMUC, showOriginalOTR, context, unread);
        setupImageOrFile(messageItem, context);
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
                        .into(new SimpleTarget<Bitmap>() {
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
}
