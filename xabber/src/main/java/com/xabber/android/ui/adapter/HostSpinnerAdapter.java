package com.xabber.android.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.xabber.android.R;

import java.util.List;

public class HostSpinnerAdapter extends ArrayAdapter<String> {

    private List<String> mData;
    private LayoutInflater mInflater;

    public HostSpinnerAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);

        mData = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {

        View view = mInflater.inflate(R.layout.item_host, parent, false);
        TextView tvHost = (TextView) view.findViewById(R.id.tvHost);
        TextView tvStatus = (TextView) view.findViewById(R.id.tvStatus);
        tvHost.setText(mData.get(position));

        return view;
    }
}
