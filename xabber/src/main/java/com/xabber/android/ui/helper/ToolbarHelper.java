package com.xabber.android.ui.helper;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.activity.ManagedActivity;

public class ToolbarHelper {

    public static int NAVIGATION_ICON = R.drawable.ic_arrow_left_grey_24dp;

    public static Toolbar setUpDefaultToolbar(@NonNull final ManagedActivity activity, @Nullable CharSequence title,
                                              @DrawableRes int navigationIconResource) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar_default);
        toolbar.setTitle(title);
        toolbar.setNavigationIcon(navigationIconResource);
        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(activity));
        return toolbar;
    }

    public static Toolbar setUpDefaultToolbar(@NonNull final ManagedActivity activity) {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            NAVIGATION_ICON = R.drawable.ic_arrow_left_grey_24dp;
        else NAVIGATION_ICON = R.drawable.ic_arrow_left_white_24dp;
        return setUpDefaultToolbar(activity, activity.getTitle(), NAVIGATION_ICON);
    }

    public static Toolbar setUpDefaultToolbar(@NonNull final ManagedActivity activity, @Nullable CharSequence title){
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            NAVIGATION_ICON = R.drawable.ic_arrow_left_grey_24dp;
        else NAVIGATION_ICON = R.drawable.ic_arrow_left_white_24dp;
        return setUpDefaultToolbar(activity, title, NAVIGATION_ICON);
    }

}
