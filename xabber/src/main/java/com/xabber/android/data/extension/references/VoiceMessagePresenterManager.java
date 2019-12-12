package com.xabber.android.data.extension.references;

import com.xabber.android.data.Application;
import com.xabber.android.ui.widget.PlayerVisualizerView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class VoiceMessagePresenterManager {

    private static VoiceMessagePresenterManager instance;

    private static final Map<String, byte[]> voiceWaveData = new HashMap<>();

    public static VoiceMessagePresenterManager getInstance() {
        if (instance == null)
            instance = new VoiceMessagePresenterManager();
        return instance;
    }

    public VoiceMessagePresenterManager() {}

    public void putWaveData(String path, byte[] data) {
        voiceWaveData.put(path, data);
    }

    public void sendWaveDataIfSaved(final String filePath, final PlayerVisualizerView view) {
        if (voiceWaveData.get(filePath) != null)
            view.updateVisualizer(voiceWaveData.get(filePath));
        else
            Application.getInstance().runInBackground(new Runnable() {
                @Override
                public void run() {
                    File file = new File(filePath);
                    int size = (int) file.length();
                    final byte[] bytes = new byte[size];
                    try {
                        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    voiceWaveData.put(filePath, bytes);
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            view.updateVisualizer(bytes);
                        }
                    });
                }
            });

    }
}
