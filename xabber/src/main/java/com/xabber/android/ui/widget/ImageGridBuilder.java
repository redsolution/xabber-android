package com.xabber.android.ui.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.xabber.android.R;

import java.util.List;

public class ImageGridBuilder {

    public View inflateView(ViewGroup parent, int imageCount) {
        return LayoutInflater.from(parent.getContext()).inflate(getLayoutResource(imageCount), parent, false);
    }

    public void bindView(View view, List<String> imageUrls) {
        TextView tvCounter = view.findViewById(R.id.tvCounter);
        int index = 0;
        loop:
        for (String url : imageUrls) {
            if (index > 5)
                break loop;

            ImageView imageView = getImageView(view, index);
            if (imageView != null) {

                Glide.with(view.getContext())
                        .load(url)
                        .centerCrop()
                        .placeholder(R.drawable.ic_recent_image_placeholder)
                        .error(R.drawable.ic_recent_image_placeholder)
                        .into(imageView);
            }
            index++;
        }

        if (tvCounter != null) {
            if (imageUrls.size() > 6) {
                tvCounter.setText("+" + (imageUrls.size() - 6));
                tvCounter.setVisibility(View.VISIBLE);
            } else tvCounter.setVisibility(View.GONE);
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
