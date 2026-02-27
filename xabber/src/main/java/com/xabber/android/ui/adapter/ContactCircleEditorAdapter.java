package com.xabber.android.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;

import java.util.ArrayList;
import java.util.Collection;


public class ContactCircleEditorAdapter extends RecyclerView.Adapter {

    private static final int CONTACT_CIRCLE = 0;
    private static final int INPUT_FIELD = 1;

    private final ArrayList<ContactCircle> contactCircles = new ArrayList<>();
    private String inputNewCircle;

    private final OnCircleActionListener listener;

    public interface OnCircleActionListener {
        void onCircleAdded();
        void onCircleToggled();
    }

    public ContactCircleEditorAdapter(OnCircleActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == CONTACT_CIRCLE) {
            return new CircleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false), listener);
        } else {
            return new InputViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group_add_footer, parent, false), listener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position != contactCircles.size()) {
            CircleViewHolder circleViewHolder = (CircleViewHolder) holder;
            ContactCircle circle = contactCircles.get(position);
            circleViewHolder.groupName.setText(circle.circleName);
            circleViewHolder.groupCheckbox.setChecked(circle.isSelected);
        } else {
            InputViewHolder circleViewHolder = (InputViewHolder) holder;
            circleViewHolder.inputField.setText(inputNewCircle);
            circleViewHolder.inputCheckbox.setChecked(false);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == contactCircles.size() ? INPUT_FIELD : CONTACT_CIRCLE;
    }

    @Override
    public int getItemCount() {
        return contactCircles.size() + 1; // 1 = input field
    }

    public void add(Collection<ContactCircle> list) {
        int oldSize = contactCircles.size();
        int range = list.size();
        contactCircles.addAll(list);
        notifyItemRangeInserted(oldSize, range);
    }

    public void add(String circle, boolean selected) {
        add(new ContactCircle(circle, selected));
    }

    public void add(ContactCircle circle) {
        contactCircles.add(circle);
        notifyItemInserted(contactCircles.size() - 1);
    }

    public void clear() {
        int range = contactCircles.size();
        contactCircles.clear();
        notifyItemRangeRemoved(0, range);
    }

    public ArrayList<String> getCircles() {
        ArrayList<String> circleNames = new ArrayList<>(contactCircles.size());
        for (ContactCircle circle : contactCircles) {
            circleNames.add(circle.circleName);
        }
        return circleNames;
    }

    public ArrayList<String> getSelected() {
        ArrayList<String> selectedCircleNames = new ArrayList<>(contactCircles.size());
        for (ContactCircle circle : contactCircles) {
            if (circle.isSelected) selectedCircleNames.add(circle.circleName);
        }
        return selectedCircleNames;
    }

    public String getInputCircleName() {
        return inputNewCircle == null ? "" : inputNewCircle;
    }

    public void setInputCircleName(String circleName) {
        inputNewCircle = circleName == null ? "" : circleName;
    }

    private class InputViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, TextWatcher {

        EditText inputField;
        CheckBox inputCheckbox;
        OnCircleActionListener listener;

        public InputViewHolder(@NonNull View itemView, OnCircleActionListener listener) {
            super(itemView);
            inputField = itemView.findViewById(R.id.group_add_input);
            inputField.addTextChangedListener(this);
            inputCheckbox = itemView.findViewById(R.id.group_add_checkbox);
            inputCheckbox.setOnClickListener(this);
            inputCheckbox.setVisibility(View.INVISIBLE);
            inputCheckbox.setChecked(false);
            this.listener = listener;
        }

        @Override
        public void onClick(View v) {
            if (inputField.getText().toString().isEmpty()) {
                return;
            }
            contactCircles.add(new ContactCircle(inputField.getText().toString().trim(), true));
            inputField.getText().clear();
            inputField.clearFocus();
            listener.onCircleAdded();
            notifyItemInserted(contactCircles.size() - 1);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            if (s == null || s.toString().isEmpty() || getCircles().contains(s.toString().trim())) {
                inputCheckbox.setVisibility(View.INVISIBLE);
                inputCheckbox.setChecked(false);
                inputNewCircle = "";
            } else {
                inputCheckbox.setVisibility(View.VISIBLE);
                inputNewCircle = s.toString().trim();
            }
        }
    }

    private class CircleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView groupName;
        CheckBox groupCheckbox;
        OnCircleActionListener listener;

        public CircleViewHolder(@NonNull View itemView, OnCircleActionListener listener) {
            super(itemView);
            groupName = itemView.findViewById(R.id.group_item_name);
            groupCheckbox = itemView.findViewById(R.id.group_item_selected_checkbox);
            itemView.setOnClickListener(this);
            this.listener = listener;
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            ContactCircle clickedContactCircle = contactCircles.get(adapterPosition);
            clickedContactCircle.toggleSelection();
            groupCheckbox.setChecked(clickedContactCircle.isSelected);
            listener.onCircleToggled();
        }
    }

    public static class ContactCircle {

        private final String circleName;
        private boolean isSelected;

        public ContactCircle(String name, boolean selected) {
            circleName = name;
            isSelected = selected;
        }

        public void toggleSelection() {
            this.isSelected = !isSelected;
        }

        public void setIsSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }

        public String getCircleName() {
            return circleName;
        }

        public boolean isSelected() {
            return isSelected;
        }
    }

}
