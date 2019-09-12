/*
package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.FingerprintQRCodeFragment;


//Создает активити для вывода фрагмента с QR-кодом фингерпринта владельца аккаунта


public class FingerprintQRCodeActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private View contactTitleView;
    private AccountJid account;

    public static Intent createIntent(Context context, AccountJid account) {
        return new EntityIntentBuilder(context, FingerprintQRCodeActivity.class)
                .setAccount(account).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_fingerprint_popup_qr);
        setContentView(R.layout.activity_with_toolbar_and_container);

        Intent intent = getIntent();
*/
/*
        Bundle bundle = getIntent().getExtras();
*//*

        if(intent.hasExtra("fingerprint")){
            Bundle bundle = intent.getExtras();
            String fingerprint = bundle.get("fingerprint").toString();
            if(savedInstanceState==null){
                getFragmentManager().beginTransaction().add(R.id.fragment_container, FingerprintQRCodeFragment.newInstance(fingerprint)).commit();
            }
        }

        if(intent.hasExtra("account_name")&&intent.hasExtra("account_address")){
            Bundle bundle = intent.getExtras();
            String accountName = bundle.get("account_name").toString();
            String accountAddress = bundle.get("account_address").toString();
            if(savedInstanceState==null){
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, FingerprintQRCodeFragment.newInstance(accountName, accountAddress)).commit();
            }
        }


        //ImageView qrCode = findViewById(R.id.qrCode);

        findViewById(R.id.fragment_container).setBackgroundColor(Color.WHITE);

        account = getAccount(intent);
        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle("My QR Code");
        setColors(account);
    }

    public void setColors(AccountJid account){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ColorManager.getInstance().getAccountPainter().getAccountDarkColor(account));
            toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
        }
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

}*/
