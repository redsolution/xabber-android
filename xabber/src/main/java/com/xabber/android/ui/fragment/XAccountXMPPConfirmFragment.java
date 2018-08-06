package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.xabber.android.R;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;

public class XAccountXMPPConfirmFragment extends Fragment implements View.OnClickListener {

    private String jid;

    private EditText edtCode;
    private Button btnConfirm;
    private Button btnResend;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_xmpp_confirm, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtCode = view.findViewById(R.id.edtCode);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        btnResend = view.findViewById(R.id.btnResend);

        btnConfirm.setOnClickListener(this);
        btnResend.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnConfirm:
                onConfirmClick();
                break;
            case R.id.btnResend:
                onResendClick();
                break;
        }
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    private void onConfirmClick() {
        // TODO: 06.08.18 verify fields
        String code = edtCode.getText().toString();
        ((XabberAccountInfoActivity)getActivity()).onConfirmXMPPClick(jid, code);
    }

    private void onResendClick() {
        // TODO: 06.08.18 implement
    }
}
