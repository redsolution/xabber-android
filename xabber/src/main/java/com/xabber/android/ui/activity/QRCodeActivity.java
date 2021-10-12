package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.color.ColorManager;

import org.jxmpp.jid.Jid;

public class QRCodeActivity extends ManagedActivity {

    private Toolbar toolbar;

    private static final String TITLE_ARGUMENT = "com.xabber.android.ui.activity.QRCodeActivity.TITLE_ARGUMENT";
    private static final String TO_BE_ENCODED_STRING = "com.xabber.android.ui.activity.QRCodeActivity.TO_BE_ENCODED_STRING";

    public static Intent createIntentForXmppEntity(Context context, String title, Jid jid) {
        return createIntent(context, title, "xmpp:".concat(jid.toString()));
    }

    public static Intent createIntent(Context context, String title, String toBeEncodedString) {
        Intent intent = new Intent(context, QRCodeActivity.class);
        intent.putExtra(TITLE_ARGUMENT, title);
        intent.putExtra(TO_BE_ENCODED_STRING, toBeEncodedString);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.qrcode_activity);

        toolbar = findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView shareIv = findViewById(R.id.ic_share);
        Drawable shareIcon = ResourcesCompat.getDrawable(
                getResources(), R.drawable.ic_share, null
        );

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            shareIcon.setColorFilter(
                    ResourcesCompat.getColor(getResources(), R.color.grey_900, null),
                    PorterDuff.Mode.SRC_ATOP
            );
        }

        shareIv.setImageDrawable(shareIcon);

        shareIv.setOnClickListener((view) -> {
            if (getIntent().hasExtra(TO_BE_ENCODED_STRING)) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                getIntent().putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        getIntent().getExtras().get(TO_BE_ENCODED_STRING).toString()
                );
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
            }
        });

        toolbar.setTitle(R.string.dialog_show_qr_code__header);

        if (getIntent().hasExtra(TO_BE_ENCODED_STRING) && getIntent().hasExtra(TITLE_ARGUMENT)){
            TextView textView = findViewById(R.id.textView);
            TextView textView2 = findViewById(R.id.textView2);

            String title = getIntent().getStringExtra(TITLE_ARGUMENT);
            String toBeEncodedString = getIntent().getStringExtra(TO_BE_ENCODED_STRING);

            if (title.equals("")) {
                textView.setText(toBeEncodedString);
            } else {
                textView.setText(title);
                textView2.setText(toBeEncodedString);
                textView2.setVisibility(View.VISIBLE);
            }

            try {
                ((ImageView) findViewById(R.id.qrCode)).setImageBitmap(
                        new BarcodeEncoder().encodeBitmap(
                                toBeEncodedString, BarcodeFormat.QR_CODE, 600, 600
                        )
                );
            } catch (Exception e){
                LogManager.exception(this, e);
            }
        }

        setColors();
    }

    public void setColors(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                getWindow().setStatusBarColor(
                        ColorManager.getInstance().getAccountPainter().getDefaultMainColor()
                );
                toolbar.setBackgroundColor(
                        ColorManager.getInstance().getAccountPainter().getDefaultRippleColor()
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

}