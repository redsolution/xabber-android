package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by valery.miller on 15.02.18.
 */

public class CategoryVO extends AbstractFlexibleItem<CategoryVO.ViewHolder> {

    private final String id;
    private final String title;

    public CategoryVO(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CategoryVO) {
            CategoryVO inItem = (CategoryVO) o;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_category_in_contact_list;
    }

    @Override
    public ViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new ViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder holder, int position, List<Object> payloads) {
        holder.tvTitle.setText(title);

        /** set up ACCOUNT COLOR indicator */
        holder.accountColorIndicator.setBackgroundColor(
                ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        holder.accountColorIndicatorBack.setBackgroundColor(
                ColorManager.getInstance().getAccountPainter().getDefaultIndicatorBackColor());
    }

    public class ViewHolder extends FlexibleViewHolder {
        final TextView tvTitle;
        final View accountColorIndicator;
        final View accountColorIndicatorBack;

        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            accountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
            tvTitle = (TextView) view.findViewById(R.id.tvTitle);
        }
    }
}
