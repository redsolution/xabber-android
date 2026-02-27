package com.xabber.android.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.xabber.android.R;
import com.xabber.android.ui.adapter.RecentImagesAdapter;
import com.xabber.android.ui.helper.PermissionsRequester;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AttachDialog extends BottomSheetDialogFragment implements RecentImagesAdapter.Listener,
        View.OnClickListener {

    private RecentImagesAdapter recentImagesAdapter;
    private TextView attachSendButtonText;
    private ImageView attachSendButtonIcon;
    private View view;
    private RecyclerView galleryRecyclerView;

    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 21;

    private Listener listener;

    public interface Listener {
        void onRecentPhotosSend(List<String> paths);
        void onGalleryClick();
        void onFilesClick();
        void onCameraClick();
        void onLocationClick();
    }

    public static AttachDialog newInstance(Listener listener) {
        AttachDialog dialog = new AttachDialog();
        dialog.setListener(listener);
        return dialog;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.attach_dialog, container, false);

        view.findViewById(R.id.attach_send_button).setOnClickListener(this);
        view.findViewById(R.id.attach_file_button).setOnClickListener(this);
        view.findViewById(R.id.attach_camera_button).setOnClickListener(this);
        view.findViewById(R.id.attach_gallery_button).setOnClickListener(this);
        view.findViewById(R.id.attach_location_button).setOnClickListener(this);

        attachSendButtonText = view.findViewById(R.id.attach_send_button_text_view);
        attachSendButtonText.setVisibility(View.INVISIBLE);
        attachSendButtonIcon = view.findViewById(R.id.attach_send_button_icon);

        galleryRecyclerView = view.findViewById(R.id.attach_recent_images_recycler_view);
        galleryRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        getActivity(), LinearLayoutManager.HORIZONTAL, false
                )
        );

        if (PermissionsRequester.requestFileReadPermissionIfNeeded(
                this, PERMISSIONS_REQUEST_ATTACH_FILE)
        ) {
            setupImagesRecycler(true);
        }

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_ATTACH_FILE){
            if (PermissionsRequester.isPermissionGranted(grantResults)) {
                setupImagesRecycler(true);
            } else {
                Toast.makeText(
                        getActivity(), R.string.no_permission_to_read_files, Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void setupImagesRecycler(boolean visibility) {
        if (visibility) {
            galleryRecyclerView.setVisibility(View.VISIBLE);

            recentImagesAdapter = new RecentImagesAdapter(this);
            recentImagesAdapter.loadGalleryPhotosAlbums();
            galleryRecyclerView.setAdapter(recentImagesAdapter);
        } else {
            galleryRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onRecentImagesSelected() {
        int size = recentImagesAdapter.getSelectedImagePaths().size();

        if (size > 0) {
            attachSendButtonText.setVisibility(View.VISIBLE);
            attachSendButtonText.setText(String.format(Locale.getDefault(),"Send (%d)", size));
            attachSendButtonIcon.setImageResource(R.drawable.ic_send_circle);
        } else {
            attachSendButtonText.setVisibility(View.INVISIBLE);
            attachSendButtonIcon.setImageResource(R.drawable.ic_down_circle);
        }
    }

    @Override
    public void tooManyFilesSelected() {
        Toast.makeText(getActivity(), R.string.too_many_files_at_once, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.attach_send_button:
                if (recentImagesAdapter != null) {
                    Set<String> selectedImagePaths = recentImagesAdapter.getSelectedImagePaths();
                    if (!selectedImagePaths.isEmpty()) {
                        listener.onRecentPhotosSend(new ArrayList<>(selectedImagePaths));
                    }
                }
                break;
            case R.id.attach_camera_button:
                listener.onCameraClick();
                break;
            case R.id.attach_file_button:
                listener.onFilesClick();
                break;
            case R.id.attach_gallery_button:
                listener.onGalleryClick();
                break;
            case R.id.attach_location_button:
                listener.onLocationClick();
                break;
        }

        dismiss();
    }
}