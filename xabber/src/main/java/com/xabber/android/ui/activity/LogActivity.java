package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realm.LogMessage;
import com.xabber.android.ui.adapter.LogAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.ToolbarHelper;
import com.xabber.android.utils.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class LogActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener {

    public static final int LOG_MENU = R.menu.activity_log;
    @BindView(R.id.activity_log_recycler_view)
    RecyclerView recyclerView;


    public static Intent createIntent(Context context) {
        return new Intent(context, LogActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        ButterKnife.bind(this);

        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(this, getString(R.string.debug_log_title));

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        toolbar.inflateMenu(LOG_MENU);
        toolbar.setOnMenuItemClickListener(this);

        DatabaseManager.getInstance().getRealm()
                .where(LogMessage.class)
                // older than week ago 7 * 24 * 60 * 60 * 1000
                .lessThan(LogMessage.Fields.DATETIME, new Date(System.currentTimeMillis() - 604800000L))
                .findAllAsync().addChangeListener(new RealmChangeListener<RealmResults<LogMessage>>() {
                    @Override
                    public void onChange(RealmResults<LogMessage> element) {
                        if (element.isValid() && element.isLoaded()) {
                            element.deleteAllFromRealm();
                        }
                        element.removeChangeListeners();
                    }
                });

        RealmResults<LogMessage> all = DatabaseManager.getInstance().getRealm()
                .where(LogMessage.class)
                .findAllSortedAsync(LogMessage.Fields.DATETIME, Sort.DESCENDING);

        LogAdapter logAdapter = new LogAdapter(this, all, true);
        recyclerView.setAdapter(logAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(LOG_MENU, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_log_to_file:
                saveLogToFile();
                return true;

            case R.id.action_clear_log:
                new AlertDialog.Builder(this)
                        .setTitle("Clear log")
                        .setMessage("Are you sure you want to delete all logs?")
                        .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                clearLog();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearLog() {
        DatabaseManager.getInstance().getRealm().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(LogMessage.class).findAll().deleteAllFromRealm();
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    public void saveLogToFile() {
        DatabaseManager.getInstance().getRealm()
                .where(LogMessage.class)
                .findAllSortedAsync(LogMessage.Fields.DATETIME, Sort.ASCENDING)
                .addChangeListener(new RealmChangeListener<RealmResults<LogMessage>>() {
                    @Override
                    public void onChange(final RealmResults<LogMessage> messages) {
                        if (messages.isValid() && messages.isLoaded()) {
                            final List<LogMessage> logMessages = DatabaseManager.getInstance().getRealm().copyFromRealm(messages);

                            Application.getInstance().runInBackground(new Runnable() {
                                @Override
                                public void run() {
                                    writeToFile(logMessages);
                                }
                            });

                            messages.removeChangeListeners();
                        }
                    }
                });
    }

    private void writeToFile(List<LogMessage> messages) {
        try {
            String filename = getString(R.string.application_title_short) + "_Log_" + StringUtils.getLogDateTimeFormat().format(new Date()) + ".txt";
            File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            FileWriter writer = new FileWriter(logFile);

            writer.append("Android release: ").append(Build.VERSION.RELEASE)
                    .append(". SDK: ").append(String.valueOf(Build.VERSION.SDK_INT));

            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                writer.append(". App version: ").append(pInfo.versionName).append("\n");
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            for (LogMessage logMessage : messages) {
                writer.append(logMessage.toString());
                writer.append("\n");
            }

            writer.append("Log file created at ")
                    .append(StringUtils.getLogDateTimeFormat().format(new Date()))
                    .append("\n");

            writer.flush();
            writer.close();

            String mimeTypeFromExtension = "text/plain";
            final DownloadManager downloadManager = (DownloadManager) Application.getInstance().getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.addCompletedDownload(logFile.getName(),
                    getString(R.string.application_title_short) + " connection log file",
                    true, mimeTypeFromExtension, logFile.getPath(), logFile.length(), true);

            Application.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LogActivity.this, "Log saved successfully", Toast.LENGTH_LONG).show();
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}