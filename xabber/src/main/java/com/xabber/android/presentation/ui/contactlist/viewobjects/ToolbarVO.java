package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.ui.adapter.contactlist.ContactListAdapter;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.flexibleadapter.items.IHeader;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by valery.miller on 05.02.18.
 */

public class ToolbarVO extends AbstractHeaderItem<ToolbarVO.ViewHolder> implements IHeader<ToolbarVO.ViewHolder> {

    private String id;

    private ContactListAdapter.ChatListState currentChatsState =
            ContactListAdapter.ChatListState.recent;

    public ToolbarVO() {
        this.id = UUID.randomUUID().toString();
    }

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
        Context context = holder.itemView.getContext();

        /** set up ACCOUNT COLOR indicator */
        holder.accountColorIndicator.setBackgroundColor(
                ColorManager.getInstance().getAccountPainter().getDefaultMainColor());

        /** set up BACKGROUND COLOR */
        final int[] accountGroupColors = context.getResources().getIntArray(
                getThemeResource(context, R.attr.contact_list_account_group_background));
        final int level = AccountManager.getInstance().getColorLevel(AccountPainter.getFirstAccount());
        holder.itemView.setBackgroundColor(accountGroupColors[level]);

        /** set up STATE TITLE */
        switch (currentChatsState) {
            case unread:
                holder.tvTitle.setText(R.string.unread_chats);
                break;
            case archived:
                holder.tvTitle.setText(R.string.archived_chats);
                break;
            case all:
                holder.tvTitle.setText(R.string.all_chats);
                break;
            default:
                holder.tvTitle.setText("Xabber");
                break;
        }
    }

    private int getThemeResource(Context context, int themeResourceId) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }

    public class ViewHolder extends FlexibleViewHolder {
        final View accountColorIndicator;
        final ImageView ivAdd;
        final ImageView ivSetStatus;
        final TextView tvTitle;

        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true);

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
