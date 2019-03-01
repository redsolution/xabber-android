package com.xabber.android.ui.dialog;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.NotificationState;

public class SnoozeDialog extends BottomSheetDialogFragment implements View.OnClickListener {

    private OnSnoozeListener listener;
    private AbstractChat chat;

    public static SnoozeDialog newInstance(AbstractChat chat, OnSnoozeListener listener) {
        SnoozeDialog dialog = new SnoozeDialog();
        dialog.chat = chat;
        dialog.listener = listener;
        return dialog;
    }

    public interface OnSnoozeListener {
        void onSnoozed();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.snooze_dialog, container, false);

        view.findViewById(R.id.itemDisable).setOnClickListener(this);
        view.findViewById(R.id.itemSnooze15m).setOnClickListener(this);
        view.findViewById(R.id.itemSnooze1h).setOnClickListener(this);
        view.findViewById(R.id.itemSnooze2h).setOnClickListener(this);
        view.findViewById(R.id.itemSnooze1d).setOnClickListener(this);

        return view;
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
}
