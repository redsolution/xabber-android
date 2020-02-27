package com.xabber.android.data.extension.references.voice;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.Attachment;
import com.xabber.android.data.database.realmobjects.MessageItem;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.service.RecordService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;
import rx.subjects.PublishSubject;


public final class VoiceManager implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaRecorder.OnErrorListener, MediaPlayer.OnSeekCompleteListener, AudioManager.OnAudioFocusChangeListener {

    private static final String LOG_TAG = VoiceManager.class.getSimpleName();

    private static VoiceManager instance;
    private AudioManager audioManager;
    private AudioAttributes audioAttributes;
    private AudioFocusRequest audioFocusRequest;
    private MediaPlayer mp;
    //private MediaRecorder mr;
    private Handler mHandler = new Handler();
    private String currentPlayingMessageId;
    private String currentPlayingAttachmentId;
    private Long messageTimestamp;
    private boolean alreadySent = true;
    private String clickedMessageId;
    private String clickedAttachmentId;
    private String clickedFilePath;
    private Long clickedDuration;
    private Long clickedTimestamp;
    private boolean clickedAlreadySent;
    private String tempOpusPath;
    private ArrayList<Float> waveForm = new ArrayList<>();
    private boolean isPaused = false;
    private boolean mPlaybackDelayed = false;
    private boolean activeFocus = false;
    private int voiceFileDuration;
    private int voiceAttachmentDuration;
    private int result;

    private final Object mFocusLock = new Object();

    public static final int COMPLETED_AUDIO_PROGRESS = 99;
    public static final int NORMAL_AUDIO_PROGRESS = 98;
    public static final int PAUSED_AUDIO_PROGRESS = 97;
    public static final int NO_AUDIO_FOCUS_MANAGEMENT_FOR_PLAYBACK = 260;
    private static final int SAMPLING_RATE = 48000;

    public static VoiceManager getInstance() {
        if (instance == null)
            instance = new VoiceManager();
        return instance;
    }

    private VoiceManager() {
        audioManager = (AudioManager) Application.getInstance().getSystemService(Context.AUDIO_SERVICE);
    }

    //private Runnable createLocalWaveform = new Runnable() {
    //    @Override
    //    public void run() {
    //        if (mr != null) {
    //            waveForm.add((float) mr.getMaxAmplitude());
    //            mHandler.postDelayed(this, 30);
    //        }
    //    }
    //};

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
        Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
        MessageItem messageItem = realm
                .where(MessageItem.class)
                .equalTo(MessageItem.Fields.UNIQUE_ID, messageId)
                .findFirst();

        if (messageItem == null || messageItem.getAttachments() == null
                || messageItem.getAttachments().size() <= attachmentIndex
                || !messageItem.getAttachments().get(attachmentIndex).isVoice()) {
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper()) realm.close();

