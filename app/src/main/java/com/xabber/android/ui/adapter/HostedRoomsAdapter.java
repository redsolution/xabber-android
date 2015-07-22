package com.xabber.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.jivesoftware.smackx.muc.HostedRoom;

public class HostedRoomsAdapter extends ArrayAdapter<HostedRoom> {

    public HostedRoomsAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false);
        } else {
            view = convertView;
        }

        TextView mainText = (TextView) view.findViewById(android.R.id.text1);
        TextView secondText = (TextView) view.findViewById(android.R.id.text2);

        HostedRoom room = getItem(position);

        mainText.setText(room.getName());
        secondText.setText(room.getJid());

        return view;
    }
}
