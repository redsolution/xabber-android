package com.xabber.android.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.adapter.ServerInfoAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.ToolbarHelper;

import java.util.ArrayList;
import java.util.Set;

public class PushLogActivity extends ManagedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_info);

        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(this, getString(R.string.push_log_title));
        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.server_info_recycler_view);
        ServerInfoAdapter serverInfoAdapter = new ServerInfoAdapter();
        Set<String> log = SettingsManager.getPushLog();
        if (log != null)
            serverInfoAdapter.setServerInfoList(new ArrayList<>(log));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(serverInfoAdapter);

        ProgressBar progressBar = findViewById(R.id.server_info_progress_bar);
        progressBar.setVisibility(View.GONE);
    }
}
