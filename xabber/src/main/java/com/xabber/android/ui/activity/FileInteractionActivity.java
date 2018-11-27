package com.xabber.android.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.adapter.chat.FileMessageVH;
import com.xabber.android.ui.helper.PermissionsRequester;

import java.io.File;

import io.realm.RealmList;

public class FileInteractionActivity extends ManagedActivity implements FileMessageVH.FileListener {

    private static final String LOG_TAG = FileInteractionActivity.class.getSimpleName();

    private final static String KEY_ACCOUNT = "account";

    public static final int FILE_SELECT_ACTIVITY_REQUEST_CODE = 11;
    private static final int REQUEST_IMAGE_CAPTURE = 12;
    public static final int SHARE_ACTIVITY_REQUEST_CODE = 25;

    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 21;
    private static final int PERMISSIONS_REQUEST_EXPORT_CHAT = 22;
    private static final int PERMISSIONS_REQUEST_CAMERA = 23;
    private static final int PERMISSIONS_REQUEST_DOWNLOAD_FILE = 24;

    private int clickedAttachmentPos;
    private String clickedMessageUID;

    private AccountJid account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) account = intent.getParcelableExtra(KEY_ACCOUNT);
    }

    @Override
    public void onImageClick(int messagePosition, int attachmentPosition, String messageUID) {
        MessageItem messageItem = MessageDatabaseManager.getInstance().getRealmUiThread().where(MessageItem.class)
                .equalTo(MessageItem.Fields.UNIQUE_ID, messageUID).findFirst();

        if (messageItem == null) {
            LogManager.w(LOG_TAG, "onMessageFileClick: null message item. Position: " + messagePosition);
            return;
        }

        if (messageItem.haveAttachments()) {
            try {
                startActivity(ImageViewerActivity.createIntent(this,
                        messageItem.getUniqueId(), attachmentPosition));
                // possible if image was not sent and don't have URL yet.
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else {
            try {
                startActivity(ImageViewerActivity.createIntent(this,
                        messageItem.getUniqueId(), messageItem.getText()));
                // possible if image was not sent and don't have URL yet.
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
    }

    @Override
    public void onFileClick(int messagePosition, int attachmentPosition, String messageUID) {
        clickedAttachmentPos = attachmentPosition;
        clickedMessageUID = messageUID;
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(
                this, PERMISSIONS_REQUEST_DOWNLOAD_FILE))
            openFileOrDownload(messageUID, attachmentPosition);
    }

    @Override
    public void onFileLongClick(final Attachment attachment, View caller) {
        PopupMenu popupMenu = new PopupMenu(this, caller);
        popupMenu.inflate(R.menu.menu_file_attachment);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_copy_link:
                        onCopyFileLink(attachment);
                        break;
                    case R.id.action_share:
                        onShareClick(attachment);
                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onDownloadCancel() {
        DownloadManager.getInstance().cancelDownload(this);
    }

    @Override
    public void onUploadCancel() {
        HttpFileUploadManager.getInstance().cancelUpload(this);
    }

    @Override
    public void onDownloadError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    private void openFileOrDownload(String messageUID, int attachmentPosition) {
        MessageItem messageItem = MessageDatabaseManager.getInstance().getRealmUiThread().where(MessageItem.class)
                .equalTo(MessageItem.Fields.UNIQUE_ID, messageUID).findFirst();

        if (messageItem == null) {
            LogManager.w(LOG_TAG, "onMessageFileClick: null message item. UID: " + messageUID);
            return;
        }

        if (messageItem.haveAttachments()) {
            RealmList<Attachment> fileAttachments = new RealmList<>();
            for (Attachment attachment : messageItem.getAttachments()) {
                if (!attachment.isImage()) fileAttachments.add(attachment);
            }

            Attachment attachment = fileAttachments.get(attachmentPosition);
            if (attachment == null) return;

            if (attachment.getFilePath() != null) {
                File file = new File(attachment.getFilePath());
                if (!file.exists()) {
                    MessageManager.setAttachmentLocalPathToNull(attachment.getUniqueId());
                    return;
                }

                Intent i = new Intent(Intent.ACTION_VIEW);
                String path = attachment.getFilePath();
                i.setDataAndType(FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName()
                                + ".provider", new File(path)), attachment.getMimeType());
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    LogManager.exception(LOG_TAG, e);
                    Toast.makeText(this, R.string.toast_could_not_open_file, Toast.LENGTH_SHORT).show();
                }

            } else DownloadManager.getInstance().downloadFile(attachment, account, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
//            case PERMISSIONS_REQUEST_ATTACH_FILE:
//                if (PermissionsRequester.isPermissionGranted(grantResults))
//                    ((ChatActivity)getActivity()).showAttachDialog();
//                else Toast.makeText(this, R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
//
//                break;
//
//            case PERMISSIONS_REQUEST_EXPORT_CHAT:
//                if (PermissionsRequester.isPermissionGranted(grantResults)) showExportChatDialog();
//                else Toast.makeText(this, R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
//                break;
//
//            case PERMISSIONS_REQUEST_CAMERA:
//                if (PermissionsRequester.isPermissionGranted(grantResults))
//                    startCamera();
//                else Toast.makeText(this, R.string.no_permission_to_camera, Toast.LENGTH_SHORT).show();
//                break;

            case PERMISSIONS_REQUEST_DOWNLOAD_FILE:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    openFileOrDownload(clickedMessageUID, clickedAttachmentPos);
                else Toast.makeText(this, R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void onShareClick(Attachment attachment) {
        if (attachment == null) return;
        String path = attachment.getFilePath();

        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                startActivityForResult(FileManager.getIntentForShareFile(file),
                        SHARE_ACTIVITY_REQUEST_CODE);
                return;
            }
        }
        Toast.makeText(this, R.string.FILE_NOT_FOUND, Toast.LENGTH_SHORT).show();
    }

    private void onCopyFileLink(Attachment attachment) {
        if (attachment == null) return;
        String url = attachment.getFileUrl();

        ClipboardManager clipboardManager =
                ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE));
        if (clipboardManager != null)
            clipboardManager.setPrimaryClip(ClipData.newPlainText(url, url));
        Toast.makeText(this, R.string.toast_link_copied, Toast.LENGTH_SHORT).show();
    }
}
