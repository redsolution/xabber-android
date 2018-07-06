package com.xabber.android.ui.widget;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.xabber.android.R;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.message.MessageManager;

import io.realm.Realm;
import io.realm.RealmList;

public class ImageGridBuilder {

    public View inflateView(ViewGroup parent, int imageCount) {
        return LayoutInflater.from(parent.getContext()).inflate(getLayoutResource(imageCount), parent, false);
    }

    public void bindView(View view, RealmList<Attachment> attachments, View.OnClickListener clickListener) {

        if (attachments.size() == 1) {
            ImageView imageView = getImageView(view, 0);
            bindOneImage(attachments.get(0), view, imageView);
            imageView.setOnClickListener(clickListener);
        } else {
            TextView tvCounter = view.findViewById(R.id.tvCounter);
            int index = 0;
            loop:
            for (Attachment attachment : attachments) {
                if (index > 5)
                    break loop;

                ImageView imageView = getImageView(view, index);
                if (imageView != null) {
                    bindImage(attachment, view, imageView);
                    imageView.setOnClickListener(clickListener);
                }
                index++;
            }

            if (tvCounter != null) {
                if (attachments.size() > 6) {
                    tvCounter.setText("+" + (attachments.size() - 6));
                    tvCounter.setVisibility(View.VISIBLE);
                } else tvCounter.setVisibility(View.GONE);
            }
        }
    }

    private void bindImage(Attachment attachment, View parent, ImageView imageView) {
        String uri = attachment.getFilePath();
        if (uri == null || uri.isEmpty())
            uri = attachment.getFileUrl();

        Glide.with(parent.getContext())
                .load(uri)
                .centerCrop()
                .placeholder(R.drawable.ic_recent_image_placeholder)
                .error(R.drawable.ic_recent_image_placeholder)
                .into(imageView);
    }

    private void bindOneImage(final Attachment attachment, View parent, final ImageView imageView) {
        String imagePath = attachment.getFilePath();
        String imageUrl = attachment.getFileUrl();
        Integer imageWidth = attachment.getImageWidth();
        Integer imageHeight = attachment.getImageHeight();
        final String uniqId = attachment.getUniqueId();

        if (imagePath != null) {
            boolean result = FileManager.loadImageFromFile(parent.getContext(), imagePath, imageView);

            if (!result) {
                MessageManager.setAttachmentLocalPathToNull(uniqId);
            }
        } else {
            final ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();

            if (imageWidth != null && imageHeight != null) {
                FileManager.scaleImage(layoutParams, imageHeight, imageWidth);
                Glide.with(parent.getContext())
                        .load(imageUrl)
                        .into(imageView);
            } else {

                Glide.with(parent.getContext())
                        .load(imageUrl)
                        .asBitmap()
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                final int width = resource.getWidth();
                                final int height = resource.getHeight();

                                if (width <= 0 || height <= 0) {
                                    return;
                                }

                                final Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
                                realm.executeTransactionAsync(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        Attachment first = realm.where(Attachment.class)
                                                .equalTo(Attachment.Fields.UNIQUE_ID, uniqId)
                                                .findFirst();
                                        if (first != null) {
                                            first.setImageWidth(width);
                                            first.setImageHeight(height);
                                        }
                                    }
                                });
                                FileManager.scaleImage(layoutParams, height, width);
                                imageView.setImageBitmap(resource);
                            }
                        });
            }
        }
    }

    private int getLayoutResource(int imageCount) {
        switch (imageCount) {
            case 1:
                return R.layout.image_grid_1;
            case 2:
                return R.layout.image_grid_2;
            case 3:
                return R.layout.image_grid_3;
            case 4:
                return R.layout.image_grid_4;
            case 5:
                return R.layout.image_grid_5;
            default:
                return R.layout.image_grid_6;
        }
    }

    private ImageView getImageView(View view, int index) {
        switch (index) {
            case 1:
                return view.findViewById(R.id.ivImage1);
            case 2:
                return view.findViewById(R.id.ivImage2);
            case 3:
                return view.findViewById(R.id.ivImage3);
            case 4:
                return view.findViewById(R.id.ivImage4);
            case 5:
                return view.findViewById(R.id.ivImage5);
            default:
                return view.findViewById(R.id.ivImage0);
        }
    }
}
