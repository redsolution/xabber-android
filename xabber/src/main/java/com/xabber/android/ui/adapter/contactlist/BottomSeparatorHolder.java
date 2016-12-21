package com.xabber.android.ui.adapter.contactlist;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.xabber.android.R;

class BottomSeparatorHolder extends RecyclerView.ViewHolder {
    View bottomLayer;
    View topLayer;
    View offlineShadowBottom;
    View offlineShadowTop;

    BottomSeparatorHolder(View itemView) {
        super(itemView);
        bottomLayer = itemView.findViewById(R.id.bottom_layer);
        topLayer = itemView.findViewById(R.id.top_layer);
        offlineShadowBottom = itemView.findViewById(R.id.offline_shadow_top);
        offlineShadowTop = itemView.findViewById(R.id.offline_shadow_bottom);
    }
}
