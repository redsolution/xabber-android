package com.xabber.android.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.helper.PreferenceSummaryHelper;

public class ChatGlobalSettingsFragment extends android.preference.PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_chat_global);

        PreferenceSummaryHelper.updateSummary(getPreferenceScreen());

        Preference chats_choose_background = findPreference("chats_choose_background");
        chats_choose_background.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference chats_choose_background) {
                if (Build.VERSION.SDK_INT >= 19) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    // Check for the freshest data.
                    startActivityForResult(intent, 10);
                    return true;
                }
                else {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, 20);
                    return true;
                }
            }

        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= 19) {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getActivity().getContentResolver().takePersistableUriPermission(Uri.parse(data.getDataString()), takeFlags);
            }
            SettingsManager.setChatsChooseBackground(data.getDataString());
        }
    }
}
