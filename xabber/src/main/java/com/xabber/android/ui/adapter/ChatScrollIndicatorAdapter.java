package com.xabber.android.ui.adapter;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.xabber.android.R;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;

public class ChatScrollIndicatorAdapter {

    private final Activity activity;
    private final LinearLayout linearLayout;

    public ChatScrollIndicatorAdapter(Activity activity, LinearLayout linearLayout) {
        this.activity = activity;
        this.linearLayout = linearLayout;
    }

    public void select(int selectedPosition) {
        for (int index = 0; index < linearLayout.getChildCount(); index++) {
            final View view = linearLayout.getChildAt(index);
            final AccountViewHolder accountViewHolder = (AccountViewHolder) view.getTag();

            accountViewHolder.selection.setVisibility(index == selectedPosition ? View.VISIBLE : View.INVISIBLE);
            accountViewHolder.body.setVisibility(index == selectedPosition ? View.INVISIBLE : View.VISIBLE);
        }
    }

    public void update(ArrayList<AbstractChat> activeChats) {
        final LayoutInflater inflater
                = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        final int size = activeChats.size() + 1;

        linearLayout.removeAllViews();

        for (int i = 0; i < size; i++) {
            View view;
            if (i == 0) {
                view = inflater.inflate(R.layout.chat_scroll_indicator_item_square, linearLayout, false);
            } else {
                view = inflater.inflate(R.layout.chat_scroll_indicator_item_circle, linearLayout, false);
            }



            linearLayout.addView(view);
            final AccountViewHolder accountViewHolder = new AccountViewHolder(view);

            final AccountPainter accountPainter = ColorManager.getInstance().getAccountPainter();

            if (i > 0) {
                final String account = activeChats.get(i - 1).getAccount();
                ((GradientDrawable)accountViewHolder.body.getDrawable()).setColor(accountPainter.getAccountMainColor(account));
                ((GradientDrawable)accountViewHolder.selection.getDrawable()).setColor(accountPainter.getAccountMainColor(account));
            } else {
                accountViewHolder.body.setImageDrawable(new ColorDrawable(accountPainter.getDefaultMainColor()));
                accountViewHolder.selection.setImageDrawable(new ColorDrawable(accountPainter.getDefaultMainColor()));
            }

            view.setTag(accountViewHolder);
        }
    }

    private static class AccountViewHolder {
        final ImageView body;
        final ImageView selection;

        public AccountViewHolder(View view) {
            body = (ImageView) view.findViewById(R.id.indicator_item_body);
            selection = (ImageView) view.findViewById(R.id.indicator_item_selection);
        }
    }
}
