package com.xabber.android.ui.adapter.contactlist;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.xabber.android.R;

/**
 * Created by valery.miller on 23.10.17.
 */

public class MainTitleViewHolder extends RecyclerView.ViewHolder {

    final View accountColorIndicator;

    public MainTitleViewHolder(View itemView) {
        super(itemView);

        accountColorIndicator = itemView.findViewById(R.id.accountColorIndicator);
    }
}
