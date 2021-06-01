package com.xabber.android.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.xabber.android.R;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.message.chat.AbstractChat;

public class SnoozeDialog extends BottomSheetDialogFragment implements View.OnClickListener {

    private OnSnoozeListener listener;
    private AbstractChat chat;
    private View view;

    public static SnoozeDialog newInstance(AbstractChat chat, OnSnoozeListener listener) {
        SnoozeDialog dialog = new SnoozeDialog();
        dialog.chat = chat;
        dialog.listener = listener;
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.snooze_dialog, container, false);

        view.findViewById(R.id.itemDisable).setOnClickListener(this);
        view.findViewById(R.id.itemSnooze15m).setOnClickListener(this);
        view.findViewById(R.id.itemSnooze1h).setOnClickListener(this);
        view.findViewById(R.id.itemSnooze2h).setOnClickListener(this);
        view.findViewById(R.id.itemSnooze1d).setOnClickListener(this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onClick(View v) {
        if (chat != null) {
            switch (v.getId()) {
                case R.id.itemDisable:
                    chat.setNotificationStateOrDefault(
                            new NotificationState(NotificationState.NotificationMode.disabled,
                                    0), true);
                    break;
                case R.id.itemSnooze15m:
                    chat.setNotificationState(
                            new NotificationState(NotificationState.NotificationMode.snooze15m,
                                    getCurrentTime()), true);
                    break;
                case R.id.itemSnooze1h:
                    chat.setNotificationState(
                            new NotificationState(NotificationState.NotificationMode.snooze1h,
                                    getCurrentTime()), true);
                    break;
                case R.id.itemSnooze2h:
                    chat.setNotificationState(
                            new NotificationState(NotificationState.NotificationMode.snooze2h,
                                    getCurrentTime()), true);
                    break;
                case R.id.itemSnooze1d:
                    chat.setNotificationState(
                            new NotificationState(NotificationState.NotificationMode.snooze1d,
                                    getCurrentTime()), true);
                    break;
            }
        }
        if (listener != null) listener.onSnoozed();
        this.dismiss();
    }

    private int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public interface OnSnoozeListener {
        void onSnoozed();
    }
}
