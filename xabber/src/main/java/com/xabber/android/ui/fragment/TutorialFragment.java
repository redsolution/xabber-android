package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;

/**
 * Created by valery.miller on 14.09.17.
 */

public class TutorialFragment extends Fragment {

    private final static String TITLEID = "titleId";
    private final static String DESCRIPTIONID = "descriptionId";
    private final static String IMAGEID = "imageId";

    private int title;
    private int description;
    private int imageId;

    public static TutorialFragment newInstance(int titleId, int descriptionId, int imageId) {
        TutorialFragment fragment = new TutorialFragment();
        Bundle args = new Bundle();
        args.putInt(TITLEID, titleId);
        args.putInt(DESCRIPTIONID, descriptionId);
        args.putInt(IMAGEID, imageId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.title = getArguments().getInt(TITLEID);
        this.description = getArguments().getInt(DESCRIPTIONID);
        this.imageId = getArguments().getInt(IMAGEID);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutorial, container, false);

        TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
        TextView tvDescription = (TextView) view.findViewById(R.id.tvDescription);
        ImageView ivImage = (ImageView) view.findViewById(R.id.ivImage);

        tvTitle.setText(getString(title));
        tvDescription.setText(getString(description));
        ivImage.setImageResource(imageId);

        return view;
    }
}
