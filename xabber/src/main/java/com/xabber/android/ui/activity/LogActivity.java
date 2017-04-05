package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.xabber.android.R;
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
                        .setTitle("Clear old logs")
                        .setMessage("Are you sure you want to delete all old log file (current log file will remain)?")
                        .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                clearLog();
                                updateFileList(logFilesAdapter);
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
        LogManager.clearLogs();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

}