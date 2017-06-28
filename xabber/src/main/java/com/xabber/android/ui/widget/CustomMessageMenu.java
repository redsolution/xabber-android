package com.xabber.android.ui.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;

import com.xabber.android.ui.adapter.CustomMessageMenuAdapter;

import java.util.HashMap;
import java.util.List;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;

/**
 * Created by valery.miller on 27.06.17.
 */

public class CustomMessageMenu {

    public static void showMenu(Context context, View anchor, List<HashMap<String, String>> items,
                                final AdapterView.OnItemClickListener itemClickListener,
                                PopupWindow.OnDismissListener dismissListener) {

        // build popup
        final ListPopupWindow popup = new ListPopupWindow(context);
        CustomMessageMenuAdapter adapter = new CustomMessageMenuAdapter(
                context,
                items);
        popup.setAdapter(adapter);
        popup.setAnchorView(anchor);
        popup.setModal(true);
        popup.setSoftInputMode(SOFT_INPUT_ADJUST_NOTHING);
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemClickListener.onItemClick(parent, view, position, id);
                popup.dismiss();
            }
        });
        popup.setOnDismissListener(dismissListener);

        // measure content dimens
        ViewGroup mMeasureParent = null;
        int height = 0;
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(context);
            }

            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemHeight = itemView.getMeasuredHeight();
            final int itemWidth = itemView.getMeasuredWidth();

            if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
            height += itemHeight;
        }

        // set dimens and show
        popup.setWidth(maxWidth);
        popup.setHeight(height);
        popup.show();
    }

    public static void addMenuItem(List<HashMap<String, String>> items, String id, String title) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(CustomMessageMenuAdapter.KEY_ID, id);
        map.put(CustomMessageMenuAdapter.KEY_TITLE, title);
        items.add(map);
    }

}
