package com.xabber.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.extension.bookmarks.BookmarkVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 06.06.17.
 */

public class BookmarkAdapter extends RecyclerView.Adapter {

    private List<BookmarkVO> items;
    private Context mContext;

    public BookmarkAdapter(Context context) {
        this.items = new ArrayList<>();
        mContext = context;
    }

    public void setItems(List<BookmarkVO> items) {
        this.items = items;
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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        BookmarkVO bookmark = items.get(position);

        switch (holder.getItemViewType()) {
            case BookmarkVO.TYPE_URL:
                UrlHolder urlHolder = (UrlHolder) holder;
                urlHolder.tvName.setText(bookmark.getName());
                urlHolder.tvUrl.setText(bookmark.getUrl());
                break;
            case BookmarkVO.TYPE_CONFERENCE:
                ConferenceHolder conferenceHolder = (ConferenceHolder) holder;
                conferenceHolder.tvName.setText(bookmark.getName());
                conferenceHolder.tvJid.setText(mContext.getString(R.string.bookmark_jid, bookmark.getJid()));
                conferenceHolder.tvNickname.setText(mContext.getString(R.string.bookmark_nick, bookmark.getNickname()));
                break;
        }
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

        UrlHolder(View itemView) {
            super(itemView);

            tvName = (TextView) itemView.findViewById(R.id.tvUrlName);
            tvUrl = (TextView) itemView.findViewById(R.id.tvUrl);
        }
    }

    private static class ConferenceHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvJid;
        TextView tvNickname;

        ConferenceHolder(View itemView) {
            super(itemView);

            tvName = (TextView) itemView.findViewById(R.id.tvConferenceName);
            tvJid = (TextView) itemView.findViewById(R.id.tvJid);
            tvNickname = (TextView) itemView.findViewById(R.id.tvNickname);
        }
    }


}
