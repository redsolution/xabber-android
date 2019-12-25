package com.xabber.android.data.extension.references.voice;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.log.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;
import rx.subjects.PublishSubject;

import static android.media.MediaRecorder.AudioSource.MIC;


public final class VoiceManager implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaRecorder.OnErrorListener {

    private static final String LOG_TAG = VoiceManager.class.getSimpleName();

    private static VoiceManager instance;
    private MediaPlayer mp;
    private MediaRecorder mr;
    private Handler mHandler = new Handler();
    private String currentPlayingMessageId;
    private String currentPlayingAttachmentId;
    private String tempFilePath;
    private ArrayList<Float> waveForm = new ArrayList<>();
    private boolean isPaused = false;
    private boolean isRecording = false;
    private int voiceFileDuration;
    private int voiceAttachmentDuration;
    private boolean alreadySent = true;
    private Long messageTimestamp;

    public static final int COMPLETED_AUDIO_PROGRESS = 99;
    public static final int NORMAL_AUDIO_PROGRESS = 98;
    public static final int PAUSED_AUDIO_PROGRESS = 97;
    private static final int SAMPLING_RATE = 48000;

    public static VoiceManager getInstance() {
        if (instance == null)
            instance = new VoiceManager();
        return instance;
    }

    private VoiceManager() {

    }

    private Runnable createLocalWaveform = new Runnable() {
        @Override
        public void run() {
            if (mr != null) {
                waveForm.add((float) mr.getMaxAmplitude());
                mHandler.postDelayed(this, 30);
            }
        }
    };

    private Runnable updateAudioProgress = new Runnable() {
        @Override
        public void run() {
            publishAudioProgressWithCustomCode(NORMAL_AUDIO_PROGRESS);
            mHandler.postDelayed(this, 100);
        }
    };

    public void voiceClicked(MessageItem message) {
        voiceClicked(message, 0, null);
    }

