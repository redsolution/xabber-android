package com.xabber.android.ui.adapter;

import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.ui.dialog.AccountColorPickerListener;


public class AccountColorPickerAdapter extends RecyclerView.Adapter<AccountColorPickerAdapter.AccountColorPickerViewHolder> implements AccountColorPickerListener {

    private final Listener listener;
    private final String[] nameList;
    private final int checked;
    private final TypedArray colors;

    public interface Listener {
        void onColorClickListener(int position);
    }

    public AccountColorPickerAdapter(String[] nameList, TypedArray colors, int checked, Listener listener) {
        this.nameList = nameList;
        this.colors = colors;
        this.checked = checked;
        this.listener = listener;
    }

    @Override
    public AccountColorPickerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dialog_color_picker_item, parent, false);

        return new AccountColorPickerViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(AccountColorPickerViewHolder holder, int position) {
        String colorName = nameList[position];
        int color = colors.getResourceId(position, 0);

        holder.colorItem.setText(colorName);
        holder.colorVisual.setImageResource(color);
        if (position == checked) {
            holder.colorItem.setChecked(true);
        } else holder.colorItem.setChecked(false);
    }

    @Override
    public int getItemCount() {
        return nameList.length;
    }

    @Override
    public void onColorClickListener(int position) {
        listener.onColorClickListener(position);
    }

    class AccountColorPickerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AccountColorPickerListener listener;
        private RadioButton colorItem;
        private ImageView colorVisual;

        public AccountColorPickerViewHolder(View itemView, AccountColorPickerListener listener) {
            super(itemView);
            this.listener = listener;

            colorItem = (RadioButton) itemView.findViewById(R.id.color_item);
            colorVisual = (ImageView) itemView.findViewById(R.id.color_item_visual);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            listener.onColorClickListener(adapterPosition);
        }
    }


}