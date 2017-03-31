package com.xabber.android.ui.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.log.LogManager;

import java.lang.reflect.InvocationTargetException;


public class IntroActivity extends ManagedActivity {

    private static final String LOG_TAG = IntroActivity.class.getSimpleName();
    private View rootLayout;

    public static Intent createIntent(Context context) {
        return new Intent(context, IntroActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_intro);
        setStatusBarTranslucent();

        rootLayout = findViewById(R.id.intro_root_layout);

        int statusBarHeight = getStatusBarHeight();
        Point navigationBarSize = getNavigationBarSize(this);
        int navBarHeight = Math.min(navigationBarSize.x, navigationBarSize.y);

        rootLayout.setPadding(rootLayout.getPaddingLeft(), rootLayout.getPaddingTop() + statusBarHeight,
                rootLayout.getPaddingRight(), rootLayout.getPaddingBottom() + navBarHeight);

        ImageView backgroundImage = (ImageView) findViewById(R.id.intro_background_image);

        Glide.with(this)
                .load(R.drawable.intro_background)
                .centerCrop()
                .into(backgroundImage);

        ImageView logoImage = (ImageView) findViewById(R.id.intro_logo_image);

        Glide.with(this)
                .load(R.drawable.xabber_logo_large)
                .into(logoImage);

        ((TextView) findViewById(R.id.intro_faq_text))
                .setMovementMethod(LinkMovementMethod.getInstance());

        Button haveAccountButton = (Button) findViewById(R.id.intro_have_account_button);
        Button registerAccountButton = (Button) findViewById(R.id.intro_register_account_button);
        Button createXabberAccountButton = (Button) findViewById(R.id.intro_create_xabber_account_button);


        haveAccountButton.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
        registerAccountButton.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
        createXabberAccountButton.getBackground().setColorFilter(getResources().getColor(R.color.red_700), PorterDuff.Mode.MULTIPLY);

        haveAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(AccountAddActivity.createIntent(IntroActivity.this));
            }
        });

        findViewById(R.id.intro_register_account_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchQuery = getString(R.string.intro_web_search_register_xmpp);
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH );
                intent.putExtra(SearchManager.QUERY, searchQuery);
                startActivity(intent);
            }
        });
    }

    void setStatusBarTranslucent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AccountManager.getInstance().hasAccounts()) {
            finish();
            startActivity(ContactListActivity.createIntent(this));
            return;
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static Point getNavigationBarSize(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }

        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        }

        // navigation bar is not present
        return new Point();
    }

    public static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 15) {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {
                LogManager.exception(LOG_TAG, e);
            } catch (InvocationTargetException e) {
                LogManager.exception(LOG_TAG, e);
            } catch (NoSuchMethodException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }

        return size;
    }
}
