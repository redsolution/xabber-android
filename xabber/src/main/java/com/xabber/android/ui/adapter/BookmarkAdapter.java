package com.xabber.android.ui.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.extension.bookmarks.BookmarkVO;
import com.xabber.android.data.extension.bookmarks.BookmarksManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by valery.miller on 06.06.17.
 */

public class BookmarkAdapter extends RecyclerView.Adapter {

    private List<BookmarkVO> items;
    private Set<BookmarkVO> checkedItems;
    private Context mContext;
    private OnBookmarkClickListener listener;

    public interface OnBookmarkClickListener {
        void onBookmarkClick();
    }

    public void setListener(@Nullable OnBookmarkClickListener listener) {
        this.listener = listener;
    }

    public BookmarkAdapter(Context context) {
        this.items = new ArrayList<>();
        checkedItems = new HashSet<>();
        mContext = context;
    }

    public void setItems(List<BookmarkVO> items) {
        this.items = items;
        this.checkedItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case BookmarkVO.TYPE_URL: return new UrlHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bookmark_url, parent, false));
            case BookmarkVO.TYPE_CONFERENCE: return new ConferenceHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bookmark_conference, parent, false));
            default: return new UrlHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bookmark_url, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

        BookmarkVO bookmark = items.get(position);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookmarkVO bookmark = items.get(position);
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.chbBookmark);

                // can't remove xabber url which need for conference sync
                if (bookmark.getUrl().equals(BookmarksManager.XABBER_URL)) return;

                if (checkedItems.contains(bookmark)) {
                    checkedItems.remove(bookmark);
                    checkBox.setChecked(false);
                } else {
                    checkedItems.add(bookmark);
                    checkBox.setChecked(true);
                }

                if (listener != null) {
                    listener.onBookmarkClick();
                }
            }
        };

        switch (holder.getItemViewType()) {
            case BookmarkVO.TYPE_URL:
                UrlHolder urlHolder = (UrlHolder) holder;
                urlHolder.tvName.setText(bookmark.getName());
                urlHolder.tvUrl.setText(bookmark.getUrl());
                urlHolder.chbBookmark.setChecked(checkedItems.contains(bookmark));
                urlHolder.chbBookmark.setOnClickListener(onClickListener);

                if (bookmark.getUrl().equals(BookmarksManager.XABBER_URL)) urlHolder.chbBookmark.setVisibility(View.INVISIBLE);
                else urlHolder.chbBookmark.setVisibility(View.VISIBLE);
                break;
            case BookmarkVO.TYPE_CONFERENCE:
                ConferenceHolder conferenceHolder = (ConferenceHolder) holder;
                conferenceHolder.tvName.setText(bookmark.getName());
                conferenceHolder.tvJid.setText(bookmark.getJid());
                conferenceHolder.tvNickname.setText(bookmark.getNickname());
                conferenceHolder.chbBookmark.setChecked(checkedItems.contains(bookmark));
                conferenceHolder.chbBookmark.setOnClickListener(onClickListener);
                break;
        }

        holder.itemView.setOnClickListener(onClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    private static class UrlHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvUrl;
        CheckBox chbBookmark;

        UrlHolder(View itemView) {
            super(itemView);

            tvName = (TextView) itemView.findViewById(R.id.tvUrlName);
            tvUrl = (TextView) itemView.findViewById(R.id.tvUrl);
            chbBookmark = (CheckBox) itemView.findViewById(R.id.chbBookmark);
        }
    }

    private static class ConferenceHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvJid;
        TextView tvNickname;
        CheckBox chbBookmark;

        ConferenceHolder(View itemView) {
            super(itemView);

            tvName = (TextView) itemView.findViewById(R.id.tvConferenceName);
            tvJid = (TextView) itemView.findViewById(R.id.tvJid);
            tvNickname = (TextView) itemView.findViewById(R.id.tvNickname);
            chbBookmark = (CheckBox) itemView.findViewById(R.id.chbBookmark);
        }
    }

    public ArrayList<BookmarkVO> getCheckedItems() {
        return new ArrayList<>(checkedItems);
    }

    public ArrayList<BookmarkVO> getAllWithoutXabberUrl() {
        ArrayList<BookmarkVO> items = new ArrayList<>();
        for (BookmarkVO item : this.items) {
            if (!item.getUrl().equals(BookmarksManager.XABBER_URL)) items.add(item);
        }
        return items;
    }

    public void setCheckedItems(List<BookmarkVO> checkedItems) {
        this.checkedItems.clear();
        this.checkedItems.addAll(checkedItems);
    }

}
