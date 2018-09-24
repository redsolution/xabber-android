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
    private static int PAGE_NUMBERS = 9;

    public TutorialAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return TutorialFragment.newInstance(R.string.tutorial_title_1, R.string.tutorial_description_1, R.drawable.art_tutorial_1);
            case 1:
                return TutorialFragment.newInstance(R.string.tutorial_title_2, R.string.tutorial_description_2, R.drawable.art_tutorial_2);
            case 2:
                return TutorialFragment.newInstance(R.string.tutorial_title_3, R.string.tutorial_description_3, R.drawable.art_tutorial_3);
            case 3:
                return TutorialFragment.newInstance(R.string.tutorial_title_4, R.string.tutorial_description_4, R.drawable.art_tutorial_4);
            case 4:
                return TutorialFragment.newInstance(R.string.tutorial_title_5, R.string.tutorial_description_5, R.drawable.art_tutorial_1);
            case 5:
                return TutorialFragment.newInstance(R.string.tutorial_title_6, R.string.tutorial_description_6, R.drawable.art_tutorial_2);
            case 6:
                return TutorialFragment.newInstance(R.string.tutorial_title_7, R.string.tutorial_description_7, R.drawable.art_tutorial_3);
            case 7:
                return TutorialFragment.newInstance(R.string.tutorial_title_8, R.string.tutorial_description_8, R.drawable.art_tutorial_4);
            case 8:
                return TutorialFragment.newInstance(R.string.tutorial_title_9, R.string.tutorial_description_9, R.drawable.art_tutorial_1);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return PAGE_NUMBERS;
    }


}
