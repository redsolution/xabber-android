package com.xabber.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.jivesoftware.smackx.muc.HostedRoom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HostedConferencesAdapter extends BaseAdapter implements Filterable {

    private List<HostedRoom> originalData = null;
    private List<HostedRoom> filteredData = null;
    private LayoutInflater inflater;
    private HostedRoomsFilter filter;

    public HostedConferencesAdapter(Context context) {
        filteredData = new ArrayList<>();
        originalData = new ArrayList<>();
        filter = new HostedRoomsFilter();
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return filteredData.size();
    }

    @Override
    public HostedRoom getItem(int position) {
        return filteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_2, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(android.R.id.text1);
            holder.jid = (TextView) convertView.findViewById(android.R.id.text2);

            // Bind the data efficiently with the holder.

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }

        // If weren't re-ordering this you could rely on what you set last time

        HostedRoom hostedRoom = filteredData.get(position);
        holder.name.setText(hostedRoom.getName());
        holder.jid.setText(hostedRoom.getJid());

        return convertView;
    }



    static class ViewHolder {
        TextView name;
        TextView jid;
    }

    public void clear() {
        filteredData.clear();
        originalData.clear();
        notifyDataSetChanged();
    }

    public void addAll(Collection<HostedRoom> hostedRooms) {
        filteredData.addAll(hostedRooms);
        originalData.addAll(hostedRooms);
        notifyDataSetChanged();
    }


    @Override
    public Filter getFilter() {
        return filter;
    }

    private class HostedRoomsFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            final List<HostedRoom> list = originalData;

            int count = list.size();
            final ArrayList<HostedRoom> newList = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                HostedRoom room = list.get(i);
                String filterableJidName = room.getJid().getLocalpart().toString().toLowerCase();
                String filterableRoomName = room.getName().toLowerCase();
                if (filterableJidName.contains(filterString) || filterableRoomName.contains(filterString)) {
                    newList.add(room);
                }
            }

            results.values = newList;
            results.count = newList.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (List<HostedRoom>) results.values;
            notifyDataSetChanged();
        }
    }

    public List<HostedRoom> getConferencesList() {
        return originalData;
    }
}
