package com.xabber.android.ui.adapter.chat;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;

public class BasicMessageVH extends RecyclerView.ViewHolder {

    TextView messageText;
    public boolean needDate;
    public String date;

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

