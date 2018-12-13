package com.xabber.android.ui.activity;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.http.CrowdfundingManager;
import com.xabber.android.ui.adapter.chat.CrowdfundingChatAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.color.StatusBarPainter;

import io.realm.RealmResults;

public class CrowdfundingChatActivity extends ManagedActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private View backgroundView;
    private RealmResults<CrowdfundingMessage> messages = null;

    private TextView name;
    private TextView status_text;
    private ImageView ivAvatar;
    private ImageView ivStatus;
    private ImageView ivReload;

    private StatusBarPainter statusBarPainter;
    private final int UPDATE_MESSAGE_DELAY = 5; // in sec

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crowdfunding_chat);

        statusBarPainter = new StatusBarPainter(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(CrowdfundingChatActivity.this);
            }
        });
        recyclerView = findViewById(R.id.recyclerView);
        backgroundView = findViewById(R.id.backgroundView);

        name = findViewById(R.id.name);
        status_text = findViewById(R.id.status_text);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivStatus = findViewById(R.id.ivStatus);
        ivReload = findViewById(R.id.ivReload);
        ivReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CrowdfundingChatActivity.this, "RELOAD", Toast.LENGTH_SHORT).show();
                CrowdfundingManager.getInstance().reloadMessages();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        CrowdfundingManager.getInstance().startUpdateTimer(0, UPDATE_MESSAGE_DELAY);
        messages = CrowdfundingManager.getInstance().getMessagesWithDelay(0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupToolbar();
        statusBarPainter.updateWithDefaultColor();

        // background
        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                backgroundView.setBackgroundResource(R.drawable.chat_background_repeat_dark);
            } else {
                backgroundView.setBackgroundResource(R.drawable.chat_background_repeat);
            }
        } else {
            backgroundView.setBackgroundColor(ColorManager.getInstance().getChatBackgroundColor());
        }

        // messages
        CrowdfundingChatAdapter adapter = new CrowdfundingChatAdapter(this, messages, true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // mark messages read
        if (CrowdfundingManager.getInstance().getUnreadMessageCount() > 0) {
            CrowdfundingManager.getInstance().markMessagesAsRead();
        }
    }

    private void setupToolbar() {
        name.setText(R.string.xabber_chat_title);
        status_text.setText(R.string.xabber_chat_description);
        ivAvatar.setImageDrawable(getResources().getDrawable(R.drawable.xabber_logo_80dp));
        ivStatus.setVisibility(View.GONE);
    }
}
