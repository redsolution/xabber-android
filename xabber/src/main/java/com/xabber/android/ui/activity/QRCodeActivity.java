package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.QRCodeFragment;

public class QRCodeActivity extends ManagedActivity {

    public static final String ACCOUNT_NAME_ARG = "com.xabber.android.ui.activity.account_name";
    public static final String ACCOUNT_ADDRESS_ARG = "com.xabber.android.ui.activity.account_address";

    private Toolbar toolbar;

    public static Intent createIntent(Context context, AccountJid account) {
        return new EntityIntentBuilder(context, QRCodeActivity.class).setAccount(account).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.qrcode_activity);

        Intent intent = getIntent();

        AccountJid account = getAccount(intent);
        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView shareIv = (ImageView) findViewById(R.id.ic_share);
        Drawable shareIcon = ResourcesCompat.getDrawable(
                getResources(), R.drawable.ic_share, null
        );
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            shareIcon.setColorFilter(
                    ResourcesCompat.getColor(
                            getResources(), R.color.grey_900, null
                    ),
                    PorterDuff.Mode.SRC_ATOP
            );
        }
        shareIv.setImageDrawable(shareIcon);
        shareIv.setOnClickListener((view) -> {
            if (intent.hasExtra(ACCOUNT_ADDRESS_ARG)) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareText = "xmpp:" + intent.getExtras().get(ACCOUNT_ADDRESS_ARG).toString();
                intent.putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        shareText
                );
                startActivity(Intent.createChooser(intent, getString(R.string.share)));
            }
        });

        toolbar.setTitle(R.string.dialog_show_qr_code__header);

        if(intent.hasExtra("fingerprint")){
            String fingerprint = intent.getExtras().get("fingerprint").toString();
            if(savedInstanceState == null){
                getFragmentManager()
                        .beginTransaction()
                        .add(
                                R.id.fragment_container,
                                QRCodeFragment.newInstance(fingerprint)
                        ).commit();
            }
        }

        if(intent.hasExtra(ACCOUNT_NAME_ARG)&&intent.hasExtra(ACCOUNT_ADDRESS_ARG)){
            Bundle bundle = intent.getExtras();
            String accountName = bundle.get(ACCOUNT_NAME_ARG).toString();
            String accountAddress = bundle.get(ACCOUNT_ADDRESS_ARG).toString();
            if(savedInstanceState == null){
                getFragmentManager()
                        .beginTransaction()
                        .add(
                                R.id.fragment_container,
                                QRCodeFragment.newInstance(accountName, accountAddress)
                        ).commit();
            }
        }

        setColors(account);
    }

    public void setColors(AccountJid account){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                getWindow().setStatusBarColor(
                        ColorManager.getInstance().getAccountPainter().getAccountMainColor(account)
                );
                toolbar.setBackgroundColor(
                        ColorManager.getInstance().getAccountPainter().getAccountRippleColor(
                                account
                        )
                );
                findViewById(R.id.fragment_container).setBackgroundColor(Color.WHITE);
            } else {
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.attr.bars_color, typedValue, true);
                toolbar.setBackgroundColor(typedValue.data);
                toolbar.setTitleTextColor(Color.WHITE);
                getWindow().setStatusBarColor(typedValue.data);
                findViewById(R.id.fragment_container).setBackgroundColor(Color.BLACK);
            }
        }
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

}