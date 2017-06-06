package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.bookmarks.BookmarkVO;
import com.xabber.android.data.extension.bookmarks.BookmarksManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.BookmarkAdapter;
import com.xabber.android.ui.color.BarPainter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bookmarks.BookmarkedConference;
import org.jivesoftware.smackx.bookmarks.BookmarkedURL;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 06.06.17.
 */

public class BookmarksActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener {

    private static final String LOG_TAG = BookmarksActivity.class.getSimpleName();
    private AccountItem accountItem;
    private BookmarkAdapter bookmarksAdapter;
    private View progressBar;
    private TextView tvNotSupport;

    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, BookmarksActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bokmarks);

        final Intent intent = getIntent();

        AccountJid account = getAccount(intent);
        if (account == null) {
            finish();
            return;
        }

        accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(R.string.account_bookmarks);
        //toolbar.inflateMenu(R.menu.toolbar_bookmark_list);
        toolbar.setOnMenuItemClickListener(this);

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.server_info_recycler_view);

        bookmarksAdapter = new BookmarkAdapter(this);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(bookmarksAdapter);


        progressBar = findViewById(R.id.server_info_progress_bar);
        tvNotSupport = (TextView) findViewById(R.id.tvNotSupport);

        requestBookmarks();
    }

    private void requestBookmarks() {
        progressBar.setVisibility(View.VISIBLE);
        tvNotSupport.setVisibility(View.GONE);

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean support = BookmarksManager.getInstance().isSupported(accountItem.getAccount());
                    if (!support) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showNotSupported();
                            }
                        });
                    }
                } catch (InterruptedException | SmackException.NoResponseException
                        | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    LogManager.exception(LOG_TAG, e);
                }

                final List<BookmarkVO> bookmarks = getBookmarks();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        bookmarksAdapter.setItems(bookmarks);
                    }
                });
            }
        });
    }

    private void showNotSupported() {
        progressBar.setVisibility(View.GONE);
        tvNotSupport.setVisibility(View.VISIBLE);
    }

    private List<BookmarkVO> getBookmarks() {
        final List<BookmarkVO> bookmarksList = new ArrayList<>();

        // urls
        List<BookmarkedURL> bookmarkedURLs =
                BookmarksManager.getInstance().getUrlFromBookmarks(accountItem.getAccount());

        for (int i = 0; i < bookmarkedURLs.size(); i++) {
            BookmarkedURL url = bookmarkedURLs.get(i);
            bookmarksList.add(new BookmarkVO(url.getName(), url.getURL()));
        }

        // conferences
        List<BookmarkedConference> bookmarkedConferences =
                BookmarksManager.getInstance().getConferencesFromBookmarks(accountItem.getAccount());

        for (int i = 0; i < bookmarkedConferences.size(); i++) {
            BookmarkedConference conference = bookmarkedConferences.get(i);
            bookmarksList.add(new BookmarkVO(
                    conference.getName(),
                    conference.getJid().toString(),
                    conference.getNickname().toString(),
                    conference.getPassword()));
        }

        return bookmarksList;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }
}