        voiceClicked(messageItem, attachmentIndex, timestamp);
    }

    public void voiceClicked(MessageItem message, int attachmentIndex, Long timestamp) {
        if (message == null || message.getAttachments() == null
                || message.getAttachments().size() <= attachmentIndex
                || !message.getAttachments().get(attachmentIndex).isVoice())
            return;

        Attachment attachment = message.getAttachments().get(attachmentIndex);

        String path;
        if (message.isInProgress()) {
            path = attachment.getFilePath();
        } else {
            if (attachment.getFilePath() != null) {
                if (new File(attachment.getFilePath()).exists())
                    path = attachment.getFilePath();
                else {
                    MessageManager.setAttachmentLocalPathToNull(attachment.getUniqueId());
                    return;
                }
            } else
                path = attachment.getFileUrl();
        }
        voiceClicked(message.getUniqueId(), attachment.getUniqueId(), path, attachment.getDuration(), true, timestamp);
    }

    private void voiceClicked(String messageId, String attachmentId, String filePath, Long duration, boolean alreadySentMessage, Long timestamp) {
        if (filePath == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mPlaybackDelayed = false;
            if (!activeFocus) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(audioAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(this, mHandler)
                        .build();
                result = audioManager.requestAudioFocus(audioFocusRequest);
            }
        } else {
            if (!activeFocus)
                result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }

        clickedMessageId = messageId;
        clickedAttachmentId = attachmentId;
        clickedFilePath = filePath;
        clickedDuration = duration;
        clickedAlreadySent = alreadySentMessage;
        clickedTimestamp = timestamp;

        synchronized (mFocusLock) {
            if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                mPlaybackDelayed = false;
            } else if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlaybackDelayed = false;
                activeFocus = true;
                startPlayback();
            } else if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                mPlaybackDelayed = true;
            }
        }
    }

    private void startPlayback() {
        if (createPlayerIfNotInitialized()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mp.setAudioAttributes(audioAttributes);
            else
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setOnPreparedListener(this);
            mp.setOnSeekCompleteListener(this);
            try {
                mp.setDataSource(clickedFilePath);
                checkDuration(clickedDuration, clickedFilePath, clickedAttachmentId);
                mp.prepareAsync();
                currentPlayingAttachmentId = clickedAttachmentId;
                currentPlayingMessageId = clickedMessageId;
                alreadySent = clickedAlreadySent;
                messageTimestamp = clickedTimestamp;
            } catch (IOException e) {
                LogManager.exception(LOG_TAG, e);
            }
        } else {
            if ((messageTimestamp != null && messageTimestamp.equals(clickedTimestamp)
                 ||
                 messageTimestamp == null && clickedTimestamp == null)
                        &&
                        currentPlayingMessageId != null && currentPlayingMessageId.equals(clickedMessageId)) {

                if (currentPlayingAttachmentId != null &&
                        currentPlayingAttachmentId.equals(clickedAttachmentId)) {
                    manageSameVoicePlaybackControls(clickedFilePath);
                    checkDuration(clickedDuration, clickedFilePath, clickedAttachmentId);
                    currentPlayingAttachmentId = clickedAttachmentId;
                } else {
                    manageDiffVoicePlaybackControl(clickedFilePath);
                    checkDuration(clickedDuration, clickedFilePath, clickedAttachmentId);
                    currentPlayingAttachmentId = clickedAttachmentId;
                }
                currentPlayingMessageId = clickedMessageId;
                alreadySent = clickedAlreadySent;
                messageTimestamp = clickedTimestamp;

            } else {
                manageDiffVoicePlaybackControl(clickedFilePath);
                checkDuration(clickedDuration, clickedFilePath, clickedAttachmentId);
                currentPlayingMessageId = clickedMessageId;
                currentPlayingAttachmentId = clickedAttachmentId;
                alreadySent = clickedAlreadySent;
                messageTimestamp = clickedTimestamp;
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

    public boolean playbackInProgress(String attachmentId, Long timestamp) {
        if (mp != null) {
            if (attachmentId != null && attachmentId.equals(currentPlayingAttachmentId)
                    && (timestamp != null && timestamp.equals(messageTimestamp) || (timestamp == null && messageTimestamp == null))) {
                return mp.isPlaying() || isPaused;
            }
        }
        return false;
    }

    public void seekAudioPlaybackTo(String attachmentId, Long timestamp, int current, int max) {
        if (playbackInProgress(attachmentId, timestamp)) {
            mHandler.removeCallbacks(updateAudioProgress);
            int seekToTime;
            int duration;
            if (mp.getDuration() > 0) {
                duration = mp.getDuration();
            } else {
                duration = getOptimalVoiceDuration();
                if (duration < 500) {
                    duration = duration * 1000;
                }
            }
            if ((float) current / (float) max <= 1 && (float) current / (float) max >= 0)
                seekToTime = duration * current / max;
            else if (current / max < 0)
                seekToTime = 0;
            else
                seekToTime = duration;

            mp.seekTo(seekToTime);
        } else return;
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
            if (mp.isPlaying()) {
                mp.stop();
                publishAudioProgressWithCustomCode(COMPLETED_AUDIO_PROGRESS);
            }
            isPaused = false;
            mHandler.removeCallbacks(updateAudioProgress);
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
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mPlaybackDelayed) {
                    synchronized (mFocusLock) {
                        mPlaybackDelayed = false;
                    }
                    startPlayback();
                }
                activeFocus = true;
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                synchronized (mFocusLock) {
                    mPlaybackDelayed = false;
                }
                releaseMediaPlayer();
                activeFocus = false;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                synchronized (mFocusLock) {
                    mPlaybackDelayed = false;
                }
                pause();
                activeFocus = false;
                break;
        }
    }

    private void releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioManager != null && audioFocusRequest != null) audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            if (audioManager != null) audioManager.abandonAudioFocus(this);
        }
        activeFocus = false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        isPaused = false;
        publishAudioProgressWithCustomCode(COMPLETED_AUDIO_PROGRESS);
        resetMediaPlayer();
        releaseAudioFocus();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        LogManager.e(LOG_TAG, "Error with MediaPlayer (" + i + ", " + i1 + "), releasing)");
        isPaused = false;
        releaseMediaPlayer();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        resume();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            updateAudioProgressBar();
            LogManager.d(LOG_TAG, "SeekTo Completed!, current position: " + mediaPlayer.getCurrentPosition() + " / " + mediaPlayer.getDuration());
            if (isPaused)
                mp.pause();
        }
    }

    private void prepareMediaPlayer(String path) {
        try {
            if (path != null) {
                mp.setDataSource(path);
                mp.prepareAsync();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetMediaPlayer() {
        mHandler.removeCallbacks(updateAudioProgress);
        if (mp != null) {
            mp.reset();
            currentPlayingMessageId = null;
            currentPlayingAttachmentId = null;
        }
    }

    public void releaseMediaPlayer() {
        mHandler.removeCallbacks(updateAudioProgress);
        releaseAudioFocus();
        if (mp != null) {
            mp.reset();
            mp.release();
            mp = null;
            currentPlayingMessageId = null;
            currentPlayingAttachmentId = null;
        }
    }

    public void startRecording() {
        File tempOpusFile;
        try {
            waveForm.clear();
            tempOpusFile = FileManager.createTempOpusFile("tempOpusFile");
            RecordService.record(Application.getInstance().getApplicationContext(), tempOpusFile.getPath());
            tempOpusPath = tempOpusFile.getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int i, int i1) {
        LogManager.e(LOG_TAG, "Error with MediaRecorder (" + i + ", " + i1 + "), releasing)");
        releaseMediaRecorder();
    }


    public void releaseMediaRecorder() {
        //mHandler.removeCallbacks(createLocalWaveform);
        stopRecording(false);
        //if (mr != null) {
        //    mr.reset();
        //    mr.release();
        //    mr = null;
        //}
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
                    LogManager.d("VoiceDebug", "COMPLETED: " + " max MP.getDuration: " + mp.getDuration() +
                            " max MMR.extractDur: " + voiceFileDuration + " voice attachment duration: " + voiceAttachmentDuration);
                    break;
            }
        }
    }

    public boolean stopRecording(boolean deleteTempFile) {
        try {
            RecordService.stopRecording(Application.getInstance().getApplicationContext());
            if (deleteTempFile)
                deleteRecordedFile();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //public String stopRecordingAndGetTempFilePath() {
    //    if(stopRecording()) {
    //        VoiceMessagePresenterManager.getInstance().addAndOptimizeWave(opusRecorder.getSampleArray(), tempOpusPath);
    //        return getTempFilePath();
    //    } else return null;
    //}

    //public String stopRecordingAndGetNewFilePath() {
    //    stopRecording();
    //    File file;
    //    try {
    //        file = FileManager.createAudioFile("Voice Message.ogg");
    //        FileManager.copy(new File(tempOpusPath), file);
    //    } catch (IOException e) {
    //        e.printStackTrace();
    //        deleteRecordedFile();
    //        return null;
    //    }
    //    if (file == null)
    //        return null;
    //
    //    VoiceMessagePresenterManager.getInstance().addAndOptimizeWave(opusRecorder.getSampleArray(), file.getPath());
    //    return file.getPath();
    //}

    public String getTempFilePath() {
        return tempOpusPath;
    }

    public String getNewFilePath() {
        File file;
        try {
            file = FileManager.createAudioFile("Voice Message.ogg");
            FileManager.copy(new File(tempOpusPath), file);
        } catch (IOException e) {
            e.printStackTrace();
            deleteRecordedFile();
            return null;
        }
        if (file == null)
            return null;

        return file.getPath();
    }

    public String getStoppedRecordingNewFilePath(String path) {
        File file;
        try {
            file = FileManager.createAudioFile("Voice Message.ogg");
            FileManager.copy(new File(path), file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (file == null)
            return null;
        VoiceMessagePresenterManager.getInstance().modifyFilePathIfSaved(path, file.getPath());
        FileManager.deleteTempFile(path);
        return file.getPath();
    }

    public void deleteRecordedFile() {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                if (tempOpusPath != null) {
                    FileManager.deleteTempFile(tempOpusPath);
                }
            }
        });
        tempOpusPath = null;
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
        Application.getInstance().runInBackground(() -> {
            final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            if (path != null) {
                mmr.setDataSource(path);

                final String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                voiceFileDuration = Integer.valueOf(dur);
                if (voiceFileDuration != 0) {
                    Realm realm = null;
                    try {
                        realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                        realm.executeTransaction(realm1 -> {
                                Attachment backgroundAttachment = realm1
                                        .where(Attachment.class)
                                        .equalTo(Attachment.Fields.UNIQUE_ID, id)
                                        .findFirst();
                                if (backgroundAttachment != null)
                                    backgroundAttachment.setDuration(Long.valueOf(dur) / 1000);
                        });
                    } catch (Exception e) {
                        LogManager.exception(LOG_TAG, e);
                    } finally { if (realm != null) realm.close(); }
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

