package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ForwardedActivity;
import com.xabber.android.ui.activity.ImageViewerActivity;
import com.xabber.android.ui.adapter.chat.FileMessageVH;
import com.xabber.android.ui.adapter.chat.ForwardedAdapter;
import com.xabber.android.ui.dialog.AttachDialog;
import com.xabber.android.ui.helper.PermissionsRequester;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.RealmList;

public class FileInteractionFragment extends Fragment implements FileMessageVH.FileListener,
        AttachDialog.Listener, ForwardedAdapter.ForwardListener {

    private static final String LOG_TAG = FileInteractionFragment.class.getSimpleName();

    private static final String SAVE_ACCOUNT = "com.xabber.android.ui.fragment.ARGUMENT_ACCOUNT";
    private static final String SAVE_USER = "com.xabber.android.ui.fragment.ARGUMENT_USER";
    private static final String SAVE_CURRENT_PICTURE_PATH = "com.xabber.android.ui.fragment.ARGUMENT_CURRENT_PICTURE_PATH";

    public static final int FILE_SELECT_ACTIVITY_REQUEST_CODE = 11;
    private static final int REQUEST_IMAGE_CAPTURE = 12;
    public static final int SHARE_ACTIVITY_REQUEST_CODE = 25;

    private static final int PERMISSIONS_REQUEST_ATTACH_FILE = 21;
    private static final int PERMISSIONS_REQUEST_CAMERA = 23;
    private static final int PERMISSIONS_REQUEST_DOWNLOAD_FILE = 24;

    private int clickedAttachmentPos;
    private String clickedMessageUID;
    private String currentPicturePath;

    protected AccountJid account;
    protected UserJid user;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            account = savedInstanceState.getParcelable(SAVE_ACCOUNT);
            user = savedInstanceState.getParcelable(SAVE_USER);
            currentPicturePath = savedInstanceState.getString(SAVE_CURRENT_PICTURE_PATH);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVE_ACCOUNT, account);
        outState.putParcelable(SAVE_USER, user);
        if (!TextUtils.isEmpty(currentPicturePath)) {
            outState.putString(SAVE_CURRENT_PICTURE_PATH, currentPicturePath);
        }
    }

    /** ActivityResult */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                addMediaToGallery(currentPicturePath);
                uploadFile(currentPicturePath);
                break;

            case FILE_SELECT_ACTIVITY_REQUEST_CODE:
                ClipData clipData = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    clipData = result.getClipData();
                }

                final List<Uri> uris = new ArrayList<>();
                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri uri = item.getUri();
                        uris.add(uri);
                    }
                } else {
                    Uri fileUri = result.getData();
                    uris.add(fileUri);
                }

                if (uris.size() == 0) {
                    Toast.makeText(getActivity(), R.string.could_not_get_path_to_file, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (uris.size() > 10) {
                    Toast.makeText(getActivity(), R.string.too_many_files_at_once, Toast.LENGTH_SHORT).show();
                    return;
                }

                HttpFileUploadManager.getInstance().uploadFileViaUri(account, user, uris, getActivity());
                break;
        }
    }

    /** Permissions */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ATTACH_FILE:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    ((ChatActivity)getActivity()).showAttachDialog();
                else Toast.makeText(getActivity(), R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
                break;

            case PERMISSIONS_REQUEST_CAMERA:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    startCamera();
                else Toast.makeText(getActivity(), R.string.no_permission_to_camera, Toast.LENGTH_SHORT).show();
                break;

            case PERMISSIONS_REQUEST_DOWNLOAD_FILE:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    openFileOrDownload(clickedMessageUID, clickedAttachmentPos);
                else Toast.makeText(getActivity(), R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /** FileMessageVH.FileListener */

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
                startActivity(ImageViewerActivity.createIntent(getActivity(),
                        messageItem.getUniqueId(), attachmentPosition));
                // possible if image was not sent and don't have URL yet.
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else {
            try {
                startActivity(ImageViewerActivity.createIntent(getActivity(),
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
    public void onDownloadCancel() {
        DownloadManager.getInstance().cancelDownload(getActivity());
    }

    @Override
    public void onUploadCancel() {
        HttpFileUploadManager.getInstance().cancelUpload(getActivity());
    }

    @Override
    public void onDownloadError(String error) {
        Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileLongClick(final Attachment attachment, View caller) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), caller);
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

    /** AttachDialog.Listener */

    @Override
    public void onRecentPhotosSend(List<String> paths) {
        uploadFiles(paths);
    }

    @Override
    public void onGalleryClick() {
        Intent intent = (new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE));
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onFilesClick() {
        Intent intent = (new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, FILE_SELECT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onCameraClick() {
        if (PermissionsRequester.requestCameraPermissionIfNeeded(this,
                PERMISSIONS_REQUEST_CAMERA)) startCamera();
    }

    /** Forwarded Listener */

    @Override
    public void onForwardClick(String messageId) {
        startActivity(ForwardedActivity.createIntent(getActivity(), messageId, user, account));
    }

    protected void onAttachButtonPressed() {
        if (!HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
            // show notification
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.error_file_upload_not_support)
                    .setTitle(getString(R.string.error_sending_file, ""))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }

        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
            ((ChatActivity)getActivity()).showAttachDialog();
        }
    }

    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File image = generatePicturePath();
        if (image != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileManager.getFileUri(image));
            currentPicturePath = image.getAbsolutePath();
        }
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    private static File generatePicturePath() {
        try {
            File storageDir = getAlbumDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            return new File(storageDir, "IMG_" + timeStamp + ".jpg");
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
        return null;
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

    private static void addMediaToGallery(String fromPath) {
        if (fromPath == null) {
            return;
        }
        File f = new File(fromPath);
        Uri contentUri = Uri.fromFile(f);
        addMediaToGallery(contentUri);
    }

    private static void addMediaToGallery(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(uri);
            Application.getInstance().sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    private void uploadFile(String path) {
        List<String> paths = new ArrayList<>();
        paths.add(path);
        HttpFileUploadManager.getInstance().uploadFile(account, user, paths, getActivity());
    }

    private void uploadFiles(List<String> paths) {
        HttpFileUploadManager.getInstance().uploadFile(account, user, paths, getActivity());
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
        Toast.makeText(getActivity(), R.string.FILE_NOT_FOUND, Toast.LENGTH_SHORT).show();
    }

    private void onCopyFileLink(Attachment attachment) {
        if (attachment == null) return;
        String url = attachment.getFileUrl();

        ClipboardManager clipboardManager = ((ClipboardManager)
                getActivity().getSystemService(Context.CLIPBOARD_SERVICE));
        if (clipboardManager != null)
            clipboardManager.setPrimaryClip(ClipData.newPlainText(url, url));
        Toast.makeText(getActivity(), R.string.toast_link_copied, Toast.LENGTH_SHORT).show();
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
                i.setDataAndType(FileProvider.getUriForFile(getActivity(),
                        getActivity().getApplicationContext().getPackageName()
                                + ".provider", new File(path)), attachment.getMimeType());
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    LogManager.exception(LOG_TAG, e);
                    Toast.makeText(getActivity(), R.string.toast_could_not_open_file, Toast.LENGTH_SHORT).show();
                }

            } else DownloadManager.getInstance().downloadFile(attachment, account, getActivity());
        }
    }

}
