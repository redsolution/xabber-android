package com.xabber.android.ui.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.xabber.android.R;
import com.xabber.android.data.message.MessageManager;

public class ImageViewerFragment extends Fragment {

    private static final String IMAGE_PATH = "IMAGE_PATH";
    private static final String IMAGE_URL = "IMAGE_URL";
    private static final String ATTACHMENT_ID = "ATTACHMENT_ID";

    private ImageView ivPhoto;
    private ProgressBar progressBar;

    public static ImageViewerFragment newInstance(String imagePath, String imageUrl, String attachmentId) {
        ImageViewerFragment fragment = new ImageViewerFragment();
        Bundle args = new Bundle();
        args.putString(IMAGE_PATH, imagePath);
        args.putString(IMAGE_URL, imageUrl);
        args.putString(ATTACHMENT_ID, attachmentId);
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
        Bundle args = getArguments();
        if (args == null) return;

        String source;
        String uri = args.getString(IMAGE_URL);
        final String path = args.getString(IMAGE_PATH);
        if (path != null) source = path;
        else source = uri;
        final String attachmentId = args.getString(ATTACHMENT_ID);

        // find views
        ivPhoto = view.findViewById(R.id.ivPhoto);
        progressBar = view.findViewById(R.id.progressBar);

        // setup image
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(getActivity()).load(source)
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                            Target<Drawable> target, boolean isFirstResource) {
                    if (e != null) showError(e.toString());
                    if (path != null) MessageManager.setAttachmentLocalPathToNull(attachmentId);
                    progressBar.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model,
                                               Target<Drawable> target, DataSource dataSource,
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