    public void voiceClicked(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            voiceClicked("", "", filePath, null, false, null);
        }
    }

    public void voiceClicked(String messageId, int attachmentIndex, Long timestamp) {
        MessageItem messageItem = MessageDatabaseManager.getInstance().getRealmUiThread().where(MessageItem.class)
                .equalTo(MessageItem.Fields.UNIQUE_ID, messageId).findFirst();

        if (messageItem == null || messageItem.getAttachments() == null
                || messageItem.getAttachments().size() <= attachmentIndex
                || !messageItem.getAttachments().get(attachmentIndex).isVoice()) {
            return;
        }

        voiceClicked(messageItem, attachmentIndex, timestamp);
    }

    public void voiceClicked(MessageItem message, int attachmentIndex, Long timestamp) {
        if (message == null || message.getAttachments() == null
                || message.getAttachments().size() <= attachmentIndex
                || !message.getAttachments().get(attachmentIndex).isVoice())
            return;

        Attachment attachment = message.getAttachments().get(attachmentIndex);

        voiceClicked(message.getUniqueId(), attachment.getUniqueId(), attachment.getFileUrl(), attachment.getDuration(), true, timestamp);
    }

    private void voiceClicked(String messageId, String attachmentId, String filePath, Long duration, boolean alreadySentMessage, Long timestamp) {
        if (createPlayerIfNotInitialized()) {
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setOnPreparedListener(this);
            try {
                mp.setDataSource(filePath);
                checkDuration(duration, filePath, attachmentId);
                mp.prepareAsync();
                currentPlayingAttachmentId = attachmentId;
                currentPlayingMessageId = messageId;
                alreadySent = alreadySentMessage;
                messageTimestamp = timestamp;
            } catch (IOException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else {
            if (currentPlayingMessageId != null &&
                    currentPlayingMessageId.equals(messageId)) {

                if (currentPlayingAttachmentId != null &&
                        currentPlayingAttachmentId.equals(attachmentId)) {
                    manageSameVoicePlaybackControls(filePath);
                    checkDuration(duration, filePath, attachmentId);
                    currentPlayingAttachmentId = attachmentId;
                } else {
                    manageDiffVoicePlaybackControl(filePath);
                    checkDuration(duration, filePath, attachmentId);
                    currentPlayingAttachmentId = attachmentId;
                }
                currentPlayingMessageId = messageId;
                alreadySent = alreadySentMessage;
                messageTimestamp = timestamp;
            } else {
                manageDiffVoicePlaybackControl(filePath);
                checkDuration(duration, filePath, attachmentId);
                currentPlayingMessageId = messageId;
                currentPlayingAttachmentId = attachmentId;
                alreadySent = alreadySentMessage;
                messageTimestamp = timestamp;
            }
        }
    }

    public void startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mr == null) {
                mr = new MediaRecorder();
                mr.setOnErrorListener(this);
                mr.setAudioSource(MIC);
                try {
                    File tempAudioFile = FileManager.createTempAudioFile("temp_audio_recording");
                    tempFilePath = tempAudioFile.getAbsolutePath();
                    waveForm.clear();
                    mr.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                    mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mr.setOutputFile(tempFilePath);
                    mr.setAudioSamplingRate(SAMPLING_RATE);
                    mr.prepare();
                    mr.start();
                    mHandler.postDelayed(createLocalWaveform, 30);
                    isRecording = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                releaseMediaRecorder();
            }
        }
    }

    private boolean createPlayerIfNotInitialized(){
        if (mp == null) {
            mp = new MediaPlayer();
            return true;
        } else
            return false;
    }

    private void manageSameVoicePlaybackControls(String path) {
        if (mp.isPlaying()) {
            pause();
        } else {
            if (isPaused) {
                resume();
            } else {
                resetMediaPlayer();
                prepareMediaPlayer(path);
            }
        }
    }

    private void manageDiffVoicePlaybackControl(String path) {
        stop();
        resetMediaPlayer();
        prepareMediaPlayer(path);
    }

    private void stop() {
        if (mp != null) {
            mp.stop();
            isPaused = false;
            mHandler.removeCallbacks(updateAudioProgress);
            publishAudioProgressWithCustomCode(COMPLETED_AUDIO_PROGRESS);
        }
    }

    private void pause() {
        if (mp != null) {
            mp.pause();
            isPaused = true;
            mHandler.removeCallbacks(updateAudioProgress);
            publishAudioProgressWithCustomCode(PAUSED_AUDIO_PROGRESS);
        }
    }

    private void resume() {
        if (mp != null) {
            mp.start();
            isPaused = false;
            updateAudioProgressBar();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        isPaused = false;
        publishAudioProgressWithCustomCode(COMPLETED_AUDIO_PROGRESS);
        resetMediaPlayer();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        LogManager.e(LOG_TAG, "Error with MediaPlayer (" + i + ", " + i1 + "), releasing)");
        isPaused = false;
        releaseMediaPlayer();
        return false;
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int i, int i1) {
        LogManager.e(LOG_TAG, "Error with MediaRecorder (" + i + ", " + i1 + "), releasing)");
        isRecording = false;
        releaseMediaRecorder();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mp.start();
        isPaused = false;
        updateAudioProgressBar();
    }

    private void prepareMediaPlayer(String path) {
        try {
            mp.setDataSource(path);
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetMediaPlayer() {
        mHandler.removeCallbacks(updateAudioProgress);
        if (mp != null) {
            mp.reset();
        }
    }

    public void releaseMediaPlayer() {
        mHandler.removeCallbacks(updateAudioProgress);
        if (mp != null) {
            mp.reset();
            mp.release();
            mp = null;
            currentPlayingMessageId = null;
            currentPlayingAttachmentId = null;
        }
    }

    public void releaseMediaRecorder() {
        mHandler.removeCallbacks(createLocalWaveform);
        if (mr != null) {
            mr.reset();
            mr.release();
            mr = null;
        }
    }

    public void updateAudioProgressBar() {
        mHandler.postDelayed(updateAudioProgress, 100);
    }

    private void publishAudioProgressWithCustomCode(int resultCode) {
        if (mp != null) {
            int duration = getOptimalVoiceDuration();
            switch (resultCode) {
                case NORMAL_AUDIO_PROGRESS:
                    PublishAudioProgress.getInstance().updateAudioProgress(mp.getCurrentPosition(), duration,
                            alreadySent ? currentPlayingAttachmentId.hashCode() : 0, NORMAL_AUDIO_PROGRESS, messageTimestamp);
                    LogManager.d("VoiceDebug", "current : " + mp.getCurrentPosition() +
                            " max MP.getDuration: " + mp.getDuration() +
                            " max MMR.extractDur: " + voiceFileDuration +
                            " voice attachment duration: " + voiceAttachmentDuration);
                    break;
                case PAUSED_AUDIO_PROGRESS:
                    PublishAudioProgress.getInstance().updateAudioProgress(mp.getCurrentPosition(), duration,
                            alreadySent ? currentPlayingAttachmentId.hashCode() : 0, PAUSED_AUDIO_PROGRESS, messageTimestamp);
                    LogManager.d("VoiceDebug", "PAUSED AT current : " + mp.getCurrentPosition() +
                            " max MP.getDuration: " + mp.getDuration() +
                            " max MMR.extractDur: " + voiceFileDuration +
                            " voice attachment duration: " + voiceAttachmentDuration);
                    break;
                case COMPLETED_AUDIO_PROGRESS:
                    PublishAudioProgress.getInstance().updateAudioProgress(0, duration,
                            alreadySent ? currentPlayingAttachmentId.hashCode() : 0, COMPLETED_AUDIO_PROGRESS, messageTimestamp);
                    break;
            }
        }
    }

    public boolean stopRecording() {
        if (isRecording) {
            isRecording = false;
            releaseMediaRecorder();
            return true;
        }
        return false;
    }

    public String stopRecordingAndGetTempFilePath() {
        if (stopRecording()) {
            VoiceMessagePresenterManager.getInstance().addAndOptimizeWave(waveForm, tempFilePath);
            return tempFilePath;
        } else
            return null;
    }

    public String stopRecordingAndGetNewFilePath() {
        if (stopRecording()) {
            try {
                File audioFile = FileManager.createAudioFile("voice_message.ogg");
                if (FileManager.copy(new File(tempFilePath), audioFile)) {
                    if (audioFile != null) {
                        VoiceMessagePresenterManager.getInstance().addAndOptimizeWave(waveForm, audioFile.getPath());
                        return audioFile.getPath();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                deleteRecordedFile();
                return null;
            }
        }
        return null;
    }

    public String getStoppedRecordingNewFilePath(String path) {
        try {
            File audioFile = FileManager.createAudioFile("voice_message.ogg");
            if (FileManager.copy(new File(path), audioFile)) {
                if (audioFile != null) {
                    VoiceMessagePresenterManager.getInstance().modifyFilePathIfSaved(tempFilePath, audioFile.getPath());
                    return audioFile.getPath();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            deleteRecordedFile();
        }
        return null;
    }

    public void deleteRecordedFile() {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                if (tempFilePath != null) {
                    FileManager.deleteTempFile(tempFilePath);
                }
            }
        });
        tempFilePath = null;
    }

    private void checkDuration(Long duration, String filePath, String attachmentId) {
        voiceFileDuration = 0;
        if (duration == null) {
            setDurationIfEmpty(filePath, attachmentId);
        } else {
            voiceAttachmentDuration = longToIntConverter(duration);
        }
    }

    private void setDurationIfEmpty(final String path, final String id) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                if (path != null) {
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
                                    if (backgroundAttachment != null)
                                        backgroundAttachment.setDuration(Long.valueOf(dur) / 1000);
                                }
                            });
                        } finally {
                            if (realm != null)
                                realm.close();
                        }
                    }
                }
            }
        });
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

    private void publishCompletedAudioProgress(int attachmentIdHash) {
        PublishAudioProgress.getInstance().updateAudioProgress(0, getOptimalVoiceDuration(), attachmentIdHash, COMPLETED_AUDIO_PROGRESS, messageTimestamp);
    }

    public static class PublishAudioProgress {

        private static PublishAudioProgress instance;
        private PublishSubject<AudioInfo> subject;

        public static PublishAudioProgress getInstance() {
            if (instance == null) instance = new PublishAudioProgress();
            return instance;
        }

        public void updateAudioProgress(int currentPosition, int duration, int attachmentIdHash, int resultCode, Long timestamp) {
            subject.onNext(new AudioInfo(currentPosition, duration, attachmentIdHash, resultCode, timestamp));
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
            final int attachmentIdHash;
            final int resultCode;
            final Long timestamp;

            public AudioInfo(int currentPosition, int duration, int attachmentIdHash, int resultCode, Long timestamp) {
                this.currentPosition = currentPosition;
                this.duration = duration;
                this.attachmentIdHash = attachmentIdHash;
                this.resultCode = resultCode;
                this.timestamp = timestamp;
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

            public int getAttachmentIdHash() {
                return attachmentIdHash;
            }

            public Long getTimestamp() {
                return timestamp;
            }
        }
    }
}

