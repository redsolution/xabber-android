package com.xabber.android.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.xabber.android.R;

public class ImageViewerFragment extends Fragment {

    private static final String IMAGE_PATH = "IMAGE_PATH";
    private static final String IMAGE_URL = "IMAGE_URL";

    private ImageView ivPhoto;
    private ProgressBar progressBar;

    public static ImageViewerFragment newInstance(String imagePath, String imageUrl) {
        ImageViewerFragment fragment = new ImageViewerFragment();
        Bundle args = new Bundle();
        args.putString(IMAGE_PATH, imagePath);
        args.putString(IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        // create view
        View view = inflater.inflate(R.layout.fragment_image_viewer, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get params
        String uri = getArguments().getString(IMAGE_PATH);
        if (uri == null)
            uri = getArguments().getString(IMAGE_URL);

        // find views
        ivPhoto = view.findViewById(R.id.ivPhoto);
        progressBar = view.findViewById(R.id.progressBar);

        // setup image
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(getActivity()).load(uri)
            .listener(new RequestListener<String, GlideDrawable>() {
                @Override
                public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                           boolean isFirstResource) {
                    showError(e.toString());
                    progressBar.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model,
                                               Target<GlideDrawable> target, boolean isFromMemoryCache,
                                               boolean isFirstResource) {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }
            })
            .into(ivPhoto);
    }

    public void showError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
}
