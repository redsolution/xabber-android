package com.xabber.android.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.xabber.android.R;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.ui.fragment.ImageViewerFragment;
import com.xabber.android.ui.helper.PermissionsRequester;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import io.realm.Realm;
import io.realm.RealmList;
import rx.subscriptions.CompositeSubscription;

public class ImageViewerActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    private static final String IMAGE_URL = "IMAGE_URL";
    private static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String ATTACHMENT_POSITION = "ATTACHMENT_POSITION";
    private static final int PERMISSIONS_REQUEST_DOWNLOAD_FILE = 24;
    public static final int SHARE_ACTIVITY_REQUEST_CODE = 25;

    private AccountJid accountJid;
    private final RealmList<AttachmentRealmObject> imageAttachmentRealmObjects = new RealmList<>();
    private Toolbar toolbar;
    private ViewPager viewPager;
    private ProgressBar progressBar;
    private ImageView ivCancelDownload;

    private final CompositeSubscription subscriptions = new CompositeSubscription();
    private final CompositeSubscription attachmentStateSubscription = new CompositeSubscription();
    private boolean waitForSharing;
    private boolean isDownloading;

    @NonNull
    public static Intent createIntent(Context context, String id, int position) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        Bundle args = new Bundle();
        args.putString(MESSAGE_ID, id);
        args.putInt(ATTACHMENT_POSITION, position);
        intent.putExtras(args);
        return intent;
    }

    @NonNull
    public static Intent createIntent(Context context, String id, String url) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        Bundle args = new Bundle();
        args.putString(MESSAGE_ID, id);
        args.putString(IMAGE_URL, url);
        intent.putExtras(args);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // get params
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        if (args == null) {
            finish();
            return;
        }

        String imageUrl = args.getString(IMAGE_URL);
        String messageId = args.getString(MESSAGE_ID);
        int imagePosition = args.getInt(ATTACHMENT_POSITION);

        // setup toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.inflateMenu(R.menu.menu_image_viewer);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(ImageViewerActivity.this));

        // get imageAttachments
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        MessageRealmObject messageRealmObject = realm
                .where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, messageId)
                .findFirst();

        if (imageUrl != null) {
            AttachmentRealmObject attachmentRealmObject = new AttachmentRealmObject();
            attachmentRealmObject.setFileUrl(imageUrl);
            imageAttachmentRealmObjects.add(attachmentRealmObject);
        } else {
            RealmList<AttachmentRealmObject> attachmentRealmObjects = messageRealmObject.getAttachmentRealmObjects();

            for (AttachmentRealmObject attachmentRealmObject : attachmentRealmObjects) {
                if (attachmentRealmObject.isImage()) imageAttachmentRealmObjects.add(attachmentRealmObject);
            }
        }

        // get account jid
        this.accountJid = messageRealmObject.getAccount();

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        // find views
        progressBar = findViewById(R.id.progressBar);
        ivCancelDownload = findViewById(R.id.ivCancelDownload);
        ivCancelDownload.setOnClickListener(v -> onCancelDownloadClick());

        viewPager = findViewById(R.id.viewPager);
        PagerAdapter pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                AttachmentRealmObject attachmentRealmObject = imageAttachmentRealmObjects.get(position);
                return ImageViewerFragment.newInstance(attachmentRealmObject.getFilePath(),
                        attachmentRealmObject.getFileUrl(), attachmentRealmObject.getUniqueId());
            }

            @Override
            public int getCount() {
                return imageAttachmentRealmObjects.size();
            }
        };
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(imagePosition);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                updateToolbar();
                unsubscribeAttachmentState();
                subscribeForAttachment(imageAttachmentRealmObjects.get(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        if (imageAttachmentRealmObjects.size() > imagePosition) {
            subscribeForAttachment(imageAttachmentRealmObjects.get(imagePosition));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToolbar();
        subscribeForDownloadProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unsubscribeAll();
        showProgress(false);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download_image:
                onImageDownloadClick();
                break;

            case R.id.action_copy_link:
                onCopyLinkClick();
                break;

            case R.id.action_share:
                onShareClick();
                break;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_DOWNLOAD_FILE) {
            if (PermissionsRequester.isPermissionGranted(grantResults)) {
                downloadImage();
            } else {
                onNoWritePermissionError();
            }
        }
    }

    private void updateToolbar() {
        int current = 0, total = 0;
        if (viewPager != null) current = viewPager.getCurrentItem() + 1;
        if (imageAttachmentRealmObjects != null) total = imageAttachmentRealmObjects.size();
        toolbar.setTitle(current + " of " + total);
        toolbar.setTitleTextColor(Color.WHITE);
        setUpMenuOptions(toolbar.getMenu());
    }

    private void setUpMenuOptions(Menu menu) {
        int position = viewPager.getCurrentItem();
        AttachmentRealmObject attachmentRealmObject = imageAttachmentRealmObjects.get(position);
        String filePath = attachmentRealmObject.getFilePath();
        Long size = attachmentRealmObject.getFileSize();
        menu.findItem(R.id.action_download_image).setVisible(filePath == null && size != null);
        menu.findItem(R.id.action_download_image).setEnabled(!isDownloading);
        menu.findItem(R.id.action_done).setVisible(filePath != null);
        menu.findItem(R.id.action_share).setVisible(size != null);
    }

    private void onShareClick() {
        int position = viewPager.getCurrentItem();
        AttachmentRealmObject attachmentRealmObject = imageAttachmentRealmObjects.get(position);
        String path = attachmentRealmObject.getFilePath();

        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                startActivityForResult(FileManager.getIntentForShareFile(file), SHARE_ACTIVITY_REQUEST_CODE);
            } else Toast.makeText(this, R.string.FILE_NOT_FOUND, Toast.LENGTH_SHORT).show();
        } else {
            waitForSharing = true;
            onImageDownloadClick();
        }
    }

    private void onCopyLinkClick() {
        int position = viewPager.getCurrentItem();
        AttachmentRealmObject attachmentRealmObject = imageAttachmentRealmObjects.get(position);
        String url = attachmentRealmObject.getFileUrl();

        ClipboardManager clipboardManager = ((ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE));
        if (clipboardManager != null) clipboardManager.setPrimaryClip(ClipData.newPlainText(url, url));
        Toast.makeText(this, R.string.toast_link_copied, Toast.LENGTH_SHORT).show();
    }

    private void onImageDownloadClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(this, PERMISSIONS_REQUEST_DOWNLOAD_FILE)){
            downloadImage();
        }
    }

    private void downloadImage() {
        int position = viewPager.getCurrentItem();
        AttachmentRealmObject attachmentRealmObject = imageAttachmentRealmObjects.get(position);
        DownloadManager.getInstance().downloadFile(attachmentRealmObject, accountJid, this);
    }

    private void onCancelDownloadClick() {
        DownloadManager.getInstance().cancelDownload(this);
    }

    private void unsubscribeAll() {
        subscriptions.clear();
        unsubscribeAttachmentState();
    }

    private void subscribeForDownloadProgress() {
        subscriptions.add(DownloadManager.getInstance().subscribeForProgress()
            .doOnNext(this::onProgressUpdated).subscribe());
    }

    private void onProgressUpdated(DownloadManager.ProgressData progressData) {
        int position = viewPager.getCurrentItem();
        AttachmentRealmObject attachmentRealmObject = imageAttachmentRealmObjects.get(position);

        if (progressData.getAttachmentId().equals(attachmentRealmObject.getUniqueId())) {
            if (progressData.isCompleted()) {
                showProgress(false);
                isDownloading = false;
                updateToolbar();
            } else if (progressData.getError() != null) {
                showProgress(false);
                showToast(progressData.getError());
                isDownloading = false;
                updateToolbar();
            } else {
                progressBar.setProgress(progressData.getProgress());
                showProgress(true);
                isDownloading = true;
                updateToolbar();
            }
        } else showProgress(false);
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            ivCancelDownload.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            ivCancelDownload.setVisibility(View.GONE);
        }
    }

    private void onNoWritePermissionError() {
        Toast.makeText(this, R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
    }

    private void subscribeForAttachment(AttachmentRealmObject attachmentRealmObject) {
        if (attachmentRealmObject == null) return;
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        AttachmentRealmObject attachmentRealmObjectForSubscribe = realm
                .where(AttachmentRealmObject.class)
                .equalTo(AttachmentRealmObject.Fields.UNIQUE_ID, attachmentRealmObject.getUniqueId())
                .findFirst();
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        if (attachmentRealmObjectForSubscribe == null) return;
        //TODO FIX THIS
//        Observable<Attachment> observable = attachmentForSubscribe.asObservable();
//
//        attachmentStateSubscription.add(observable.doOnNext(new Action1<Attachment>() {
//            @Override
//            public void call(Attachment attachment) {
//                updateToolbar();
//                if (waitForSharing) {
//                    waitForSharing = false;
//                    onShareClick();
//                }
//            }
//        }).subscribe());
    }

    private void unsubscribeAttachmentState() {
        attachmentStateSubscription.clear();
    }

}
