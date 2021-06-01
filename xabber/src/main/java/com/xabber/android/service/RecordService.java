package com.xabber.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import top.oply.opuslib.OpusEvent;
import top.oply.opuslib.OpusRecorder;

public class RecordService extends Service {

    private String TAG = RecordService.class.getName();

    //Looper
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    //This server
    private static final String ACTION_OPUSSERVICE = "top.oply.opuslib.action.OPUSSERVICE";

    private static final String EXTRA_FILE_NAME = "FILE_NAME";
    private static final String EXTRA_FILE_NAME_OUT = "FILE_NAME_OUT";
    private static final String EXTRA_OPUS_CODING_OPTION = "OPUS_CODING_OPTION";
    private static final String EXTRA_CMD = "CMD";
    private static final String EXTRA_SEEKFILE_SCALE = "SEEKFILE_SCALE";

    private static final int CMD_PLAY           = 10001;
    private static final int CMD_PAUSE          = 10002;
    private static final int CMD_STOP_PLAYING   = 10003;
    private static final int CMD_TOGGLE         = 10004;
    private static final int CMD_SEEK_FILE      = 10005;
    private static final int CMD_GET_TRACK_INFO = 10006;
    private static final int CMD_ENCODE         = 20001;
    private static final int CMD_DECODE         = 20002;
    private static final int CMD_RECORD         = 30001;
    private static final int CMD_STOP_RECORDING = 30002;
    private static final int CMD_RECORD_TOGGLE  = 30003;

    //private OpusPlayer mPlayer;
    private OpusRecorder mRecorder;
    //private OpusConverter mConverter;
    //private OpusTrackInfo mTrackInfo;
    private OpusEvent mEvent = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        mEvent = new OpusEvent(getApplicationContext());
        mRecorder = OpusRecorder.getInstance();

        mRecorder.setEventSender(mEvent);

        //start looper in onCreate() instead of onStartCommand()
        HandlerThread thread = new HandlerThread("RecordServiceHandler");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

    }

    public void onDestroy() {
        //quit looper
        mServiceLooper.quit();

        mRecorder.release();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    public static void record(Context context, String fileName) {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, CMD_RECORD);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        context.startService(intent);
    }

    public static void stopRecording(Context context) {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_OPUSSERVICE);
        intent.putExtra(EXTRA_CMD, CMD_STOP_RECORDING);
        context.startService(intent);
    }


    public void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_OPUSSERVICE.equals(action)) {
                mRecorder = OpusRecorder.getInstance();
                int request = intent.getIntExtra(EXTRA_CMD, 0);
                String fileName;
                switch (request) {
                    case CMD_RECORD:
                        fileName = intent.getStringExtra(EXTRA_FILE_NAME);
                        handleActionRecord(fileName);
                        break;
                    case CMD_STOP_RECORDING:
                        handleActionStopRecording();
                        stopSelf();
                        break;
                    default:
                        Log.e(TAG,"Unknown intent CMD,discarded!");
                }

            } else {
                Log.e(TAG,"Unknown intent action,discarded!");
            }

        }
    }

    private void handleActionRecord(String fileName) {
        mRecorder.startRecording(fileName);
    }
    private void handleActionStopRecording() {
        mRecorder.stopRecording();
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
            //stopSelf()
        }
    }

}
