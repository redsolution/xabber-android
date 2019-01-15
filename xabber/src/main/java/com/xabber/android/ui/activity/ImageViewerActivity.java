package com.xabber.android.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.ui.fragment.ImageViewerFragment;
import com.xabber.android.ui.helper.PermissionsRequester;

import java.io.File;

import io.realm.Realm;
import io.realm.RealmList;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class ImageViewerActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    private static final String IMAGE_URL = "IMAGE_URL";
    private static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String ATTACHMENT_POSITION = "ATTACHMENT_POSITION";
    private static final int PERMISSIONS_REQUEST_DOWNLOAD_FILE = 24;
    public static final int SHARE_ACTIVITY_REQUEST_CODE = 25;

    private AccountJid accountJid;
    private RealmList<Attachment> imageAttachments = new RealmList<>();
    private Toolbar toolbar;
    private ViewPager viewPager;
    private ProgressBar progressBar;
    private ImageView ivCancelDownload;

    private CompositeSubscription subscriptions = new CompositeSubscription();
    private CompositeSubscription attachmentStateSubscription = new CompositeSubscription();
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
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(ImageViewerActivity.this);
            }
        });

        // get imageAttachments
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        MessageItem messageItem = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.UNIQUE_ID, messageId)
                .findFirst();

        if (imageUrl != null) {
            Attachment attachment = new Attachment();
            attachment.setFileUrl(imageUrl);
            imageAttachments.add(attachment);
        } else {
            RealmList<Attachment> attachments = messageItem.getAttachments();

            for (Attachment attachment : attachments) {
                if (attachment.isImage()) imageAttachments.add(attachment);
            }
        }

        // get account jid
        this.accountJid = messageItem.getAccount();

        // find views
        progressBar = findViewById(R.id.progressBar);
        ivCancelDownload = findViewById(R.id.ivCancelDownload);
        ivCancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelDownloadClick();
            }
        });

        viewPager = findViewById(R.id.viewPager);
        PagerAdapter pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Attachment attachment = imageAttachments.get(position);
                return ImageViewerFragment.newInstance(attachment.getFilePath(),
                        attachment.getFileUrl(), attachment.getUniqueId());
            }

            @Override
            public int getCount() {
                return imageAttachments.size();
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
                subscribeForAttachment(imageAttachments.get(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        if (imageAttachments.size() > imagePosition) subscribeForAttachment(imageAttachments.get(imagePosition));
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_DOWNLOAD_FILE:
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    downloadImage();
                } else {
                    onNoWritePermissionError();
                }
                break;
        }
    }

    private void updateToolbar() {
        int current = 0, total = 0;
        if (viewPager != null) current = viewPager.getCurrentItem() + 1;
        if (imageAttachments != null) total = imageAttachments.size();
        toolbar.setTitle(current + " of " + total);
        setUpMenuOptions(toolbar.getMenu());
    }

    private void setUpMenuOptions(Menu menu) {
        int position = viewPager.getCurrentItem();
        Attachment attachment = imageAttachments.get(position);
        String filePath = attachment.getFilePath();
        Long size = attachment.getFileSize();
        menu.findItem(R.id.action_download_image).setVisible(filePath == null && size != null);
        menu.findItem(R.id.action_download_image).setEnabled(!isDownloading);
        menu.findItem(R.id.action_done).setVisible(filePath != null);
        menu.findItem(R.id.action_share).setVisible(size != null);
    }

    private void onShareClick() {
        int position = viewPager.getCurrentItem();
        Attachment attachment = imageAttachments.get(position);
        String path = attachment.getFilePath();

        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                startActivityForResult(FileManager.getIntentForShareFile(file),
                        SHARE_ACTIVITY_REQUEST_CODE);
                return;
            } else Toast.makeText(this, R.string.FILE_NOT_FOUND, Toast.LENGTH_SHORT).show();
        } else {
            waitForSharing = true;
            onImageDownloadClick();
        }
    }

    private void onCopyLinkClick() {
        int position = viewPager.getCurrentItem();
        Attachment attachment = imageAttachments.get(position);
        String url = attachment.getFileUrl();

        ClipboardManager clipboardManager = ((ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE));
        if (clipboardManager != null) clipboardManager.setPrimaryClip(ClipData.newPlainText(url, url));
        Toast.makeText(this, R.string.toast_link_copied, Toast.LENGTH_SHORT).show();
    }

    private void onImageDownloadClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(
                this, PERMISSIONS_REQUEST_DOWNLOAD_FILE)) downloadImage();
    }

    private void downloadImage() {
        int position = viewPager.getCurrentItem();
        Attachment attachment = imageAttachments.get(position);
        DownloadManager.getInstance().downloadFile(attachment, accountJid, this);
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
            .doOnNext(new Action1<DownloadManager.ProgressData>() {
                @Override
                public void call(DownloadManager.ProgressData progressData) {
                    onProgressUpdated(progressData);
                }
            }).subscribe());
    }

    private void onProgressUpdated(DownloadManager.ProgressData progressData) {
        int position = viewPager.getCurrentItem();
        Attachment attachment = imageAttachments.get(position);

        if (progressData.getAttachmentId().equals(attachment.getUniqueId())) {
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

    private void subscribeForAttachment(Attachment attachment) {
        if (attachment == null) return;
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        Attachment attachmentForSubscribe = realm.where(Attachment.class)
                .equalTo(Attachment.Fields.UNIQUE_ID, attachment.getUniqueId())
                .findFirst();

        if (attachmentForSubscribe == null) return;
        Observable<Attachment> observable = attachmentForSubscribe.asObservable();

        attachmentStateSubscription.add(observable.doOnNext(new Action1<Attachment>() {
            @Override
            public void call(Attachment attachment) {
                updateToolbar();
                if (waitForSharing) {
                    waitForSharing = false;
                    onShareClick();
                }
            }
        }).subscribe());
    }

    private void unsubscribeAttachmentState() {
        attachmentStateSubscription.clear();
    }
}
