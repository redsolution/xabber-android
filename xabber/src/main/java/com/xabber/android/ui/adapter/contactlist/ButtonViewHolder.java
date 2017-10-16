package com.xabber.android.ui.adapter.contactlist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.xabber.android.R;

/**
 * Created by valery.miller on 16.10.17.
 */

public class ButtonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    final Button btnListAction;
    private final ButtonClickListener listener;

    interface ButtonClickListener {
        void onButtonClick(int position);
    }

    public ButtonViewHolder(View itemView, ButtonClickListener listener) {
        super(itemView);
        this.listener = listener;

        btnListAction = (Button) itemView.findViewById(R.id.btnListAction);
        btnListAction.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        listener.onButtonClick(getAdapterPosition());
    }
}
