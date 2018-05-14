package com.xabber.android.ui.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.RecentImagesAdapter;
import com.xabber.android.ui.helper.PermissionsRequester;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class AttachDialog extends BottomSheetDialogFragment implements RecentImagesAdapter.Listener,
        View.OnClickListener {

    public static final int FILE_SELECT_ACTIVITY_REQUEST_CODE = 23;
    private static final int REQUEST_IMAGE_CAPTURE = 24;
    private static final String LOG_TAG = AttachDialog.class.getSimpleName();
    private static final String SAVE_CURRENT_PICTURE_PATH = "com.xabber.android.ui.dialog.AttachDialog.SAVE_CURRENT_PICTURE_PATH";

    private RecentImagesAdapter recentImagesAdapter;
    private TextView attachSendButtonText;
    private ImageView attachSendButtonIcon;

    private String currentPicturePath;


//    private final Listener listener;
//
//    public interface Listener {
//        void onSendFiles(List<String> paths);
//        void onSendFile(String path);
//    }

    public static AttachDialog newInstance() {
        return new AttachDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.attach_dialog, container, false);

        view.findViewById(R.id.attach_send_button).setOnClickListener(this);
        view.findViewById(R.id.attach_file_button).setOnClickListener(this);
        view.findViewById(R.id.attach_camera_button).setOnClickListener(this);
        view.findViewById(R.id.attach_gallery_button).setOnClickListener(this);

        attachSendButtonText = view.findViewById(R.id.attach_send_button_text_view);
        attachSendButtonText.setVisibility(View.INVISIBLE);
        attachSendButtonIcon = view.findViewById(R.id.attach_send_button_icon);

        RecyclerView recyclerView = view.findViewById(R.id.attach_recent_images_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

        recentImagesAdapter = new RecentImagesAdapter(this);
        recentImagesAdapter.loadGalleryPhotosAlbums();
        recyclerView.setAdapter(recentImagesAdapter);

        return view;
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.attach_send_button:
                onSendButtonClick();
                break;
            case R.id.attach_camera_button:
                onCameraClick();
                break;
            case R.id.attach_file_button:
                Intent fileIntent = (new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
                Activity activity = getActivity();
                if (activity != null) activity.startActivityForResult(fileIntent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
                break;
            case R.id.attach_gallery_button:
                Intent galleryIntent = (new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE));
                Activity activity1 = getActivity();
                if (activity1 != null) activity1.startActivityForResult(galleryIntent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
                break;
        }

        dismiss();
    }

    public static File generatePicturePath() {
        try {
            File storageDir = getAlbumDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            return new File(storageDir, "IMG_" + timeStamp + ".jpg");
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
        return null;
    }

    private void onCameraClick() {
        if (!PermissionsRequester.hasCameraPermission()) {
            PermissionsRequester.requestCameraPermissionIfNeeded(getActivity());
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File image = generatePicturePath();
            if (image != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileManager.getFileUri(image));
                currentPicturePath = image.getAbsolutePath();
            }
            Activity activity = getActivity();
            if (activity != null) activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private static File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    Application.getInstance().getString(R.string.application_title_short));
            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()){
                    LogManager.w(LOG_TAG, "failed to create directory");
                    return null;
                }
            }
        } else {
            LogManager.w(LOG_TAG, "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private void onSendButtonClick() {
        Set<String> selectedImagePaths = recentImagesAdapter.getSelectedImagePaths();
        if (!selectedImagePaths.isEmpty()) {
            //listener.onSendFiles(new ArrayList<>(selectedImagePaths));
        }
    }
}