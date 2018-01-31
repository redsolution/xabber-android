package com.xabber.android.ui.adapter.contactlist;

import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.xabber.android.R;

/**
 * Created by valery.miller on 31.01.18.
 */

public class ChatWithButtonViewHolder extends RosterChatViewHolder {

    public RelativeLayout buttonView;
    public Button btnListAction;

    public ChatWithButtonViewHolder(View view, ContactListItemViewHolder.ContactClickListener listener) {
        super(view, listener);

        buttonView = (RelativeLayout) view.findViewById(R.id.buttonView);
        btnListAction = (Button) view.findViewById(R.id.btnListAction);
        btnListAction.setOnClickListener(this);
    }

}
