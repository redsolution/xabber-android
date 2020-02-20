package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.LogFilesAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.ToolbarHelper;


public class LogActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener {

    public static final int LOG_MENU = R.menu.toolbar_log;
    RecyclerView recyclerView;
    private LogFilesAdapter logFilesAdapter;


    public static Intent createIntent(Context context) {
        return new Intent(context, LogActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(this, getString(R.string.debug_log_files_activity_title));

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        toolbar.inflateMenu(LOG_MENU);
        toolbar.setOnMenuItemClickListener(this);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_grey_24dp));
        else toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        recyclerView = (RecyclerView) findViewById(R.id.activity_log_recycler_view);
        logFilesAdapter = new LogFilesAdapter();
        recyclerView.setAdapter(logFilesAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        updateFileList(logFilesAdapter);
    }

    private void updateFileList(LogFilesAdapter logFilesAdapter) {
        logFilesAdapter.setFiles(LogManager.getLogFiles());
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
            case R.id.action_clear_log:
                new AlertDialog.Builder(this)
                        .setTitle("Clear logs")
                        .setMessage("Are you sure you want to delete all log files?")
                        .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                clearLog();
                                updateFileList(logFilesAdapter);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            case R.id.send_all_log_files:
                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("vnd.android.cursor.dir/email");
                intent.putExtra(Intent.EXTRA_STREAM, LogManager.getAllLogFilesUris());
                intent.putExtra(Intent.EXTRA_SUBJECT, "Send all log files");
                recyclerView.getContext().startActivity(Intent.createChooser(intent , "Send log file"));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearLog() {
        LogManager.clearLogs();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

}