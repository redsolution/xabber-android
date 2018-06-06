package com.xabber.android.ui.adapter;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.helper.PermissionsRequester;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class RecentImagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final String[] projectionPhotos = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION
    };
    private static final String LOG_TAG = RecentImagesAdapter.class.getSimpleName();

    @NonNull
    private final List<String> imagePaths;
    @NonNull
    private final Set<String> selectedImagePaths;
    @NonNull
    private final Listener listener;

    public interface Listener {
        void onRecentImagesSelected();
        void tooManyFilesSelected();
    }

    public RecentImagesAdapter(@NonNull Listener listener) {
        this.imagePaths = new ArrayList<>();
        this.selectedImagePaths = new HashSet<>();
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecentImageViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_image, parent, false));
    }
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final RecentImageViewHolder recentImageViewHolder = (RecentImageViewHolder) holder;
        final String path = imagePaths.get(position);

        Glide.with(recentImageViewHolder.image.getContext())
                .load(new File(path))
                .centerCrop()
                .placeholder(R.drawable.ic_image)
                .into(recentImageViewHolder.image);

        recentImageViewHolder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recentImageViewHolder.checkBox.setChecked(!recentImageViewHolder.checkBox.isChecked());
            }
        });

        recentImageViewHolder.checkBox.setOnCheckedChangeListener(null);

        recentImageViewHolder.checkBox.setChecked(selectedImagePaths.contains(path));

        recentImageViewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (selectedImagePaths.size() < 10)
                        selectedImagePaths.add(path);
                    else {
                        buttonView.setChecked(false);
                        listener.tooManyFilesSelected();
                    }
                } else {
                    selectedImagePaths.remove(path);
                }

                listener.onRecentImagesSelected();
            }
        });
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    @NonNull
    public Set<String> getSelectedImagePaths() {
        return selectedImagePaths;
    }

    static class RecentImageViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        CheckBox checkBox;

        RecentImageViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.recent_image_item_image);
            checkBox = itemView.findViewById(R.id.recent_image_item_checkbox);
        }
    }

    public void loadGalleryPhotosAlbums() {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                final List<String> imagePaths = new ArrayList<>();

                Cursor cursor = null;
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                            || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && PermissionsRequester.hasFileReadPermission()) {
                        cursor = MediaStore.Images.Media.query(Application.getInstance().getContentResolver(),
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projectionPhotos, null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC");
                        if (cursor != null) {
                            int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                            while (cursor.moveToNext()) {
                                String path = cursor.getString(dataColumn);
                                if (!TextUtils.isEmpty(path)) {
                                    imagePaths.add(path);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LogManager.exception(this, e);
                } finally {
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Exception e) {
                            LogManager.exception(this, e);
                        }
                    }
                }

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update(imagePaths);
                    }
                });
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void update(List<String> imagePaths) {
        this.imagePaths.clear();
        this.imagePaths.addAll(imagePaths);
        this.selectedImagePaths.clear();

        notifyDataSetChanged();
    }

    public void clearSelection() {
        this.selectedImagePaths.clear();
        notifyDataSetChanged();
    }
}
