package com.xabber.android.ui.helper;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.ui.activity.ManagedActivity;

public class ToolbarHelper {


    public static final int NAVIGATION_ICON = R.drawable.ic_arrow_left_white_24dp;

    public static Toolbar setUpDefaultToolbar(@NonNull final ManagedActivity activity, @Nullable CharSequence title,
                                              @DrawableRes int navigationIconResource) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_default);
        toolbar.setTitle(title);
        toolbar.setNavigationIcon(navigationIconResource);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(activity);
            }
        });
        return toolbar;
    }

    public static Toolbar setUpDefaultToolbar(@NonNull final ManagedActivity activity) {
        return setUpDefaultToolbar(activity, activity.getTitle(), NAVIGATION_ICON);
    }

    public static Toolbar setUpDefaultToolbar(@NonNull final ManagedActivity activity, @Nullable CharSequence title) {
        return setUpDefaultToolbar(activity, title, NAVIGATION_ICON);
    }
}
