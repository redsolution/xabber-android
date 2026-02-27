package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.IntentHelpersKt;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.bookmarks.BookmarkVO;
import com.xabber.android.data.extension.bookmarks.BookmarksManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.BookmarkAdapter;
import com.xabber.android.ui.color.BarPainter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bookmarks.BookmarkedConference;
import org.jivesoftware.smackx.bookmarks.BookmarkedURL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by valery.miller on 06.06.17.
 */

public class BookmarksActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener,
        BookmarkAdapter.OnBookmarkClickListener {

    private static final String LOG_TAG = BookmarksActivity.class.getSimpleName();
    private AccountItem accountItem;
    private BookmarkAdapter bookmarksAdapter;
    private View progressBar;
    private TextView tvNotSupport;
    private int previousSize;
    private Toolbar toolbar;
    private BarPainter barPainter;

    public static Intent createIntent(Context context, AccountJid account) {
        return IntentHelpersKt.createAccountIntent(context, BookmarksActivity.class, account);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bokmarks);

        AccountJid account = IntentHelpersKt.getAccountJid(getIntent());
        if (account == null) {
            finish();
            return;
        }

        accountItem = AccountManager.INSTANCE.getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);

        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(R.string.account_bookmarks);
        toolbar.inflateMenu(R.menu.toolbar_bookmark_list);
        toolbar.setOnMenuItemClickListener(this);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_grey_24dp));
        }
        else {
            toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        }

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.server_info_recycler_view);

        bookmarksAdapter = new BookmarkAdapter(this);
        bookmarksAdapter.setListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(bookmarksAdapter);

        progressBar = findViewById(R.id.server_info_progress_bar);
        tvNotSupport = (TextView) findViewById(R.id.tvNotSupport);

        requestBookmarks(false);
    }

    @Override
    public void onBookmarkClick() {
        updateToolbar();
        updateMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean checkItemsIsEmpty = bookmarksAdapter.getCheckedItems().isEmpty();
        menu.findItem(R.id.action_remove_all).setVisible(bookmarksAdapter.getItemCount() > 0 && checkItemsIsEmpty);
        menu.findItem(R.id.action_synchronize).setVisible(checkItemsIsEmpty);
        menu.findItem(R.id.action_remove_selected).setVisible(!checkItemsIsEmpty);
        return true;
    }

    private void requestBookmarks(final boolean cleanCache) {
        progressBar.setVisibility(View.VISIBLE);
        tvNotSupport.setVisibility(View.GONE);

        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            boolean support = false;
            try {
               support = BookmarksManager.getInstance().isSupported(accountItem.getAccount());
            } catch (InterruptedException | SmackException.NoResponseException
                    | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                LogManager.exception(LOG_TAG, e);
            }

            if (!support) {
                runOnUiThread(() -> showNotSupported());
            } else {
                final List<BookmarkVO> bookmarks = getBookmarks(cleanCache);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    bookmarksAdapter.setItems(bookmarks);
                });
            }
        });
    }

    private void showNotSupported() {
        progressBar.setVisibility(View.GONE);
        tvNotSupport.setVisibility(View.VISIBLE);
    }

    private List<BookmarkVO> getBookmarks(boolean cleanCache) {
        final List<BookmarkVO> bookmarksList = new ArrayList<>();

        if (cleanCache) BookmarksManager.getInstance().cleanCache(accountItem.getAccount());

        // urls
        List<BookmarkedURL> bookmarkedURLs =
                BookmarksManager.getInstance().getUrlFromBookmarks(accountItem.getAccount());

        for (int i = 0; i < bookmarkedURLs.size(); i++) {
            BookmarkedURL url = bookmarkedURLs.get(i);
            bookmarksList.add(new BookmarkVO(url.getName(), url.getURL()));
        }

        List<BookmarkedConference> bookmarkedConferences;
        try {
            bookmarkedConferences = BookmarksManager.getInstance()
                    .getConferencesFromBookmarks(accountItem.getAccount());

        } catch (SmackException.NoResponseException | InterruptedException |
                SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            LogManager.exception(this, e);
            bookmarkedConferences = Collections.emptyList();
        }

        for (int i = 0; i < bookmarkedConferences.size(); i++) {
            BookmarkedConference conference = bookmarkedConferences.get(i);
            bookmarksList.add(new BookmarkVO(
                    conference.getName() != null ? conference.getName() : "",
                    conference.getJid() != null ? conference.getJid().toString() : "",
                    conference.getNickname() != null ? conference.getNickname().toString() : "",
                    conference.getPassword() != null ? conference.getPassword() : ""
                    ));
        }

        return bookmarksList;
    }

    private void updateMenu() {
        onPrepareOptionsMenu(toolbar.getMenu());
    }

    private void updateToolbar() {
        final ArrayList<BookmarkVO> checkedItems = bookmarksAdapter.getCheckedItems();

        final int currentSize = checkedItems.size();

        if (currentSize == previousSize) {
            return;
        }

        if (currentSize == 0) {
            toolbar.setTitle(getString(R.string.account_bookmarks));
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
            else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
            barPainter.updateWithAccountName(accountItem.getAccount());

            toolbar.setNavigationOnClickListener(v -> finish());

        } else {
            toolbar.setTitle(String.valueOf(currentSize));
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp);
            else toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
            barPainter.setGrey();

            toolbar.setNavigationOnClickListener(v -> {
                bookmarksAdapter.setCheckedItems(new ArrayList<>());
                bookmarksAdapter.notifyDataSetChanged();
                updateToolbar();
                updateMenu();
            });
        }

        previousSize = currentSize;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_synchronize:
                requestBookmarks(true);
                return true;
            case R.id.action_remove_all:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog = builder
                        .setMessage(String.format(getString(R.string.remove_all_bookmarks_confirm),
                                AccountManager.INSTANCE.getVerboseName(accountItem.getAccount())))
                        .setPositiveButton(R.string.remove_all_bookmarks, (dialog1, which) -> {
                            BookmarksManager.getInstance().removeBookmarks(accountItem.getAccount(),
                                    bookmarksAdapter.getAllWithoutXabberUrl());
                            bookmarksAdapter.setCheckedItems(new ArrayList<>());
                            requestBookmarks(false);
                            updateToolbar();
                            updateMenu();
                        })
                        .setNegativeButton(android.R.string.cancel, null).create();
                dialog.show();
                return true;
            case R.id.action_remove_selected:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                AlertDialog dialog2 = builder2
                        .setMessage(String.format(getString(R.string.remove_selected_bookmarks_confirm),
                                AccountManager.INSTANCE.getVerboseName(accountItem.getAccount())))
                        .setPositiveButton(R.string.remove_selected_bookmarks, (dialog12, which) -> {
                            BookmarksManager.getInstance().removeBookmarks(accountItem.getAccount(),
                                    bookmarksAdapter.getCheckedItems());
                            bookmarksAdapter.setCheckedItems(new ArrayList<>());
                            requestBookmarks(false);
                            updateToolbar();
                            updateMenu();
                        })
                        .setNegativeButton(android.R.string.cancel, null).create();
                dialog2.show();
                return true;
            default:
                return true;
        }
    }

}
