package com.xabber.android.ui.adapter.chat;

import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.R;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;

public class ForwardedVH extends FileMessageVH {

    private TextView tvForwardedCount;

    public ForwardedVH(View itemView, MessageClickListener messageListener,
                       MessageLongClickListener longClickListener, FileListener listener,
                       int appearance) {
        super(itemView, messageListener, longClickListener, listener, appearance);
        tvForwardedCount = itemView.findViewById(R.id.tvForwardedCount);
    }

    public void bind(MessageItem messageItem, MessagesAdapter.MessageExtraData extraData, String accountJid) {
        super.bind(messageItem, extraData);

        // hide STATUS ICONS
        statusIcon.setVisibility(View.GONE);

        // setup MESSAGE AUTHOR
        UserJid jid = null;
        try {
            jid = UserJid.from(messageItem.getOriginalFrom());
        } catch (UserJid.UserJidCreateException e) {
            e.printStackTrace();
        }
        String author = RosterManager.getDisplayAuthorName(messageItem);

        if (author != null && !author.isEmpty()) {
            messageHeader.setText(author);
            messageHeader.setTextColor(ColorManager.changeColor(
                    ColorGenerator.MATERIAL.getColor(author), 0.8f));
            messageHeader.setVisibility(View.VISIBLE);
        } else messageHeader.setVisibility(View.GONE);

        // setup BACKGROUND COLOR
        if (jid != null && !accountJid.equals(jid.getBareJid().toString()))
            setUpMessageBalloonBackground(messageBalloon, extraData.getColorStateList());
        else {
            TypedValue typedValue = new TypedValue();
            extraData.getContext().getTheme().resolveAttribute(R.attr.message_background,
                    typedValue, true);
            setUpMessageBalloonBackground(messageBalloon,
                    extraData.getContext().getResources().getColorStateList(typedValue.resourceId));
        }

        // setup FORWARDED
        if (messageItem.haveForwardedMessages()) {
            tvForwardedCount.setText(String.format(extraData.getContext()
                    .getResources().getString(R.string.forwarded_messages_count), messageItem.getForwardedIds().size()));
            tvForwardedCount.setPaintFlags(tvForwardedCount.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            tvForwardedCount.setVisibility(View.VISIBLE);
        } else tvForwardedCount.setVisibility(View.GONE);

    }
}
