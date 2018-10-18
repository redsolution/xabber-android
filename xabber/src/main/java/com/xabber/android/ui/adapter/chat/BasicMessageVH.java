package com.xabber.android.ui.adapter.chat;

import android.support.annotation.StyleRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;

public class BasicMessageVH extends RecyclerView.ViewHolder {

    TextView messageText;

    BasicMessageVH(View itemView, @StyleRes int appearance) {
        super(itemView);

        messageText = (TextView) itemView.findViewById(R.id.message_text);
        messageText.setTextAppearance(itemView.getContext(), appearance);
    }

    BasicMessageVH(View itemView) {
        super(itemView);

        messageText = (TextView) itemView.findViewById(R.id.message_text);
    }
}

