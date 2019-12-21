package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.references.ReferenceElement;
import com.xabber.android.data.extension.references.VoiceMessagePresenterManager;
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
import java.io.IOException;
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

import static android.media.MediaRecorder.AudioSource.MIC;

public class FileInteractionFragment extends Fragment implements FileMessageVH.FileListener,
        AttachDialog.Listener, ForwardedAdapter.ForwardListener {

    private static final String LOG_TAG = FileInteractionFragment.class.getSimpleName();

    private static final String SAVE_ACCOUNT = "com.xabber.android.ui.fragment.ARGUMENT_ACCOUNT";
    private static final String SAVE_USER = "com.xabber.android.ui.fragment.ARGUMENT_USER";
    private static final String SAVE_CURRENT_PICTURE_PATH = "com.xabber.android.ui.fragment.ARGUMENT_CURRENT_PICTURE_PATH";
    public static final int COMPLETED_AUDIO_PROGRESS = 99;
    public static final int NORMAL_AUDIO_PROGRESS = 98;
    public static final int PAUSED_AUDIO_PROGRESS = 97;
    private static final int SAMPLING_RATE = 48000;
    private static final int ENCODING_BIT_RATE = 96000;

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
    private boolean recordingInProgress = false;
    private MediaPlayer mp;
    private ArrayList<Float> waveForm = new ArrayList<>();
    private int currentTime;
    private int maxTime;
    private MediaRecorder mr;
    private Handler mHandler = new Handler();
    private int voiceAttachmentHash;
    private int voiceFileDuration;
    private int voiceAttachmentDuration;
    private PublishSubject<DownloadManager.ProgressData> voiceDownload;
    private Subscription voiceDownloadSubscription;

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

    @Override
    public void onDetach() {
        super.onDetach();
        mHandler.removeCallbacks(updateAudioProgress);
        if (voiceDownloadSubscription != null) voiceDownloadSubscription.unsubscribe();
        if (mp != null) {
            mp.release();
            mp = null;
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
    public void onVoiceClick(int messagePosition, int attachmentPosition, String attachmentId, String messageUID, boolean saved) {
        LogManager.d("VoiceDebug", "onVoiceClick start! attachmentPosition = " + attachmentPosition + " attachmentId = " + attachmentId + " messageUID = " + messageUID);
        clickedAttachmentPos = attachmentPosition;
        clickedMessageUID = messageUID;
        clickedAttachmentUID = attachmentId;
        currentTime = -1;
        maxTime = -1;
        if (!saved) subscribeForVoiceDownloadProgress();
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(
                this, PERMISSIONS_REQUEST_DOWNLOAD_FILE))
            openFileOrDownload(messageUID, attachmentPosition);
    }

    //@Override
    //public void onVoiceProgressClick(int messagePosition, int attachmentPosition, String attachmentId, String messageUID, int current, int max) {
    //    clickedAttachmentPos = attachmentPosition;
    //    clickedMessageUID = messageUID;
    //    clickedAttachmentUID = attachmentId;
    //    currentTime = current;
    //    maxTime = max;
    //    openFileOrDownload(messageUID, attachmentPosition);
    //}

    protected void subscribeForVoiceDownloadProgress() {
        LogManager.d("VoiceDebug", "progress Subscribed to!~");
        voiceDownload = DownloadManager.getInstance().subscribeForProgress();
        if (voiceDownloadSubscription != null) voiceDownloadSubscription.unsubscribe();
        voiceDownloadSubscription = voiceDownload.doOnNext(new Action1<DownloadManager.ProgressData>() {
            @Override
            public void call(DownloadManager.ProgressData progressData) {
                waitForVoiceDownloadFinish(progressData);
            }
        }).subscribe();
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
            String serverName = account.getFullJid().getDomain().toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getActivity().getResources().getString(R.string.error_file_upload_not_support, serverName))
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

    protected final Runnable record = new Runnable() {
        @Override
        public void run() {
            onVoiceRecordPressed();
        }
    };

    protected final Runnable createLocalWaveform = new Runnable() {
        @Override
        public void run() {
            if (mr != null) {
                waveForm.add((float) mr.getMaxAmplitude());
                mHandler.postDelayed(this, 30);
            }
        }
    };

    private void onVoiceRecordPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            mr = new MediaRecorder();
            mr.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                @Override
                public void onError(MediaRecorder mediaRecorder, int i, int i1) {
                    LogManager.e(LOG_TAG, "Error with MediaRecorder (" + i + ", " + i1 + "), releasing)");
                    mediaRecorder.reset();
                    mediaRecorder.release();
                    mr = null;
                }
            });
            mr.setAudioSource(MIC);
            try {
                File tempAudioFile = FileManager.createTempAudioFile("temp_audio_recording");
                tempFilePath = tempAudioFile.getAbsolutePath();
                waveForm.clear();
                mr.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

                mr.setOutputFile(tempFilePath);
                mr.setAudioSamplingRate(SAMPLING_RATE);
                mr.setAudioEncodingBitRate(ENCODING_BIT_RATE);
                mr.prepare();
                mr.start();
                mHandler.postDelayed(createLocalWaveform, 30);
                recordingInProgress = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String tempFilePath;

    String stopRecordingIfPossibleForCheckup() {
        if (recordingInProgress) {
            recordingInProgress = false;
            if (mr != null) {
                releaseMediaRecorder();
                VoiceMessagePresenterManager.getInstance().addAndOptimizeWave(waveForm, tempFilePath);
                File file = new File(tempFilePath);
                if (file.exists()) return tempFilePath;
                else return null;
            }
        }
        return null;
    }

    boolean releaseRecordedVoicePlayback(String filePath) {
        releaseMediaPlayer();
        VoiceMessagePresenterManager.getInstance().deleteOldPath(filePath);
        File file = new File(filePath);
        if (file.exists()) {
            FileManager.deleteTempFile(file);
            return !file.exists();
        }
        return true;
    }



    void sendStoppedVoiceMessage(String filePath) {
        releaseMediaPlayer();
        try {
            File audioFile = FileManager.createAudioFile("voice_message.ogg");
            if (FileManager.copy(new File(filePath), audioFile)) {
                if (audioFile != null) {
                    VoiceMessagePresenterManager.getInstance().modifyFilePathIfSaved(tempFilePath, audioFile.getPath());
                    uploadVoiceFile(audioFile.getPath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stopRecordingIfPossible(boolean saveFile) {
        if (recordingInProgress) {
            recordingInProgress = false;
            if (mr != null) {
                releaseMediaRecorder();
                if (saveFile) {
                    try {
                        File audioFile = FileManager.createAudioFile("voice_message.ogg");
                        if (FileManager.copy(new File(tempFilePath), audioFile)) {
                            if (audioFile != null) {
                                VoiceMessagePresenterManager.getInstance().addAndOptimizeWave(waveForm, audioFile.getPath());
                                uploadVoiceFile(audioFile.getPath());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!saveFile) FileManager.deleteTempFile(new File(tempFilePath));
        }
    }

    void stopRecordingIfPossibleAsync(final boolean saveFile) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                stopRecordingIfPossible(saveFile);
            }
        });
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

    private void uploadVoiceFile(String path) {
        List<String> paths = new ArrayList<>();
        paths.add(path);
        HttpFileUploadManager.getInstance().uploadFile(account, user, paths, null, null, ReferenceElement.Type.voice.name(), getActivity());
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
        LogManager.d("VoiceDebug", "openFileOrDownload start! attachmentPosition = " + attachmentPosition + " messageUID = " + messageUID);

        if (messageItem == null) {
            LogManager.w(LOG_TAG, "onMessageFileClick: null message item. UID: " + messageUID);
            return;
        }

        if (messageItem.haveAttachments()) {
            RealmList<Attachment> fileAttachments = new RealmList<>();
            for (Attachment attachment : messageItem.getAttachments()) {
                if (!attachment.isImage()) fileAttachments.add(attachment);
            }

            final Attachment attachment = fileAttachments.get(attachmentPosition);
            if (attachment == null) return;

            LogManager.d("VoiceDebug", "openFileOrDownload fork! dl or open?");
            if (attachment.getFilePath() != null) {
                LogManager.d("VoiceDebug", "Opening file shortly!");
                File file = new File(attachment.getFilePath());
                if (!file.exists()) {
                    MessageManager.setAttachmentLocalPathToNull(attachment.getUniqueId());
                    return;
                }

                if (attachment.isVoice()) {
                    manageVoicePlayback(attachment);
                } else {
                    manageOpeningFile(attachment);
                }
            } else {
                LogManager.d("VoiceDebug", "Download Starting Shortly! attachment.getUniqueId = " + attachment.getUniqueId());
                DownloadManager.getInstance().downloadFile(attachment, account, getActivity());
            }
        }
    }

    private void manageOpeningFile(Attachment attachment) {
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
    }

    void releaseMediaRecorder() {
        mHandler.removeCallbacks(createLocalWaveform);
        if (mr != null) {
            mr.reset();
            mr.release();
            mr = null;
        }
    }

    void clearCachedVoiceFile() {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                if (tempFilePath != null) {
                    FileManager.deleteTempFile(tempFilePath);
                }
            }
        });
    }

    void releaseMediaPlayer() {
        if (mp != null) {
            mp.reset();
            mp.release();
            mp = null;
        }
    }

    void changeVoicePlayback() {

    }

    void manageRecordedVoicePlayback(String filePath) {
        if (mp == null) {
            voiceAttachmentHash = 0;
            mp = new MediaPlayer();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    LogManager.i(LOG_TAG, "onCompletion() has been called");
                    mHandler.removeCallbacks(updateAudioProgress);
                    publishCompletedAudioProgress(voiceAttachmentHash);
                    releaseMediaPlayer();
                }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    LogManager.e(LOG_TAG, "Error with MediaPlayer (" + i + ", " + i1 + "), releasing)");
                    mHandler.removeCallbacks(updateAudioProgress);
                    mediaPlayer.reset();
                    mediaPlayer.release();
                    mp = null;
                    return false;
                }
            });
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mp.start();
                    updateAudioProgressBar();
                }
            });
            try {
                mp.setDataSource(filePath);
                mp.prepareAsync();
            } catch (IOException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else {
            if (voiceAttachmentHash == 0) {
                if (mp.isPlaying()) {
                    mp.pause();
                    publishAudioProgressWithCustomCode(PAUSED_AUDIO_PROGRESS);
                    mHandler.removeCallbacks(updateAudioProgress);
                } else {
                    mp.start();
                    updateAudioProgressBar();
                }
            } else {
                if (mp.isPlaying())
                    mp.stop();
                publishCompletedAudioProgress(voiceAttachmentHash);
                mp.reset();
                mHandler.removeCallbacks(updateAudioProgress);
                try {
                    voiceAttachmentHash = 0;
                    mp.setDataSource(filePath);
                    mp.prepareAsync();
                } catch (IOException e) {
                    LogManager.exception(LOG_TAG, e);
                }
            }
        }
    }

    private void manageVoicePlayback(Attachment attachment) {
        final String path = attachment.getFilePath();
        final String id = attachment.getUniqueId();
        if (path == null) {
            LogManager.e(LOG_TAG, "Error with media playback! Local attachment path is null, abandoning playback");
            return;
        }
        if (mp == null) {
            mp = new MediaPlayer();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    LogManager.i(LOG_TAG, "onCompletion() has been called");
                    mHandler.removeCallbacks(updateAudioProgress);
                    publishCompletedAudioProgress(voiceAttachmentHash);
                    releaseMediaPlayer();
                }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    LogManager.e(LOG_TAG, "Error with MediaPlayer (" + i + ", " + i1 + "), releasing)");
                    mHandler.removeCallbacks(updateAudioProgress);
                    mediaPlayer.reset();
                    mediaPlayer.release();
                    mp = null;
                    return false;
                }
            });
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    if (currentTime != -1 && maxTime != -1) {
                        mp.seekTo(currentTime);
                        return;
                    }
                    mp.start();
                    updateAudioProgressBar();
                }
            });
            try {
                if (attachment.getDuration() == null)
                        setDurationIfEmpty(path, id);
                else voiceAttachmentDuration = longToIntConverter(attachment.getDuration());
                voiceAttachmentHash = id.hashCode();
                if (attachment.getFileUrl() == null) {
                    if (attachment.getFilePath() != null)
                        mp.setDataSource(attachment.getFilePath());
                    else {
                        releaseMediaPlayer();
                        return;
                    }
                } else mp.setDataSource(attachment.getFileUrl());
                mp.prepareAsync();
            } catch (IOException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else if (voiceAttachmentHash == attachment.getUniqueId().hashCode()) {
            if (mp.isPlaying()) {
                mp.pause();
                if (currentTime != -1 && maxTime != -1) {
                    mp.seekTo(currentTime);
                } else {
                    publishAudioProgressWithCustomCode(PAUSED_AUDIO_PROGRESS);
                    mHandler.removeCallbacks(updateAudioProgress);
                }
            } else {
                if (currentTime != -1 && maxTime != -1) {
                    mp.seekTo(currentTime);
                } else {
                    mp.start();
                    updateAudioProgressBar();
                }
            }
        } else {
            if (mp.isPlaying()) {
                mp.stop();
            }
            publishCompletedAudioProgress(voiceAttachmentHash);
            mp.reset();
            mHandler.removeCallbacks(updateAudioProgress);
            try {
                if (attachment.getDuration() == null)
                    setDurationIfEmpty(path, id);
                else voiceAttachmentDuration = longToIntConverter(attachment.getDuration());
                voiceAttachmentHash = id.hashCode();
                mp.setDataSource(attachment.getFileUrl());
                mp.prepareAsync();
            } catch (IOException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
    }

    private void setDurationIfEmpty(final String path, final String id) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(path);
                final String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                voiceFileDuration = Integer.valueOf(dur);
                if (voiceFileDuration != 0) {
                    Realm realm = null;
                    try {
                        realm = MessageDatabaseManager.getInstance().getNewBackgroundRealm();
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                Attachment backgroundAttachment = realm.where(Attachment.class).equalTo(Attachment.Fields.UNIQUE_ID, id).findFirst();
                                backgroundAttachment.setDuration(Long.valueOf(dur) / 1000);
                            }
                        });
                    } finally {
                        if (realm != null)
                            realm.close();
                    }
                }
            }
        });
    }

    private void waitForVoiceDownloadFinish(DownloadManager.ProgressData progressData) {
        if (progressData.isCompleted()) {
            LogManager.d("VoiceDebug", "Download Completed Progress Data arrived! attachmentId = " + progressData.getAttachmentId());
            if (progressData.getAttachmentId() != null && clickedAttachmentUID != null && clickedAttachmentUID.equals(progressData.getAttachmentId())) {
                final Handler handler = new Handler();
                final String messageId = clickedMessageUID;
                final int attachmentPosition = clickedAttachmentPos;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openFileOrDownload(messageId, attachmentPosition);
                        handler.removeCallbacks(null);
                    }
                }, 50);
            }
        }
    }


    public void updateAudioProgressBar() {
        mHandler.postDelayed(updateAudioProgress, 100);
    }

    private Runnable updateAudioProgress = new Runnable() {
        @Override
        public void run() {
            publishAudioProgressWithCustomCode(NORMAL_AUDIO_PROGRESS);
            mHandler.postDelayed(this, 100);
        }
    };

    private void publishAudioProgressWithCustomCode(int resultCode) {
        if (mp != null) {
            int duration = getOptimalVoiceDuration();
            PublishAudioProgress.getInstance().updateAudioProgress(mp.getCurrentPosition(), duration, voiceAttachmentHash, resultCode);
            LogManager.d("VoiceDebug", "current : " + mp.getCurrentPosition() + " max MP.getDuration: " + mp.getDuration() + " max MMR.extractDur: " + voiceFileDuration);
        }
    }

    private int getOptimalVoiceDuration() {
        if (voiceFileDuration != 0)
            return voiceFileDuration;
        else if (mp.getDuration() > 500) return mp.getDuration();
        else return voiceAttachmentDuration;
    }

    public Integer longToIntConverter(long number) {
        if (number <= Integer.MAX_VALUE && number > 0)
            return (int) number;
        else if (number > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else return 0;
    }

    private void publishCompletedAudioProgress(int attachmentId) {
        PublishAudioProgress.getInstance().updateAudioProgress(0, getOptimalVoiceDuration(), attachmentId, COMPLETED_AUDIO_PROGRESS);
    }

    public static class PublishAudioProgress {

        private static PublishAudioProgress instance;
        private PublishSubject<AudioInfo> subject;

        public static PublishAudioProgress getInstance() {
            if (instance == null) instance = new PublishAudioProgress();
            return instance;
        }

        public void updateAudioProgress(int currentPosition, int duration, int attachmentId, int resultCode) {
            subject.onNext(new AudioInfo(currentPosition, duration, attachmentId, resultCode));
        }

        private PublishAudioProgress() {
            createSubject();
        }

        private void createSubject() {
            subject = PublishSubject.create();
        }


        public PublishSubject<AudioInfo> subscribeForProgress() {
            return subject;
        }

        public class AudioInfo {
            final int currentPosition;
            final int duration;
            final int attachmentId;
            final int resultCode;

            public AudioInfo(int currentPosition, int duration, int attachmentId, int resultCode) {
                this.currentPosition = currentPosition;
                this.duration = duration;
                this.attachmentId = attachmentId;
                this.resultCode = resultCode;
            }

            public int getDuration() {
                return duration;
            }

            public int getCurrentPosition() {
                return currentPosition;
            }

            public int getResultCode() {
                return resultCode;
            }

            public int getAttachmentId() {
                return attachmentId;
            }
        }
    }
}
