package com.xabber.android.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.xabber.android.R;
import com.xabber.android.data.push.PushManager;
import com.xabber.android.ui.adapter.ServerInfoAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.helper.ToolbarHelper;

import java.util.List;

public class PushLogActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener {

    private ServerInfoAdapter serverInfoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_info);

        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(this, getString(R.string.push_log_title));
        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        toolbar.inflateMenu(R.menu.toolbar_log);
        toolbar.setOnMenuItemClickListener(this);

        RecyclerView recyclerView = findViewById(R.id.server_info_recycler_view);
        serverInfoAdapter = new ServerInfoAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(serverInfoAdapter);

        ProgressBar progressBar = findViewById(R.id.server_info_progress_bar);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_clear_log) {
            PushManager.clearPushLog();
            updateList();
            return true;
        }
        return false;
    }

    private void updateList() {
        List<String> log = PushManager.getPushLogs();
        if (log != null) {
            serverInfoAdapter.setServerInfoList(log);
            serverInfoAdapter.notifyDataSetChanged();
        }
    }
}
