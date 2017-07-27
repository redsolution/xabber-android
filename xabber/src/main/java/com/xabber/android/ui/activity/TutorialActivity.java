package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.github.paolorotolo.appintro.AppIntroBase;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.xabber.android.R;

/**
 * Created by valery.miller on 14.07.17.
 */

public class TutorialActivity extends AppIntroBase {

    private ImageButton imbDone;
    private ImageButton imbNext;
    private RelativeLayout rlLogin;
    private RelativeLayout rlRegister;

    public static Intent createIntent(Context context) {
        return new Intent(context, TutorialActivity.class);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_tutorial;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStatusBarTranslucent();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        ImageView backgroundImage = (ImageView) findViewById(R.id.intro_background_image);

        rlLogin = (RelativeLayout) findViewById(R.id.rlLogin);
        rlLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(XabberLoginActivity.createIntent(TutorialActivity.this));
            }
        });

        rlRegister = (RelativeLayout) findViewById(R.id.rlRegister);
        rlRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = XabberLoginActivity.createIntent(TutorialActivity.this);
                intent.putExtra(XabberLoginActivity.CURRENT_FRAGMENT, XabberLoginActivity.FRAGMENT_SIGNUP);
                startActivity(intent);
            }
        });

        Glide.with(this)
                .load(R.drawable.intro_background)
                .centerCrop()
                .into(backgroundImage);

        addSlide(AppIntroFragment.newInstance(
                "Lorem ipsum dolor sit amet",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi risus purus, lobortis at pulvinar eu, venenatis et erat.",
                R.drawable.xabber_logo_large,
                ContextCompat.getColor(this, R.color.transparent)
        ));

        addSlide(AppIntroFragment.newInstance(
                "Consectetur adipiscing elit",
                "Suspendisse efficitur consectetur leo eu malesuada. Ut lacinia nisl vel mattis faucibus.",
                R.drawable.xabber_logo_large,
                ContextCompat.getColor(this, R.color.transparent)
        ));

        addSlide(AppIntroFragment.newInstance(
                "Morbi risus purus",
                "Pellentesque ante tortor, ultrices nec ornare in, ultricies nec ipsum. Donec et vehicula sapien.",
                R.drawable.xabber_logo_large,
                ContextCompat.getColor(this, R.color.transparent)
        ));

        addSlide(AppIntroFragment.newInstance(
                "Lobortis at pulvinar eu",
                "Ut porttitor magna sit amet mi maximus, at cursus est luctus. Aliquam fermentum gravida neque, in ultrices dui pellentesque vitae.",
                R.drawable.xabber_logo_large,
                ContextCompat.getColor(this, R.color.transparent)
        ));

        addSlide(AppIntroFragment.newInstance(
                "Venenatis et erat",
                "Nunc dictum velit eu lacus sodales, eget efficitur lectus iaculis. Cras nec leo magna.",
                R.drawable.xabber_logo_large,
                ContextCompat.getColor(this, R.color.transparent)
        ));

        showSkipButton(false);
        setIndicatorColor(ContextCompat.getColor(this, R.color.red_700), DEFAULT_COLOR);

        imbDone = (ImageButton) findViewById(R.id.done);
        imbNext = (ImageButton) findViewById(R.id.next);
        imbDone.setVisibility(View.GONE);
        imbNext.setVisibility(View.GONE);
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        imbDone.setVisibility(View.GONE);
        imbNext.setVisibility(View.GONE);
    }

    void setStatusBarTranslucent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
