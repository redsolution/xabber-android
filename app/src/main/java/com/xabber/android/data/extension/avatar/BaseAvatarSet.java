/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.extension.avatar;

import android.content.res.TypedArray;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLowMemoryListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Set of default avatars.
 * <p/>
 * Calculation takes not much time, so we don't use locks.
 *
 * @author alexander.ivanov
 */
public class BaseAvatarSet implements OnLowMemoryListener {
    /**
     * Default resources.
     */
    private final int[] avatarIconsResources;

    private final int[] colors;

    /**
     * Map with resource ids for specified uses.
     */
    private final Map<String, DefaultAvatar> resources;

    public static class DefaultAvatar {
        public DefaultAvatar(int iconResource, int backgroundColor) {
            this.iconResource = iconResource;
            this.backgroundColor = backgroundColor;
        }

        public int getIconResource() {
            return iconResource;
        }

        public int getBackgroundColor() {
            return backgroundColor;
        }

        private int iconResource;
        private int backgroundColor;
    }


    public BaseAvatarSet(Application application, int avatarIconsArrayId, int avatarColorsArrayId) {
        TypedArray defaultAvatars = application.getResources().obtainTypedArray(avatarIconsArrayId);
        avatarIconsResources = new int[defaultAvatars.length()];
        for (int index = 0; index < defaultAvatars.length(); index++) {
            avatarIconsResources[index] = defaultAvatars.getResourceId(index, -1);
        }
        defaultAvatars.recycle();

        colors = application.getResources().getIntArray(avatarColorsArrayId);

        resources = new HashMap<>();
    }

    /**
     * Calculate avatar index for specified user.
     *
     * @param user
     * @return
     */
    protected int getIndex(String user) {
        return user.hashCode();
    }

    /**
     * Get drawable with default avatar of user.
     *
     * @param user
     * @return
     */
    public DefaultAvatar getResourceId(String user) {
        DefaultAvatar avatar = resources.get(user);
        if (avatar == null) {
            avatar = getElement(getIndex(user));
            resources.put(user, avatar);
        }
        return avatar;
    }

    /**
     * Gets element from array by index.
     *
     * @param index
     * @param array
     * @return Always return element even if array's length is less then index.
     */
    private DefaultAvatar getElement(int index) {
        int uniqueCombinationsNumber = avatarIconsResources.length * colors.length;

        index = index % uniqueCombinationsNumber;

        if (index < 0) {
            index += uniqueCombinationsNumber;
        }

        return new DefaultAvatar(avatarIconsResources[index / colors.length], colors[index % colors.length]);
    }

    @Override
    public void onLowMemory() {
        resources.clear();
    }
}
