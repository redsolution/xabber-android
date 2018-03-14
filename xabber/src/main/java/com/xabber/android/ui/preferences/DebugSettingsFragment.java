package com.xabber.android.ui.preferences;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.mam.MamManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.activity.PreferenceSummaryHelperActivity;

import java.util.Collection;

public class DebugSettingsFragment extends android.preference.PreferenceFragment {

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_debug);

        PreferenceScreen preferenceScreen = getPreferenceScreen();

        preferenceScreen.removePreference(preferenceScreen.findPreference(getString(R.string.debug_log_key)));
        preferenceScreen.removePreference(preferenceScreen.findPreference(getString(R.string.cache_clear_key)));
        preferenceScreen.removePreference(preferenceScreen.findPreference(getString(R.string.debug_connection_errors_key)));

        Preference prefDownloadArchive = preferenceScreen.findPreference(getString(R.string.debug_download_all_messages_key));
        prefDownloadArchive.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startMessageArchiveDownload();
                return true;
            }
        });

        if (!BuildConfig.DEBUG) {
            preferenceScreen.removePreference(prefDownloadArchive);
        }

        if (!SettingsManager.isCrashReportsSupported()) {
            preferenceScreen.removePreference(preferenceScreen.findPreference(getString(R.string.debug_crash_reports_key)));
        }

        PreferenceSummaryHelperActivity.updateSummary(preferenceScreen);
    }

    private void showDownloadArchiveDialog() {
        if (getActivity() != null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle("Please waiting..");
            progressDialog.setMessage("Downloading message archive");
            progressDialog.show();
        }
    }

    private void closeDownloadArchiveDialog() {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null)
                    progressDialog.dismiss();
            }
        });
    }

    private void setDownloadProgress(final int total, final int downloaded) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog == null || !progressDialog.isShowing())
                    showDownloadArchiveDialog();
                progressDialog.setMessage("Downloading message archive " + downloaded + "/" + total);
            }
        });
    }

    private void startMessageArchiveDownload() {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                Collection<AbstractChat> chats = MessageManager.getInstance().getChats();

                if (chats == null || chats.size() == 0) {
                    closeDownloadArchiveDialog();
                    return;
                }

                int downloadedArchives = 0;
                int totalArchives = chats.size();

                for (AbstractChat chat : chats) {
                    setDownloadProgress(totalArchives, downloadedArchives);
                    MamManager.getInstance().requestFullChatHistory(chat);
                    downloadedArchives++;
                }
                closeDownloadArchiveDialog();
            }
        });
    }
}
