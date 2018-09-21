package com.xabber.android.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.xaccount.AuthManager;

import java.util.List;

public class HostSpinnerAdapter extends ArrayAdapter<String> {

    private List<AuthManager.Host> mData;
    private LayoutInflater mInflater;
    private int currentPosition;

    public HostSpinnerAdapter(@NonNull Context context, int resource, @NonNull List<String> hosts,  @NonNull List<AuthManager.Host> objects) {
        super(context, resource, hosts);

        mData = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setCurrentPosition(int position) {
        this.currentPosition = position;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        AuthManager.Host host = mData.get(position);

        View view = mInflater.inflate(R.layout.item_host, parent, false);
        TextView tvHost = (TextView) view.findViewById(R.id.tvHost);
        TextView tvStatus = (TextView) view.findViewById(R.id.tvStatus);

        tvHost.setText(host.getHost());
        tvStatus.setText(host.isFree() ? Application.getInstance().getResources().getString(R.string.free) : host.getPrice());

        if (position == currentPosition) view.setBackgroundColor(
                Application.getInstance().getResources().getColor(R.color.grey_200));

        return view;
    }
}
