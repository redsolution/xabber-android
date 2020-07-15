package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager;
import com.xabber.android.data.extension.references.mutable.voice.VoiceMessagePresenterManager;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ForwardedActivity;
import com.xabber.android.ui.activity.ImageViewerActivity;
import com.xabber.android.ui.adapter.chat.FileMessageVH;
import com.xabber.android.ui.adapter.chat.ForwardedAdapter;
import com.xabber.android.ui.dialog.AttachDialog;
import com.xabber.android.ui.dialog.VoiceDownloadDialog;
import com.xabber.android.ui.helper.PermissionsRequester;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import top.oply.opuslib.OpusEvent;

public class FileInteractionFragment extends Fragment implements FileMessageVH.FileListener,
        ForwardedAdapter.ForwardListener, AttachDialog.Listener {

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
    static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 37;

    private int clickedAttachmentPos;
    private String clickedMessageUID;
    private String clickedAttachmentUID;
    private String currentPicturePath;
    private Long messageTimestamp;
    private List<String> forwardIds = new ArrayList<>();
    boolean sendImmediately = false;
    boolean ignoreReceiver = true;

    private OpusReceiver opusReceiver = new OpusReceiver();
    private PublishSubject<DownloadManager.ProgressData> voiceDownload;
    private Subscription voiceDownloadSubscription;

    protected AccountJid account;
    protected ContactJid user;

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

    /**
     * ActivityResult
     */

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

                if (forwardIds.size() == 0)
                    HttpFileUploadManager.getInstance().uploadFileViaUri(account, user, uris, getActivity());
                else {
                    HttpFileUploadManager.getInstance().uploadFile(account, user, null, uris, forwardIds, null, null, getActivity());
                    forwardIds.clear();
                    if (getActivity() != null)
                        ((ChatActivity) getActivity()).hideForwardPanel();
                }
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (voiceDownloadSubscription != null) voiceDownloadSubscription.unsubscribe();
    }

    /**
     * Permissions
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ATTACH_FILE:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    ((ChatActivity) getActivity()).showAttachDialog();
                else
                    Toast.makeText(getActivity(), R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
                break;

            case PERMISSIONS_REQUEST_CAMERA:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    startCamera();
                else
                    Toast.makeText(getActivity(), R.string.no_permission_to_camera, Toast.LENGTH_SHORT).show();
                break;

            case PERMISSIONS_REQUEST_DOWNLOAD_FILE:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    openFileOrDownload(clickedMessageUID, clickedAttachmentPos);
                else
                    Toast.makeText(getActivity(), R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * FileMessageVH.FileListener
     */

    @Override
    public void onImageClick(int messagePosition, int attachmentPosition, String messageUID) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        MessageRealmObject messageRealmObject = realm
                .where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageUID)
                .findFirst();

        if (messageRealmObject == null) {
            LogManager.w(LOG_TAG, "onMessageFileClick: null message item. Position: " + messagePosition);
            return;
        }

        if (messageRealmObject.haveAttachments()) {
            try {
                startActivity(ImageViewerActivity.createIntent(getActivity(),
                        messageRealmObject.getUniqueId(), attachmentPosition));
                // possible if image was not sent and don't have URL yet.
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else {
            try {
                startActivity(ImageViewerActivity.createIntent(getActivity(),
                        messageRealmObject.getUniqueId(), messageRealmObject.getText()));
                // possible if image was not sent and don't have URL yet.
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
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
    public void onVoiceClick(int messagePosition, int attachmentPosition, String attachmentId, String messageUID, Long timestamp) {
        clickedAttachmentPos = attachmentPosition;
        clickedMessageUID = messageUID;
        clickedAttachmentUID = attachmentId;
        messageTimestamp = timestamp;
        subscribeForVoiceDownloadProgress();
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(
                this, PERMISSIONS_REQUEST_DOWNLOAD_FILE))
            openFileOrDownload(messageUID, attachmentPosition);
    }

    protected void subscribeForVoiceDownloadProgress() {
        voiceDownload = DownloadManager.getInstance().subscribeForProgress();
        if (voiceDownloadSubscription != null) voiceDownloadSubscription.unsubscribe();
        voiceDownloadSubscription = voiceDownload.doOnNext(this::waitForVoiceDownloadFinish).subscribe();
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
    public void onFileLongClick(final AttachmentRealmObject attachmentRealmObject, View caller) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), caller);
        popupMenu.inflate(R.menu.menu_file_attachment);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_copy_link:
                    onCopyFileLink(attachmentRealmObject);
                    break;
                case R.id.action_share:
                    onShareClick(attachmentRealmObject);
                    break;
            }
            return true;
        });
        popupMenu.show();
    }

    /**
     * AttachDialog.Listener
     */

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

    /**
     * Forwarded Listener
     */

    @Override
    public void onForwardClick(String messageId) {
        startActivity(ForwardedActivity.createIntent(getActivity(), messageId, user, account));
    }

    protected void onAttachButtonPressed() {
        if (!HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
            // show notification
            String serverName = account.getFullJid().getDomain().toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.error_sending_file, ""))
                   .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
            if (HttpFileUploadManager.getInstance().isFileUploadDiscoveryInProgress(account)) {
                builder.setMessage(getActivity().getResources().getString(R.string.error_file_upload_disco_in_progress, serverName));
            } else {
                builder.setMessage(getActivity().getResources().getString(R.string.error_file_upload_not_support, serverName));
            }
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }

        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, PERMISSIONS_REQUEST_ATTACH_FILE)) {
            ((ChatActivity) getActivity()).showAttachDialog();
        }
    }

    protected void forwardIdsForAttachments(List<String> forwardIds) {
        if (forwardIds == null) {
            this.forwardIds.clear();
        } else {
            this.forwardIds = new ArrayList<>(forwardIds);
        }
    }

    protected final Runnable record = () -> VoiceManager.getInstance().startRecording();

    boolean releaseRecordedVoicePlayback(String filePath) {
        VoiceManager.getInstance().releaseMediaPlayer();
        VoiceMessagePresenterManager.getInstance().deleteOldPath(filePath);
        File file = new File(filePath);
        if (file.exists()) {
            FileManager.deleteTempFile(file);
            return !file.exists();
        }
        return true;
    }

    void sendStoppedVoiceMessage(String filePath) {
        sendStoppedVoiceMessage(filePath, null);
    }

    void sendStoppedVoiceMessage(String filePath, List<String> forwardIDs) {
        VoiceManager.getInstance().releaseMediaPlayer();
        String path = VoiceManager.getInstance().getStoppedRecordingNewFilePath(filePath);
        if (path != null) {
            if (forwardIDs != null) {
                uploadVoiceFile(path, forwardIDs);
            } else {
                uploadVoiceFile(path);
            }
        }
    }

    void stopRecordingAndSend(boolean send) {
        stopRecordingAndSend(send, null);
    }

    void stopRecordingAndSend(boolean send, List<String> forwardIDs) {
        if (send) {
            sendImmediately = true;
            //ignore = false;
            forwardIdsForAttachments(forwardIDs);
            VoiceManager.getInstance().stopRecording(false);
        } else {
            ignoreReceiver = true;
            VoiceManager.getInstance().stopRecording(true);
        }
    }

    void registerOpusBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(OpusEvent.ACTION_OPUS_UI_RECEIVER);
        getActivity().registerReceiver(opusReceiver, filter);
    }

    void unregisterOpusBroadcastReceiver() {
        getActivity().unregisterReceiver(opusReceiver);
    }

    //void stopRecordingIfPossibleAsync(final boolean saveFile) {
    //    Application.getInstance().runInBackground(new Runnable() {
    //        @Override
    //        public void run() {
    //            stopRecordingAndSend(saveFile);
    //        }
    //    });
    //}

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
                if (!storageDir.exists()) {
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
        if (forwardIds.size() == 0)
            HttpFileUploadManager.getInstance().uploadFile(account, user, paths, getActivity());
        else {
            HttpFileUploadManager.getInstance().uploadFile(account, user, paths, null, forwardIds, null, null, getActivity());
            forwardIds.clear();
            if (getActivity() != null)
                ((ChatActivity) getActivity()).hideForwardPanel();
        }
    }

    private void uploadVoiceFile(String path) {
        uploadVoiceFile(path, null);
    }

    private void uploadVoiceFile(String path, List<String> forwardIds) {
        List<String> paths = new ArrayList<>();
        paths.add(path);
        HttpFileUploadManager.getInstance().uploadFile(account, user, paths, null, forwardIds, null, "voice", getActivity());
        if (forwardIds != null && forwardIds.size() != 0) {
            forwardIds.clear();
            if (getActivity() != null)
                ((ChatActivity) getActivity()).hideForwardPanel();
        }
    }

    private void uploadFiles(List<String> paths) {
        if (forwardIds.size() == 0)
            HttpFileUploadManager.getInstance().uploadFile(account, user, paths, getActivity());
        else {
            HttpFileUploadManager.getInstance().uploadFile(account, user, paths, null, forwardIds, null, null, getActivity());
            forwardIds.clear();
            if (getActivity() != null)
                ((ChatActivity) getActivity()).hideForwardPanel();
        }
    }

    private void onShareClick(AttachmentRealmObject attachmentRealmObject) {
        if (attachmentRealmObject == null) return;
        String path = attachmentRealmObject.getFilePath();

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

    private void onCopyFileLink(AttachmentRealmObject attachmentRealmObject) {
        if (attachmentRealmObject == null) return;
        String url = attachmentRealmObject.getFileUrl();

        ClipboardManager clipboardManager = ((ClipboardManager)
                getActivity().getSystemService(Context.CLIPBOARD_SERVICE));
        if (clipboardManager != null)
            clipboardManager.setPrimaryClip(ClipData.newPlainText(url, url));
        Toast.makeText(getActivity(), R.string.toast_link_copied, Toast.LENGTH_SHORT).show();
    }

    private void openFileOrDownload(String messageUID, int attachmentPosition) {
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        MessageRealmObject messageRealmObject = realm
                .where(MessageRealmObject.class)
                .equalTo(MessageRealmObject.Fields.UNIQUE_ID, messageUID)
                .findFirst();
        LogManager.d("VoiceDebug", "openFileOrDownload start! attachmentPosition = " + attachmentPosition + " messageUID = " + messageUID);

        if (messageRealmObject == null) {
            LogManager.w(LOG_TAG, "onMessageFileClick: null message item. UID: " + messageUID);
            return;
        }

        if (messageRealmObject.haveAttachments()) {
            RealmList<AttachmentRealmObject> fileAttachmentRealmObjects = new RealmList<>();
            for (AttachmentRealmObject attachmentRealmObject : messageRealmObject.getAttachmentRealmObjects()) {
                if (!attachmentRealmObject.isImage()) fileAttachmentRealmObjects.add(attachmentRealmObject);
            }

            final AttachmentRealmObject attachmentRealmObject = fileAttachmentRealmObjects.get(attachmentPosition);
            if (attachmentRealmObject == null) return;

            LogManager.d("VoiceDebug", "openFileOrDownload fork! dl or open?");
            if (attachmentRealmObject.getFilePath() != null) {
                LogManager.d("VoiceDebug", "Opening file shortly!");
                File file = new File(attachmentRealmObject.getFilePath());
                if (!file.exists()) {
                    MessageManager.setAttachmentLocalPathToNull(attachmentRealmObject.getUniqueId());
                    return;
                }

                if (!attachmentRealmObject.isVoice()) {
                    manageOpeningFile(attachmentRealmObject);
                }
            } else {
                LogManager.d("VoiceDebug", "Download Starting Shortly! attachment.getUniqueId = " + attachmentRealmObject.getUniqueId());
                DownloadManager.getInstance().downloadFile(attachmentRealmObject, account, getActivity());
                if (attachmentRealmObject.isVoice()) {
                    showAutoDownloadDialog();
                }
            }
        }
        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
    }

    private void showAutoDownloadDialog() {
        if (!SettingsManager.autoDownloadVoiceMessageSuggested()) {
            if (!SettingsManager.chatsAutoDownloadVoiceMessage()) {
                if (getFragmentManager() != null && getFragmentManager().findFragmentByTag("VoiceDownloadDialog") == null) {
                    VoiceDownloadDialog dialog = VoiceDownloadDialog.newInstance(account);
                    dialog.show(getFragmentManager(), "VoiceDownloadDialog");
                }
            }
        }
    }

    private void manageOpeningFile(AttachmentRealmObject attachmentRealmObject) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        String path = attachmentRealmObject.getFilePath();
        i.setDataAndType(FileProvider.getUriForFile(getActivity(),
                getActivity().getApplicationContext().getPackageName()
                        + ".provider", new File(path)), attachmentRealmObject.getMimeType());
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            LogManager.exception(LOG_TAG, e);
            Toast.makeText(getActivity(), R.string.toast_could_not_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void waitForVoiceDownloadFinish(DownloadManager.ProgressData progressData) {
        if (progressData.isCompleted()) {
            if (progressData.getAttachmentId() != null && clickedAttachmentUID != null && clickedAttachmentUID.equals(progressData.getAttachmentId())) {
                VoiceManager.getInstance().voiceClicked(clickedMessageUID, clickedAttachmentPos, messageTimestamp);
            }
        }
    }

    class OpusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ignoreReceiver) {
                return;
            }
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                int type = bundle.getInt(OpusEvent.EVENT_TYPE, 0);
                switch (type) {
                    case OpusEvent.CONVERT_STARTED:
                    case OpusEvent.RECORD_STARTED:
                    case OpusEvent.PLAY_PROGRESS_UPDATE:
                    case OpusEvent.PLAY_GET_AUDIO_TRACK_INFO:
                    case OpusEvent.PLAYING_PAUSED:
                    case OpusEvent.PLAYING_STARTED:
                    case OpusEvent.RECORD_PROGRESS_UPDATE:
                        break;

                    case OpusEvent.RECORD_FINISHED:
                        if (sendImmediately) {
                            String path = VoiceManager.getInstance().getNewFilePath();
                            uploadVoiceFile(path, forwardIds);
                        } else {
                            String tempPath = VoiceManager.getInstance().getTempFilePath();
                            ((ChatActivity)getActivity()).setUpVoiceMessagePresenter(tempPath);
                        }
                        ignoreReceiver = true;
                        break;
                    case OpusEvent.RECORD_FAILED:
                        ((ChatActivity)getActivity()).finishVoiceRecordLayout();
                        Toast.makeText(Application.getInstance(), getResources().getString(R.string.VOICE_RECORDING_ERROR), Toast.LENGTH_LONG).show();
                        ignoreReceiver = true;
                        break;
                    case OpusEvent.PLAYING_FAILED:
                    case OpusEvent.CONVERT_FAILED:
                    case OpusEvent.CONVERT_FINISHED:
                    case OpusEvent.PLAYING_FINISHED:
                    default:
                        break;
                }
            }
        }
    }
}
