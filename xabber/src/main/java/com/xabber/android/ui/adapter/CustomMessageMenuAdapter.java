package com.xabber.android.ui.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.message.MessageStatus;

import java.util.HashMap;
import java.util.List;

/**
 * Created by valery.miller on 23.06.17.
 */

public class CustomMessageMenuAdapter extends BaseAdapter {

    public final static String KEY_ID_STATUS = "action_message_status";
    public final static String KEY_ID_TIMESTAMP = "action_message_timestamp";

    public final static String KEY_ID = "id";
    public final static String KEY_TITLE = "title";

    List<HashMap<String, String>> items;
    LayoutInflater lInflater;

    public CustomMessageMenuAdapter(Context context, List<HashMap<String, String>> items) {
        this.items = items;
        lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HashMap<String, String> item = items.get(position);

        View view;
        TextView textView;
        ImageView ivStatus;

        if (item.get(KEY_ID).equals(KEY_ID_TIMESTAMP)) {
            view = lInflater.inflate(R.layout.item_menu_message_timestamp, parent, false);
            textView = (TextView) view.findViewById(R.id.tvStatus);
            textView.setText(item.get(KEY_TITLE));
        } else if (item.get(KEY_ID).equals(KEY_ID_STATUS)) {

            view = lInflater.inflate(R.layout.item_menu_message_status, parent, false);
            textView = (TextView) view.findViewById(R.id.tvStatus);
            ivStatus = (ImageView) view.findViewById(R.id.ivStatus);

            switch (MessageStatus.valueOf(item.get(KEY_TITLE))) {
                case ERROR:
                    textView.setText(R.string.message_status_error);
                    ivStatus.setImageResource(R.drawable.ic_message_has_error_14dp);
                    textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    break;
                case SENT:
                    textView.setText(R.string.message_status_not_sent);
                    ivStatus.setImageResource(R.drawable.ic_message_not_sent_14dp);
                    break;
                case DISPLAYED:
                    textView.setText(R.string.message_status_displayed);
                    ivStatus.setImageResource(R.drawable.ic_message_displayed);
                    break;
                case RECEIVED:
                    textView.setText(R.string.message_status_delivered);
                    ivStatus.setImageResource(R.drawable.ic_message_delivered_14dp);
                    break;
                case DELIVERED:
                    textView.setText(R.string.message_status_sent);
                    ivStatus.setImageResource(R.drawable.ic_message_acknowledged_14dp);
                    break;
                default:
            }
        } else {
            view = lInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(item.get(KEY_TITLE));
        }
        return view;
    }

}
