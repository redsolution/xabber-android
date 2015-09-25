package com.xabber.android.ui.adapter;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.xabber.android.R;

import java.util.List;


public class GroupEditorAdapter extends ArrayAdapter<GroupEditorAdapter.Group> {

    private FragmentActivity activity;
    private int layoutResourceId;

    public GroupEditorAdapter(FragmentActivity activity, int layoutResourceId, List<GroupEditorAdapter.Group> objects) {
        super(activity, layoutResourceId, objects);

        this.activity = activity;
        this.layoutResourceId = layoutResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        GroupHolder holder;

        if (row == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new GroupHolder();
            holder.groupCheckbox = (CheckBox) row.findViewById(R.id.group_item_selected_checkbox);
            holder.groupName = (TextView) row.findViewById(R.id.group_item_name);

            row.setTag(holder);
        } else {
            holder = (GroupHolder)row.getTag();
        }

        Group group = getItem(position);

        holder.groupName.setText(group.groupName);
        holder.groupCheckbox.setChecked(group.isSelected);

        return row;
    }



    static class GroupHolder {
        TextView groupName;
        CheckBox groupCheckbox;
    }

    public static class Group {
        String groupName;
        boolean isSelected;

        public Group(String groupName, boolean isSelected) {
            this.groupName = groupName;
            this.isSelected = isSelected;
        }

        public String getGroupName() {
            return groupName;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setIsSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }

    }
}
