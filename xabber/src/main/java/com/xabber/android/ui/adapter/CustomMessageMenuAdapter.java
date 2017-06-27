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

import java.util.HashMap;
import java.util.List;

/**
 * Created by valery.miller on 23.06.17.
 */

public class CustomMessageMenuAdapter extends BaseAdapter {

    public final static String STATUS_ACK = "status_acknowledged";
    public final static String STATUS_DELIVERED = "status_delivered";
    public final static String STATUS_ERROR = "status_error";
    public final static String STATUS_FORWARDED = "status_forwarded";
    public final static String STATUS_SYNCED = "status_synced";
    public final static String STATUS_NOT_SEND = "status_not_send";

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

        View view = convertView;
        TextView textView;
        ImageView ivStatus;

        if (item.get(KEY_ID).equals("action_message_status")) {
            view = lInflater.inflate(R.layout.item_menu_message_status, parent, false);
            textView = (TextView) view.findViewById(R.id.tvStatus);
            ivStatus = (ImageView) view.findViewById(R.id.ivStatus);

            switch (item.get(KEY_TITLE)) {
                case STATUS_ACK:
                    textView.setText(R.string.message_status_sent);
                    ivStatus.setImageResource(R.drawable.ic_message_acknowledged_14dp);
                    break;
                case STATUS_DELIVERED:
                    textView.setText(R.string.message_status_delivered);
                    ivStatus.setImageResource(R.drawable.ic_message_delivered_14dp);
                    break;
                case STATUS_SYNCED:
                    textView.setText(R.string.message_status_synced);
                    ivStatus.setImageResource(R.drawable.ic_message_synced_14dp);
                    break;
                case STATUS_ERROR:
                    textView.setText(R.string.message_status_error);
                    ivStatus.setImageResource(R.drawable.ic_message_has_error_14dp);
                    textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    break;
                case STATUS_FORWARDED:
                    textView.setText(R.string.message_status_forwarded);
                    ivStatus.setImageResource(R.drawable.ic_message_forwarded_14dp);
                    break;
                case STATUS_NOT_SEND:
                    textView.setText(R.string.message_status_not_sent);
                    ivStatus.setImageResource(R.drawable.ic_message_not_sent_14dp);
                    break;
            }

        } else {
            view = lInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(item.get(KEY_TITLE));
        }
        return view;
    }
}
