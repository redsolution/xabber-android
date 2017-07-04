package com.xabber.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.xabber.android.R;

import java.util.List;
import java.util.Map;

/**
 * Created by valery.miller on 03.07.17.
 */

public class ResourceAdapter extends BaseAdapter {

    public final static String KEY_CLIENT = "key_client";
    public final static String KEY_RESOURCE = "key_resource";

    private List<Map<String, String>> items;
    private LayoutInflater lInflater;
    private int checkedItem;

    public ResourceAdapter(Context context, List<Map<String, String>> items) {
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
    public View getView(final int position, View convertView, ViewGroup parent) {

        View view = lInflater.inflate(R.layout.item_user_resource, parent, false);
        TextView tvClient = (TextView) view.findViewById(R.id.tvClient);
        TextView tvResource = (TextView) view.findViewById(R.id.tvResource);
        final RadioButton radioButton = (RadioButton) view.findViewById(R.id.radioButton);

        if (position == checkedItem) radioButton.setChecked(true);

        tvClient.setText(items.get(position).get(KEY_CLIENT));
        tvResource.setText(items.get(position).get(KEY_RESOURCE));

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioButton.setChecked(true);
                checkedItem = position;
                notifyDataSetChanged();
            }
        });

        return view;
    }

    public int getCheckedItem() {
        return checkedItem;
    }

    public void setCheckedItem(int checkedItem) {
        this.checkedItem = checkedItem;
    }
}
