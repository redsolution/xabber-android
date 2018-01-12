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

/**
 * Created by valery.miller on 11.01.18.
 */

public class XabberAccountConfirmPhoneFragment extends Fragment {

    private Button btnSetPhone;
    private Button btnConfirm;
    private EditText edtPhone;
    private EditText edtCode;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xabber_account_phone_number_confirm, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSetPhone = (Button) view.findViewById(R.id.btnSetPhone);
        btnSetPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPhoneNumber();
            }
        });

        btnConfirm = (Button) view.findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCode();
            }
        });

        edtPhone = (EditText) view.findViewById(R.id.edtPhone);
        edtCode = (EditText) view.findViewById(R.id.edtCode);
    }

    private void checkPhoneNumber() {
        String phone = edtPhone.getText().toString().replaceAll(" ", "");

        if (phone.isEmpty() || phone.length() < 12) {
            edtPhone.setError(getString(R.string.empty_field));
            return;
        }

        ((XabberAccountInfoActivity)getActivity()).onSetPhoneClick(phone);
    }

    private void checkCode() {
        String code = edtCode.getText().toString().replaceAll(" ", "");

        if (code.isEmpty()) {
            edtCode.setError(getString(R.string.empty_field));
            return;
        }

        if (code.length() != 6) {
            edtCode.setError(getString(R.string.code_length_wrong));
            return;
        }

        ((XabberAccountInfoActivity)getActivity()).onConfirmCodeClick(code);
    }
}
