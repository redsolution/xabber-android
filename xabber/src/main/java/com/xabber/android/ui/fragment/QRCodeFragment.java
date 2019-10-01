package com.xabber.android.ui.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import android.app.Fragment;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.xabber.android.R;



public class QRCodeFragment extends Fragment {

    private String fingerprint;
    private String accountName;
    private String accountAddress;

    public QRCodeFragment(){
    }

    public static QRCodeFragment newInstance(String fingerprint){
        QRCodeFragment fragment = new QRCodeFragment();
        Bundle arg = new Bundle();
        arg.putString("fingerprint", fingerprint);
        fragment.setArguments(arg);
        return fragment;
    }

    public static QRCodeFragment newInstance(String accountName, String accountAddress){
        QRCodeFragment fragment = new QRCodeFragment();
        Bundle arg = new Bundle();
        arg.putString("account_name", accountName);
        arg.putString("account_address", accountAddress);
        fragment.setArguments(arg);
        return fragment;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fingerprint_popup_qr, container, false);
        ImageView qrCode = view.findViewById(R.id.qrCode);
        TextView textView = view.findViewById(R.id.textView);
        TextView textView2 = view.findViewById(R.id.textView2);

        if(getArguments()!=null){
            if(getArguments().getString("fingerprint")!=null) {
                fingerprint = getArguments().getString("fingerprint");
            }else {
                accountName = getArguments().getString("account_name");
                accountAddress = getArguments().getString("account_address");
                if(accountName.equals(""))
                    textView.setText(accountAddress);
                else {
                    textView.setText(accountName);
                    textView2.setText(accountAddress);
                    textView2.setVisibility(View.VISIBLE);
                }
            }
        }

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap;
            if(fingerprint!=null)
                bitmap = barcodeEncoder.encodeBitmap(fingerprint, BarcodeFormat.QR_CODE, 600, 600);
            else bitmap = barcodeEncoder.encodeBitmap("xmpp:" + accountAddress, BarcodeFormat.QR_CODE, 600, 600);
            qrCode.setImageBitmap(bitmap);
        }catch (Exception e){

        }

        return view;
    }

}
