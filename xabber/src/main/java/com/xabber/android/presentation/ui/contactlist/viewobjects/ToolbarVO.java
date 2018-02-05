package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.ui.adapter.contactlist.MainTitleViewHolder;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by valery.miller on 05.02.18.
 */

public class ToolbarVO extends AbstractFlexibleItem<ToolbarVO.ViewHolder> {

    private String id;

    @Override
    public boolean equals(Object o) {
        if (o instanceof ToolbarVO) {
            ToolbarVO inItem = (ToolbarVO) o;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_main_title_in_contact_list;
    }

    @Override
    public ViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new ViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder holder, int position, List<Object> payloads) {

    }

    public class ViewHolder extends FlexibleViewHolder {
        final View accountColorIndicator;
        final ImageView ivAdd;
        final ImageView ivSetStatus;
        final TextView tvTitle;

        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);

            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            ivAdd = (ImageView) view.findViewById(R.id.ivAdd);
            ivAdd.setOnClickListener(this);
            ivSetStatus = (ImageView) view.findViewById(R.id.ivSetStatus);
            ivSetStatus.setOnClickListener(this);
            tvTitle = (TextView) view.findViewById(R.id.tvTitle);
            tvTitle.setOnClickListener(this);
        }
    }



}
