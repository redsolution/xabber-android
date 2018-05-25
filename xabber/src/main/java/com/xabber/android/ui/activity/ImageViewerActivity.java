package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.ui.fragment.ImageViewerFragment;

import io.realm.Realm;
import io.realm.RealmList;

public class ImageViewerActivity extends AppCompatActivity {

    private static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String ATTACHMENT_POSITION = "ATTACHMENT_POSITION";

    private RealmList<Attachment> imageAttachments = new RealmList<>();
    private Toolbar toolbar;
    ViewPager viewPager;

    @NonNull
    public static Intent createIntent(Context context, String id, int position) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        Bundle args = new Bundle();
        args.putString(MESSAGE_ID, id);
        args.putInt(ATTACHMENT_POSITION, position);
        intent.putExtras(args);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // get params
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        String messageId = args.getString(MESSAGE_ID);
        int imagePosition = args.getInt(ATTACHMENT_POSITION);

        // setup toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(ImageViewerActivity.this);
            }
        });

        // get imageAttachments
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        MessageItem messageItem = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.UNIQUE_ID, messageId)
                .findFirst();
        RealmList<Attachment> attachments = messageItem.getAttachments();

        for (Attachment attachment : attachments) {
            if (attachment.isImage()) imageAttachments.add(attachment);
        }

        // find views
        viewPager = findViewById(R.id.viewPager);
        PagerAdapter pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Attachment attachment = imageAttachments.get(position);
                return ImageViewerFragment.newInstance(attachment.getFilePath(), attachment.getFileUrl());
            }

            @Override
            public int getCount() {
                return imageAttachments.size();
            }
        };
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(imagePosition);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                updateToolbar();
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToolbar();
    }

    private void updateToolbar() {
        int current = 0, total = 0;
        if (viewPager != null) current = viewPager.getCurrentItem() + 1;
        if (imageAttachments != null) total = imageAttachments.size();
        toolbar.setTitle(current + " of " + total);
    }
}
