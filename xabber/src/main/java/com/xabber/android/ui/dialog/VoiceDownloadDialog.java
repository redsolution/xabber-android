package com.xabber.android.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.color.ColorManager;

public class VoiceDownloadDialog extends BottomSheetDialogFragment implements View.OnClickListener {

    private AccountJid accountJid;

    public static VoiceDownloadDialog newInstance(AccountJid accountJid) {
        VoiceDownloadDialog dialog = new VoiceDownloadDialog();
        dialog.accountJid = accountJid;
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.voice_download_dialog, container, false);

        view.findViewById(R.id.voice_download_agree).setOnClickListener(this);
        ((Button)view.findViewById(R.id.voice_download_agree)).setTextColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(accountJid));
        view.findViewById(R.id.voice_download_disagree).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.voice_download_agree:
                SettingsManager.setAutoDownloadVoiceMessageSuggested();
                SettingsManager.setChatsAutoDownloadVoiceMessage(true);
                break;
            case R.id.voice_download_disagree:
                SettingsManager.setAutoDownloadVoiceMessageSuggested();
                SettingsManager.setChatsAutoDownloadVoiceMessage(false);
                break;
        }
        this.dismiss();
    }
}
