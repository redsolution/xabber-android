package com.xabber.android.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;

import java.util.List;

public class ForwardPanel extends Fragment {

    private List<String> forwardedIds;
    private TextView tvForwardedFrom;
    private TextView tvForwardedText;
    private ImageView ivCloseForwardPanel;
    private OnCloseListener listener;

    public interface OnCloseListener {
        void onClose();
    }

    public static ForwardPanel newInstance(List<String> forwardedIds) {
        ForwardPanel panel = new ForwardPanel();
        panel.forwardedIds = forwardedIds;
        return panel;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnCloseListener) {
            listener = (OnCloseListener) getParentFragment();
        } else {
            throw new ClassCastException("must implement OnCloseListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_chat_forward, container, false);

        tvForwardedText = view.findViewById(R.id.tvForwardedText);
        tvForwardedFrom = view.findViewById(R.id.tvForwardedFrom);
        ivCloseForwardPanel = view.findViewById(R.id.ivCloseForwardPanel);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int count = forwardedIds.size();
        tvForwardedFrom.setText("Valery Miller");
        tvForwardedText.setText(count + " forwarded messages");

        ivCloseForwardPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClose();
            }
        });
    }
}
