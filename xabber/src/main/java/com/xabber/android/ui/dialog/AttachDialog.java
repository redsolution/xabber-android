package com.xabber.android.ui.dialog;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.ui.adapter.RecentImagesAdapter;

import java.util.ArrayList;

public class AttachDialog extends BottomSheetDialogFragment implements RecentImagesAdapter.Listener {

    private RecentImagesAdapter recentImagesAdapter;

    public static AttachDialog newInstance() {
        return new AttachDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.attach_dialog, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.attach_recent_images_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

        recentImagesAdapter = new RecentImagesAdapter(this);
        recentImagesAdapter.loadGalleryPhotosAlbums();
        recyclerView.setAdapter(recentImagesAdapter);

//        RecyclerView rvFiles = view.findViewById(R.id.rvFiles);
//
//        List<PhotoVO> items = new ArrayList<>();
//        items.add(new ButtonVO("test"));
//        for (int i = 0; i < 50; i++) {
//            items.add(new PhotoVO("test"));
//        }
//        PhotoGalleryAdapter adapter = new PhotoGalleryAdapter(items, getActivity());
//
//        rvFiles.setAdapter(adapter);
//        rvFiles.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        return view;
    }

    @Override
    public void onRecentImagesSelected() {

    }
}