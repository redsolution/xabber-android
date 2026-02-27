package com.xabber.android.data.extension.references.mutable.voice;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.widget.PlayerVisualizerView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class VoiceMessagePresenterManager {

    private static VoiceMessagePresenterManager instance;
    private Handler handler = new Handler();

    private static final Map<String, ArrayList<Integer>> voiceWaveData = new HashMap<>();
    private static final Map<String, PlayerVisualizerView> voiceWaveFreshViews = new HashMap<>();
    private static final ArrayList<String> voiceWaveInProgress = new ArrayList<>();
    private static final ArrayList<String> voiceWaveForRemoval = new ArrayList<>();

    public static VoiceMessagePresenterManager getInstance() {
        if (instance == null)
            instance = new VoiceMessagePresenterManager();
        return instance;
    }

    public VoiceMessagePresenterManager() {
        handler.postDelayed(refreshAvailableViews, 1000);
    }


    private Runnable refreshAvailableViews = new Runnable() {
        @Override
        public void run() {
            int size = voiceWaveInProgress.size();
            if (size != 0) {
                for (int i=0;i<size;i++) {
                    String voicePath = voiceWaveInProgress.get(i);
                    if (voiceWaveData.get(voicePath)!=null) {
                        PlayerVisualizerView view = voiceWaveFreshViews.get(voicePath);
                        if (view != null) {
                            view.updateVisualizer(voiceWaveData.get(voicePath));
                            voiceWaveFreshViews.remove(voicePath);
                            voiceWaveForRemoval.add(voicePath);
                        }
                    }
                }
            }
            int removeSize = voiceWaveForRemoval.size();
            if (removeSize != 0) {
                for (int i = 0; i < removeSize; i++) {
                    voiceWaveInProgress.remove(voiceWaveForRemoval.get(i));
                }
                voiceWaveForRemoval.clear();
            }
            handler.postDelayed(this, 1000);
        }
    };


    public void addAndOptimizeWave(ArrayList<Float> wave, String filePath) {
        ArrayList<Integer> optimized = new ArrayList<>();
        optimizeWaveData(wave, optimized);
        voiceWaveData.put(filePath, optimized);
    }

    public void modifyFilePathIfSaved(String oldPath, String newPath) {
        if (voiceWaveData.get(oldPath) != null) {
            voiceWaveData.put(newPath, voiceWaveData.get(oldPath));
            voiceWaveData.remove(oldPath);
        }
    }

    public void deleteOldPath(String oldPath) {
        if (voiceWaveData.get(oldPath) != null) {
            voiceWaveData.remove(oldPath);
        }
    }

    public void sendWaveDataIfSaved(final String filePath, final PlayerVisualizerView view) {
        if (voiceWaveData.get(filePath) != null) {
            view.updateVisualizer(voiceWaveData.get(filePath));
        } else
            Application.getInstance().runInBackgroundUserRequest(new Runnable() {
                @Override
                public void run() {
                    File file = new File(filePath);
                    int size = (int) file.length();
                    final byte[] bytes = new byte[size];
                    try {
                        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();
                    } catch (Exception e) {
                        LogManager.exception(getClass().getSimpleName(), e);
                    }
                    voiceWaveFreshViews.put(filePath, view);
                    if (!voiceWaveInProgress.contains(filePath)) {
                        voiceWaveInProgress.add(filePath);
                        createWaveform(file, view);
                    }
                }
            });
    }


    public void createWaveform(final File file, final PlayerVisualizerView view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final MediaCodec codec;
            MediaFormat format;
            final ArrayList<Float> sampleArray = new ArrayList<>();
            final ArrayList<Integer> squashedArray = new ArrayList<>();
            try {
                final MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(file.getPath());
                format = extractor.getTrackFormat(0);
                extractor.selectTrack(0);

                String decoderForFormat = new MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(format);
                codec = MediaCodec.createByCodecName(decoderForFormat);

                codec.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int index) {
                        ByteBuffer input = codec.getInputBuffer(index);
                        if (input != null) {
                            try {
                                int size = extractor.readSampleData(input, 0);
                                if (size > 0) {
                                    extractor.advance();
                                    codec.queueInputBuffer(index, 0, input.limit(), extractor.getSampleTime(), 0);
                                } else {
                                    codec.queueInputBuffer(index, 0, 0, extractor.getSampleTime(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                }
                            } catch (Exception e) {
                                LogManager.exception("MediaCodec", e);
                            }
                        }
                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index, @NonNull MediaCodec.BufferInfo bufferInfo) {
                        ByteBuffer output = codec.getOutputBuffer(index);
                        MediaFormat bufferFormat = codec.getOutputFormat(index);
                        int pcmType;
                        try {
                            pcmType = bufferFormat.getInteger("pcm-encoding");
                        } catch (NullPointerException e) {
                            pcmType = 0;
                        }
                        //int pcmType = bufferFormat.getInteger("pcm-encoding");//2 - 16bit (short); [-32768;32767].
                        //3 - 8bit; [0;255]
                        //4 - 32bit (float); [-1.0;1.0]

                        ShortBuffer out = output.order(ByteOrder.nativeOrder()).asShortBuffer();
                        short[] buf = new short[out.limit()];
                        out.get(buf, 0, out.limit());

                        ShortBuffer shBuff = ShortBuffer.wrap(buf);
                        float variable;
                        if (pcmType == 2 || pcmType == 0) {
                            variable = 3276.8f;
                        } else
                        if (pcmType == 3) {
                            variable = 25.5f;
                        } else
                            variable = 0.1f;

                        float samples = 0;
                        for (int i = 0; i < buf.length; i++) {
                            if (shBuff.get(i) < 0)
                                samples += -((float) shBuff.get(i) / variable);
                            else
                                samples += ((float) shBuff.get(i) / variable);
                        }
                        if (samples > 0.01)
                            sampleArray.add(samples);

                        codec.releaseOutputBuffer(index, false);

                        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            LogManager.d("MediaCodec", "BUFFER_FLAG_END_OF_STREAM");
                            codec.stop();
                            codec.release();
                            optimizeWaveData(sampleArray, squashedArray);
                            //long num = 0;
                            //int sampleRate = sampleArray.size() / 50;
                            //if (sampleRate != 0) {
                            //    for (int i = 0; i < sampleArray.size(); i++) {
                            //        if (i % sampleRate == 0) {
                            //            squashedArray.add(Utils.longToInt(num));
                            //            num = 0;
                            //        }
                            //        if (sampleArray.get(i) < 0)
                            //            num += -sampleArray.get(i);
                            //        else
                            //            num += sampleArray.get(i);
                            //    }
                            //    squashedArray.add(Utils.longToInt(num));
                            //} else squashedArray.add(0);
                            if ((voiceWaveData.get(file.getPath()) == null || voiceWaveData.get(file.getPath()).isEmpty()) && !squashedArray.isEmpty()) {
                                voiceWaveData.put(file.getPath(), squashedArray);
                                LogManager.i(this, "Finished wave modifications for the file with path = " + file.getPath());
                            }
                        } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                            LogManager.d("MediaCodec", "BUFFER_FLAG_CODEC_CONFIG");
                        } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                            LogManager.d("MediaCodec", "BUFFER_FLAG_KEY_FRAME");
                        } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) {
                            LogManager.d("MediaCodec", "BUFFER_FLAG_PARTIAL_FRAME");
                        }
                    }

                    @Override
                    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                        mediaCodec.release();
                        LogManager.e("MediaCodec", e.getDiagnosticInfo());
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {

                    }
                });

                codec.configure(format, null, null, 0);
                codec.start();
            } catch (IOException e) {
                LogManager.exception(getClass().getSimpleName(), e);
            }
        } else {
            ArrayList<Integer> emptyArray = new ArrayList<>(0);
            voiceWaveData.put(file.getPath(), emptyArray);
        }
    }

    private static void optimizeWaveData(ArrayList<Float> waveData, ArrayList<Integer> optimisedDataForReturn) {
        long num = 0;
        int sampleRate = waveData.size() / 50;
        if (sampleRate != 0) {
            for (int i = 0; i < waveData.size(); i++) {
                if (i % sampleRate == 0) {
                    optimisedDataForReturn.add(longToInt(num));
                    num = 0;
                }
                if (waveData.get(i) < 0)
                    num += -waveData.get(i);
                else
                    num += waveData.get(i);
            }
            optimisedDataForReturn.add(longToInt(num));
        } else {
            for (int i = 0; i < waveData.size(); i++) {
                if (waveData.get(i) < 0) {
                    num -= waveData.get(i);
                } else
                    num += waveData.get(i);
                optimisedDataForReturn.add(longToInt(num));
                num = 0;
            }
        }
    }

    private static int longToInt(long number) {
        if (number > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (number < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else return (int) number;
    }

}

