package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;

import org.jetbrains.annotations.NotNull;

public class GroupchatInviteReasonDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String LOG_TAG = GroupchatInviteReasonDialog.class.getSimpleName();

    private GroupchatInviteReasonListener listener;

    private EditText edtInviteReason;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        if (getActivity() instanceof GroupchatInviteReasonListener) {
            listener = (GroupchatInviteReasonListener) getActivity();
        } else {
            throw new RuntimeException(getActivity() + " needs to implement GroupchatInviteReasonListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_groupchat_invite_reason, null);

        edtInviteReason = view.findViewById(R.id.edt_invite_reason);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.groupchat_invite_reason_dialog_title)
                .setMessage(R.string.groupchat_invite_reason_dialog_message)
                .setView(view)
                .setPositiveButton(R.string.chat_send, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setNeutralButton(R.string.skip, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
            case DialogInterface.BUTTON_NEUTRAL:
                if (listener != null) {
                    listener.onReasonSelected(edtInviteReason.getText().toString().trim());
                }
            case DialogInterface.BUTTON_NEGATIVE:
            default:
                dismiss();
                break;
        }
    }

    public interface GroupchatInviteReasonListener {
        void onReasonSelected(String reason);
    }

}
