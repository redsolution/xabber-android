package com.xabber.android.ui.adapter;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;


import com.xabber.android.R;
import com.xabber.android.ui.fragment.TutorialFragment;

/**
 * Created by valery.miller on 14.09.17.
 */

public class TutorialAdapter extends FragmentPagerAdapter {
    private static int PAGE_NUMBERS = 5;

    public TutorialAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return TutorialFragment.newInstance(R.string.tutorial_title_1, R.string.tutorial_description_1, R.drawable.art_welcome);
            case 1:
                return TutorialFragment.newInstance(R.string.tutorial_title_2, R.string.tutorial_description_2, R.drawable.art_tutorial_1);
            case 2:
                return TutorialFragment.newInstance(R.string.tutorial_title_6, R.string.tutorial_description_6, R.drawable.art_tutorial_3);
            case 3:
                return TutorialFragment.newInstance(R.string.tutorial_title_8, R.string.tutorial_description_8, R.drawable.art_tutorial_4);
            case 4:
                return TutorialFragment.newInstance(R.string.tutorial_title_9, R.string.tutorial_description_9, R.drawable.art_tutorial_2);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return PAGE_NUMBERS;
    }


}
