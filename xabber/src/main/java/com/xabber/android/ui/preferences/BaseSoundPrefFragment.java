package com.xabber.android.ui.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.ui.helper.PermissionsRequester;

import java.io.IOException;

public abstract class BaseSoundPrefFragment<T extends BaseSoundPrefFragment.RingtoneHolder> extends android.preference.PreferenceFragment {

    private final static int PERMISSIONS_REQUEST_SELECT_RINGTONE = 10;

    private T ringtoneHolderWaitingForPermission;

    protected abstract void setNewRingtone(T ringtoneHolder);

    protected boolean trySetNewRingtone(T ringtoneHolder) {
        if (checkAccessForRingtone(ringtoneHolder.uri)) {
            setNewRingtone(ringtoneHolder);
            return true;
        } else {
            ringtoneHolderWaitingForPermission = ringtoneHolder;
            showWrongRingtoneAlert();
            return false;
        }
    }

    private Boolean checkAccessForRingtone(String ringtone) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(getActivity(), Uri.parse(ringtone));
            player.release();
            return true;
        } catch (IOException e) {
            player.release();
            return false;
        }
    }

    private void showWrongRingtoneAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.events_ringtone_not_available);

        if (!PermissionsRequester.hasFileReadPermission())
            builder.setMessage(R.string.events_ringtone_not_available_summary_permission)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            PermissionsRequester.requestFileReadPermissionIfNeeded(
                                    BaseSoundPrefFragment.this, PERMISSIONS_REQUEST_SELECT_RINGTONE);
                        }
                    }).show();
        else
            builder.setMessage(R.string.events_ringtone_not_available_summary)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_SELECT_RINGTONE:
                if (PermissionsRequester.isPermissionGranted(grantResults))
                    trySetNewRingtone(ringtoneHolderWaitingForPermission);
                else Toast.makeText(getActivity(),R.string.events_ringtone_not_available, Toast.LENGTH_LONG).show();
                break;
        }
    }

    static abstract class RingtoneHolder {
        String uri;

        RingtoneHolder(String uri) {
            this.uri = uri;
        }
    }

}
